package com.vollailelink.backend.model;

import com.vollailelink.backend.model.enums.DemandeStatut;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "demandes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Product produit;

    @Column(name = "type_client", length = 50)
    private String typeClient;

    @Column(name = "zone_livraison", length = 100)
    private String zoneLivraison;

    @Column(name = "poids_souhaite", precision = 8, scale = 2)
    private BigDecimal poidsSouhaite;

    @Column(name = "adresse_livraison", length = 300)
    private String adresseLivraison;

    @Column(name = "preference_contact", length = 50)
    private String preferenceContact;

    @Column(name = "offre_id")
    private Long offreId;

    @Column(name = "consentement", nullable = false)
    @Builder.Default
    private Boolean consentement = false;

    @Column(name = "quantite_souhaitee")
    private Integer quantiteSouhaitee;

    @Column(name = "prix_unitaire_souhaite", precision = 15, scale = 2)
    private BigDecimal prixUnitaireSouhaite;

    @Column(name = "nom_produit", length = 200)
    private String nomProduit;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DemandeStatut statut = DemandeStatut.EN_COURS;

    @Column(name = "date_souhaitee")
    private OffsetDateTime dateSouhaitee;

    @Column(name = "lieu_livraison", length = 200)
    private String lieuLivraison;

    @Column(name = "commentaires_admin", columnDefinition = "TEXT")
    private String commentairesAdmin;

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
