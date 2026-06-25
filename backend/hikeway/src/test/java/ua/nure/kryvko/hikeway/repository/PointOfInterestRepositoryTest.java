package ua.nure.kryvko.hikeway.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PointOfInterestRepositoryTest {
    @Autowired
    private PointOfInterestRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsNonDeletedPoisWithinRadiusOrderedByDistance() {
        long nearestId = insertPoi("Nearest", 24.0005, 49.8, false);
        long nextId = insertPoi("Next", 24.002, 49.8, false);
        insertPoi("Outside", 24.02, 49.8, false);
        insertPoi("Deleted", 24.0001, 49.8, true);

        var firstPage = repository.findNearby(24.0, 49.8, 500.0, PageRequest.of(0, 1));
        var secondPage = repository.findNearby(24.0, 49.8, 500.0, PageRequest.of(1, 1));

        assertEquals(2, firstPage.getTotalElements());
        assertEquals(nearestId, firstPage.getContent().getFirst().getId());
        assertEquals(nextId, secondPage.getContent().getFirst().getId());
        assertTrue(firstPage.getContent().getFirst().getDistanceMeters() >= 0);
        assertTrue(firstPage.getContent().getFirst().getDistanceMeters()
                < secondPage.getContent().getFirst().getDistanceMeters());
    }

    private long insertPoi(String name, double longitude, double latitude, boolean deleted) {
        return jdbcTemplate.queryForObject(
                """
                        INSERT INTO point_of_interest (
                            owner_id,
                            owner_display_name,
                            name,
                            description,
                            location,
                            created_at,
                            updated_at,
                            deleted
                        )
                        VALUES (
                            'repository-test-user',
                            'Repository Test',
                            ?,
                            ?,
                            ST_SetSRID(ST_MakePoint(?, ?), 4326),
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP,
                            ?
                        )
                        RETURNING id
                        """,
                new Object[]{name, name + " description", longitude, latitude, deleted},
                new int[]{Types.VARCHAR, Types.VARCHAR, Types.DOUBLE, Types.DOUBLE, Types.BOOLEAN},
                Long.class
        );
    }
}
