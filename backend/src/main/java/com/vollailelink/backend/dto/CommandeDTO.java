package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.ModePaiement;
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
public class CommandeDTO {
    private Long id;
    private Long demandeId;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private Long produitId;
    private String produitNom;
    /**
     * Offre commandee (le lot de production). C'est elle qui porte le stock :
     * deux offres du meme produit n'ont pas la meme disponibilite.
     */
    private Long offreId;
    private Integer quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal prixUnitaireAchat;
    private BigDecimal remisePourcentage;
    private BigDecimal remiseMontant;
    private BigDecimal totalHt;
    private BigDecimal totalTtc;
    private CommandeStatut statut;
    private ModePaiement modePaiement;
    private OffsetDateTime datePaiement;
    private OffsetDateTime dateLivraisonPrev;
    private OffsetDateTime dateLivraisonReelle;
    private String lieuLivraison;
    private String numeroSuivi;
    private String commentaires;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
