package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.Demande;
import com.vollailelink.backend.model.enums.DemandeStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface DemandeRepository extends JpaRepository<Demande, Long> {
    List<Demande> findByClientId(Long clientId);
    List<Demande> findByStatut(DemandeStatut statut);

    long countByStatut(DemandeStatut statut);

    long countByCreatedAtAfter(OffsetDateTime date);
}
