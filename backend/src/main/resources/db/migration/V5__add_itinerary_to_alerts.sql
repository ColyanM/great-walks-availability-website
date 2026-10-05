ALTER TABLE alerts
    ADD COLUMN itinerary_id INTEGER REFERENCES itineraries(id);