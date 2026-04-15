CREATE TABLE processed_events
(
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_id     VARCHAR(36) NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP   NOT NULL,

    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uq_processed_events_event_id UNIQUE (event_id)
);
