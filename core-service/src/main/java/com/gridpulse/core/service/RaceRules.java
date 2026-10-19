package com.gridpulse.core.service;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Comparator.reverseOrder;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.gridpulse.core.dto.CompetitionRule;
import com.gridpulse.core.exceptions.BookingException;
import com.gridpulse.core.model.RaceBooking;

// car rules shared by practice and competition control: NOT_STARTED -> RACING -> RETIRED or FINISHED
// the helpers only change the RaceBooking, the calling service saves it
public final class RaceRules {

    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String RACING = "RACING";
    public static final String RETIRED = "RETIRED";
    public static final String FINISHED = "FINISHED";

    // retired last, more laps first, earlier last lap first, better grid place (competition only), lower car number
    private static final Comparator<RaceBooking> STANDINGS_ORDER = Comparator
            .comparing(RaceRules::isRetired)
            .thenComparing(RaceRules::lapsOf, reverseOrder())
            .thenComparing(RaceBooking::getLastLapAt, nullsLast(naturalOrder()))
            .thenComparing(RaceBooking::getGridPosition, nullsLast(naturalOrder()))
            .thenComparing(RaceBooking::getCarNumber, nullsLast(naturalOrder()))
            .thenComparing(RaceBooking::getRaceBookingId, nullsLast(naturalOrder()));

    private RaceRules() {
    }

    // the only place with the rule numbers. a competition keeps its own copy, so a change here affects new ones only
    public static CompetitionRule ruleFor(String trackCategory) {
        return switch (trackCategory) {
            // numbers = qualifying laps, race laps, max cars
            case "Circuit" -> new CompetitionRule(trackCategory, 3, 15, 20);
            case "Karting" -> new CompetitionRule(trackCategory, 5, 12, 12);
            case "Rally" -> new CompetitionRule(trackCategory, 1, 3, 10);
            case "Drag Strip" -> new CompetitionRule(trackCategory, 1, 1, 8);
            default -> throw new BookingException("No competition rule for track category '" + trackCategory + "'");
        };
    }

    public static List<RaceBooking> rank(List<RaceBooking> bookings) {
        return bookings.stream()
                .sorted(STANDINGS_ORDER)
                .toList();
    }

    public static void start(RaceBooking booking) {
        String state = stateOf(booking);
        // a car can be started only once
        Optional.of(state)
                .filter(NOT_STARTED::equals)
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber() + " is " + state
                        + " - only a NOT_STARTED car can be started"));
        booking.setRaceState(RACING);
    }

    public static void addLap(RaceBooking booking) {
        String state = stateOf(booking);
        // a lap needs a started car
        Optional.of(state)
                .filter(current -> !NOT_STARTED.equals(current))
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber()
                        + " has not been started - press Start before adding a lap"));
        // retired or finished cars take no more laps
        Optional.of(state)
                .filter(RACING::equals)
                .orElseThrow(() -> new BookingException("Car #" + booking.getCarNumber() + " is " + state
                        + " - laps can no longer be added"));
        booking.setLapCount(lapsOf(booking) + 1);
        booking.setLastLapAt(Instant.now());
    }

    public static void retire(RaceBooking booking) {
        // retiring twice changes nothing, only a car on track can retire
        switch (stateOf(booking)) {
            case RETIRED -> {
            }
            case NOT_STARTED -> throw new BookingException("Car #" + booking.getCarNumber()
                    + " has not been started - only a RACING car can be retired");
            case FINISHED -> throw new BookingException("Car #" + booking.getCarNumber()
                    + " has already FINISHED - a finished car cannot be retired");
            default -> {
                booking.setRaceState(RETIRED);
                booking.setFinalPosition(null);
            }
        }
    }

    public static void reset(RaceBooking booking) {
        booking.setLapCount(0);
        booking.setLastLapAt(null);
        booking.setRaceState(NOT_STARTED);
        booking.setFinalPosition(null);
    }

    // the list must already be ranked. every car that did not retire (also one that never started) gets P1, P2 ...
    // returns how many cars got a position
    public static int classify(List<RaceBooking> rankedBookings) {
        // retired cars on one side, the rest on the other, both keep the ranked order
        Map<Boolean, List<RaceBooking>> retired = rankedBookings.stream()
                .collect(Collectors.partitioningBy(RaceRules::isRetired));
        // retired cars get no place
        retired.get(true).forEach(car -> car.setFinalPosition(null));
        // the rest P1, P2 .. in ranked order
        List<RaceBooking> classified = retired.get(false);
        IntStream.range(0, classified.size())
                .forEach(i -> finish(classified.get(i), i + 1));
        return classified.size();
    }

    private static void finish(RaceBooking booking, int position) {
        booking.setRaceState(FINISHED);
        booking.setFinalPosition(position);
    }

    public static String stateOf(RaceBooking booking) {
        return Objects.requireNonNullElse(booking.getRaceState(), NOT_STARTED);
    }

    public static int lapsOf(RaceBooking booking) {
        return Objects.requireNonNullElse(booking.getLapCount(), 0);
    }

    private static boolean isRetired(RaceBooking booking) {
        return RETIRED.equals(booking.getRaceState());
    }
}
