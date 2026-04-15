CREATE TABLE processed_events
(
    id           BIGSERIAL    NOT NULL,
    event_id     VARCHAR(100) NOT NULL,
    event_type   VARCHAR(50)  NOT NULL,
    processed_at TIMESTAMP    NOT NULL,

    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uk_processed_events_event_id UNIQUE (event_id)
);
