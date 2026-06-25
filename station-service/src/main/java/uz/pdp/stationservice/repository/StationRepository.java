package uz.pdp.stationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.pdp.stationservice.entity.Station;

import java.util.List;
import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {

    List<Station> findByIsActiveTrue();

    /**
     * Find nearest stations using Haversine formula approximation.
     * Orders by distance from given lat/lon.
     */
    @Query(value = """
            SELECT *, (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(latitude))
                    * cos(radians(longitude) - radians(:lon))
                    + sin(radians(:lat)) * sin(radians(latitude))
                )
            ) AS distance
            FROM stations
            WHERE is_active = true
            ORDER BY distance
            LIMIT :limit
            """, nativeQuery = true)
    List<Station> findNearestStations(double lat, double lon, int limit);
}
