package com.vollailelink.backend.model;

import com.vollailelink.backend.model.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producer_id", nullable = false)
    private Producer producer;

    /**
     * Quantite physiquement mise en vente sur cette offre.
     * Seul l'ENTREE / SORTIE / AJUSTEMENT / INVENTAIRE modifie ce compteur.
     */
    @Column(name = "quantity_total", nullable = false)
    @Builder.Default
    private Integer quantityTotal = 0;

    /**
     * Quantite actuellement bloquee par des reservations actives.
     * Une reservation incremente ce compteur et decremente le disponible,
     * SANS toucher au total : c'est ce qui distingue une reservation d'une vente.
     */
    @Column(name = "quantity_reserved", nullable = false)
    @Builder.Default
    private Integer quantityReserved = 0;

    /**
     * Quantite definitivement vendue (livraison confirmee).
     */
    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Integer quantitySold = 0;

    /**
     * Colonne derivee par PostgreSQL : quantity_total - quantity_reserved - quantity_sold.
     * L'invariant est donc impose par la base, pas seulement par l'application.
     * Non insérable / non updatable cote JPA : la base est la source de verite.
     */
    @Column(name = "quantity_available", insertable = false, updatable = false)
    private Integer quantityAvailable;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_value", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal totalValue;

    /**
     * Verrou optimiste : deux transactions concurrentes sur la meme offre
     * ne peuvent pas s'ecrire l'une sur l'autre. Le verrou pessimiste
     * (SELECT ... FOR UPDATE) est pose par StockService sur la ligne offre.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    /**
     * Stock disponible = total - reserve - vendu (CDC section 2.3).
     */
    @Transient
    public int getDisponible() {
        return nvl(quantityTotal) - nvl(quantityReserved) - nvl(quantitySold);
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OfferStatus status = OfferStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "is_negotiable", nullable = false)
    @Builder.Default
    private Boolean isNegotiable = false;

    @Column(columnDefinition = "TEXT")
    private String description;

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
