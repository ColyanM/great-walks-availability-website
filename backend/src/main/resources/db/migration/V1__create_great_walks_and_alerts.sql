CREATE TABLE walks (
    id INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO walks (id, name)
VALUES
    (1, 'Kepler Track'),
    (2, 'Milford Track'),
    (3, 'Routeburn Track');

CREATE TABLE alerts (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    walk_id INTEGER NOT NULL REFERENCES walks(id),
    start_date DATE NOT NULL,
    party_size INTEGER NOT NULL CHECK (party_size > 0)
);