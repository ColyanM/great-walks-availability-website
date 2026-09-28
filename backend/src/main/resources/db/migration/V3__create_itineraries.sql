CREATE TABLE itineraries (
    id INTEGER PRIMARY KEY,
    walk_id INTEGER NOT NULL REFERENCES walks(id),
    name VARCHAR(100) NOT NULL,
    UNIQUE (walk_id, name)
);

CREATE TABLE itinerary_stops (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    itinerary_id INTEGER NOT NULL REFERENCES itineraries(id),
    facility_id INTEGER NOT NULL REFERENCES facilities(id),
    night_offset INTEGER NOT NULL CHECK (night_offset >= 0),
    UNIQUE (itinerary_id, night_offset)
);

INSERT INTO itineraries (id, walk_id, name)
VALUES (1, 1, 'Kepler - three hut nights');

INSERT INTO itinerary_stops (itinerary_id, facility_id, night_offset)
VALUES
    (1, 101, 0),
    (1, 102, 1),
    (1, 103, 2);