package uz.pdp.stationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.pdp.stationservice.dto.*;
import uz.pdp.stationservice.service.StationService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/stations")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    /** GET /v1/stations?lat=...&lon=... — List all stations, optionally sorted by distance */
    @GetMapping
    public ResponseEntity<List<StationResponse>> getStations(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon) {
        return ResponseEntity.ok(stationService.getAllStations(lat, lon));
    }

    /** GET /v1/stations/{id} — Get station details including all slots */
    @GetMapping("/{id}")
    public ResponseEntity<StationDetailResponse> getStation(@PathVariable UUID id) {
        return ResponseEntity.ok(stationService.getStationById(id));
    }
}
