-- tidy-up for an OLD local appdb (MySQL 8). A database made by the current code has none of this and nothing changes.
--
-- Hibernate (ddl-auto=update) only adds tables and columns, so an appdb that existed before payments, the FAQ chat
-- and the session model moved still carries what the code no longer reads:
--   tables   payment, payment_audit (payment-service keeps payments in payment_db now)
--            faqs, chat_messages (ai-service keeps them in ai_db now)
--   columns  race_booking: track_id, race_date, start_time, end_time, session_type (a booking points to its session)
--            race_booking: payment_id (the old link to appdb.payment, dropped together with that table)
--            feedback: track_id, track_name, active          race_track: price_per_session (each session has a price)
--
-- rules: only appdb changes. no DELETE anywhere, kept tables keep every row. a foreign key goes before its column.
-- every step looks in information_schema first, so running it again changes nothing.
-- payment, payment_audit and chat_messages are dropped only when their rows are already in payment_db / ai_db;
-- otherwise they stay and a "kept ..." line says why. faqs is rebuilt by ai-service from faqs.json, so it just goes.
--
-- 1. stop the core service (dropping a column locks its table for a moment)
-- 2. back up:  mysqldump -uroot -p --databases appdb payment_db ai_db > gridpulse-before-cleanup.sql
-- 3. run:      mysql -uroot -p < scripts/db/drop-legacy-tables.sql

-- the current core has started on this database at least once (it added race_booking.payment_order_id)
SET @current = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_booking' AND COLUMN_NAME = 'payment_order_id');
SELECT IF(@current = 1, 'appdb found: looking for old tables and columns',
          'nothing changed: appdb.race_booking has no payment_order_id - start the current core once first') AS step;


-- 1. race_booking: the date, times, track and type come from its race_session now
SET @sql = (SELECT CONCAT('ALTER TABLE appdb.race_booking ',
                          GROUP_CONCAT(DISTINCT CONCAT('DROP FOREIGN KEY `', CONSTRAINT_NAME, '`') SEPARATOR ', '))
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE @current = 1 AND TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_booking'
              AND REFERENCED_TABLE_NAME IS NOT NULL
              AND COLUMN_NAME IN ('track_id', 'race_date', 'start_time', 'end_time', 'session_type'));
SELECT COALESCE(@sql, 'race_booking: no old foreign keys') AS step;
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT CONCAT('ALTER TABLE appdb.race_booking ',
                          GROUP_CONCAT(CONCAT('DROP COLUMN `', COLUMN_NAME, '`') ORDER BY ORDINAL_POSITION SEPARATOR ', '))
            FROM information_schema.COLUMNS
            WHERE @current = 1 AND TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_booking'
              AND COLUMN_NAME IN ('track_id', 'race_date', 'start_time', 'end_time', 'session_type'));
SELECT COALESCE(@sql, 'race_booking: no old columns') AS step;
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 2. feedback: it belongs to a session now, not to a track
SET @sql = (SELECT CONCAT('ALTER TABLE appdb.feedback ',
                          GROUP_CONCAT(DISTINCT CONCAT('DROP FOREIGN KEY `', CONSTRAINT_NAME, '`') SEPARATOR ', '))
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE @current = 1 AND TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'feedback'
              AND REFERENCED_TABLE_NAME IS NOT NULL
              AND COLUMN_NAME IN ('track_id', 'track_name', 'active'));
SELECT COALESCE(@sql, 'feedback: no old foreign keys') AS step;
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT CONCAT('ALTER TABLE appdb.feedback ',
                          GROUP_CONCAT(CONCAT('DROP COLUMN `', COLUMN_NAME, '`') ORDER BY ORDINAL_POSITION SEPARATOR ', '))
            FROM information_schema.COLUMNS
            WHERE @current = 1 AND TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'feedback'
              AND COLUMN_NAME IN ('track_id', 'track_name', 'active'));
SELECT COALESCE(@sql, 'feedback: no old columns') AS step;
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 3. race_track: every session has its own price now
SET @sql = (SELECT CONCAT('ALTER TABLE appdb.race_track DROP COLUMN `', COLUMN_NAME, '`')
            FROM information_schema.COLUMNS
            WHERE @current = 1 AND TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_track'
              AND COLUMN_NAME = 'price_per_session');
