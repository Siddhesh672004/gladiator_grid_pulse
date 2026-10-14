package com.examly.springapp.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.PaymentOrderResponse;
import com.examly.springapp.exceptions.GridPulseException;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.SessionStatus;
import com.examly.springapp.repository.RaceSessionRepo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// the one method gemini may call from the chatbot: finds the session, then uses the same services as the booking page
// prototype = a new object per try of a chat message, it keeps the payment order of that try
@Component
@Scope("prototype")
@RequiredArgsConstructor
public class BookingTool {

    private static final String TERMS = "You pay when you book. If GridPulse rejects the booking, or you cancel it "
            + "while it is still Pending, the full amount is refunded. An approved booking can be cancelled too, "
            + "but then the amount is not refunded.";

    // the day and start time gemini asked for, start null = any
    private record When(LocalDate day, LocalTime start) {
    }

    // one thing about the car gemini may still have to ask: when it is missing + the answer for gemini
    private record CarCheck(BooleanSupplier missing, Supplier<String> answer) {
    }

    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingService raceBookingService;
    private final PaymentService paymentService;
    private final UserService userService;

    // filled only when a payment was started, ChatServiceImpl sends both to the chat window
    @Getter
    private RaceBookingRequest booking;
    @Getter
    private PaymentOrderResponse paymentOrder;

    // whatever this returns goes back to gemini, which turns it into the reply for the driver
    @Tool(description = "Book a race session for the logged-in driver. Call it with confirmed=false first: it finds "
            + "the session, runs every booking rule and returns a summary or the problem. Call it with "
            + "confirmed=true only after the driver said yes to the summary and the terms: it starts the payment.")
    public String bookSession(
            @ToolParam(description = "date of the session, yyyy-MM-dd") String date,
            @ToolParam(description = "start time, 24 hour HH:mm", required = false) String time,
            @ToolParam(description = "track name, only when the driver said one", required = false) String trackName,
            @ToolParam(description = "GT3, Formula, Karting, Rally or Stock", required = false) String carCategory,
            @ToolParam(description = "car number from 1 to 999", required = false) Integer carNumber,
            @ToolParam(description = "true only after the driver confirmed the summary and accepted the terms")
            boolean confirmed) {

        // only a logged-in driver can book
        return isDriver()
                ? bookForDriver(date, time, trackName, carCategory, carNumber, confirmed)
                : "Not possible: the user is not logged in as a driver. Ask them to log in first.";
    }

    private String bookForDriver(String date, String time, String trackName, String carCategory, Integer carNumber,
            boolean confirmed) {
        // a date or time that can't be read goes back to gemini with the right format
        return readWhen(date, time)
                .map(when -> pickSession(when, trackName, carCategory, carNumber, confirmed))
                .orElse("The date must look like 2026-10-09 and the time like 16:00.");
    }

