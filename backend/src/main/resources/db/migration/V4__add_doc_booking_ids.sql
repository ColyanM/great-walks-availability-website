ALTER TABLE walks
    ADD COLUMN doc_place_id INTEGER;

ALTER TABLE facilities
    ADD COLUMN doc_facility_id INTEGER;

UPDATE walks
SET doc_place_id = 872
WHERE id = 1;

UPDATE facilities
SET doc_facility_id = CASE id
    WHEN 101 THEN 2855
    WHEN 102 THEN 2854
    WHEN 103 THEN 2856
END
WHERE id IN (101, 102, 103);