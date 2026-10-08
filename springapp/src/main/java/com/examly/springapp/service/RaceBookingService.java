package com.examly.springapp.service;

import java.util.List;

import com.examly.springapp.dto.request.BookingStatusRequest;
import com.examly.springapp.dto.request.RaceBookingRequest;
import com.examly.springapp.dto.response.CarCategoryResponse;
import com.examly.springapp.dto.response.RaceBookingResponse;
import com.examly.springapp.model.RaceBooking;
import com.examly.springapp.model.RaceSession;

public interface RaceBookingService {

    List<RaceBookingResponse> getAllRaceBookings();

    List<CarCategoryResponse> getCarCategories();

    // only your own (admins: anybody's) -> AccessDeniedException (403) otherwise
    List<RaceBookingResponse> getRaceBookingsByUserId(int userId);

    RaceBookingResponse getRaceBookingById(Long bookingId);

    RaceSession checkCanBook(RaceBookingRequest request);

    // after paying, only PaymentServiceImpl calls this: payment PAID + booking saved in one transaction
    RaceBookingResponse addRaceBooking(RaceBookingRequest request, String razorpayOrderId, String razorpayPaymentId);

    // a reject needs a note. the approval that fills the last seat rejects everyone still waiting
    RaceBookingResponse updateRaceBooking(Long bookingId, BookingStatusRequest request);

    // driver: Pending -> refund, Approved -> allowed but no refund. admin: always a refund
    void deleteRaceBooking(Long bookingId);

    void rejectWaitingBookings(Long sessionId, String note);

    // Pending and Approved -> Rejected + refund + email (session / competition cancelled)
    void rejectAllBookings(Long sessionId, String note);

    // seats taken = Approved bookings
    long countApprovedCars(Long sessionId);

    // the cars of a session: live boards, race control
    List<RaceBooking> findApprovedCars(Long sessionId);
}
