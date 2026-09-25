package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.Commande;
import com.vollailelink.backend.model.enums.CommandeStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByClientId(Long clientId);
    List<Commande> findByStatut(CommandeStatut statut);

    long countByStatut(CommandeStatut statut);
}
