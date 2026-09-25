package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.MouvementStockDTO;
import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.dto.StockReservationDTO;
import com.vollailelink.backend.exception.InsufficientStockException;
import com.vollailelink.backend.exception.InvalidStatusTransitionException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.mapper.MouvementStockMapper;
import com.vollailelink.backend.model.Commande;
import com.vollailelink.backend.model.MouvementStock;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.model.enums.ReferenceTypeStock;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.model.enums.TypeMouvementStock;
import com.vollailelink.backend.repository.MouvementStockRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.ProductRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Moteur de stock du projet.
 *
 * Regle unique (CDC section 2.3) :
 * <pre>disponible = total - reserve - vendu</pre>
 *
 * Le stock est porte par l'OFFRE, pas par le produit : deux offres du meme
 * produit (producteurs differents) possedent des stocks distincts.
 *
 * Toutes les ecritures passent par cette classe, dans une transaction,
 * avec un verrou pessimiste (SELECT ... FOR UPDATE) sur la ligne offre.
 * Aucune ecriture directe dans une offre n'est autorisee ailleurs.
 *
 * Les trois evenements distincts, historiquement melanges :
 * <ul>
 *   <li>RESERVATION : reserved += q  -> le disponible baisse, le total non</li>
 *   <li>LIBERATION_RESERVATION : reserved -= q -> le disponible remonte</li>
 *   <li>VENTE : reserved -= q ET sold += q -> le disponible ne bouge pas</li>
 * </ul>
 * C'est ce qui supprime le double decrement de l'ancien modele.
 */
@Slf4j
@Service
public class StockService {

    private final OfferRepository offerRepository;
    private final StockReservationRepository reservationRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final MouvementStockMapper mouvementStockMapper;
    private final AuditService auditService;

    /**
     * Provider du proxy Spring de ce meme bean. Indispensable : un appel
     * direct a {@code this.expireOne(...)} n'ouvrirait aucune transaction,
     * donc l'isolation REQUIRES_NEW de l'expiration serait inoperante.
     * ObjectProvider evite la dependance circulaire a la construction du bean.
     */
    private final ObjectProvider<StockService> selfProvider;

    /**
     * Duree de validite d'une reservation avant liberation automatique.
     * 20 minutes par defaut, conformement au plan de test (docs/TEST_PLAN.md, §4).
     */
    @Value("${stock.reservation.ttl-minutes:20}")
    private long reservationTtlMinutes;

