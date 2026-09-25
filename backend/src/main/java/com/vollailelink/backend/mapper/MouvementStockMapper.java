package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.MouvementStockDTO;
import com.vollailelink.backend.model.MouvementStock;
import org.springframework.stereotype.Component;

@Component
public class MouvementStockMapper {

    public MouvementStockDTO toDto(MouvementStock m) {
        if (m == null) return null;
        return MouvementStockDTO.builder()
                .id(m.getId())
                .produitId(m.getProduit() != null ? m.getProduit().getId() : null)
                .produitNom(m.getProduit() != null ? m.getProduit().getName() : null)
                .offerId(m.getOffer() != null ? m.getOffer().getId() : null)
                .reservationId(m.getReservation() != null ? m.getReservation().getId() : null)
                .typeMouvement(m.getTypeMouvement())
                .quantite(m.getQuantite())
                .quantiteApresMouvement(m.getQuantiteApresMouvement())
                .reference(m.getReference())
                .referenceType(m.getReferenceType())
                .motif(m.getMotif())
                .isActive(m.getIsActive())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
