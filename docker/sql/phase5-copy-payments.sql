-- phase 5, optional, run once: the payments made before phase 5 move from appdb to payment_db
-- (after payment-service started once, so its tables exist; best right away, before new payments come in)
-- payment-service has no user / session tables: the driver and session details are copied onto each payment
-- without this an old PAID booking that gets rejected can't be refunded (payment-service logs a warning)
INSERT INTO payment_db.payment (user_id, session_id, booking_id, driver_name, driver_email, driver_mobile,
    track_name, track_location, session_date, start_time, end_time, session_type, phase, competition_name,
    amount, status, razorpay_order_id, razorpay_payment_id, razorpay_refund_id, note, created_at, paid_at, refunded_at)
SELECT p.user_id, p.session_id, b.race_booking_id, u.username, u.email, u.mobile_number,
    t.track_name, t.location, s.session_date, s.start_time, s.end_time, s.session_type, s.phase, c.competition_name,
    p.amount, p.status, p.razorpay_order_id, p.razorpay_payment_id, p.razorpay_refund_id, p.note,
    p.created_at, p.paid_at, p.refunded_at
FROM appdb.payment p
JOIN appdb.user u ON u.user_id = p.user_id
JOIN appdb.race_session s ON s.session_id = p.session_id
JOIN appdb.race_track t ON t.track_id = s.track_id
LEFT JOIN appdb.competition c ON c.competition_id = s.competition_id
LEFT JOIN appdb.race_booking b ON b.payment_order_id = p.razorpay_order_id
WHERE p.razorpay_order_id NOT IN (SELECT razorpay_order_id FROM payment_db.payment)
ORDER BY p.payment_id;

-- their audit trail, for the orders that have none in payment_db yet
INSERT INTO payment_db.payment_audit (razorpay_order_id, event, detail, created_at)
SELECT a.razorpay_order_id, a.event, a.detail, a.created_at
FROM appdb.payment_audit a
WHERE a.razorpay_order_id NOT IN (SELECT DISTINCT razorpay_order_id FROM payment_db.payment_audit
                                  WHERE razorpay_order_id IS NOT NULL)
ORDER BY a.audit_id;
