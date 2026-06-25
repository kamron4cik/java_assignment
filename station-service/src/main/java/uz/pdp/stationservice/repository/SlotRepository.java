package uz.pdp.stationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.pdp.stationservice.entity.Slot;
import uz.pdp.stationservice.entity.Slot.SlotStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlotRepository extends JpaRepository<Slot, UUID> {

    List<Slot> findByStationId(UUID stationId);

    long countByStationIdAndStatus(UUID stationId, SlotStatus status);

    @Query("SELECT s FROM Slot s WHERE s.station.id = :stationId AND s.status = 'FREE' " +
           "AND s.powerBank.status = 'AVAILABLE' ORDER BY s.slotNumber ASC")
    Optional<Slot> findFirstAvailableSlot(UUID stationId);
}
