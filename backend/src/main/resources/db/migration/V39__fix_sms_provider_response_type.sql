ALTER TABLE sms_messages
    ALTER COLUMN provider_response TYPE TEXT
    USING provider_response::TEXT;
