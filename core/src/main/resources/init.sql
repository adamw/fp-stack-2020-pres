CREATE TABLE languages
(
    id   uuid PRIMARY KEY,
    name text NOT NULL,
    UNIQUE (name)
);

CREATE TABLE likes
(
    id          uuid PRIMARY KEY,
    language_id uuid        NOT NULL,
    username    text        NOT NULL,
    created     timestamptz NOT NULL,
    FOREIGN KEY (language_id) REFERENCES languages (id)
);
