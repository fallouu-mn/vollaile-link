package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Produit du catalogue, avec son stock agrege.
 *
 * Le stock n'est plus porte par le produit mais par les OFFRES qui le
 * comercialisent. On expose donc un agregat par produit, en distinguant
 * clairement total / reserve / vendu / disponible (regle du CDC 2.3),
 * plutot qu'un unique "stock" trompeur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private String unitType;
    private Long categoryId;
    private String categoryNom;
    private BigDecimal defaultUnitPrice;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    /** Nombre d'offres actives commercialisation ce produit. */
    private long activeOfferCount;

    /** Stock agrege des offres actives. */
    private Stock stock;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stock {
        private long total;
        private long reserved;
        private long sold;
        /** total - reserved - sold */
        private long available;
    }
}
