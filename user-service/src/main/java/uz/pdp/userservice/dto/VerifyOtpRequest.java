package uz.pdp.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "OTP code is required")
    @Size(min = 4, max = 10, message = "OTP code must be between 4 and 10 characters")
    private String code;
}