    public StockService(
            OfferRepository offerRepository,
            StockReservationRepository reservationRepository,
            MouvementStockRepository mouvementStockRepository,
            MouvementStockMapper mouvementStockMapper,
            AuditService auditService,
            ObjectProvider<StockService> selfProvider
    ) {
        this.offerRepository = offerRepository;
        this.reservationRepository = reservationRepository;
        this.mouvementStockRepository = mouvementStockRepository;
        this.mouvementStockMapper = mouvementStockMapper;
        this.auditService = auditService;
        this.selfProvider = selfProvider;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public OfferStockDTO getOfferStock(Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée avec l'id: " + offerId));
        return toStockDto(offer);
    }

    @Transactional(readOnly = true)
    public List<OfferStockDTO> getAllOfferStock() {
        return offerRepository.findAll().stream()
                .map(this::toStockDto)
                .collect(Collectors.toList());
    }

    /**
     * stock disponible = total - reserve - vendu (CDC section 2.3).
     * Le total n'est jamais decremente par une reservation : il ne baisse
     * que sur ENTREE (-1), SORTIE (-1), AJUSTEMENT et INVENTAIRE.
     */
    private OfferStockDTO toStockDto(Offer offer) {
        int activeReservations = reservationRepository
                .findByOfferIdAndStatut(offer.getId(), ReservationStatut.ACTIVE)
                .size();

        return OfferStockDTO.builder()
                .offerId(offer.getId())
                .productId(offer.getProduct() != null ? offer.getProduct().getId() : null)
                .productNom(offer.getProduct() != null ? offer.getProduct().getName() : null)
                .producerId(offer.getProducer() != null ? offer.getProducer().getId() : null)
                .quantityTotal(nz(offer.getQuantityTotal()))
                .quantityReserved(nz(offer.getQuantityReserved()))
                .quantitySold(nz(offer.getQuantitySold()))
                .quantityAvailable(offer.getDisponible())
                .activeReservationCount(activeReservations)
                .computedAt(OffsetDateTime.now())
                .unitPrice(offer.getUnitPrice())
                .status(offer.getStatus())
                .startsAt(offer.getStartsAt())
                .expiresAt(offer.getExpiresAt())
                .isNegotiable(offer.getIsNegotiable())
                .description(offer.getDescription())
                .build();
    }

    @Transactional(readOnly = true)
    public List<StockReservationDTO> getActiveReservations() {
        return reservationRepository.findByStatutOrderByIdAsc(ReservationStatut.ACTIVE).stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockReservationDTO> getReservationsByOffer(Long offerId) {
        return reservationRepository.findByOfferIdAndStatut(offerId, ReservationStatut.ACTIVE).stream()
                .map(this::toReservationDto)
                .collect(Collectors.toList());
    }

    private StockReservationDTO toReservationDto(StockReservation r) {
        return StockReservationDTO.builder()
                .id(r.getId())
                .offerId(r.getOffer() != null ? r.getOffer().getId() : null)
                .commandeId(r.getCommande() != null ? r.getCommande().getId() : null)
                .commandeReference(r.getCommande() != null ? "CMD-" + r.getCommande().getId() : null)
                .quantite(r.getQuantite())
                .statut(r.getStatut())
                .expiresAt(r.getExpiresAt())
                .createdAt(r.getCreatedAt())
                .closedAt(r.getClosedAt())
                .closeReason(r.getCloseReason())
                .build();
    }

    // ------------------------------------------------------------------
    // Ecriture : reservation
    // ------------------------------------------------------------------

    /**
     * Reserve une quantite sur une offre.
     *
     * Idempotent : rejouer la meme cle d'idempotence retourne la reservation
     * deja creee au lieu de decrementer le disponible une seconde fois.
     */
    @Transactional
    public StockReservationDTO reserve(Long offerId, int quantite, Commande commande, String idempotencyKey) {
        Optional<StockReservation> existante = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existante.isPresent()) {
            return toReservationDto(existante.get());
        }

        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantité réservée doit être supérieure à 0.");
        }

        Offer offer = lockOffer(offerId);

        if (offer.getStatus() != OfferStatus.ACTIVE) {
            throw new InvalidStatusTransitionException(
                    "Impossible de réserver sur une offre au statut: " + offer.getStatus());
        }

        int disponible = offer.getDisponible();
        if (disponible < quantite) {
            throw new InsufficientStockException(
                    "Stock insuffisant sur l'offre #" + offerId + ". Disponible: " + disponible
                            + ", quantité demandée: " + quantite);
        }

        // Le total ne bouge pas : seule la reservation est incrémentée.
        offer.setQuantityReserved(nz(offer.getQuantityReserved()) + quantite);
        offerRepository.saveAndFlush(offer);

        OffsetDateTime expiration = OffsetDateTime.now()
                .plus(reservationTtlMinutes, ChronoUnit.MINUTES);

        StockReservation reservation = StockReservation.builder()
                .offer(offer)
                .commande(commande)
                .quantite(quantite)
                .statut(ReservationStatut.ACTIVE)
                .expiresAt(expiration)
                .idempotencyKey(idempotencyKey)
                .createdAt(OffsetDateTime.now())
                .build();
        StockReservation saved = reservationRepository.save(reservation);

        writeMovement(offer, saved, TypeMouvementStock.RESERVATION, quantite, "CMD-RES-" + saved.getId(),
                ReferenceTypeStock.COMMANDE,
                "Réservation de " + quantite + " sur l'offre #" + offerId);

        return toReservationDto(saved);
    }

