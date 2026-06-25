package uz.pdp.userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.pdp.userservice.entity.OtpCode;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    @Query("SELECT o FROM OtpCode o WHERE o.phone = :phone AND o.code = :code " +
           "AND o.isUsed = false AND o.expiresAt > :now")
    Optional<OtpCode> findValidOtp(String phone, String code, OffsetDateTime now);

    void deleteByPhoneAndIsUsedTrue(String phone);
}
