package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.ZoneLivraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneLivraisonRepository extends JpaRepository<ZoneLivraison, Long> {
    List<ZoneLivraison> findByEstActiveTrueOrderByNomAsc();
    List<ZoneLivraison> findAllByOrderByNomAsc();
    Optional<ZoneLivraison> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByNom(String nom);
}
