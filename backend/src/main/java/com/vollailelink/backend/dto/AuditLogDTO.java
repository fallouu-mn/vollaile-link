package com.vollailelink.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Entree du journal d'audit (CDC section 2.5).
 *
 * `metadata` est un objet JSON : il contient le detail technique de
 * l'evenement (identifiants, quantites, compteurs de stock avant/apres).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private Long administratorId;
    private String administratorPhone;
    private String eventType;
    private String eventDescription;
    private String ipAddress;
    private OffsetDateTime eventTimestamp;
    /** Brut JSON, renvoyé tel quel : linterpretation dépend du eventType. */
    private String metadata;
}
