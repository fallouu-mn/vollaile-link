package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.CommandeDTO;
import com.vollailelink.backend.model.Commande;
import org.springframework.stereotype.Component;

@Component
public class CommandeMapper {

    public CommandeDTO toDto(Commande commande) {
        if (commande == null) return null;
        return CommandeDTO.builder()
                .id(commande.getId())
                .demandeId(commande.getDemande() != null ? commande.getDemande().getId() : null)
                .clientId(commande.getClient() != null ? commande.getClient().getId() : null)
                .clientNom(commande.getClient() != null ? commande.getClient().getNom() : null)
                .clientPrenom(commande.getClient() != null ? commande.getClient().getPrenom() : null)
                .clientTelephone(commande.getClient() != null ? commande.getClient().getTelephone() : null)
                .produitId(commande.getProduit() != null ? commande.getProduit().getId() : null)
                .produitNom(commande.getProduit() != null ? commande.getProduit().getName() : null)
                .offreId(commande.getOffre() != null ? commande.getOffre().getId() : null)
                .quantite(commande.getQuantite())
                .prixUnitaire(commande.getPrixUnitaire())
                .prixUnitaireAchat(commande.getPrixUnitaireAchat())
                .remisePourcentage(commande.getRemisePourcentage())
                .remiseMontant(commande.getRemiseMontant())
                .totalHt(commande.getTotalHt())
                .totalTtc(commande.getTotalTtc())
                .statut(commande.getStatut())
                .modePaiement(commande.getModePaiement())
                .datePaiement(commande.getDatePaiement())
                .dateLivraisonPrev(commande.getDateLivraisonPrev())
                .dateLivraisonReelle(commande.getDateLivraisonReelle())
                .lieuLivraison(commande.getLieuLivraison())
                .numeroSuivi(commande.getNumeroSuivi())
                .commentaires(commande.getCommentaires())
                .isActive(commande.getIsActive())
                .createdAt(commande.getCreatedAt())
                .updatedAt(commande.getUpdatedAt())
                .build();
    }
}
