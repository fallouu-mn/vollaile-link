package com.vollailelink.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vollailelink.backend.model.Administrator;
import com.vollailelink.backend.model.AuditLog;
import com.vollailelink.backend.repository.AdministratorRepository;
import com.vollailelink.backend.repository.AuditLogRepository;
import com.vollailelink.backend.security.AdminPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Journal d'audit metier (CDC section 2.5).
 *
 * Le CDC impose une piste d'audit sur les connexions, les mouvements de
 * stock, les variations de prix, les changements de statuts et les exports.
 * Les connexions sont deja tracees par SecurityAuditService ; ce service
 * couvre les evenements metier.
 *
 * Choix important : l'ecriture d'audit partage la transaction de l'operation
 * metier (propagation REQUIRED). Si l'operation est annulee, l'entree
 * d'audit disparait avec elle, et inversement. On ne peut donc pas avoir
 * un mouvement de stock trace sans etre reellement applique.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    public static final String EVENT_STOCK_ENTREE = "STOCK_ENTREE";
    public static final String EVENT_STOCK_SORTIE = "STOCK_SORTIE";
    public static final String EVENT_STOCK_AJUSTEMENT = "STOCK_AJUSTEMENT";
    public static final String EVENT_STOCK_RESERVATION = "STOCK_RESERVATION";
    public static final String EVENT_STOCK_LIBERATION = "STOCK_LIBERATION";
    public static final String EVENT_STOCK_VENTE = "STOCK_VENTE";
    public static final String EVENT_COMMANDE_STATUT = "COMMANDE_STATUT_CHANGEMENT";
    public static final String EVENT_DEMANDE_STATUT = "DEMANDE_STATUT_CHANGEMENT";
    public static final String EVENT_PRIX_OFFRE = "PRIX_OFFRE_CHANGEMENT";
    public static final String EVENT_PRIX_PRODUIT = "PRIX_PRODUIT_CHANGEMENT";
    public static final String EVENT_OFFRE_STATUT = "OFFRE_STATUT_CHANGEMENT";

    private final AuditLogRepository auditLogRepository;
    private final AdministratorRepository administratorRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enregistre un evenement metier.
     *
     * @param eventType    type stable, en majuscules (ex: STOCK_RESERVATION)
     * @param description  description lisible en français
     * @param metadata     donnees contextuelles serialisees en JSONB
     *                     (ids, quantités, compteurs avant/après)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, String description, Map<String, Object> metadata) {
        AuditLog entry = AuditLog.builder()
                .administrator(currentAdministrator())
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(currentIpAddress())
                .metadata(toJson(metadata))
                .build();

        auditLogRepository.save(entry);
    }

    /**
     * Evenement sans administrateur identifie : traitements automatiques
     * (expiration planifiee des reservations) ou actions systeme.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSystemEvent(String eventType, String description, Map<String, Object> metadata) {
        AuditLog entry = AuditLog.builder()
                .eventType(eventType)
                .eventDescription(description)
                .ipAddress(currentIpAddress())
                .metadata(toJson(metadata))
                .build();

        auditLogRepository.save(entry);
    }

    // ------------------------------------------------------------------

    /**
     * Administrateur connecte, ou null hors contexte securise.
     * L'entite est rechargee pour que la FK soit valide : le principal ne
     * porte qu'un id et un numero de telephone.
     */
    private Administrator currentAdministrator() {
        AdminPrincipal principal = currentPrincipal();
        if (principal == null || principal.getId() == null) {
            return null;
        }
        return administratorRepository.findById(principal.getId()).orElse(null);
    }

    private AdminPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal() instanceof AdminPrincipal principal ? principal : null;
    }

    /**
     * Adresse IP de l'appel HTTP courant, si elle existe.
     * Le planificateur tourne hors requête : le champ reste alors null.
     */
    private String currentIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            // Un échec de sérialisation ne doit pas masquer l'événement :
            // on journalise un marqueur explicite plutôt que de perdre la trace.
            log.error("Serialisation impossible des metadonnees d'audit", e);
            return "{\"erreur\":\"serialisation_impossible\"}";
        }
    }

    /** Raccourci pour construire une carte d'audit lisible et ordonnee. */
    public static Map<String, Object> metadata(Object... clesValeurs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < clesValeurs.length; i += 2) {
            map.put(String.valueOf(clesValeurs[i]), clesValeurs[i + 1]);
        }
        return map;
    }
}
