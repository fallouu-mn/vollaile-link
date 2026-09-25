package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Etat du stock d'une offre selon la regle du CDC :
 * disponible = total - reserve - vendu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferStockDTO {
    private Long offerId;
    private Long productId;
    private String productNom;
    private Long producerId;
    private Integer quantityTotal;
    private Integer quantityReserved;
    private Integer quantitySold;
    private Integer quantityAvailable;
    private Integer activeReservationCount;
    private OffsetDateTime computedAt;

    /**
     * Prix et statut de l'offre, exposes sur la meme reponse que le stock
     * pour eviter un second aller-retour dans l'ecran de gestion.
     * Le prix d'achat n'est volontairement pas inclus (CDC 2.1).
     */
    private java.math.BigDecimal unitPrice;
    private com.vollailelink.backend.model.enums.OfferStatus status;
    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private Boolean isNegotiable;
    private String description;
}