    // empty = the date or the time could not be read
    private Optional<When> readWhen(String date, String time) {
        try {
            return Optional.of(new When(LocalDate.parse(date), isBlank(time) ? null : LocalTime.parse(time)));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private String pickSession(When when, String trackName, String carCategory, Integer carNumber, boolean confirmed) {
        List<RaceSession> open = openSessions();
        List<RaceSession> matches = findMatches(open, when.day(), when.start(), trackName);
        // none: offer other sessions, one: go on with it, more: the driver has to pick
        return switch (matches.size()) {
            case 0 -> "No open session matches that. Sessions the driver can book instead:\n"
                    + describe(sameDayOrAll(open, when.day()));
            case 1 -> bookWithCar(matches.get(0), carCategory, carNumber, confirmed);
            default -> "More than one session matches, ask the driver which one:\n" + describe(matches);
        };
    }

    // the sessions of that day, or every open one when that day has none
    private List<RaceSession> sameDayOrAll(List<RaceSession> open, LocalDate day) {
        List<RaceSession> sameDay = findMatches(open, day, null, null);
        return sameDay.isEmpty() ? open : sameDay;
    }

    private String bookWithCar(RaceSession session, String carCategory, Integer carNumber, boolean confirmed) {
        // a missing car detail is asked first, the booking rules only after that
        return missingDetail(session, carCategory, carNumber)
                .orElseGet(() -> book(session, carCategory, carNumber, confirmed));
    }

    private String book(RaceSession session, String carCategory, Integer carNumber, boolean confirmed) {
        RaceBookingRequest request = new RaceBookingRequest(session.getSessionId(), carNumber, carCategory,
                "Booked with the GridPulse assistant", confirmed);
        try {
            // not confirmed yet = only the checks and the summary
            return confirmed ? startPayment(session, request) : summary(session, request, carCategory, carNumber);
        } catch (GridPulseException e) {
            return "Not possible: " + e.getMessage();
        }
    }

    // the rules of the booking page: profile complete, session open, seat left, car number free ...
    private String summary(RaceSession session, RaceBookingRequest request, String carCategory, Integer carNumber) {
        raceBookingService.checkCanBook(request);
        return "All checks passed. Show the driver this summary and the terms word for word, then ask "
                + "them to confirm.\n" + describe(session) + "\nCar: " + carCategory + " #" + carNumber
                + "\nTerms: " + TERMS;
    }

    // same call as "Pay & Register": the rules once more, then a Razorpay order
    private String startPayment(RaceSession session, RaceBookingRequest request) {
        paymentOrder = paymentService.createOrder(request);
        booking = request;
        return "The payment window is opening now for " + price(session)
                + ". The booking is made as soon as the payment goes through.";
    }

    // the open sessions of that day; start / trackName empty = any
    private List<RaceSession> findMatches(List<RaceSession> open, LocalDate day, LocalTime start, String trackName) {
        return open.stream()
                .filter(session -> isMatch(session, day, start, trackName))
                .toList();
    }

    private boolean isMatch(RaceSession session, LocalDate day, LocalTime start, String trackName) {
        boolean sameDay = session.getSessionDate().equals(day);
        boolean sameStart = start == null || session.getStartTime().equals(start);
        boolean sameTrack = isBlank(trackName) || trackNameMatches(session, trackName);
        return sameDay && sameStart && sameTrack;
    }

    // what gemini still has to ask the driver about the car, empty = nothing
    private Optional<String> missingDetail(RaceSession session, String carCategory, Integer carNumber) {
        // category, then number, then its range: the first one that is missing is the answer
        return Stream.of(
                        new CarCheck(() -> isBlank(carCategory), () -> "Session found: " + describe(session)
                                + "\nStill needed from the driver: car category (GT3, Formula, Karting, Rally "
                                + "or Stock)."),
                        new CarCheck(() -> carNumber == null, () -> "Session found: " + describe(session)
                                + "\nStill needed from the driver: car number (1 to 999)."),
                        new CarCheck(() -> carNumber < 1 || carNumber > 999,
                                () -> "Not possible: the car number must be between 1 and 999."))
                .filter(check -> check.missing().getAsBoolean())
                .map(check -> check.answer().get())
                .findFirst();
    }

    private boolean isDriver() {
        try {
            return userService.getLoggedInUser().getUserRole() == Role.USER;
        } catch (AccessDeniedException e) {
            return false;
        }
    }

    // what the booking page would offer: still bookable, on an active track, a seat left
    private List<RaceSession> openSessions() {
        return raceSessionRepo.findByStatusAndSessionDateGreaterThanEqualOrderBySessionDateAscStartTimeAsc(
                        SessionStatus.SCHEDULED, RaceSession.today()).stream()
                .filter(RaceSession::isBookable)
                .filter(session -> session.getTrack().isActive())
                .filter(this::hasSeat)
                .toList();
    }

    // seats = approved cars only
    private boolean hasSeat(RaceSession session) {
        return raceBookingService.countApprovedCars(session.getSessionId()) < session.getMaxCars();
    }

    private boolean trackNameMatches(RaceSession session, String trackName) {
        return session.getTrack().getTrackName().toLowerCase().contains(trackName.trim().toLowerCase());
    }

    // at most 8 lines, so the answer to gemini stays short
    private String describe(List<RaceSession> sessions) {
        return sessions.isEmpty()
                ? "none at the moment"
                : sessions.stream()
                        .limit(8)
                        .map(this::describe)
                        .collect(Collectors.joining("\n"));
    }

    private String describe(RaceSession session) {
        String kind = session.isPractice() ? "Practice" : "Competition qualifying";
        return kind + " at " + session.getTrack().getTrackName() + " on " + session.getSessionDate() + " "
                + session.getStartTime() + "-" + session.getEndTime() + ", " + price(session);
    }

    private String price(RaceSession session) {
        return "Rs." + Math.round(session.getPrice());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
