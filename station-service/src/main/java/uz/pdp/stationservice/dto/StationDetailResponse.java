package uz.pdp.stationservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StationDetailResponse {
    private UUID id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String address;
    private Boolean isActive;
    private List<SlotResponse> slots;
    private OffsetDateTime createdAt;
}
