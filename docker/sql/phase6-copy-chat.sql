-- phase 6, optional, run once BEFORE the first start of ai-service on an existing appdb:
-- the faqs with their gemini vectors and the chat history move to ai_db. no faq has to be embedded again
-- (only the ones whose text changed), which saves gemini's daily quota
CREATE DATABASE IF NOT EXISTS ai_db;

CREATE TABLE IF NOT EXISTS ai_db.faqs LIKE appdb.faqs;
INSERT IGNORE INTO ai_db.faqs SELECT * FROM appdb.faqs;

CREATE TABLE IF NOT EXISTS ai_db.chat_messages LIKE appdb.chat_messages;
INSERT IGNORE INTO ai_db.chat_messages SELECT * FROM appdb.chat_messages;
