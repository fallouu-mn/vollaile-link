package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.ReferenceTypeStock;
import com.vollailelink.backend.model.enums.TypeMouvementStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStockDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    /**
     * Offre concernee : le stock est porte par l'offre, pas par le produit.
     * Sans ce champ, impossible de savoir quel lot a bouge.
     */
    private Long offerId;
    /** Reservation a l'origine du mouvement, si elle existe. */
    private Long reservationId;
    private TypeMouvementStock typeMouvement;
    private Integer quantite;
    private Integer quantiteApresMouvement;
    private String reference;
    private ReferenceTypeStock referenceType;
    private String motif;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