    // ------------------------------------------------------------------
    // Ecriture : consommation / liberation
    // ------------------------------------------------------------------

    /**
     * Convertit une reservation en vente definite (reserved -> sold).
     * Idempotent : rejouer la conversion ne double pas la vente.
     */
    @Transactional
    public StockReservationDTO consume(Long reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id: " + reservationId));

        if (reservation.getStatut() == ReservationStatut.CONSUMED) {
            return toReservationDto(reservation);
        }
        if (reservation.getStatut() != ReservationStatut.ACTIVE) {
            throw new InvalidStatusTransitionException(
                    "Impossible de convertir une réservation au statut " + reservation.getStatut() + " en vente.");
        }

        Offer offer = lockOffer(reservation.getOffer().getId());
        int quantite = reservation.getQuantite();

        // reserved baisse ET sold augmente : le disponible reste inchange.
        offer.setQuantityReserved(nz(offer.getQuantityReserved()) - quantite);
        offer.setQuantitySold(nz(offer.getQuantitySold()) + quantite);
        offerRepository.saveAndFlush(offer);

        reservation.setStatut(ReservationStatut.CONSUMED);
        reservation.setClosedAt(OffsetDateTime.now());
        reservation.setCloseReason("Vente confirmée");
        StockReservation saved = reservationRepository.save(reservation);

        writeMovement(offer, saved, TypeMouvementStock.VENTE, quantite,
                "CMD-VTE-" + reservationId, ReferenceTypeStock.COMMANDE,
                "Vente de " + quantite + " sur l'offre #" + offer.getId());

        return toReservationDto(saved);
    }

    /**
     * Libere une reservation (annulation de commande).
     * Idempotent : liberer deux fois ne restaure pas le stock deux fois.
     */
    @Transactional
    public StockReservationDTO release(Long reservationId, String motif, ReservationStatut statutCloture) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée avec l'id: " + reservationId));

        if (reservation.getStatut() != ReservationStatut.ACTIVE) {
            // Déjà libérée ou déjà consommée : rien à rendre.
            return toReservationDto(reservation);
        }

        Offer offer = lockOffer(reservation.getOffer().getId());
        int quantite = reservation.getQuantite();

        // Seul reserved baisse : le disponible remonte.
        offer.setQuantityReserved(nz(offer.getQuantityReserved()) - quantite);
        offerRepository.saveAndFlush(offer);

        reservation.setStatut(statutCloture);
        reservation.setClosedAt(OffsetDateTime.now());
        reservation.setCloseReason(motif);
        StockReservation saved = reservationRepository.save(reservation);

        writeMovement(offer, saved, TypeMouvementStock.LIBERATION_RESERVATION, quantite,
                "CMD-LIB-" + reservationId, ReferenceTypeStock.COMMANDE, motif);

