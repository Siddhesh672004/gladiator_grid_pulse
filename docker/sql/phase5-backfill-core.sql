-- phase 5, run once on an existing appdb (after the core started once with the phase-5 code, so the columns exist):
-- every booking gets its razorpay order id and a copy of its payment status on its own row
UPDATE appdb.race_booking b
JOIN appdb.payment p ON p.payment_id = b.payment_id
SET b.payment_order_id = p.razorpay_order_id,
    b.payment_status = p.status
WHERE b.payment_order_id IS NULL;
