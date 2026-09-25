package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Synthese du tableau de bord (back-office).
 *
 * Toutes les valeurs proviennent de la base. Aucun chiffre code en dur :
 * un dashboard affichant une activite fictive est pire qu'un dashboard vide.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private Stat clientsActifs;
    private Stat demandesEnCours;
    private Stat commandesEnAttente;
    private Stat stockDisponible;
    private Stat reservationsActives;
    private Stat producteursActifs;
    private Stat offresActives;
    private Stat notificationsNonLues;

    /** Statistiques de stock, reparties selon la regle du CDC. */
    private StockSummary stock;

    /** Derniers evenements d'audit, du plus recent au plus ancien. */
    private List<Activity> activitesRecentes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stat {
        private long valeur;
        /** Evolution sur la periode, ou null si non pertinent. */
        private Long evolution;
        private String libelleEvolution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSummary {
        private long total;
        private long reserve;
        private long vendu;
        private long disponible;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Activity {
        private String icone;
        private String couleur;
        private String texte;
        private String type;
        private OffsetDateTime horodatage;
    }
}
