package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.DemandeDTO;
import com.vollailelink.backend.model.Demande;
import org.springframework.stereotype.Component;

@Component
public class DemandeMapper {

    public DemandeDTO toDto(Demande demande) {
        if (demande == null) return null;
        return DemandeDTO.builder()
                .id(demande.getId())
                .clientId(demande.getClient() != null ? demande.getClient().getId() : null)
                .clientNom(demande.getClient() != null ? demande.getClient().getNom() : null)
                .clientPrenom(demande.getClient() != null ? demande.getClient().getPrenom() : null)
                .clientTelephone(demande.getClient() != null ? demande.getClient().getTelephone() : null)
                .produitId(demande.getProduit() != null ? demande.getProduit().getId() : null)
                .produitNom(demande.getProduit() != null ? demande.getProduit().getName() : null)
                .typeClient(demande.getTypeClient())
                .zoneLivraison(demande.getZoneLivraison())
                .poidsSouhaite(demande.getPoidsSouhaite())
                .adresseLivraison(demande.getAdresseLivraison())
                .preferenceContact(demande.getPreferenceContact())
                .offreId(demande.getOffreId())
                .consentement(demande.getConsentement())
                .quantiteSouhaitee(demande.getQuantiteSouhaitee())
                .prixUnitaireSouhaite(demande.getPrixUnitaireSouhaite())
                .nomProduit(demande.getNomProduit())
                .description(demande.getDescription())
                .statut(demande.getStatut())
                .dateSouhaitee(demande.getDateSouhaitee())
                .lieuLivraison(demande.getLieuLivraison())
                .commentairesAdmin(demande.getCommentairesAdmin())
                .isActive(demande.getIsActive())
                .createdAt(demande.getCreatedAt())
                .updatedAt(demande.getUpdatedAt())
                .build();
    }
}
