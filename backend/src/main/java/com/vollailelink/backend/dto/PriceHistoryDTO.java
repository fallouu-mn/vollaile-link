package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Historique d'une variation de prix (CDC section 2.5).
 *
 * `prixAchat` n'apparait que dans les DTO admin : le prix d'achat du
 * producteur ne doit jamais etre expose publiquement (CDC 2.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryDTO {
    private Long id;
    private Long productId;
    private String productNom;
    private BigDecimal price;
    private Long changedBy;
    private String changedByPhone;
    private OffsetDateTime changedAt;
    private String reason;
}
