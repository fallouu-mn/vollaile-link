package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.MouvementStock;
import com.vollailelink.backend.model.enums.TypeMouvementStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    List<MouvementStock> findByProduitIdOrderByCreatedAtDesc(Long produitId);
    List<MouvementStock> findByTypeMouvement(TypeMouvementStock typeMouvement);

    List<MouvementStock> findByOfferIdOrderByIdDesc(Long offerId);

    List<MouvementStock> findByReservationIdOrderByIdAsc(Long reservationId);

    Optional<MouvementStock> findFirstByReservationIdAndTypeMouvementOrderByIdAsc(
            Long reservationId,
            TypeMouvementStock typeMouvement
    );

    @Query("SELECT m FROM MouvementStock m WHERE m.produit.id = :produitId ORDER BY m.id DESC LIMIT 1")
    Optional<MouvementStock> findLatestByProduitId(@Param("produitId") Long produitId);
}
