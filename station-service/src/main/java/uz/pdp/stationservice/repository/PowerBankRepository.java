package uz.pdp.stationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.stationservice.entity.PowerBank;
import uz.pdp.stationservice.entity.PowerBank.PowerBankStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PowerBankRepository extends JpaRepository<PowerBank, UUID> {

    Optional<PowerBank> findBySlotIdAndStatus(UUID slotId, PowerBankStatus status);

    List<PowerBank> findByStatus(PowerBankStatus status);
}
