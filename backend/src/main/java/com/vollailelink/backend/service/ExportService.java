package com.vollailelink.backend.service;

import com.vollailelink.backend.exception.BusinessRuleException;
import com.vollailelink.backend.model.AuditLog;
import com.vollailelink.backend.model.Client;
import com.vollailelink.backend.model.Commande;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.AuditLogRepository;
import com.vollailelink.backend.repository.ClientRepository;
import com.vollailelink.backend.repository.CommandeRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Exports CSV du back-office.
 *
 * Le CDC (section 2.5) impose une piste d'audit sur les exports : chaque
 * extraction est tracee avec son type, le nombre de lignes et les filtres
 * appliques. Les donnees exportees ne sont PAS recopiees dans l'audit :
 * l'audit enregistre qu'une extraction a eu lieu, pas son contenu.
 *
 * L'echappement et la neutralisation des formules sont geres par CsvWriter.
 *
 * NOTE : ces methodes ne sont volontairement PAS en @Transactional(readOnly = true).
 * Elles ecrivent une entree d'audit (obligation du CDC, section 2.5) ; en lecture
 * seule, PostgreSQL refuse l'INSERT et l'export echoue. Une export non trace
 * n'est pas acceptable, donc l'ecriture l'emporte sur l'optimisation.
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    public static final String EVENT_EXPORT = "EXPORT_DONNEES";

    /**
     * Plafond de lignes. Au-dela, l'export est refuse plutot que tronque :
     * un fichier incomplet passe pour complet, ce qui est pire qu'un echec.
     */
    private static final int LIGNE_MAX = 50_000;

    private final ClientRepository clientRepository;
    private final CommandeRepository commandeRepository;
    private final OfferRepository offerRepository;
    private final StockReservationRepository reservationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Exports
    // ------------------------------------------------------------------

    /**
     * Base clients. Contient des donnees personnelles (telephone, email,
     * adresse) : l'export est reserve au back-office et toujours trace.
     */
    @Transactional
    public String exportClients() {
        long total = clientRepository.count();
        verifierPlafond(total, "clients");

        List<Client> clients = clientRepository.findAll();

        CsvWriter csv = new CsvWriter()
                .header("ID", "Nom", "Prenom", "Telephone", "Email", "Adresse",
                        "Ville", "Region", "Professionnel", "Entreprise", "Date naissance", "Actif", "Inscrit le");

        csv.rows(clients, c -> new Object[]{
                c.getId(), c.getNom(), c.getPrenom(), c.getTelephone(), c.getEmail(),
                c.getAdresse(), c.getVille(), c.getRegion(),
                c.getProfessionnel() != null && c.getProfessionnel() ? "Oui" : "Non",
                c.getEntreprise(), c.getDateNaissance(),
                c.getIsActive() != null && c.getIsActive() ? "Oui" : "Non",
                c.getCreatedAt()
        });

        tracer(csv.lineCount() - 1, "clients", Map.of());
        return csv.build();
    }

    /** Commandes, avec le detail de leur stock engage. */
    @Transactional
    public String exportCommandes(CommandeStatut statut) {
        long total = statut != null
                ? commandeRepository.countByStatut(statut)
                : commandeRepository.count();
        verifierPlafond(total, "commandes");

        List<Commande> commandes = statut != null
                ? commandeRepository.findByStatut(statut)
                : commandeRepository.findAll();

        // Chargement unique des reservations actives : evite une requete par
        // ligne d'export (N+1), qui devient lente des quelques centaines de
        // commandes.
        java.util.Set<Long> commandesReservees = reservationRepository
                .findByStatutOrderByIdAsc(ReservationStatut.ACTIVE)
                .stream()
                .map(r -> r.getCommande())
                .filter(java.util.Objects::nonNull)
                .map(c -> c.getId())
                .collect(java.util.stream.Collectors.toSet());

        CsvWriter csv = new CsvWriter()
                .header("ID", "Client", "Telephone client", "Produit", "Offre", "Quantite",
                        "Prix unitaire", "Total HT", "Statut", "Mode paiement",
                        "Livraison prevue", "Livraison reelle", "Lieu", "Reserve", "Cree le");

        csv.rows(commandes, c -> new Object[]{
                c.getId(),
                c.getClient() != null ? c.getClient().getNom() + " " + c.getClient().getPrenom() : null,
                c.getClient() != null ? c.getClient().getTelephone() : null,
                c.getProduit() != null ? c.getProduit().getName() : null,
                c.getOffre() != null ? c.getOffre().getId() : null,
                c.getQuantite(), c.getPrixUnitaire(), c.getTotalHt(),
                c.getStatut(), c.getModePaiement(),
                c.getDateLivraisonPrev(), c.getDateLivraisonReelle(), c.getLieuLivraison(),
                commandesReservees.contains(c.getId()) ? "Oui" : "Non",
                c.getCreatedAt()
        });

        tracer(csv.lineCount() - 1, "commandes",
                statut != null ? Map.of("statut", statut.name()) : Map.of());
        return csv.build();
    }

    /**
     * Etat du stock par offre, avec la repartition de la regle du CDC.
     * C'est l'export le plus utile en exploitation : il permet de verifier
     * que disponible = total - reserve - vendu hors de l'application.
     */
    @Transactional
    public String exportStock() {
        verifierPlafond(offerRepository.count(), "stock");

        List<Offer> offres = offerRepository.findAll();

        // Compteur de reservations par offre, calcule en une seule lecture.
        java.util.Map<Long, Long> reservationsParOffre = reservationRepository
                .findByStatutOrderByIdAsc(ReservationStatut.ACTIVE)
                .stream()
                .filter(r -> r.getOffer() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getOffer().getId(), java.util.stream.Collectors.counting()));

        CsvWriter csv = new CsvWriter()
                .header("Offre", "Produit", "Producteur", "Statut", "Prix unitaire",
                        "Total", "Reserve", "Vendu", "Disponible", "Reservations actives", "Expire le");

        csv.rows(offres, o -> new Object[]{
                o.getId(),
                o.getProduct() != null ? o.getProduct().getName() : null,
                o.getProducer() != null ? o.getProducer().getName() : null,
                o.getStatus(), o.getUnitPrice(),
                o.getQuantityTotal(), o.getQuantityReserved(), o.getQuantitySold(), o.getDisponible(),
                reservationsParOffre.getOrDefault(o.getId(), 0L),
                o.getExpiresAt()
        });

        tracer(csv.lineCount() - 1, "stock", Map.of());
        return csv.build();
    }

    /** Reservations de stock, avec leur etat de cloture. */
    @Transactional
    public String exportReservations(ReservationStatut statut) {
        List<StockReservation> reservations = statut != null
                ? reservationRepository.findByStatutOrderByIdAsc(statut)
                : reservationRepository.findByStatutOrderByIdAsc(ReservationStatut.ACTIVE);

        CsvWriter csv = new CsvWriter()
                .header("ID", "Offre", "Commande", "Quantite", "Statut",
                        "Cree le", "Expire le", "Cloture le", "Motif");

        csv.rows(reservations, r -> new Object[]{
                r.getId(),
                r.getOffer() != null ? r.getOffer().getId() : null,
                r.getCommande() != null ? r.getCommande().getId() : null,
                r.getQuantite(), r.getStatut(),
                r.getCreatedAt(), r.getExpiresAt(), r.getClosedAt(), r.getCloseReason()
        });

        tracer(csv.lineCount() - 1, "reservations",
                statut != null ? Map.of("statut", statut.name()) : Map.of("statut", "ACTIVE"));
        return csv.build();
    }

    /**
     * Journal d'audit lui-meme. Exporter la piste d'audit est un acte
     * sensible : il est trace comme les autres.
     */
    @Transactional
    public String exportAuditLog(String eventType) {
        List<AuditLog> entries = eventType != null && !eventType.isBlank()
                ? auditLogRepository.findRecent(eventType, PageRequest.of(0, LIGNE_MAX))
                : auditLogRepository.findRecent(null, PageRequest.of(0, LIGNE_MAX));

        CsvWriter csv = new CsvWriter()
                .header("ID", "Date", "Type", "Description", "Administrateur", "IP", "Metadonnees");

        csv.rows(entries, a -> new Object[]{
                a.getId(),
                a.getEventTimestamp(),
                a.getEventType(),
                a.getEventDescription(),
                a.getAdministrator() != null ? a.getAdministrator().getPhone() : "systeme",
                a.getIpAddress(),
                a.getMetadata()
        });

        tracer(csv.lineCount() - 1, "audit_log",
                eventType != null && !eventType.isBlank() ? Map.of("eventType", eventType) : Map.of());
        return csv.build();
    }

    // ------------------------------------------------------------------

    /**
     * Trace l'export. Le contenu n'est PAS recopie : l'audit indique
     * qu'une extraction a eu lieu, son volume et ses filtres.
     */
    /**
     * Refuse un export trop volumineux plutot que de le tronquer.
     * Un fichier incomplet est pire qu'une erreur : il passe pour complet.
     */
    private void verifierPlafond(long nbLignes, String type) {
        if (nbLignes > LIGNE_MAX) {
            throw new BusinessRuleException(
                    "Export " + type + " refusé : " + nbLignes + " lignes dépassent la limite de "
                            + LIGNE_MAX + ". Filtrez l'export ou ajoutez des critères.");
        }
    }

    private void tracer(int nbLignes, String type, Map<String, Object> filtres) {        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("typeExport", type);
        metadata.put("nombreLignes", nbLignes);
        metadata.put("filtres", filtres);

        auditService.record(
                EVENT_EXPORT,
                "Export " + type + " (" + nbLignes + " ligne(s))",
                metadata
        );
    }
}
