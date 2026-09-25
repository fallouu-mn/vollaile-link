package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.CommandeDTO;
import com.vollailelink.backend.exception.InvalidStatusTransitionException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.mapper.CommandeMapper;
import com.vollailelink.backend.model.Client;
import com.vollailelink.backend.model.Commande;
import com.vollailelink.backend.model.Demande;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.NotificationType;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.ClientRepository;
import com.vollailelink.backend.repository.CommandeRepository;
import com.vollailelink.backend.repository.DemandeRepository;
import com.vollailelink.backend.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cycle de vie des commandes.
 *
 * Une commande est toujours liee a une OFFRE, et la reservation de stock
 * suit l'etat de la commande :
 * <pre>
 *   creation        -> RESERVATION           (disponible baisse)
 *   ANNULEE         -> LIBERATION_RESERVATION (disponible remonte)
 *   expiration      -> LIBERATION_RESERVATION (disponible remonte)
 *   LIVREE          -> VENTE                  (reserve -> vendu)
 * </pre>
 *
 * Chaque transition est idempotente cote stock : un double appel ne
 * decremente ni ne restaure deux fois la meme quantite.
 */
@Service
@RequiredArgsConstructor
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final ClientRepository clientRepository;
    private final DemandeRepository demandeRepository;
    private final OfferRepository offerRepository;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final CommandeMapper commandeMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CommandeDTO> getAllCommandes() {
        return commandeRepository.findAll().stream()
                .map(commandeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommandeDTO getCommandeById(Long id) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'id: " + id));
        return commandeMapper.toDto(commande);
    }

    @Transactional
    public CommandeDTO createCommande(CommandeDTO dto) {
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id: " + dto.getClientId()));

        if (dto.getQuantite() == null || dto.getQuantite() <= 0) {
            throw new IllegalArgumentException("La quantité commandée doit être supérieure à 0.");
        }

        // L'offre designe le lot reellement commande : deux offres du meme
        // produit (producteurs differents) ont des stocks distincts.
        Offer offre = resolveOffre(dto);

        Demande demande = null;
        if (dto.getDemandeId() != null) {
            demande = demandeRepository.findById(dto.getDemandeId()).orElse(null);
        }

        BigDecimal prixUnitaire = dto.getPrixUnitaire() != null
                ? dto.getPrixUnitaire()
                : offre.getUnitPrice();

        BigDecimal grossTotal = prixUnitaire.multiply(BigDecimal.valueOf(dto.getQuantite()));
        BigDecimal remisePctAmount = BigDecimal.ZERO;
        if (dto.getRemisePourcentage() != null && dto.getRemisePourcentage().compareTo(BigDecimal.ZERO) > 0) {
            remisePctAmount = grossTotal
                    .multiply(dto.getRemisePourcentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal totalRemise = remisePctAmount
                .add(dto.getRemiseMontant() != null ? dto.getRemiseMontant() : BigDecimal.ZERO);
        BigDecimal totalHt = grossTotal.subtract(totalRemise).max(BigDecimal.ZERO);

        Commande commande = Commande.builder()
                .demande(demande)
                .client(client)
                .produit(offre.getProduct())
                .offre(offre)
                .quantite(dto.getQuantite())
                .prixUnitaire(prixUnitaire)
                .prixUnitaireAchat(dto.getPrixUnitaireAchat())
                .remisePourcentage(dto.getRemisePourcentage() != null ? dto.getRemisePourcentage() : BigDecimal.ZERO)
                .remiseMontant(dto.getRemiseMontant() != null ? dto.getRemiseMontant() : BigDecimal.ZERO)
                .totalHt(totalHt)
                .totalTtc(totalHt)
                .statut(CommandeStatut.EN_ATTENTE_PAIEMENT)
                .modePaiement(dto.getModePaiement())
                .dateLivraisonPrev(dto.getDateLivraisonPrev() != null
                        ? dto.getDateLivraisonPrev()
                        : OffsetDateTime.now().plusDays(2))
                .lieuLivraison(dto.getLieuLivraison())
                .commentaires(dto.getCommentaires())
                .isActive(true)
                .build();

        Commande saved = commandeRepository.saveAndFlush(commande);

        // La reservation est creee dans la meme transaction que la commande :
        // si le stock est insuffisant, la commande est annulee aussi.
        stockService.reserve(offre.getId(), dto.getQuantite(), saved,
                "CMD-" + saved.getId() + ":RESERVE");

        notificationService.createNotification(
                "Nouvelle commande #" + saved.getId(),
                "Commande de " + dto.getQuantite() + " " + offre.getProduct().getName()
                        + " pour " + client.getPrenom() + " " + client.getNom(),
                NotificationType.COMMANDE_CONFIRMEE,
                saved.getId(),
                "COMMANDE"
        );

        return commandeMapper.toDto(saved);
    }

    @Transactional
    public CommandeDTO updateStatut(Long id, CommandeStatut nouveauStatut) {
        Commande commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande non trouvée avec l'id: " + id));

        CommandeStatut current = commande.getStatut();

        if (!StockService.transitionAutorisee(current, nouveauStatut)) {
            throw new InvalidStatusTransitionException(
                    "Transition de statut non autorisée: " + current + " -> " + nouveauStatut);
        }

        // Meme statut : on ne rejoue pas l'effet sur le stock.
        if (current != nouveauStatut) {
            appliquerEffetStock(commande, nouveauStatut);
            commande.setStatut(nouveauStatut);
            commandeRepository.save(commande);

            // Piste d'audit (CDC 2.5) : changements de statuts.
            auditService.record(
                    AuditService.EVENT_COMMANDE_STATUT,
                    "Commande #" + commande.getId() + " : " + current + " -> " + nouveauStatut,
                    AuditService.metadata(
                            "commandeId", commande.getId(),
                            "statutPrecedent", current.name(),
                            "statutNouveau", nouveauStatut.name(),
                            "offreId", commande.getOffre() != null ? commande.getOffre().getId() : null,
                            "quantite", commande.getQuantite()
                    )
            );
        }

        notificationService.createNotification(
                "Changement de statut commande #" + commande.getId(),
                "La commande #" + commande.getId() + " passe en statut: " + nouveauStatut,
                NotificationType.CHANGEMENT_STATUT,
                commande.getId(),
                "COMMANDE"
        );

        return commandeMapper.toDto(commande);
    }

    /**
     * Traduit la transition de commande en mouvement de stock.
     * Les effets sont idempotants : une reservation deja consommee
     * ou deja liberee ne bouge pas le stock une seconde fois.
     */
    private void appliquerEffetStock(Commande commande, CommandeStatut nouveauStatut) {
        switch (nouveauStatut) {
            case ANNULEE -> stockService.releaseByCommande(
                    commande.getId(),
                    "Libération du stock suite annulation commande #" + commande.getId(),
                    ReservationStatut.RELEASED);
            case EXPIREE -> stockService.releaseByCommande(
                    commande.getId(),
                    "Libération du stock suite expiration commande #" + commande.getId(),
                    ReservationStatut.EXPIRED);
            case LIVREE -> {
                stockService.consumeForCommande(commande.getId());
                commande.setDateLivraisonReelle(OffsetDateTime.now());
            }
            default -> {
                // EN_ATTENTE_PAIEMENT, PAYEE, EN_PREPARATION, EXPEDIEE :
                // la reservation reste active, le disponible ne bouge pas.
            }
        }
    }

    /**
     * Resout l'offre commandee.
     * Si l'appelant n'en fournit pas, on tente de deduire une offre unique
     * et active pour le produit : on refuse d'inventer un lot au hasard.
     */
    private Offer resolveOffre(CommandeDTO dto) {
        if (dto.getOffreId() != null) {
            return offerRepository.findById(dto.getOffreId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Offre non trouvée avec l'id: " + dto.getOffreId()));
        }

        if (dto.getProduitId() == null) {
            throw new IllegalArgumentException(
                    "Une offre (offreId) ou un produit (produitId) est requis pour créer une commande.");
        }

        List<Offer> candidates = offerRepository.findByProductIdAndStatus(
                dto.getProduitId(), com.vollailelink.backend.model.enums.OfferStatus.ACTIVE);

        if (candidates.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Aucune offre active pour le produit " + dto.getProduitId());
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException(
                    "Plusieurs offres actives existent pour ce produit : specifyz offreId pour choisir le lot.");
        }
        return candidates.get(0);
    }
}
