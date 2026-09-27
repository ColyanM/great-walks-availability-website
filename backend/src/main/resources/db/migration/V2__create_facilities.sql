CREATE TABLE facilities (
    id INTEGER PRIMARY KEY,
    walk_id INTEGER NOT NULL REFERENCES walks(id),
    name VARCHAR(100) NOT NULL,
    facility_type VARCHAR(20) NOT NULL,
    UNIQUE (walk_id, name),
    CHECK (facility_type IN ('HUT', 'CAMPSITE'))
);

INSERT INTO facilities (id, walk_id, name, facility_type)
VALUES
    (101, 1, 'Luxmore Hut', 'HUT'),
    (102, 1, 'Iris Burn Hut', 'HUT'),
    (103, 1, 'Moturau Hut', 'HUT');