package com.vollailelink.backend.model;

import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.ModePaiement;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "commandes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private Demande demande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Product produit;

    /**
     * Offre sur laquelle la commande a ete passee.
     * Indispensable pour reserver le bon lot : deux offres du meme produit
     * (producteurs differents) ont des stocks distincts.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_id")
    private Offer offre;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "prix_unitaire", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "prix_unitaire_achat", precision = 15, scale = 2)
    private BigDecimal prixUnitaireAchat;

    @Column(name = "remise_pourcentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal remisePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_montant", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    @Column(name = "total_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalHt;

    @Column(name = "total_ttc", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTtc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CommandeStatut statut = CommandeStatut.EN_ATTENTE_PAIEMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    @Column(name = "date_paiement")
    private OffsetDateTime datePaiement;

    @Column(name = "date_livraison_prev", nullable = false)
    private OffsetDateTime dateLivraisonPrev;

    @Column(name = "date_livraison_reelle")
    private OffsetDateTime dateLivraisonReelle;

    @Column(name = "lieu_livraison", nullable = false, length = 200)
    private String lieuLivraison;

    @Column(name = "numero_suivi", length = 50)
    private String numeroSuivi;

    @Column(columnDefinition = "TEXT")
    private String commentaires;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

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
