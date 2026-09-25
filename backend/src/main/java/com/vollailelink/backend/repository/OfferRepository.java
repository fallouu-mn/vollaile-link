package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.enums.OfferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    List<Offer> findByStatus(OfferStatus status);
    List<Offer> findByProductIdAndStatus(Long productId, OfferStatus status);

    long countByStatus(OfferStatus status);

    /**
     * Stock disponible cumule sur les offres actives.
     * quantity_available est une colonne GENERATED : la somme se fait en base,
     * sans charger les offres en memoire.
     */
    @Query("SELECT COALESCE(SUM(o.quantityAvailable), 0) FROM Offer o WHERE o.status = :status")
    long sumAvailableByStatus(@Param("status") OfferStatus status);

    /** Offres actives de plusieurs produits : evite le N+1 sur le catalogue. */
    List<Offer> findByProductIdInAndStatus(Collection<Long> productIds, OfferStatus status);

    /** Total engage (reserve + vendu) sur les offres actives. */
    @Query("SELECT COALESCE(SUM(o.quantityReserved), 0) FROM Offer o WHERE o.status = :status")
    long sumReservedByStatus(@Param("status") OfferStatus status);

    @Query("SELECT COALESCE(SUM(o.quantitySold), 0) FROM Offer o WHERE o.status = :status")
    long sumSoldByStatus(@Param("status") OfferStatus status);

    /**
     * Verrou pessimiste d'ecriture sur la ligne offre (SELECT ... FOR UPDATE).
     * C'est ce verrou qui rend la reservation atomique : deux commandes
     * concurrentes sur la meme offre sont serialisees, donc la seconde
     * relit le compteur reserve deja mis a jour et ne peut pas survendre.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offer o WHERE o.id = :id")
    Optional<Offer> findByIdForUpdate(@Param("id") Long id);
}
