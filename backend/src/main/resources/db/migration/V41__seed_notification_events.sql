INSERT INTO notification_events (event_code, display_name, description, default_enabled, target_role) VALUES
('MANAGER_ACCOUNT_CREATED', 'Manager Account Created', 'Notification when a manager account is created by Super Admin', TRUE, 'MANAGER'),
('MANAGER_ASSIGNED_TO_SCHOOL', 'Manager Assigned To School', 'Notification when a manager is assigned to a school', TRUE, 'MANAGER'),
('MANAGER_ASSIGNMENT_CHANGED', 'Manager Assignment Changed', 'Notification when manager school assignment is changed', TRUE, 'MANAGER'),
('PLATFORM_ANNOUNCEMENT', 'Platform Announcement', 'Important platform or school announcements', TRUE, 'MANAGER'),
('ACCOUNT_SECURITY', 'Account Security', 'Account or security related notifications', TRUE, 'MANAGER')
ON CONFLICT (event_code) DO NOTHING;

INSERT INTO notification_event_channels (event_id, channel, enabled)
SELECT ne.id, 'SMS', TRUE FROM notification_events ne
ON CONFLICT (event_id, channel) DO NOTHING;

INSERT INTO notification_event_channels (event_id, channel, enabled)
SELECT ne.id, 'EMAIL', TRUE FROM notification_events ne
ON CONFLICT (event_id, channel) DO NOTHING;

INSERT INTO notification_event_channels (event_id, channel, enabled)
SELECT ne.id, 'IN_APP', TRUE FROM notification_events ne
ON CONFLICT (event_id, channel) DO NOTHING;
