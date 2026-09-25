package com.vollailelink.backend.model;

import com.vollailelink.backend.model.enums.ReservationStatut;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Reservation d'une quantite sur une offre.
 *
 * Elle porte le decrement du stock disponible sans le decrementer du total :
 * c'est offers.quantity_reserved qui est incremente, donc
 * disponible = total - reserved - sold baisse, et la liberation le restaure.
 */
@Entity
@Table(name = "stock_reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @Column(nullable = false)
    private Integer quantite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatut statut = ReservationStatut.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "close_reason", length = 200)
    private String closeReason;

    @Column(name = "idempotency_key", nullable = false, length = 160)
    private String idempotencyKey;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (statut == null) statut = ReservationStatut.ACTIVE;
    }
}
