package com.vollailelink.backend.repository;

import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.ReservationStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    Optional<StockReservation> findByIdempotencyKey(String idempotencyKey);

    Optional<StockReservation> findFirstByCommandeIdAndStatut(Long commandeId, ReservationStatut statut);

    List<StockReservation> findByOfferIdAndStatut(Long offerId, ReservationStatut statut);

    List<StockReservation> findByStatutAndExpiresAtBefore(ReservationStatut statut, OffsetDateTime limite);

    List<StockReservation> findByStatutOrderByIdAsc(ReservationStatut statut);
}
