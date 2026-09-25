package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.StockReservationDTO;
import com.vollailelink.backend.model.StockReservation;
import org.springframework.stereotype.Component;

@Component
public class StockReservationMapper {

    public StockReservationDTO toDto(StockReservation r) {
        if (r == null) return null;
        return StockReservationDTO.builder()
                .id(r.getId())
                .offerId(r.getOffer() != null ? r.getOffer().getId() : null)
                .commandeId(r.getCommande() != null ? r.getCommande().getId() : null)
                .commandeReference(r.getCommande() != null ? "CMD-" + r.getCommande().getId() : null)
                .quantite(r.getQuantite())
                .statut(r.getStatut())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .closedAt(r.getClosedAt())
                .closeReason(r.getCloseReason())
                .build();
    }
}
