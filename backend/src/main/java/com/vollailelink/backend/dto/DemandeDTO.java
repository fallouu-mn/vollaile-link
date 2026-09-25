package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.DemandeStatut;
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
public class DemandeDTO {
    private Long id;
    private Long clientId;
    private String clientNom;
    private String clientPrenom;
    private String clientTelephone;
    private Long produitId;
    private String produitNom;
    private String typeClient;
    private String zoneLivraison;
    private BigDecimal poidsSouhaite;
    private String adresseLivraison;
    private String preferenceContact;
    private Long offreId;
    private Boolean consentement;
    private Integer quantiteSouhaitee;
    private BigDecimal prixUnitaireSouhaite;
    private String nomProduit;
    private String description;
    private DemandeStatut statut;
    private OffsetDateTime dateSouhaitee;
    private String lieuLivraison;
    private String commentairesAdmin;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
