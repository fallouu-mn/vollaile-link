package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.dto.PriceHistoryDTO;
import com.vollailelink.backend.exception.BusinessRuleException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.PriceHistory;
import com.vollailelink.backend.model.Product;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.AdministratorRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.PriceHistoryRepository;
import com.vollailelink.backend.repository.ProductRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import com.vollailelink.backend.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestion des prix et du cycle de vie des offres.
 *
 * Le CDC (section 2.5) impose une piste d'audit sur les variations de prix.
 * Cette classe est le SEUL point d'ecriture des prix : toute modification
 * enregistre automatiquement une ligne dans `price_history` ET une entree
 * dans `audit_log`, dans la meme transaction.
 *
 * Regle metier : une reservation creee conserve le prix convenu au moment
 * de la commande (PriceHistory est fige par la ligne de commande), donc
 * modifier le prix d'une offre ne modifie pas le prix des commandes en cours.
 */
@Service
@RequiredArgsConstructor
public class PriceService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockReservationRepository reservationRepository;
    private final AdministratorRepository administratorRepository;
    private final StockService stockService;
    private final AuditService auditService;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PriceHistoryDTO> getProductPriceHistory(Long productId) {
        return priceHistoryRepository.findByProductIdOrderByChangedAtDesc(productId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Historique des prix d'une offre.
     *
     * `price_history` est historiquement indexee par produit (contrainte
     * existante non modifiable sans migration). On retourne donc l'historique
     * du produit de l'offre, et l'endpoint precise dans sa documentation que
     * le perimetre est le produit.
     */
    @Transactional(readOnly = true)
    public List<PriceHistoryDTO> getOfferPriceHistory(Long offerId) {
        Offer offer = requireOffer(offerId);
        return priceHistoryRepository
                .findByProductIdOrderByChangedAtDesc(offer.getProduct().getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Ecriture : prix
    // ------------------------------------------------------------------

    /**
     * Change le prix de vente d'une offre.
     *
     * Le prix est incremente de 0.01 FCFA si l'offre n'en a pas encore
     * (colonne NOT NULL) : on part du prix produit comme reference.
     *
     * @return etat du stock de l'offre apres modification
     */
    @Transactional
    public OfferStockDTO updateOfferPrice(Long offerId, BigDecimal nouveauPrix, String motif) {
        if (nouveauPrix == null || nouveauPrix.signum() < 0) {
            throw new BusinessRuleException(
                    "Le prix doit être positif ou nul (reçu: " + nouveauPrix + " FCFA).");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException(
                    "Un motif est obligatoire pour justifier une variation de prix (traçabilité §2.5 du CDC).");
        }

        Offer offer = requireOffer(offerId);
        BigDecimal ancienPrix = offer.getUnitPrice();

        // Prix inchangé : on n'ecrit ni historique ni audit (rien à tracer).
        if (ancienPrix != null && ancienPrix.compareTo(nouveauPrix) == 0) {
            return stockService.getOfferStock(offerId);
        }

        offer.setUnitPrice(nouveauPrix);
        offerRepository.save(offer);

        recordPriceChange(
                offer.getProduct(),
                ancienPrix,
                nouveauPrix,
                motif,
                offer.getId()
        );

        return stockService.getOfferStock(offerId);
    }

    /**
     * Change le prix par defaut d'un produit (prix indicatif catalogue).
     * N'agit pas sur les offres existantes : chaque offre porte son prix.
     */
    @Transactional
    public PriceHistoryDTO updateProductDefaultPrice(Long productId, BigDecimal nouveauPrix, String motif) {
        if (nouveauPrix == null || nouveauPrix.signum() < 0) {
            throw new BusinessRuleException(
                    "Le prix doit être positif ou nul (reçu: " + nouveauPrix + " FCFA).");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessRuleException(
                    "Un motif est obligatoire pour justifier une variation de prix (traçabilité §2.5 du CDC).");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'id: " + productId));

        BigDecimal ancienPrix = product.getDefaultUnitPrice();
        if (ancienPrix != null && ancienPrix.compareTo(nouveauPrix) == 0) {
            return getProductPriceHistory(productId).stream().findFirst().orElse(null);
        }

        product.setDefaultUnitPrice(nouveauPrix);
        productRepository.save(product);

        PriceHistory saved = recordPriceChange(product, ancienPrix, nouveauPrix, motif, null);
        return toDto(saved);
    }

    // ------------------------------------------------------------------
    // Ecriture : cycle de vie des offres
    // ------------------------------------------------------------------

    /**
     * Change le statut d'une offre.
     * Retirer une offre ACTIVE avec des reservations actives est refuse :
     * cela laisserait des commandes sans stock engage.
     */
    @Transactional
    public OfferStockDTO updateOfferStatus(Long offerId, OfferStatus nouveauStatut, String motif) {
        Offer offer = requireOffer(offerId);
        OfferStatus ancienStatut = offer.getStatus();

        if (ancienStatut == nouveauStatut) {
            return stockService.getOfferStock(offerId);
        }

        long reservationsActives = reservationRepository
                .findByOfferIdAndStatut(offerId, ReservationStatut.ACTIVE)
                .size();

        if (nouveauStatut != OfferStatus.ACTIVE && ancienStatut == OfferStatus.ACTIVE && reservationsActives > 0) {
            throw new BusinessRuleException(
                    "Impossible de retirer l'offre #" + offerId + " : " + reservationsActives
                            + " réservation(s) active(s) lui sont rattachées. "
                            + "Libérez ou livrez les commandes concernées avant.");
        }

        offer.setStatus(nouveauStatut);
        offerRepository.save(offer);

        auditService.record(
                AuditService.EVENT_OFFRE_STATUT,
                "Offre #" + offerId + " : " + ancienStatut + " -> " + nouveauStatut
                        + (motif != null && !motif.isBlank() ? " (" + motif + ")" : ""),
                AuditService.metadata(
                        "offreId", offerId,
                        "produitId", offer.getProduct() != null ? offer.getProduct().getId() : null,
                        "statutPrecedent", ancienStatut.name(),
                        "statutNouveau", nouveauStatut.name(),
                        "reservationsActives", reservationsActives
                )
        );

        return stockService.getOfferStock(offerId);
    }

    // ------------------------------------------------------------------
    // Interne
    // ------------------------------------------------------------------

    /**
     * Ecrit la ligne d'historique ET l'entree d'audit.
     * Les deux partagent la transaction courante : impossible d'avoir un
     * prix modifie sans trace, ou une trace sans modification.
     */
    private PriceHistory recordPriceChange(
            Product product,
            BigDecimal ancienPrix,
            BigDecimal nouveauPrix,
            String motif,
            Long offreId
    ) {
        PriceHistory history = PriceHistory.builder()
                .product(product)
                .price(nouveauPrix)
                .changedBy(currentAdministrator())
                .reason(motif)
                .build();
        PriceHistory saved = priceHistoryRepository.save(history);

        BigDecimal variation = ancienPrix == null
                ? nouveauPrix
                : nouveauPrix.subtract(ancienPrix);

        auditService.record(
                offreId != null ? AuditService.EVENT_PRIX_OFFRE : AuditService.EVENT_PRIX_PRODUIT,
                (offreId != null ? "Prix de l'offre #" + offreId : "Prix par défaut du produit #" + product.getId())
                        + " : " + format(ancienPrix) + " -> " + format(nouveauPrix) + " FCFA",
                AuditService.metadata(
                        "offreId", offreId,
                        "produitId", product.getId(),
                        "priceHistoryId", saved.getId(),
                        "ancienPrix", ancienPrix,
                        "nouveauPrix", nouveauPrix,
                        "variation", variation,
                        "variationPourcent", variationPourcent(ancienPrix, nouveauPrix),
                        "motif", motif
                )
        );

        return saved;
    }

    private static BigDecimal variationPourcent(BigDecimal ancien, BigDecimal nouveau) {
        if (ancien == null || ancien.signum() == 0) {
            return null;
        }
        return nouveau.subtract(ancien)
                .multiply(BigDecimal.valueOf(100))
                .divide(ancien, 2, java.math.RoundingMode.HALF_UP);
    }

    private static String format(BigDecimal value) {
        if (value == null) {
            return "non défini";
        }
        // Les montants sont en FCFA : pas de decimales utiles.
        return value.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private Administrator currentAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if (authentication.getPrincipal() instanceof AdminPrincipal principal && principal.getId() != null) {
            return administratorRepository.findById(principal.getId()).orElse(null);
        }
        return null;
    }

    private Offer requireOffer(Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre non trouvée avec l'id: " + offerId));
    }

    private PriceHistoryDTO toDto(PriceHistory h) {
        return PriceHistoryDTO.builder()
                .id(h.getId())
                .productId(h.getProduct() != null ? h.getProduct().getId() : null)
                .productNom(h.getProduct() != null ? h.getProduct().getName() : null)
                .price(h.getPrice())
                .changedBy(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByPhone(h.getChangedBy() != null ? h.getChangedBy().getPhone() : null)
                .changedAt(h.getChangedAt())
                .reason(h.getReason())
                .build();
    }
}
