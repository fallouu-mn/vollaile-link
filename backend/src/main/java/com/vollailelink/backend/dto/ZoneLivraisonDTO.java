package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoneLivraisonDTO {
    private Long id;
    private String nom;
    private String code;
    private String description;
    /** Frais en FCFA. 0 = livraison incluse. */
    private BigDecimal fraisLivraison;
    private Integer delaiMinHeures;
    private Integer delaiMaxHeures;
    private Boolean estActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
