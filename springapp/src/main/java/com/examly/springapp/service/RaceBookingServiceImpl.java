package com.examly.springapp.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.request.InternalBookingRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.BookingQuote;
import com.examly.springapp.dto.response.CarCategoryResponse;
import com.examly.springapp.dto.response.InternalBookingResult;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.exceptions.BookingException;
import com.examly.springapp.exceptions.ConflictException;
import com.examly.springapp.exceptions.DuplicateCarNumberException;
import com.examly.springapp.exceptions.ResourceNotFoundException;
import com.examly.springapp.mapper.RaceBookingMapper;
import com.examly.springapp.model.BookingStatus;
import com.examly.springapp.model.Competition;
import com.examly.springapp.model.PaymentStatus;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.RaceBookingRepo;
import com.examly.springapp.repository.RaceSessionRepo;
import com.examly.springapp.repository.SessionCars;
import com.gridpulse.common.event.BookingDecided;
import com.gridpulse.common.event.BookingPromoted;
import com.gridpulse.common.event.BookingRefundRequested;
import com.gridpulse.common.event.BookingWaitlisted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// a booking only comes from a paid order: payment-service -> /internal/bookings (createPaidBooking)
// a reject / cancel asks payment-service for the money back (BookingRefundRequested), its answers come back as
// PaymentStatusChanged (applyPaymentStatus)
// only Approved bookings take a seat, any number can wait as Pending; a full session puts new and waiting bookings
// on its waitlist (Waitlisted), a seat that opens moves the oldest one up
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceBookingServiceImpl implements RaceBookingService {

    private static final List<String> STATUSES = List.of(BookingStatus.PENDING, BookingStatus.APPROVED,
            BookingStatus.REJECTED, BookingStatus.WAITLISTED);
    private static final List<String> CAR_CATEGORIES = List.of("GT3", "Formula", "Karting", "Rally", "Stock");
    private static final String CAR_PICTURE_PREFIX = "gridpulse_car_";
    // a rejected booking holds no car number and no time slot
    private static final List<String> HOLDS_A_PLACE = List.of(BookingStatus.PENDING, BookingStatus.APPROVED,
            BookingStatus.WAITLISTED);
    // what payment-service's events may change on a booking
    private static final Set<PaymentStatus> COPIED_PAYMENT_STATUSES = EnumSet.of(PaymentStatus.REFUND_PENDING,
            PaymentStatus.REFUNDED, PaymentStatus.REFUND_FAILED);

    // one rule of a session: when it is broken + the message for it
    private record SessionRule(Predicate<RaceSession> broken, Function<RaceSession, String> message) {
    }

    // a driver books only a session that waits for its start, on an active track
    private static final List<SessionRule> OPEN_FOR_BOOKING = List.of(
            new SessionRule(RaceSession::isRacing,
                    session -> "Race places come from qualifying - book the qualifying session instead"),
            new SessionRule(session -> !session.isScheduled(),
                    session -> "This session is " + session.getStatus() + " - booking is closed"),
            new SessionRule(RaceSession::hasStarted,
                    session -> "Booking is closed: this session has already started"),
            new SessionRule(session -> !session.getTrack().isActive(),
                    session -> "This track is not available right now"));

    // approve / reject only before the session starts
    private static final List<SessionRule> BOOKINGS_CAN_CHANGE = List.of(
            new SessionRule(RaceSession::isRacing,
                    session -> "Race places come from qualifying and cannot be changed"),
            new SessionRule(session -> !session.isScheduled(),
                    session -> "This session is " + session.getStatus() + " - its bookings can no longer be changed"));

    private final RaceBookingRepo raceBookingRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final UserService userService;
    private final RaceBookingMapper raceBookingMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CloudinaryService cloudinaryService;
    private final TicketService ticketService;

    @Override
    public List<RaceBookingResponse> getAllRaceBookings() {
        return toResponses(raceBookingRepo.findAll());
    }

    @Override
    public PageResponse<RaceBookingResponse> getBookingPage(String search, String status, int page, int size) {
        String wanted = Optional.ofNullable(Paging.filter(status))
                .map(text -> allowedValue(STATUSES, text, "Status"))
                .orElse(null);
        return Paging.toResponse(raceBookingRepo.searchPage(wanted, Paging.likePattern(search),
                Paging.pageable(page, size, Sort.by(Sort.Direction.DESC, "raceBookingId"))),
                this::toResponse);
    }

    @Override
    public List<CarCategoryResponse> getCarCategories() {
        return CAR_CATEGORIES.stream()
                .map(name -> new CarCategoryResponse(name,
                        cloudinaryService.fixedImageUrl(CAR_PICTURE_PREFIX + name.toLowerCase())))
                .toList();
    }

    @Override
    public List<RaceBookingResponse> getRaceBookingsByUserId(int userId) {
        userService.checkOwnerOrAdmin(userId);
        return toResponses(raceBookingRepo.findByUserUserId(userId));
    }

    @Override
    public RaceBookingResponse getRaceBookingById(Long bookingId) {
        RaceBooking booking = findBooking(bookingId);
        userService.checkOwnerOrAdmin(booking.getUser().getUserId());
        return toResponse(booking);
    }

    // the rules before the driver pays (quote, the chat). only reads, no transaction
    // a full session is no error: the booking would join the waitlist
    @Override
    public RaceSession checkCanBook(RaceBookingRequest request) {
        User driver = userService.getLoggedInUser();
        RaceSession session = findSessionToBook(request.sessionId());
        checkBookingRules(newBooking(request, driver, session));
        return session;
    }

    @Override
    public BookingQuote quote(RaceBookingRequest request) {
        RaceSession session = checkCanBook(request);
        User driver = userService.getLoggedInUser();
        RaceTrack track = session.getTrack();
        String competitionName = Optional.ofNullable(session.getCompetition())
                .map(Competition::getCompetitionName)
                .orElse(null);
        return new BookingQuote(session.getSessionId(), session.getPrice(), description(session), isFull(session),
                driver.getUserId(), driver.getUsername(), driver.getEmail(), driver.getMobileNumber(),
                track.getTrackName(), track.getLocation(), session.getSessionDate(), session.getStartTime(),
                session.getEndTime(), session.getSessionType(), session.getPhase(), competitionName);
    }

    // the order of the payment is the key: the same order again (payment-service retrying) gets the same booking
    // READ_COMMITTED: after waiting for the session lock the rules must see what the other request just saved
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InternalBookingResult createPaidBooking(InternalBookingRequest request) {
        User driver = userService.getLoggedInUser();
        RaceSession session = lockSession(request.booking().sessionId());
        // looked up under the lock: a retry that raced the first call finds its booking here
        Optional<RaceBooking> existing = raceBookingRepo.findByPaymentOrderId(request.razorpayOrderId());
        if (existing.isPresent()) {
            RaceBooking booking = existing.get();
            userService.checkOwnerOrAdmin(booking.getUser().getUserId());
            return new InternalBookingResult(toResponse(booking), false);
        }
        RaceBooking booking = newBooking(request.booking(), driver, session);
        checkBookingRules(booking);
        booking.setPaymentOrderId(request.razorpayOrderId());
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setTotalPrice(request.amount());
        // full: paid, but it waits for a seat
        boolean full = isFull(session);
        booking.setStatus(full ? BookingStatus.WAITLISTED : BookingStatus.PENDING);
        booking.setTermsAcceptedAt(Instant.now());
        RaceBooking saved = raceBookingRepo.save(booking);
        Optional.of(saved)
                .filter(waiting -> full)
                .ifPresent(waiting -> eventPublisher.publishEvent(waitlistedEvent(waiting)));
        log.info("Booking {} created for order {}: car #{} in session {}, paid {}, {}", saved.getRaceBookingId(),
                request.razorpayOrderId(), saved.getCarNumber(), session.getSessionId(), saved.getTotalPrice(),
                saved.getStatus());
        return new InternalBookingResult(toResponse(saved), true);
    }

    @Override
    public Optional<RaceBookingResponse> findByOrder(String razorpayOrderId) {
        return raceBookingRepo.findByPaymentOrderId(razorpayOrderId).map(this::toResponse);
    }

    // what the razorpay window shows: "Buddh Circuit · 2026-10-20 10:00-11:00"
    private String description(RaceSession session) {
        return session.getTrack().getTrackName() + " \u00b7 " + session.getSessionDate() + " "
                + session.getStartTime() + "-" + session.getEndTime();
    }

    // the refund statuses only: PAID is set here when the booking is made, a late PAID must not undo a refund
    @Override
    @Transactional
    public void applyPaymentStatus(String razorpayOrderId, String status) {
        Arrays.stream(PaymentStatus.values())
                .filter(known -> known.name().equals(status))
                .filter(COPIED_PAYMENT_STATUSES::contains)
                .findFirst()
                .ifPresent(copied -> raceBookingRepo.findByPaymentOrderId(razorpayOrderId)
                        // REFUNDED is final: retry topics can bring a late REFUND_PENDING / REFUND_FAILED after it
                        .filter(booking -> booking.getPaymentStatus() != PaymentStatus.REFUNDED)
                        .ifPresent(booking -> booking.setPaymentStatus(copied)));
    }

    // READ_COMMITTED: same reason as createPaidBooking, the seat count must be fresh after
    // waiting for the session lock
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RaceBookingResponse updateRaceBooking(Long bookingId, BookingStatusRequest request) {
        RaceBooking booking = findBooking(bookingId);
        checkBookingCanChange(booking.getSession());

        String oldStatus = booking.getStatus();
        // no status sent = the status stays
        String newStatus = Optional.ofNullable(request.status())
                .map(status -> allowedValue(STATUSES, status, "Status"))
                .orElse(oldStatus);
        String note = cleanNote(request.adminNote());

        return newStatus.equals(oldStatus)
                ? saveNote(booking, note)
                : changeStatus(booking, oldStatus, newStatus, note);
    }

    // only the note was sent
    private RaceBookingResponse saveNote(RaceBooking booking, String note) {
        Optional.ofNullable(note)
                .ifPresent(booking::setAdminNote);
        return raceBookingMapper.toResponse(raceBookingRepo.save(booking));
    }

    // refund: driver cancelling Pending / Waitlisted, or admin removing. driver cancelling
    // Approved gets none (the terms say so), its seat goes to the waitlist
    // the payment row is never deleted
    @Override
    @Transactional
    public void deleteRaceBooking(Long bookingId) {
        RaceBooking booking = findBooking(bookingId);
        userService.checkOwnerOrAdmin(booking.getUser().getUserId());
        checkCanBeDeleted(booking);

        boolean callerIsAdmin = userService.getLoggedInUser().getUserRole().isAdmin();
        boolean pending = BookingStatus.PENDING.equals(booking.getStatus());
        boolean waitlisted = BookingStatus.WAITLISTED.equals(booking.getStatus());
        boolean approved = BookingStatus.APPROVED.equals(booking.getStatus());
        // the first case that fits gives the refund reason, no case = no refund
        Stream.of(Map.entry(callerIsAdmin, "Booking removed by GridPulse"),
                        Map.entry(pending, "Cancelled by the driver before approval"),
                        Map.entry(waitlisted, "Cancelled by the driver while on the waitlist"))
                .filter(Map.Entry::getKey)
                .map(Map.Entry::getValue)
                .findFirst()
                .ifPresent(reason -> refund(booking, reason));
        raceBookingRepo.delete(booking);
        log.info("Booking {} deleted", bookingId);
        Optional.of(booking)
                .filter(freed -> approved)
                .ifPresent(freed -> promoteFromWaitlist(freed.getSession().getSessionId()));
    }

    @Override
    @Transactional
    public void rejectWaitingBookings(Long sessionId, String note) {
        rejectBookings(waitingBookings(sessionId).toList(), note);
    }

    @Override
    @Transactional
    public void rejectAllBookings(Long sessionId, String note) {
        // the waiting ones first, then the approved ones
        List<RaceBooking> bookings = Stream.concat(waitingBookings(sessionId),
                        raceBookingRepo.findBySessionSessionIdAndStatus(sessionId, BookingStatus.APPROVED).stream())
                .toList();
        rejectBookings(bookings, note);
    }

    // READ_COMMITTED: the seat count must be fresh after waiting for the session lock
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void promoteFromWaitlist(Long sessionId) {
        RaceSession session = lockSession(sessionId);
        long free = Math.max(0, session.getMaxCars() - countApprovedCars(sessionId));
        Optional.of(session)
                .filter(RaceSession::isScheduled)
                .ifPresent(open -> raceBookingRepo.findBySessionSessionIdAndStatusOrderByRaceBookingIdAsc(sessionId,
                                BookingStatus.WAITLISTED).stream()
                        .limit(free)
                        .forEach(this::promote));
    }

    @Override
    public long countApprovedCars(Long sessionId) {
        return raceBookingRepo.countBySessionSessionIdAndStatus(sessionId, BookingStatus.APPROVED);
    }

    @Override
    public Map<Long, Long> countApprovedCars(Collection<Long> sessionIds) {
        // an empty page needs no query
        return sessionIds.isEmpty() ? Map.of()
                : raceBookingRepo.countApprovedPerSession(sessionIds).stream()
                        .collect(Collectors.toMap(SessionCars::getSessionId, SessionCars::getCars));
    }

    @Override
    public List<RaceBooking> findApprovedCars(Long sessionId) {
        return raceBookingRepo.findBySessionSessionIdAndStatus(sessionId, BookingStatus.APPROVED);
    }

    // checks first, then save -> refund -> email -> the approval that fills the last seat puts everyone still
    // waiting on the waitlist, a seat given back moves the waitlist up
    private RaceBookingResponse changeStatus(RaceBooking booking, String oldStatus, String newStatus, String note) {
        boolean approving = BookingStatus.APPROVED.equals(newStatus);
        boolean rejecting = BookingStatus.REJECTED.equals(newStatus);
        boolean wasRejected = BookingStatus.REJECTED.equals(oldStatus);
        boolean seatFreed = BookingStatus.APPROVED.equals(oldStatus);
        checkWaitlistChange(oldStatus, newStatus);

        // one at a time per session: two admins can't give away the last seat or one
        // car number twice
        RaceSession session = (approving || wasRejected)
                ? lockSession(booking.getSession().getSessionId())
                : booking.getSession();
        // approving needs a free seat and money that was not sent back yet
        Optional.of(session)
                .filter(locked -> approving)
                .ifPresent(locked -> {
                    checkSeatFree(locked);
                    checkMoneyStillHere(booking);
                });
        // a rejected booking gave its car number back, somebody else may have taken it
        Optional.of(booking)
                .filter(again -> wasRejected)
                .ifPresent(this::checkCarNumberStillFree);
        Optional.of(booking)
                .filter(rejected -> rejecting)
                .ifPresent(rejected -> checkReason(note));

        booking.setStatus(newStatus);
        booking.setAdminNote(note);
        RaceBooking saved = raceBookingRepo.save(booking);
        // a rejected driver gets the money back
        Optional.of(booking)
                .filter(rejected -> rejecting)
                .ifPresent(rejected -> refund(rejected, "Booking rejected: " + note));

        log.info("Booking {} status {} -> {}", booking.getRaceBookingId(), oldStatus, newStatus);
        emailIfDecided(saved);
        // the approval that fills the last seat: the drivers still waiting go on the waitlist
        Optional.of(session)
                .filter(locked -> approving)
                .ifPresent(this::waitlistPendingIfFull);
        Optional.of(session)
                .filter(freed -> seatFreed)
                .ifPresent(freed -> promoteFromWaitlist(freed.getSessionId()));
        return toResponse(saved);
    }

    private void checkBookingRules(RaceBooking booking) {
        RaceSession session = booking.getSession();
        checkProfileComplete(booking.getUser());
        checkOpenForBooking(session);
        checkDriverIsFree(booking.getUser(), session);
        checkCarNumberFree(booking);
    }

    private RaceBooking newBooking(RaceBookingRequest request, User driver, RaceSession session) {
        RaceBooking booking = raceBookingMapper.toEntity(request, driver, session);
        booking.setCarCategory(allowedValue(CAR_CATEGORIES, booking.getCarCategory(), "Car category"));
        return booking;
    }

    private RaceSession findSessionToBook(Long sessionId) {
        return raceSessionRepo.findById(sessionId)
                .orElseThrow(() -> new BookingException("Session with ID: " + sessionId + " not found."));
    }

    // the other request waits here until this transaction is over
    private RaceSession lockSession(Long sessionId) {
        return raceSessionRepo.findWithLockBySessionId(sessionId)
                .orElseThrow(() -> new BookingException("Session with ID: " + sessionId + " not found."));
    }

    private void checkProfileComplete(User driver) {
        Optional.of(driver)
                .filter(User::isProfileComplete)
                .orElseThrow(() -> new BookingException("Please complete your profile before booking a race"));
    }

    // one driver, one session at a time on any track. back to back (13:30 end,
    // 13:30 start) is fine
    private void checkDriverIsFree(User driver, RaceSession session) {
        raceBookingRepo.findByUserUserIdAndSessionSessionDateAndStatusIn(driver.getUserId(),
                        session.getSessionDate(), HOLDS_A_PLACE).stream()
                .map(RaceBooking::getSession)
                .filter(taken -> session.getStartTime().isBefore(taken.getEndTime())
                        && taken.getStartTime().isBefore(session.getEndTime()))
                .findFirst()
                .ifPresent(taken -> {
                    throw new ConflictException(
                            "This driver is already registered for an overlapping session on this date");
                });
    }

    private void checkOpenForBooking(RaceSession session) {
        checkSessionRules(OPEN_FOR_BOOKING, session);
    }

    private void checkBookingCanChange(RaceSession session) {
        checkSessionRules(BOOKINGS_CAN_CHANGE, session);
    }

    // rules are tried from the top, the first broken one is the answer
    private void checkSessionRules(List<SessionRule> rules, RaceSession session) {
        rules.stream()
                .filter(rule -> rule.broken().test(session))
                .map(rule -> rule.message().apply(session))
                .findFirst()
                .ifPresent(message -> {
                    throw new BookingException(message);
                });
    }

    private void checkSeatFree(RaceSession session) {
        Optional.of(session)
                .filter(open -> !isFull(open))
                .orElseThrow(() -> new ConflictException("Session is full (" + session.getMaxCars() + " cars)"));
    }

    private boolean isFull(RaceSession session) {
        return countApprovedCars(session.getSessionId()) >= session.getMaxCars();
    }

    // a reject needs a reason of at least 5 characters
    private void checkReason(String note) {
        String reason = Optional.ofNullable(note)
                .orElseThrow(() -> new BookingException("Please write a reason for rejecting this booking"));
        Optional.of(reason)
                .filter(text -> text.length() >= 5)
                .orElseThrow(() -> new BookingException("Reason must be at least 5 characters"));
    }

    // a rejected booking does not hold its car number
    private void checkCarNumberFree(RaceBooking booking) {
        Optional.of(booking)
                .filter(car -> !raceBookingRepo.existsBySessionSessionIdAndCarNumberAndStatusNot(
                        car.getSession().getSessionId(), car.getCarNumber(), BookingStatus.REJECTED))
                .orElseThrow(() -> new DuplicateCarNumberException("Car number " + booking.getCarNumber()
                        + " is already registered for this session"));
    }

    private void checkCarNumberStillFree(RaceBooking booking) {
        Optional.of(booking)
                .filter(car -> !raceBookingRepo.existsBySessionSessionIdAndCarNumberAndStatusNotAndRaceBookingIdNot(
                        car.getSession().getSessionId(), car.getCarNumber(), BookingStatus.REJECTED,
                        car.getRaceBookingId()))
                .orElseThrow(() -> new DuplicateCarNumberException("Car number " + booking.getCarNumber()
                        + " was taken by another booking in the meantime"));
    }

    private void checkCanBeDeleted(RaceBooking booking) {
        RaceSession session = booking.getSession();
        Optional.of(session)
                .filter(stage -> !stage.isRacing())
                .orElseThrow(() -> new BookingException("Race places come from qualifying and cannot be removed"));
        // an approved car of a session that already ran is part of the results
        Optional.of(booking)
                .filter(car -> BookingStatus.APPROVED.equals(car.getStatus()) && !session.isScheduled())
                .ifPresent(car -> {
                    throw new BookingException("This session is " + session.getStatus() + " - its results are kept");
                });
    }

    // a refunded booking can't be approved again (no payment status = a free race place)
    private void checkMoneyStillHere(RaceBooking booking) {
        Optional.ofNullable(booking.getPaymentStatus())
                .filter(status -> status != PaymentStatus.PAID)
                .ifPresent(status -> {
                    throw new BookingException(
                            "The payment of this booking was refunded - the driver has to book again");
                });
    }

    // only GridPulse puts a booking on the waitlist, and a waitlisted one only moves up by itself (or is rejected)
    private void checkWaitlistChange(String oldStatus, String newStatus) {
        Optional.of(newStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .ifPresent(status -> {
                    throw new BookingException("Waitlisted is set by GridPulse when the session is full");
                });
        // waitlisted = the session is full: approving it is the same clash as approving into a full session
        Optional.of(oldStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .filter(waiting -> BookingStatus.APPROVED.equals(newStatus))
                .ifPresent(waiting -> {
                    throw new ConflictException(
                            "Session is full - a waitlisted booking moves up by itself when a seat opens");
                });
        Optional.of(oldStatus)
                .filter(BookingStatus.WAITLISTED::equals)
                .filter(waiting -> !BookingStatus.REJECTED.equals(newStatus))
                .ifPresent(waiting -> {
                    throw new BookingException(
                            "A waitlisted booking can only be rejected - it moves up by itself when a seat opens");
                });
    }

    // Pending + Waitlisted: everyone without a seat
    private Stream<RaceBooking> waitingBookings(Long sessionId) {
        return Stream.concat(raceBookingRepo.findBySessionSessionIdAndStatus(sessionId, BookingStatus.PENDING).stream(),
                raceBookingRepo.findBySessionSessionIdAndStatusOrderByRaceBookingIdAsc(sessionId,
                        BookingStatus.WAITLISTED).stream());
    }

    // no refund: the booking keeps its money and its place in the queue
    private void waitlistPendingIfFull(RaceSession session) {
        Optional.of(session)
                .filter(this::isFull)
                .ifPresent(full -> raceBookingRepo.findBySessionSessionIdAndStatusOrderByRaceBookingIdAsc(
                        full.getSessionId(), BookingStatus.PENDING).forEach(this::waitlist));
    }

    private void waitlist(RaceBooking booking) {
        booking.setStatus(BookingStatus.WAITLISTED);
        raceBookingRepo.save(booking);
        eventPublisher.publishEvent(waitlistedEvent(booking));
        log.info("Booking {} put on the waitlist", booking.getRaceBookingId());
    }

    private void promote(RaceBooking booking) {
        booking.setStatus(BookingStatus.PENDING);
        raceBookingRepo.save(booking);
        User driver = booking.getUser();
        RaceSession session = booking.getSession();
        eventPublisher.publishEvent(new BookingPromoted(booking.getRaceBookingId(), driver.getEmail(),
                driver.getUsername(), session.getTrack().getTrackName(), session.getTrack().getLocation(),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), booking.getCarNumber(),
                booking.getCarCategory(), booking.getStatus(), "A seat opened up", null));
        log.info("Booking {} moved up from the waitlist", booking.getRaceBookingId());
    }

    private BookingWaitlisted waitlistedEvent(RaceBooking booking) {
        User driver = booking.getUser();
        RaceSession session = booking.getSession();
        return new BookingWaitlisted(booking.getRaceBookingId(), driver.getEmail(), driver.getUsername(),
                session.getTrack().getTrackName(), session.getTrack().getLocation(), session.getSessionDate(),
                session.getStartTime(), session.getEndTime(), booking.getCarNumber(), booking.getCarCategory(),
                booking.getStatus(), "Session is full - you are number " + waitlistPosition(booking)
                        + " on the waitlist", null);
    }

    // 1 = the next one to move up
    private int waitlistPosition(RaceBooking booking) {
        return (int) raceBookingRepo.countBySessionSessionIdAndStatusAndRaceBookingIdLessThan(
                booking.getSession().getSessionId(), BookingStatus.WAITLISTED, booking.getRaceBookingId()) + 1;
    }

    // a waitlisted booking shows its place in the queue
    private RaceBookingResponse toResponse(RaceBooking booking) {
        Integer position = Optional.of(booking)
                .filter(waiting -> BookingStatus.WAITLISTED.equals(waiting.getStatus()))
                .map(this::waitlistPosition)
                .orElse(null);
        return raceBookingMapper.toResponse(booking, position);
    }

    private void rejectBookings(List<RaceBooking> bookings, String note) {
        // one after the other: saved, money back, email
        bookings.forEach(booking -> reject(booking, note));
        Optional.of(bookings)
                .filter(rejected -> !rejected.isEmpty())
                .ifPresent(rejected -> log.info("{} booking(s) rejected automatically: {}", rejected.size(), note));
    }

    private void reject(RaceBooking booking, String note) {
        booking.setStatus(BookingStatus.REJECTED);
        booking.setAdminNote(note);
        raceBookingRepo.save(booking);
        refund(booking, "Booking rejected: " + note);
        emailIfDecided(booking);
    }

    // a paid booking shows REFUND_PENDING at once, payment-service sends the money (BookingRefundRequested, outbox)
    // nothing paid (free race place, booking older than payments) or already on its way back -> nothing
    private void refund(RaceBooking booking, String reason) {
        Optional.of(booking)
                .filter(paid -> paid.getPaymentStatus() == PaymentStatus.PAID)
                .ifPresent(paid -> {
                    paid.setPaymentStatus(PaymentStatus.REFUND_PENDING);
                    eventPublisher.publishEvent(new BookingRefundRequested(paid.getPaymentOrderId(),
                            paid.getRaceBookingId(), paid.getTotalPrice(), reason));
                });
    }

    // a still Pending booking gets no email yet (the waitlist has its own)
    private void emailIfDecided(RaceBooking booking) {
        Optional.of(booking)
                .filter(decided -> !BookingStatus.PENDING.equals(decided.getStatus())
                        && !BookingStatus.WAITLISTED.equals(decided.getStatus()))
                .ifPresent(this::sendStatusEmail);
    }

    // goes to kafka with this transaction (messaging/DomainEventOutboxListener), a rollback sends nothing
    // an approved booking carries its e-ticket code, notification-service attaches the pdf + calendar file
    private void sendStatusEmail(RaceBooking booking) {
        User driver = booking.getUser();
        RaceSession session = booking.getSession();
        String ticketCode = BookingStatus.APPROVED.equals(booking.getStatus())
                ? ticketService.codeFor(booking.getRaceBookingId())
                : null;
        eventPublisher.publishEvent(new BookingDecided(booking.getRaceBookingId(), driver.getEmail(),
                driver.getUsername(), session.getTrack().getTrackName(), session.getTrack().getLocation(),
                session.getSessionDate(), session.getStartTime(), session.getEndTime(), booking.getCarNumber(),
                booking.getCarCategory(), booking.getStatus(), booking.getAdminNote(), refundAmount(booking),
                ticketCode));
    }

    // only money that is on its way back shows in the email
    private Double refundAmount(RaceBooking booking) {
        return Optional.of(booking)
                .filter(refunding -> refunding.getPaymentStatus() == PaymentStatus.REFUND_PENDING)
                .map(RaceBooking::getTotalPrice)
                .orElse(null);
    }

    // an empty note counts as no note
    private String cleanNote(String note) {
        return Optional.ofNullable(note)
                .filter(text -> !text.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    private RaceBooking findBooking(Long bookingId) {
        return raceBookingRepo.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Race booking with ID: " + bookingId + " not found."));
    }

    private List<RaceBookingResponse> toResponses(List<RaceBooking> bookings) {
        return bookings.stream()
                .map(this::toResponse)
                .toList();
    }

    // "gt3" -> "GT3": the spelling from our list is what gets saved
    private String allowedValue(List<String> allowed, String value, String fieldName) {
        String typed = Optional.ofNullable(value)
                .filter(text -> !text.isBlank())
                .map(String::trim)
                .orElseThrow(() -> new BookingException(fieldName + " is required"));
        // capital or small letters do not matter
        return allowed.stream()
                .filter(option -> option.equalsIgnoreCase(typed))
                .findFirst()
                .orElseThrow(() -> new BookingException(fieldName + " must be one of " + allowed));
    }
}
