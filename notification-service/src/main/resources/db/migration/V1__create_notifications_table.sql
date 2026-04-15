CREATE TABLE notifications
(
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    recipient_id    UUID        NOT NULL,
    recipient_email VARCHAR(255),
    type            VARCHAR(30) NOT NULL,
    channel         VARCHAR(10) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    subject         VARCHAR(255),
    body            TEXT,
    error_message   VARCHAR(500),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL,

    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (type);
