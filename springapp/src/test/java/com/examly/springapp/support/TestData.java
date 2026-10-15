package com.examly.springapp.support;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.test.context.TestComponent;

import com.examly.springapp.config.JwtUtils;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;
import com.examly.springapp.model.RaceTrack;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.RaceBookingRepo;
import com.examly.springapp.repository.RaceSessionRepo;
import com.examly.springapp.repository.RaceTrackRepo;
import com.examly.springapp.repository.UserRepo;

import lombok.RequiredArgsConstructor;

// rows written straight through the repositories: fast, no sign up code or validation on the way
// every call makes new rows (own emails, track names ...), so tests never share data
@TestComponent
@RequiredArgsConstructor
public class TestData {

    private static final AtomicInteger NEXT = new AtomicInteger();
    // emails differ from run to run, so a mail file of an older run never counts
    private static final String RUN = Long.toString(System.currentTimeMillis(), 36);

    private final UserRepo userRepo;
    private final RaceTrackRepo raceTrackRepo;
    private final RaceSessionRepo raceSessionRepo;
    private final RaceBookingRepo raceBookingRepo;
    private final JwtUtils jwtUtils;

    // date of birth + license on file = may book
    public User driver() {
        int n = NEXT.incrementAndGet();
        User driver = new User("driver" + n + "." + RUN + "@gridpulse.test", "hash-" + n, "Driver " + n,
                String.format("9%09d", n), Role.USER);
        driver.setDateOfBirth(LocalDate.of(1995, 5, 20));
        driver.setDriverLicenseNumber("DL-TEST-" + n);
        return userRepo.save(driver);
    }

    public User admin() {
        int n = NEXT.incrementAndGet();
        return userRepo.save(new User("admin" + n + "." + RUN + "@gridpulse.test", "hash-" + n, "Admin " + n, null,
                Role.ADMIN));
    }

    public User superAdmin() {
        int n = NEXT.incrementAndGet();
        return userRepo.save(new User("super" + n + "." + RUN + "@gridpulse.test", "hash-" + n, "Super Admin", null,
                Role.SUPER_ADMIN));
    }

    // a mail address no other test uses (for accounts made through the api)
    public String newEmail(String name) {
        return name + NEXT.incrementAndGet() + "." + RUN + "@gridpulse.test";
    }

    public User disable(User user) {
        user.setEnabled(false);
        return userRepo.save(user);
    }

    public String token(User user) {
        return jwtUtils.generateToken(user);
    }

    // active circuit, open 06:00 - 22:00
    public RaceTrack track() {
        int n = NEXT.incrementAndGet();
        RaceTrack track = new RaceTrack("Test Circuit " + n, "Track for the integration tests", "Pune", "Circuit",
                false, LocalTime.of(6, 0), LocalTime.of(22, 0), "Pit lane");
        track.setImageUrls(List.of("https://res.cloudinary.com/demo/image/upload/v1/gridpulse/test_track_" + n + ".jpg"));
        return raceTrackRepo.save(track);
    }

    // practice tomorrow (india date) 10:00 - 11:00 on its own track, 500 rupees
    public RaceSession practiceSession(int maxCars) {
        return raceSessionRepo.save(new RaceSession(track(), RaceSession.today().plusDays(1), LocalTime.of(10, 0),
                LocalTime.of(11, 0), 500.0, maxCars));
    }

    // practice on a given track, some days ahead, 10:00 - 11:00
    public RaceSession practiceSession(RaceTrack track, int daysAhead, int maxCars) {
        return raceSessionRepo.save(new RaceSession(track, RaceSession.today().plusDays(daysAhead), LocalTime.of(10, 0),
                LocalTime.of(11, 0), 500.0, maxCars));
    }

    public List<RaceBooking> bookingsOf(RaceSession session) {
        return bookingsOf(session.getSessionId());
    }

    public List<RaceBooking> bookingsOf(Long sessionId) {
        return raceBookingRepo.findAll().stream()
                .filter(booking -> booking.getSession().getSessionId().equals(sessionId))
                .toList();
    }

    public RaceBooking booking(Long bookingId) {
        return raceBookingRepo.findById(bookingId).orElseThrow();
    }

    // what the driver pays: the session's own price (a test may only know the session id)
    public Double priceOf(RaceSession session) {
        return raceSessionRepo.findById(session.getSessionId()).orElseThrow().getPrice();
    }

    public RaceBooking bookingOfOrder(String razorpayOrderId) {
        return raceBookingRepo.findByPaymentOrderId(razorpayOrderId).orElseThrow();
    }
}
