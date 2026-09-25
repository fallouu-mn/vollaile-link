package com.vollailelink.backend.publicapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse volontairement minimale exposée par l'API publique.
 * Elle ne contient aucune donnée client ni aucun champ administrateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicDemandeAcknowledgementDTO {

    private String reference;
    private String message;
}
