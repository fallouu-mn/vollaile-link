package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.ClientDTO;
import com.vollailelink.backend.dto.DemandeDTO;
import com.vollailelink.backend.exception.InvalidStatusTransitionException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.mapper.DemandeMapper;
import com.vollailelink.backend.model.Client;
import com.vollailelink.backend.model.Demande;
import com.vollailelink.backend.model.Product;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.enums.DemandeStatut;
import com.vollailelink.backend.model.enums.NotificationType;
import com.vollailelink.backend.publicapi.dto.PublicDemandeAcknowledgementDTO;
import com.vollailelink.backend.publicapi.dto.PublicDemandeRequestDTO;
import com.vollailelink.backend.repository.ClientRepository;
import com.vollailelink.backend.repository.DemandeRepository;
import com.vollailelink.backend.repository.ProductRepository;
import com.vollailelink.backend.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeRepository demandeRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final ClientService clientService;
    private final NotificationService notificationService;
    private final DemandeMapper demandeMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<DemandeDTO> getAllDemandes() {
        return demandeRepository.findAll().stream()
                .map(demandeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DemandeDTO getDemandeById(Long id) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id: " + id));
        return demandeMapper.toDto(demande);
    }

    @Transactional
    public PublicDemandeAcknowledgementDTO createPublicDemande(PublicDemandeRequestDTO dto) {
        ClientDTO clientDTO = clientService.getOrCreateClient(
                dto.getNom(),
                dto.getPrenom(),
                dto.getTelephone(),
                dto.getEmail()
        );

        Client client = clientRepository.findById(clientDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        Offer offre = null;
        if (dto.getOffreId() != null) {
            offre = offerRepository.findById(dto.getOffreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Offre sélectionnée non disponible"));
        }

        Product produit = null;
        if (dto.getProduitId() != null) {
            produit = productRepository.findById(dto.getProduitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit sélectionné non disponible"));
        } else if (offre != null) {
            produit = offre.getProduct();
        }

        String nomProduit = dto.getNomProduit();
        if (nomProduit == null || nomProduit.isBlank()) {
            nomProduit = produit != null ? produit.getName() : "Volaille";
        }

        String lieuLivraison = dto.getZoneLivraison();

        Demande demande = Demande.builder()
                .client(client)
                .produit(produit)
                .typeClient(dto.getTypeClient())
                .zoneLivraison(dto.getZoneLivraison())
                .poidsSouhaite(dto.getPoidsSouhaite())
                .adresseLivraison(dto.getAdresseLivraison())
                .preferenceContact(dto.getPreferenceContact())
                .offreId(dto.getOffreId())
                .consentement(Boolean.TRUE.equals(dto.getConsentement()))
                .quantiteSouhaitee(dto.getQuantiteSouhaitee())
                .prixUnitaireSouhaite(dto.getPrixUnitaireSouhaite())
                .nomProduit(nomProduit)
                .description(dto.getMessage())
                .statut(DemandeStatut.EN_COURS)
                .dateSouhaitee(dto.getDateSouhaitee() == null
                        ? null
                        : dto.getDateSouhaitee().atStartOfDay().atOffset(ZoneOffset.UTC))
                .lieuLivraison(lieuLivraison)
                .isActive(true)
                .build();

        Demande saved = demandeRepository.save(demande);

        notificationService.createNotification(
                "Nouvelle demande d'approvisionnement",
                "Demande #" + saved.getId() + " reçue de " + client.getPrenom() + " " + client.getNom() + " (" + client.getTelephone() + ")",
                NotificationType.NOUVELLE_DEMANDE,
                saved.getId(),
                "DEMANDE"
        );

        return PublicDemandeAcknowledgementDTO.builder()
                .reference(String.format(Locale.ROOT, "DEM-%06d", saved.getId()))
                .message("Votre demande a bien été reçue. Notre équipe va vérifier la disponibilité et vous contacter rapidement.")
                .build();
    }

    @Transactional
    public DemandeDTO updateStatut(Long id, DemandeStatut nouveauStatut, String commentairesAdmin) {
        Demande demande = demandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id: " + id));

        validateStatusTransition(demande.getStatut(), nouveauStatut);

        // Capture avant mutation : apres setStatut, l'ancien statut est perdu
        // et la piste d'audit ne retiendrait qu'un « EN_COURS -> EN_COURS ».
        DemandeStatut statutPrecedent = demande.getStatut();

        demande.setStatut(nouveauStatut);
        if (commentairesAdmin != null) {
            demande.setCommentairesAdmin(commentairesAdmin);
        }

        Demande updated = demandeRepository.save(demande);

        // Piste d'audit (CDC 2.5) : changements de statuts.
        auditService.record(
                AuditService.EVENT_DEMANDE_STATUT,
                "Demande #" + updated.getId() + " : " + statutPrecedent + " -> " + nouveauStatut,
                AuditService.metadata(
                        "demandeId", updated.getId(),
                        "statutPrecedent", statutPrecedent != null ? statutPrecedent.name() : null,
                        "statutNouveau", nouveauStatut.name(),
                        "offreId", demande.getOffreId()
                )
        );

        notificationService.createNotification(
                "Changement de statut demande #" + updated.getId(),
                "La demande #" + updated.getId() + " est maintenant: " + nouveauStatut,
                NotificationType.CHANGEMENT_STATUT,
                updated.getId(),
                "DEMANDE"
        );

        return demandeMapper.toDto(updated);
    }

    private void validateStatusTransition(DemandeStatut current, DemandeStatut target) {
        if (current == target) return;
        if (current == DemandeStatut.REFUSEE || current == DemandeStatut.EXPIREE || current == DemandeStatut.SANS_SUITE) {
            throw new InvalidStatusTransitionException("Impossible de modifier une demande déjà terminée en statut: " + current);
        }
    }
}
