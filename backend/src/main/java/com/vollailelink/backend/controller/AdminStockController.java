package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.MouvementStockDTO;
import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.dto.StockReservationDTO;
import com.vollailelink.backend.mapper.StockReservationMapper;
import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.StockReservationRepository;
import com.vollailelink.backend.service.MouvementStockService;
import com.vollailelink.backend.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Back-office stock.
 *
 * Toutes les mutations passent par StockService, qui applique
 * disponible = total - reserve - vendu sous verrou pessimiste.
 */
@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class AdminStockController {

    private final StockService stockService;
    private final MouvementStockService mouvementStockService;
    private final StockReservationRepository reservationRepository;
    private final StockReservationMapper reservationMapper;

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @GetMapping("/offres")
    public ResponseEntity<List<OfferStockDTO>> getAllOfferStock() {
        return ResponseEntity.ok(stockService.getAllOfferStock());
    }

    @GetMapping("/offres/{offerId}")
    public ResponseEntity<OfferStockDTO> getOfferStock(@PathVariable Long offerId) {
        return ResponseEntity.ok(stockService.getOfferStock(offerId));
    }

    @GetMapping("/mouvements")
    public ResponseEntity<List<MouvementStockDTO>> getMouvements(
            @RequestParam(required = false) Long produitId,
            @RequestParam(required = false) Long offerId,
            @RequestParam(required = false) Long reservationId
    ) {
        if (reservationId != null) {
            return ResponseEntity.ok(mouvementStockService.getMouvementsByReservation(reservationId));
        }
        if (offerId != null) {
            return ResponseEntity.ok(mouvementStockService.getMouvementsByOffer(offerId));
        }
        if (produitId != null) {
            return ResponseEntity.ok(mouvementStockService.getMouvementsByProduit(produitId));
        }
        return ResponseEntity.ok(mouvementStockService.getAllMouvements());
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<StockReservationDTO>> getReservations(
            @RequestParam(required = false) Long offerId
    ) {
        if (offerId != null) {
            return ResponseEntity.ok(stockService.getReservationsByOffer(offerId));
        }
        return ResponseEntity.ok(stockService.getActiveReservations());
    }

    // ------------------------------------------------------------------
    // Ecriture : mouvements physiques de stock
    // ------------------------------------------------------------------

    @PostMapping("/offres/{offerId}/entree")
    public ResponseEntity<OfferStockDTO> entree(
            @PathVariable Long offerId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return ResponseEntity.ok(stockService.entree(offerId, request.getQuantite(), request.getMotif()));
    }

    @PostMapping("/offres/{offerId}/sortie")
    public ResponseEntity<OfferStockDTO> sortie(
            @PathVariable Long offerId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return ResponseEntity.ok(stockService.sortie(offerId, request.getQuantite(), request.getMotif()));
    }

    @PostMapping("/offres/{offerId}/ajustement")
    public ResponseEntity<OfferStockDTO> ajuster(
            @PathVariable Long offerId,
            @Valid @RequestBody StockMovementRequest request
    ) {
        return ResponseEntity.ok(stockService.ajuster(offerId, request.getQuantite(), request.getMotif()));
    }

    // ------------------------------------------------------------------
    // Ecriture : reservations
    // ------------------------------------------------------------------

    @PostMapping("/reservations/{reservationId}/liberer")
    public ResponseEntity<StockReservationDTO> liberer(
            @PathVariable Long reservationId,
            @RequestBody(required = false) ReleaseRequest request
    ) {
        String motif = (request != null && request.getMotif() != null && !request.getMotif().isBlank())
                ? request.getMotif()
                : "Libération manuelle de la réservation #" + reservationId;

        return ResponseEntity.ok(
                stockService.release(reservationId, motif, ReservationStatut.RELEASED));
    }

    @PostMapping("/reservations/expire")
    public ResponseEntity<Map<String, Object>> expire() {
        int liberees = stockService.expireOverdueReservations();
        return ResponseEntity.ok(Map.of("reservationsLiberees", liberees));
    }

    @GetMapping("/reservations/{reservationId}")
    public ResponseEntity<StockReservationDTO> getReservation(@PathVariable Long reservationId) {
        StockReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée: " + reservationId));
        return ResponseEntity.ok(reservationMapper.toDto(reservation));
    }

    // ------------------------------------------------------------------
    // DTO de requete
    // ------------------------------------------------------------------

    @Data
    public static class StockMovementRequest {
        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité doit être supérieure à 0")
        private Integer quantite;

        private String motif;
    }

    @Data
    public static class ReleaseRequest {
        private String motif;
    }
}
