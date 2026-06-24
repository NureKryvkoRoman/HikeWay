CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS point_of_interest (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    owner_display_name VARCHAR(255) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_poi_owner ON point_of_interest(owner_id);
CREATE INDEX IF NOT EXISTS idx_poi_deleted ON point_of_interest(deleted);
CREATE INDEX IF NOT EXISTS idx_poi_location ON point_of_interest USING GIST(location);

CREATE TABLE IF NOT EXISTS poi_rating (
    id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES point_of_interest(id),
    user_id VARCHAR(255) NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_poi_rating_user UNIQUE (poi_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_poi_rating_poi ON poi_rating(poi_id);

CREATE TABLE IF NOT EXISTS poi_comment (
    id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES point_of_interest(id),
    author_id VARCHAR(255) NOT NULL,
    author_display_name VARCHAR(255) NOT NULL,
    text VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_poi_comment_poi_created ON poi_comment(poi_id, created_at);

CREATE TABLE IF NOT EXISTS poi_photo (
    id BIGSERIAL PRIMARY KEY,
    poi_id BIGINT NOT NULL REFERENCES point_of_interest(id),
    contributor_id VARCHAR(255) NOT NULL,
    contributor_display_name VARCHAR(255) NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    public_url VARCHAR(1000),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    caption VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    ready_at TIMESTAMPTZ,
    CONSTRAINT uk_poi_photo_object_key UNIQUE (object_key)
);

CREATE INDEX IF NOT EXISTS idx_poi_photo_poi_status ON poi_photo(poi_id, status);

-- The legacy schema is still completed by Hibernate update. Creating the route
-- identity table here keeps a brand-new database migratable before JPA starts.
CREATE TABLE IF NOT EXISTS route (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS route_poi (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT NOT NULL REFERENCES route(id),
    poi_id BIGINT NOT NULL REFERENCES point_of_interest(id),
    position INTEGER NOT NULL,
    CONSTRAINT uk_route_poi UNIQUE (route_id, poi_id),
    CONSTRAINT uk_route_poi_position UNIQUE (route_id, position)
);

CREATE INDEX IF NOT EXISTS idx_route_poi_route ON route_poi(route_id, position);
