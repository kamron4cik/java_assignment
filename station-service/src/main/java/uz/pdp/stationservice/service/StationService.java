package uz.pdp.stationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.stationservice.dto.*;
import uz.pdp.stationservice.entity.Slot;
import uz.pdp.stationservice.entity.Slot.SlotStatus;
import uz.pdp.stationservice.entity.Station;
import uz.pdp.stationservice.exception.StationNotFoundException;
import uz.pdp.stationservice.repository.SlotRepository;
import uz.pdp.stationservice.repository.StationRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;
    private final SlotRepository slotRepository;

    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations(Double lat, Double lon) {
        List<Station> stations = (lat != null && lon != null)
                ? stationRepository.findNearestStations(lat, lon, 20)
                : stationRepository.findByIsActiveTrue();
        return stations.stream().map(this::mapToStationResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StationDetailResponse getStationById(UUID id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new StationNotFoundException("Station not found: " + id));
        return mapToDetailResponse(station);
    }

    // ─── mapping helpers ──────────────────────────────────────────────────────

    private StationResponse mapToStationResponse(Station station) {
        long total = slotRepository.countByStationIdAndStatus(station.getId(), SlotStatus.FREE)
                + slotRepository.countByStationIdAndStatus(station.getId(), SlotStatus.OCCUPIED);
        long available = slotRepository.countByStationIdAndStatus(station.getId(), SlotStatus.FREE);

        return StationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .address(station.getAddress())
                .totalSlots((int) total)
                .availableSlots((int) available)
                .isActive(station.getIsActive())
                .createdAt(station.getCreatedAt())
                .build();
    }

    private StationDetailResponse mapToDetailResponse(Station station) {
        List<SlotResponse> slotResponses = slotRepository.findByStationId(station.getId())
                .stream()
                .map(this::mapToSlotResponse)
                .collect(Collectors.toList());

        return StationDetailResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .address(station.getAddress())
                .isActive(station.getIsActive())
                .slots(slotResponses)
                .createdAt(station.getCreatedAt())
                .build();
    }

    private SlotResponse mapToSlotResponse(Slot slot) {
        PowerBankResponse pbResponse = null;
        if (slot.getPowerBank() != null) {
            pbResponse = PowerBankResponse.builder()
                    .id(slot.getPowerBank().getId())
                    .batteryLevel(slot.getPowerBank().getBatteryLevel())
                    .status(slot.getPowerBank().getStatus())
                    .build();
        }
        return SlotResponse.builder()
                .id(slot.getId())
                .slotNumber(slot.getSlotNumber())
                .status(slot.getStatus())
                .powerBank(pbResponse)
                .build();
    }
}
