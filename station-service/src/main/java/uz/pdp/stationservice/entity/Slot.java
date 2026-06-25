package uz.pdp.stationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "slots", indexes = {
        @Index(name = "idx_slots_station_id", columnList = "station_id"),
        @Index(name = "idx_slots_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SlotStatus status = SlotStatus.FREE;

    @OneToOne(mappedBy = "slot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PowerBank powerBank;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    public enum SlotStatus {
        FREE, OCCUPIED
    }
}
