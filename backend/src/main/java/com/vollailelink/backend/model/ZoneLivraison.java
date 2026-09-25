package com.vollailelink.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Zone de livraison desservie, avec frais et delai annonces au client.
 *
 * Les montants sont en FCFA (regle metier du CDC, section 2.4).
 */
@Entity
@Table(name = "zone_livraison")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneLivraison {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Frais de livraison en FCFA. 0 signifie "inclus". */
    @Column(name = "frais_livraison", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal fraisLivraison = BigDecimal.ZERO;

    @Column(name = "delai_min_heures", nullable = false)
    @Builder.Default
    private Integer delaiMinHeures = 24;

    @Column(name = "delai_max_heures", nullable = false)
    @Builder.Default
    private Integer delaiMaxHeures = 72;

    @Column(name = "est_active", nullable = false)
    @Builder.Default
    private Boolean estActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
