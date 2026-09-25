package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAdministratorIdOrderByEventTimestampDesc(Long administratorId);

    List<AuditLog> findByEventTypeOrderByEventTimestampDesc(String eventType);

    /** Pagine du plus recent au plus ancien, avec filtrage optionnel par type. */
    @Query("SELECT a FROM AuditLog a "
            + "WHERE (:eventType IS NULL OR a.eventType = :eventType) "
            + "ORDER BY a.eventTimestamp DESC, a.id DESC")
    List<AuditLog> findRecent(@Param("eventType") String eventType, Pageable pageable);

    /**
     * Piste d'audit d'une offre : toutes les operations qui l'ont concernee.
     *
     * Requete native car metadata est un JSONB : on interroge la cle
     * 'offreId' de maniere exacte. Un simple LIKE serait faux
     * (l'offre 1 matcherait aussi les offres 12 ou 21).
     */
    @Query(value = "SELECT * FROM audit_log "
            + "WHERE metadata ->> 'offreId' = :offreId "
            + "ORDER BY event_timestamp DESC, id DESC "
            + "LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<AuditLog> findByOfferInMetadata(
            @Param("offreId") String offreId,
            @Param("limit") int limit,
            @Param("offset") long offset
    );
}
