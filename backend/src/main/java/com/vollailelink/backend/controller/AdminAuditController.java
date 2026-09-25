package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.AuditLogDTO;
import com.vollailelink.backend.model.AuditLog;
import com.vollailelink.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consultation de la piste d'audit (CDC section 2.5).
 *
 * Les evenements traces sont les connexions, les mouvements de stock,
 * les changements de statuts de commande et de demande.
 *
 * Acces reserve au back-office : ces entrees exposent des identifiants
 * internes et des compteurs de stock.
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository auditLogRepository;

    /**
     * Journal pagine, du plus recent au plus ancien.
     *
     * @param eventType filtre optionnel (ex: STOCK_RESERVATION)
     */
    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> getAuditLog(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int taille = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNumber = Math.max(page, 0);

        List<AuditLog> entries = auditLogRepository.findRecent(
                eventType, PageRequest.of(pageNumber, taille));

        return ResponseEntity.ok(entries.stream().map(this::toDto).toList());
    }

    /** Journal limite a une offre : permet de rejouer l'historique d'un lot. */
    @GetMapping("/offres/{offreId}")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogByOffer(
            @PathVariable Long offreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        int taille = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        long offset = (long) Math.max(page, 0) * taille;

        List<AuditLog> entries = auditLogRepository.findByOfferInMetadata(
                String.valueOf(offreId), taille, offset);

        return ResponseEntity.ok(entries.stream().map(this::toDto).toList());
    }

    private AuditLogDTO toDto(AuditLog a) {
        return AuditLogDTO.builder()
                .id(a.getId())
                .administratorId(a.getAdministrator() != null ? a.getAdministrator().getId() : null)
                .administratorPhone(a.getAdministrator() != null ? a.getAdministrator().getPhone() : null)
                .eventType(a.getEventType())
                .eventDescription(a.getEventDescription())
                .ipAddress(a.getIpAddress())
                .eventTimestamp(a.getEventTimestamp())
                .metadata(a.getMetadata())
                .build();
    }
}
