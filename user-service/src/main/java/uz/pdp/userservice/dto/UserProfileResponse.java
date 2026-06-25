package uz.pdp.userservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String phone;
    private String fullName;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
