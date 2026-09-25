package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.DashboardDTO;
import com.vollailelink.backend.model.AuditLog;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.DemandeStatut;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.model.enums.ProducerStatus;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.AuditLogRepository;
import com.vollailelink.backend.repository.ClientRepository;
import com.vollailelink.backend.repository.CommandeRepository;
import com.vollailelink.backend.repository.DemandeRepository;
import com.vollailelink.backend.repository.NotificationRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.ProducerRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Synthese reelle du tableau de bord.
 *
 * Toutes les valeurs sont calculees en base. L'ancien ecran affichait des
 * chiffres codes en dur (12 producteurs, 89 clients, 1540 unites), ce qui
 * donnait une fausse impression d'activite sur une base vide.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Periode de comparaison pour les evolutions : 30 derniers jours. */
    private static final int PERIODE_EVOLUTION_JOURS = 30;

    /** Nombre d'evenements affiches dans l'activite recente. */
    private static final int TAILLE_ACTIVITES = 8;

    private final ClientRepository clientRepository;
    private final DemandeRepository demandeRepository;
    private final CommandeRepository commandeRepository;
    private final OfferRepository offerRepository;
    private final ProducerRepository producerRepository;
    private final NotificationRepository notificationRepository;
    private final StockReservationRepository reservationRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public DashboardDTO getDashboard() {
        OffsetDateTime maintenant = OffsetDateTime.now();
        OffsetDateTime debutPeriode = maintenant.minus(PERIODE_EVOLUTION_JOURS, ChronoUnit.DAYS);

        long stockTotal = offreTotal();
        long stockReserve = offerRepository.sumReservedByStatus(OfferStatus.ACTIVE);
        long stockVendu = offerRepository.sumSoldByStatus(OfferStatus.ACTIVE);
        long stockDisponible = offerRepository.sumAvailableByStatus(OfferStatus.ACTIVE);

        return DashboardDTO.builder()
                .clientsActifs(DashboardDTO.Stat.builder()
                        .valeur(clientRepository.count())
                        .evolution(clientRepository.countByCreatedAtAfter(debutPeriode))
                        .libelleEvolution("sur " + PERIODE_EVOLUTION_JOURS + " jours")
                        .build())
                .demandesEnCours(DashboardDTO.Stat.builder()
                        .valeur(demandeRepository.countByStatut(DemandeStatut.EN_COURS))
                        .evolution(demandeRepository.countByCreatedAtAfter(debutPeriode))
                        .libelleEvolution("sur " + PERIODE_EVOLUTION_JOURS + " jours")
                        .build())
                .commandesEnAttente(DashboardDTO.Stat.builder()
                        .valeur(commandeRepository.countByStatut(CommandeStatut.EN_ATTENTE_PAIEMENT))
                        .libelleEvolution("en attente de paiement")
                        .build())
                .stockDisponible(DashboardDTO.Stat.builder()
                        .valeur(stockDisponible)
                        .libelleEvolution("unites disponibles")
                        .build())
                .reservationsActives(DashboardDTO.Stat.builder()
                        .valeur(reservationRepository
                                .findByStatutOrderByIdAsc(ReservationStatut.ACTIVE).size())
                        .libelleEvolution("reservations en cours")
                        .build())
                .producteursActifs(DashboardDTO.Stat.builder()
                        .valeur(producerRepository.countByStatus(ProducerStatus.ACTIVE))
                        .libelleEvolution("producteurs verifies")
                        .build())
                .offresActives(DashboardDTO.Stat.builder()
                        .valeur(offerRepository.countByStatus(OfferStatus.ACTIVE))
                        .libelleEvolution("lots disponibles")
                        .build())
                .notificationsNonLues(DashboardDTO.Stat.builder()
                        .valeur(notificationRepository.findByLuFalseOrderByCreatedAtDesc().size())
                        .libelleEvolution("non lues")
                        .build())
                .stock(DashboardDTO.StockSummary.builder()
                        .total(stockTotal)
                        .reserve(stockReserve)
                        .vendu(stockVendu)
                        .disponible(stockDisponible)
                        .build())
                .activitesRecentes(loadActivites())
                .build();
    }

    private long offreTotal() {
        return offerRepository.sumAvailableByStatus(OfferStatus.ACTIVE)
                + offerRepository.sumReservedByStatus(OfferStatus.ACTIVE)
                + offerRepository.sumSoldByStatus(OfferStatus.ACTIVE);
    }

    /**
     * Activite recente tiree de la piste d'audit (CDC 2.5).
     * Une seule source de verite : ce qui est affiche ici est trace ailleurs.
     */
    private List<DashboardDTO.Activity> loadActivites() {
        return auditLogRepository.findRecent(null, PageRequest.of(0, TAILLE_ACTIVITES)).stream()
                .map(this::toActivity)
                .toList();
    }

    private DashboardDTO.Activity toActivity(AuditLog a) {
        return DashboardDTO.Activity.builder()
                .type(a.getEventType())
                .icone(icone(a.getEventType()))
                .couleur(couleur(a.getEventType()))
                .texte(a.getEventDescription())
                .horodatage(a.getEventTimestamp())
                .build();
    }

    private String icone(String eventType) {
        if (eventType == null) return "info";
        return switch (eventType) {
            case "STOCK_ENTREE" -> "inventory";
            case "STOCK_SORTIE" -> "outbound";
            case "STOCK_AJUSTEMENT" -> "tune";
            case "STOCK_RESERVATION" -> "bookmark";
            case "STOCK_LIBERATION" -> "lock_open";
            case "STOCK_VENTE" -> "shopping_cart";
            case "PRIX_OFFRE_CHANGEMENT", "PRIX_PRODUIT_CHANGEMENT" -> "price_change";
            case "OFFRE_STATUT_CHANGEMENT" -> "swap_horiz";
            case "COMMANDE_STATUT_CHANGEMENT" -> "assignment_turned_in";
            case "DEMANDE_STATUT_CHANGEMENT" -> "assignment";
            case "LOGIN_SUCCESS" -> "login";
            case "LOGIN_FAILURE" -> "lock";
            default -> "info";
        };
    }

    private String couleur(String eventType) {
        if (eventType == null) return "blue";
        return switch (eventType) {
            case "STOCK_ENTREE", "STOCK_VENTE", "LOGIN_SUCCESS" -> "green";
            case "STOCK_SORTIE", "LOGIN_FAILURE" -> "red";
            case "STOCK_RESERVATION", "STOCK_AJUSTEMENT" -> "amber";
            case "STOCK_LIBERATION", "PRIX_OFFRE_CHANGEMENT", "PRIX_PRODUIT_CHANGEMENT" -> "purple";
            case "COMMANDE_STATUT_CHANGEMENT", "DEMANDE_STATUT_CHANGEMENT" -> "blue";
            default -> "blue";
        };
    }
}
