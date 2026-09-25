package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.ZoneLivraisonDTO;
import com.vollailelink.backend.exception.DuplicateResourceException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.model.ZoneLivraison;
import com.vollailelink.backend.repository.ZoneLivraisonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Zones de livraison desservies.
 *
 * L'ecran existait dans la navigation du back-office mais ne pouvait rien
 * charger : ni la table ni l'API n'existaient. Cette classe comble l'ecart.
 */
@Service
@RequiredArgsConstructor
public class ZoneLivraisonService {

    private final ZoneLivraisonRepository zoneLivraisonRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ZoneLivraisonDTO> getAllZones() {
        return zoneLivraisonRepository.findAllByOrderByNomAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ZoneLivraisonDTO getZoneById(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public ZoneLivraisonDTO createZone(ZoneLivraisonDTO dto) {
        String code = normalize(dto.getCode());
        String nom = dto.getNom() == null ? null : dto.getNom().trim();

        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom de la zone est obligatoire.");
        }
        if (zoneLivraisonRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Une zone avec le code " + code + " existe déjà.");
        }
        if (zoneLivraisonRepository.existsByNom(nom)) {
            throw new DuplicateResourceException("Une zone nommée « " + nom + " » existe déjà.");
        }

        validateDelai(dto.getDelaiMinHeures(), dto.getDelaiMaxHeures());
        validateFrais(dto.getFraisLivraison());

        ZoneLivraison zone = ZoneLivraison.builder()
                .nom(nom)
                .code(code)
                .description(dto.getDescription())
                .fraisLivraison(dto.getFraisLivraison() != null ? dto.getFraisLivraison() : java.math.BigDecimal.ZERO)
                .delaiMinHeures(dto.getDelaiMinHeures() != null ? dto.getDelaiMinHeures() : 24)
                .delaiMaxHeures(dto.getDelaiMaxHeures() != null ? dto.getDelaiMaxHeures() : 72)
                .estActive(dto.getEstActive() != null ? dto.getEstActive() : true)
                .build();

        ZoneLivraison saved = zoneLivraisonRepository.save(zone);

        auditService.record(
                "ZONE_LIVRAISON_CREEE",
                "Zone de livraison créée : " + saved.getNom() + " (" + saved.getCode() + ")",
                AuditService.metadata(
                        "zoneId", saved.getId(),
                        "code", saved.getCode(),
                        "fraisLivraison", saved.getFraisLivraison()
                )
        );

        return toDto(saved);
    }

    @Transactional
    public ZoneLivraisonDTO updateZone(Long id, ZoneLivraisonDTO dto) {
        ZoneLivraison zone = require(id);
        String code = normalize(dto.getCode());

        if (!zone.getCode().equalsIgnoreCase(code) && zoneLivraisonRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Une zone avec le code " + code + " existe déjà.");
        }
        if (dto.getNom() != null && !dto.getNom().isBlank()
                && !dto.getNom().trim().equalsIgnoreCase(zone.getNom())
                && zoneLivraisonRepository.existsByNom(dto.getNom().trim())) {
            throw new DuplicateResourceException("Une zone nommée « " + dto.getNom().trim() + " » existe déjà.");
        }

        if (dto.getDelaiMinHeures() != null) zone.setDelaiMinHeures(dto.getDelaiMinHeures());
        if (dto.getDelaiMaxHeures() != null) zone.setDelaiMaxHeures(dto.getDelaiMaxHeures());
        validateDelai(zone.getDelaiMinHeures(), zone.getDelaiMaxHeures());

        if (dto.getFraisLivraison() != null) {
            validateFrais(dto.getFraisLivraison());
            zone.setFraisLivraison(dto.getFraisLivraison());
        }
        if (dto.getNom() != null && !dto.getNom().isBlank()) zone.setNom(dto.getNom().trim());
        if (dto.getDescription() != null) zone.setDescription(dto.getDescription());
        if (dto.getEstActive() != null) zone.setEstActive(dto.getEstActive());
        zone.setCode(code);

        ZoneLivraison saved = zoneLivraisonRepository.save(zone);

        auditService.record(
                "ZONE_LIVRAISON_MODIFIEE",
                "Zone de livraison modifiée : " + saved.getNom() + " (" + saved.getCode() + ")",
                AuditService.metadata(
                        "zoneId", saved.getId(),
                        "code", saved.getCode(),
                        "fraisLivraison", saved.getFraisLivraison(),
                        "estActive", saved.getEstActive()
                )
        );

        return toDto(saved);
    }

    @Transactional
    public void deleteZone(Long id) {
        ZoneLivraison zone = require(id);
        zoneLivraisonRepository.delete(zone);

        auditService.record(
                "ZONE_LIVRAISON_SUPPRIMEE",
                "Zone de livraison supprimée : " + zone.getNom() + " (" + zone.getCode() + ")",
                AuditService.metadata("zoneId", id, "code", zone.getCode())
        );
    }

    // ------------------------------------------------------------------

    private void validateDelai(Integer min, Integer max) {
        if (min != null && max != null && max < min) {
            throw new IllegalArgumentException(
                    "Le délai maximal (" + max + " h) ne peut pas être inférieur au délai minimal (" + min + " h).");
        }
    }

    private void validateFrais(java.math.BigDecimal frais) {
        if (frais != null && frais.signum() < 0) {
            throw new IllegalArgumentException("Les frais de livraison ne peuvent pas être négatifs.");
        }
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code de la zone est obligatoire.");
        }
        return code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private ZoneLivraison require(Long id) {
        return zoneLivraisonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone de livraison non trouvée: " + id));
    }

    private ZoneLivraisonDTO toDto(ZoneLivraison z) {
        return ZoneLivraisonDTO.builder()
                .id(z.getId())
                .nom(z.getNom())
                .code(z.getCode())
                .description(z.getDescription())
                .fraisLivraison(z.getFraisLivraison())
                .delaiMinHeures(z.getDelaiMinHeures())
                .delaiMaxHeures(z.getDelaiMaxHeures())
                .estActive(z.getEstActive())
                .createdAt(z.getCreatedAt())
                .updatedAt(z.getUpdatedAt())
                .build();
    }
}
