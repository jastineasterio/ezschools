INSERT INTO notification_global_settings (setting_key, setting_value, description) VALUES
('notifications.global.enabled', 'true', 'Enable all notifications globally'),
('sms.enabled', 'true', 'Enable SMS notifications'),
('email.enabled', 'true', 'Enable email notifications'),
('email.smtp.host', '', 'SMTP host'),
('email.smtp.port', '587', 'SMTP port'),
('email.smtp.username', '', 'SMTP username'),
('email.smtp.password', '', 'SMTP password (encrypted in practice)'),
('email.smtp.from', '', 'From email address'),
('sms.provider', 'DARSMS', 'SMS provider'),
('sms.sender_id', '', 'SMS sender ID'),
('sms.api_key', '', 'SMS API key'),
('test.recipient.email', '', 'Test email recipient'),
('test.recipient.phone', '', 'Test phone recipient')
ON CONFLICT (setting_key) DO NOTHING;
