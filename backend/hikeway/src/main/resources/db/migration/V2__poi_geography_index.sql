CREATE INDEX IF NOT EXISTS idx_poi_location_geography
    ON point_of_interest
    USING GIST ((CAST(location AS geography)));
