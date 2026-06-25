package uz.pdp.stationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.stationservice.entity.PowerBank;
import uz.pdp.stationservice.entity.PowerBank.PowerBankStatus;
import uz.pdp.stationservice.entity.Slot;
import uz.pdp.stationservice.entity.Slot.SlotStatus;
import uz.pdp.stationservice.kafka.*;
import uz.pdp.stationservice.repository.PowerBankRepository;
import uz.pdp.stationservice.repository.SlotRepository;
import uz.pdp.stationservice.repository.StationRepository;

/**
 * Simulates IoT station behavior:
 * - Cabinet lock: locks a slot so user can take a powerbank
 * - Eject: physically ejects the powerbank from its slot
 *
 * In production, these operations would call actual IoT firmware APIs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StationSimulationService {

    private final StationRepository stationRepository;
    private final SlotRepository slotRepository;
    private final PowerBankRepository powerBankRepository;
    private final StationEventProducer eventProducer;

    /**
     * Simulates locking the cabinet slot.
     * Finds an available slot with a powerbank, reserves it.
     */
    @Transactional
    public void simulateCabinetLock(AcquireCabinetLockEvent event) {
        // Simulate some async IoT latency
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        slotRepository.findFirstAvailableSlot(event.getStationId())
                .ifPresentOrElse(
                        slot -> {
                            // Lock the slot
                            slot.setStatus(SlotStatus.OCCUPIED);
                            slotRepository.save(slot);

                            CabinetLockResultEvent result = CabinetLockResultEvent.builder()
                                    .rentalId(event.getRentalId())
                                    .stationId(event.getStationId())
                                    .slotId(slot.getId())
                                    .powerBankId(slot.getPowerBank().getId())
                                    .success(true)
                                    .correlationId(event.getCorrelationId())
                                    .build();
                            eventProducer.publishLockResult(result);
                        },
                        () -> {
                            log.warn("No available slots at station: {}", event.getStationId());
                            CabinetLockResultEvent result = CabinetLockResultEvent.builder()
                                    .rentalId(event.getRentalId())
                                    .stationId(event.getStationId())
                                    .success(false)
                                    .failureReason("No available slots")
                                    .correlationId(event.getCorrelationId())
                                    .build();
                            eventProducer.publishLockResult(result);
                        }
                );
    }

    /**
     * Simulates physically ejecting the powerbank from its slot.
     * Updates powerbank status to RENTED.
     */
    @Transactional
    public void simulateEjectPowerBank(EjectPowerBankEvent event) {
        // Simulate IoT eject operation latency
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        powerBankRepository.findById(event.getPowerBankId())
                .ifPresentOrElse(
                        powerBank -> {
                            powerBank.setStatus(PowerBankStatus.RENTED);
                            powerBank.setSlot(null); // no longer in slot
                            powerBankRepository.save(powerBank);

                            EjectPowerBankResultEvent result = EjectPowerBankResultEvent.builder()
                                    .rentalId(event.getRentalId())
                                    .stationId(event.getStationId())
                                    .slotId(event.getSlotId())
                                    .powerBankId(event.getPowerBankId())
                                    .success(true)
                                    .correlationId(event.getCorrelationId())
                                    .build();
                            eventProducer.publishEjectResult(result);
                        },
                        () -> {
                            log.error("PowerBank not found: {}", event.getPowerBankId());
                            EjectPowerBankResultEvent result = EjectPowerBankResultEvent.builder()
                                    .rentalId(event.getRentalId())
                                    .success(false)
                                    .failureReason("PowerBank not found")
                                    .correlationId(event.getCorrelationId())
                                    .build();
                            eventProducer.publishEjectResult(result);
                        }
                );
    }
}
