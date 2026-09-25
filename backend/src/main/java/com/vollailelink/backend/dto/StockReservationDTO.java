package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.ReservationStatut;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationDTO {
    private Long id;
    private Long offerId;
    private Long commandeId;
    private String commandeReference;
    private Integer quantite;
    private ReservationStatut statut;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime closedAt;
    private String closeReason;
}
