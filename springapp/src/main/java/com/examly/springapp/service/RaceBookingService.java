package com.examly.springapp.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.request.InternalBookingRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.BookingQuote;
import com.examly.springapp.dto.response.CarCategoryResponse;
import com.examly.springapp.dto.response.InternalBookingResult;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;

public interface RaceBookingService {

    List<RaceBookingResponse> getAllRaceBookings();

    // admin list: newest first, search = track name, status Pending / Approved / Rejected / All
    PageResponse<RaceBookingResponse> getBookingPage(String search, String status, int page, int size);

    List<CarCategoryResponse> getCarCategories();

    // only your own (admins: anybody's) -> AccessDeniedException (403) otherwise
    List<RaceBookingResponse> getRaceBookingsByUserId(int userId);

    RaceBookingResponse getRaceBookingById(Long bookingId);

    RaceSession checkCanBook(RaceBookingRequest request);

    // payment-service before the razorpay order (the driver's token): the rules + what the payment keeps
    BookingQuote quote(RaceBookingRequest request);

    // payment-service after the money came in: the booking, or the one this order already made
    InternalBookingResult createPaidBooking(InternalBookingRequest request);

    Optional<RaceBookingResponse> findByOrder(String razorpayOrderId);

    // PaymentStatusChanged: the booking of this order copies the status (unknown order / status -> nothing)
    void applyPaymentStatus(String razorpayOrderId, String status);

    // a reject needs a note. the approval that fills the last seat rejects everyone still waiting
    RaceBookingResponse updateRaceBooking(Long bookingId, BookingStatusRequest request);

    // driver: Pending -> refund, Approved -> allowed but no refund. admin: always a refund
    void deleteRaceBooking(Long bookingId);

    void rejectWaitingBookings(Long sessionId, String note);

    // Pending and Approved -> Rejected + refund + email (session / competition cancelled)
    void rejectAllBookings(Long sessionId, String note);

    // seats taken = Approved bookings
    long countApprovedCars(Long sessionId);

    // session id -> approved cars, one query for many sessions
    Map<Long, Long> countApprovedCars(Collection<Long> sessionIds);

    // the cars of a session: live boards, race control
    List<RaceBooking> findApprovedCars(Long sessionId);
}