SELECT COALESCE(@sql, 'race_track: no old columns') AS step;
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 4. payment + payment_audit: dropped only when every old order and its trail are in payment_db and every old
--    booking already has its order id (race_booking.payment_order_id), so no refund can lose its payment
SET @old_payment = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'payment');
SET @new_payment = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'payment_db' AND TABLE_NAME = 'payment');
SET @old_audit = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'payment_audit');
SET @new_audit = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'payment_db' AND TABLE_NAME = 'payment_audit');
SET @booking_link = (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_booking' AND COLUMN_NAME = 'payment_id');

-- NULL = not known to be safe (the copy target is missing)
SET @payments_left = IF(@old_payment = 0, 0, NULL);
SET @sql = IF(@current = 1 AND @old_payment = 1 AND @new_payment = 1,
              'SELECT COUNT(*) INTO @payments_left FROM appdb.payment o WHERE o.razorpay_order_id IS NOT NULL
                 AND NOT EXISTS (SELECT 1 FROM payment_db.payment n WHERE n.razorpay_order_id = o.razorpay_order_id)',
              'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @audit_left = IF(@old_audit = 0, 0, NULL);
SET @sql = IF(@current = 1 AND @old_audit = 1 AND @new_audit = 1,
              'SELECT COUNT(*) INTO @audit_left FROM appdb.payment_audit o WHERE o.razorpay_order_id IS NOT NULL
                 AND NOT EXISTS (SELECT 1 FROM payment_db.payment_audit n WHERE n.razorpay_order_id = o.razorpay_order_id)',
              'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @unlinked = IF(@booking_link = 0, 0, NULL);
SET @sql = IF(@current = 1 AND @booking_link = 1,
              'SELECT COUNT(*) INTO @unlinked FROM appdb.race_booking WHERE payment_id IS NOT NULL AND payment_order_id IS NULL',
              'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @payments_safe = COALESCE(@current = 1 AND @payments_left = 0 AND @audit_left = 0 AND @unlinked = 0, 0);
SELECT CASE
           WHEN @current = 0 THEN 'payment tables: skipped'
           WHEN @old_payment = 0 AND @old_audit = 0 AND @booking_link = 0 THEN 'payment tables: already gone'
           WHEN @payments_safe = 1 THEN 'payment tables: every row is in payment_db, dropping them and race_booking.payment_id'
           WHEN @payments_left IS NULL OR @audit_left IS NULL
               THEN 'kept appdb.payment and payment_audit: payment_db has no copy yet (start payment-service once, then copy the old payments)'
           WHEN @payments_left > 0 OR @audit_left > 0
               THEN CONCAT('kept appdb.payment and payment_audit: ', @payments_left, ' payment(s) and ', @audit_left,
                           ' audit order(s) are not in payment_db yet')
           ELSE CONCAT('kept appdb.payment and payment_audit: ', @unlinked, ' booking(s) have a payment_id but no payment_order_id')
       END AS step;

SET @sql = IF(@payments_safe = 1,
              (SELECT CONCAT('ALTER TABLE appdb.race_booking ',
                             GROUP_CONCAT(DISTINCT CONCAT('DROP FOREIGN KEY `', CONSTRAINT_NAME, '`') SEPARATOR ', '))
               FROM information_schema.KEY_COLUMN_USAGE
               WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'race_booking' AND REFERENCED_TABLE_NAME IS NOT NULL
                 AND (COLUMN_NAME = 'payment_id' OR REFERENCED_TABLE_NAME IN ('payment', 'payment_audit'))),
              NULL);
SET @sql = COALESCE(@sql, 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@payments_safe = 1 AND @booking_link = 1, 'ALTER TABLE appdb.race_booking DROP COLUMN `payment_id`', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@payments_safe = 1 AND (@old_payment = 1 OR @old_audit = 1),
              'DROP TABLE IF EXISTS appdb.payment_audit, appdb.payment', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 5. faqs: ai-service loads them from ai-service/src/main/resources/faqs.json into ai_db at every start
SET @old_faqs = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'faqs');
SELECT CASE
           WHEN @current = 0 THEN 'faqs: skipped'
           WHEN @old_faqs = 0 THEN 'faqs: already gone'
           ELSE 'faqs: dropping appdb.faqs (ai-service has its own)'
       END AS step;
SET @sql = IF(@current = 1 AND @old_faqs = 1, 'DROP TABLE IF EXISTS appdb.faqs', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 6. chat_messages: the FAQ chat history lives in ai_db now; dropped only when every old row is there
SET @old_chat = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'appdb' AND TABLE_NAME = 'chat_messages');
SET @new_chat = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ai_db' AND TABLE_NAME = 'chat_messages');
SET @chat_left = IF(@old_chat = 0, 0, NULL);
SET @sql = IF(@current = 1 AND @old_chat = 1 AND @new_chat = 1,
              'SELECT COUNT(*) INTO @chat_left FROM appdb.chat_messages o
                 WHERE NOT EXISTS (SELECT 1 FROM ai_db.chat_messages n WHERE n.id = o.id)',
              'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT CASE
           WHEN @current = 0 THEN 'chat_messages: skipped'
           WHEN @old_chat = 0 THEN 'chat_messages: already gone'
           WHEN @chat_left = 0 THEN 'chat_messages: every row is in ai_db, dropping appdb.chat_messages'
           WHEN @chat_left IS NULL
               THEN 'kept appdb.chat_messages: ai_db has no chat_messages table yet (start ai-service once)'
           ELSE CONCAT('kept appdb.chat_messages: ', @chat_left, ' row(s) are not in ai_db yet - copy them with: ',
                       'INSERT IGNORE INTO ai_db.chat_messages SELECT * FROM appdb.chat_messages')
       END AS step;
SET @sql = IF(@current = 1 AND @old_chat = 1 AND @chat_left = 0, 'DROP TABLE IF EXISTS appdb.chat_messages', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 7. a check only: bookings from before sessions existed have no session and the site cannot show them
SET @no_session = 0;
SET @sql = IF(@current = 1, 'SELECT COUNT(*) INTO @no_session FROM appdb.race_booking WHERE session_id IS NULL', 'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SELECT IF(@no_session = 0, 'done',
          CONCAT('done; note: ', @no_session, ' booking(s) have no session_id (older than sessions) - review them by hand'))
       AS step;