        return toReservationDto(saved);
    }

    @Transactional
    public StockReservationDTO releaseByCommande(Long commandeId, String motif, ReservationStatut statutCloture) {
        return reservationRepository.findFirstByCommandeIdAndStatut(commandeId, ReservationStatut.ACTIVE)
                .map(r -> release(r.getId(), motif, statutCloture))
                .orElse(null);
    }

    /**
     * Convertit en vente la reservation active d'une commande.
     * Si aucune reservation active n'existe (commande deja consommee, ou
     * commande creee avant la mise en place du moteur de stock), la
     * commande est traitee sans effet de stock plutot que d'echouer.
     */
    @Transactional
    public StockReservationDTO consumeForCommande(Long commandeId) {
        return reservationRepository.findFirstByCommandeIdAndStatut(commandeId, ReservationStatut.ACTIVE)
                .map(r -> consume(r.getId()))
                .orElseGet(() -> {
                    log.info("Commande {} : aucune reservation active, aucune conversion en vente.", commandeId);
                    return null;
                });
    }

    // ------------------------------------------------------------------
    // Ecriture : expiration automatique
    // ------------------------------------------------------------------

    /**
     * Libere les reservations dont l'echeance est depassee.
     *
     * Chaque liberation est traitee dans sa propre transaction ({@code REQUIRES_NEW})
     * pour qu'une reservation problematiche n'annule pas le traitement des suivantes.
     * L'appel passe par le proxy auto-injecte : un appel direct a {@code this}
     * n'ouvrirait pas de nouvelle transaction.
     *
     * @return nombre de reservations liberees
     */
    public int expireOverdueReservations() {
        List<Long> dueIds = reservationRepository
                .findByStatutAndExpiresAtBefore(ReservationStatut.ACTIVE, OffsetDateTime.now())
                .stream()
                .map(StockReservation::getId)
                .toList();

        int liberees = 0;
        for (Long id : dueIds) {
            if (selfProvider.getObject().expireOne(id)) {
                liberees++;
            }
        }
        return liberees;
    }

    /**
     * Traite une reservation expiree dans une transaction isolee.
     * L'erreur est interceptee ici, hors du commit, pour ne pas marquer
     * la transaction comme rollback-only.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireOne(Long reservationId) {
        try {
            release(reservationId, "Expiration automatique de la réservation", ReservationStatut.EXPIRED);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Expiration de la reservation {} impossible : {}", reservationId, ex.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Ecriture : mouvements manuels (entree / sortie / ajustement)
    // ------------------------------------------------------------------

    /**
     * Entree de stock physique : le total augmente, le disponible aussi.
     */
    @Transactional
    public OfferStockDTO entree(Long offerId, int quantite, String motif) {
        return adjustTotal(offerId, quantite, TypeMouvementStock.ENTREE, motif);
    }

    /**
     * Sortie de stock physique : le total baisse.
     * Refuse si le total tombe sous ce qui est deja reserve ou vendu.
     */
    @Transactional
    public OfferStockDTO sortie(Long offerId, int quantite, String motif) {
        return adjustTotal(offerId, -Math.abs(quantite), TypeMouvementStock.SORTIE, motif);
    }

    /**
     * Ajustement d'inventaire avec un delta signe.
     */
    @Transactional
    public OfferStockDTO ajuster(Long offerId, int delta, String motif) {
        return adjustTotal(offerId, delta, TypeMouvementStock.AJUSTEMENT, motif);
    }

    private OfferStockDTO adjustTotal(Long offerId, int delta, TypeMouvementStock type, String motif) {
        Offer offer = lockOffer(offerId);

        int nouveauTotal = nz(offer.getQuantityTotal()) + delta;
        if (nouveauTotal < 0) {
            throw new InsufficientStockException(
                    "Stock total insuffisant sur l'offre #" + offerId + ". Total actuel: "
                            + nz(offer.getQuantityTotal()) + ", ajustement demandé: " + delta);
        }

        int engage = nz(offer.getQuantityReserved()) + nz(offer.getQuantitySold());
        if (nouveauTotal < engage) {
            throw new InsufficientStockException(
                    "Impossible de réduire le total de l'offre #" + offerId + " à " + nouveauTotal
                            + " : " + engage + " unité(s) sont déjà réservées ou vendues.");
        }

        offer.setQuantityTotal(nouveauTotal);
        offerRepository.saveAndFlush(offer);

        writeMovement(offer, null, type, Math.abs(delta),
                "STK-" + type + "-" + offerId + "-" + System.currentTimeMillis(),
                ReferenceTypeStock.AJUSTEMENT,
                motif);

        return toStockDto(offer);
    }

    // ------------------------------------------------------------------
    // Interne
    // ------------------------------------------------------------------

    /**
     * Verrou pessimiste sur la ligne offre : serialize les ecritures concurrentes.
     * Sans ce verrou, deux commandes simultanees lisent le meme disponible
     * et le stock peut etre survendu.
     */
    private Offer lockOffer(Long offerId) {
        return offerRepository.findByIdForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée avec l'id: " + offerId));
    }

    private MouvementStockDTO writeMovement(
            Offer offer,
            StockReservation reservation,
            TypeMouvementStock type,
            int quantite,
            String reference,
            ReferenceTypeStock referenceType,
            String motif
    ) {
        MouvementStock mouvement = MouvementStock.builder()
                .produit(offer.getProduct())
                .offer(offer)
                .reservation(reservation)
                .typeMouvement(type)
                .quantite(quantite)
                .quantiteApresMouvement(offer.getDisponible())
                .reference(reference)
                .referenceType(referenceType)
                .motif(motif)
                .isActive(true)
                .build();
        MouvementStock saved = mouvementStockRepository.save(mouvement);

        // Piste d'audit (CDC 2.5) : tracee dans la MEME transaction que le
        // mouvement, donc impossibles a dissocier.
        auditMovement(saved, offer, type, quantite, motif);

        return mouvementStockMapper.toDto(saved);
    }

    /**
     * Journalise un mouvement de stock dans audit_log.
     * Le detail des compteurs (total / reserve / vendu / disponible) est
     * conserve : il permet de rejouer l'etat du stock a n'importe quelle date.
     */
    private void auditMovement(
            MouvementStock mouvement,
            Offer offer,
            TypeMouvementStock type,
            int quantite,
            String motif
    ) {
        auditService.record(
                auditEventType(type),
                "Mouvement de stock " + type + " de " + quantite + " unité(s) sur l'offre #" + offer.getId()
                        + (motif != null && !motif.isBlank() ? " (" + motif + ")" : ""),
                AuditService.metadata(
                        "mouvementId", mouvement.getId(),
                        "offreId", offer.getId(),
                        "produitId", offer.getProduct() != null ? offer.getProduct().getId() : null,
                        "typeMouvement", type.name(),
                        "quantite", quantite,
                        "quantityTotal", nz(offer.getQuantityTotal()),
                        "quantityReserved", nz(offer.getQuantityReserved()),
                        "quantitySold", nz(offer.getQuantitySold()),
                        "quantityAvailable", offer.getDisponible(),
                        "reference", mouvement.getReference(),
                        "referenceType", mouvement.getReferenceType() != null ? mouvement.getReferenceType().name() : null
                )
        );
    }

    private String auditEventType(TypeMouvementStock type) {
        return switch (type) {
            case ENTREE -> AuditService.EVENT_STOCK_ENTREE;
            case SORTIE -> AuditService.EVENT_STOCK_SORTIE;
            case AJUSTEMENT, INVENTAIRE -> AuditService.EVENT_STOCK_AJUSTEMENT;
            case RESERVATION -> AuditService.EVENT_STOCK_RESERVATION;
            case LIBERATION_RESERVATION -> AuditService.EVENT_STOCK_LIBERATION;
            case VENTE -> AuditService.EVENT_STOCK_VENTE;
        };
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * Verifie qu'une commande peut transitionner vers le statut cible.
     * Workflow : EN_ATTENTE_PAIEMENT -> PAYEE -> EN_PREPARATION -> EXPEDIEE -> LIVREE
     *          ANNULEE et EXPIREE sont terminaux.
     */
    public static boolean transitionAutorisee(CommandeStatut current, CommandeStatut target) {
        if (current == target) {
            return true;
        }
        return switch (current) {
            case EN_ATTENTE_PAIEMENT -> target == CommandeStatut.PAYEE
                    || target == CommandeStatut.ANNULEE
                    || target == CommandeStatut.EXPIREE;
            case PAYEE -> target == CommandeStatut.EN_PREPARATION
                    || target == CommandeStatut.ANNULEE;
            case EN_PREPARATION -> target == CommandeStatut.EXPEDIEE
                    || target == CommandeStatut.ANNULEE;
            case EXPEDIEE -> target == CommandeStatut.LIVREE
                    || target == CommandeStatut.ANNULEE;
            case LIVREE, ANNULEE, EXPIREE -> false;
        };
    }
}
