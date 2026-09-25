package com.vollailelink.backend.controller;

import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.dto.PriceHistoryDTO;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.service.PriceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Back-office prix et offres.
 *
 * Toute variation de prix est automatiquement historisée dans
 * `price_history` et tracée dans `audit_log` (CDC section 2.5).
 */
@RestController
@RequestMapping("/api/admin/offres")
@RequiredArgsConstructor
public class AdminOfferController {

    private final PriceService priceService;

    // --- Lecture des historiques ---

    /** Historique des prix d'un produit. */
    @GetMapping("/produits/{productId}/prix")
    public ResponseEntity<List<PriceHistoryDTO>> getProductPriceHistory(@PathVariable Long productId) {
        return ResponseEntity.ok(priceService.getProductPriceHistory(productId));
    }

    /**
     * Historique des prix rattaches a une offre.
     * Attention : la table `price_history` est indexee par produit,
     * le retour porte donc sur le produit de l'offre.
     */
    @GetMapping("/{offerId}/prix")
    public ResponseEntity<List<PriceHistoryDTO>> getOfferPriceHistory(@PathVariable Long offerId) {
        return ResponseEntity.ok(priceService.getOfferPriceHistory(offerId));
    }

    // --- Ecriture ---

    @PatchMapping("/{offerId}/prix")
    public ResponseEntity<OfferStockDTO> updateOfferPrice(
            @PathVariable Long offerId,
            @Valid @RequestBody PriceChangeRequest request
    ) {
        return ResponseEntity.ok(
                priceService.updateOfferPrice(offerId, request.getPrix(), request.getMotif()));
    }

    @PatchMapping("/produits/{productId}/prix-defaut")
    public ResponseEntity<PriceHistoryDTO> updateProductDefaultPrice(
            @PathVariable Long productId,
            @Valid @RequestBody PriceChangeRequest request
    ) {
        return ResponseEntity.ok(
                priceService.updateProductDefaultPrice(productId, request.getPrix(), request.getMotif()));
    }

    @PatchMapping("/{offerId}/statut")
    public ResponseEntity<OfferStockDTO> updateOfferStatus(
            @PathVariable Long offerId,
            @Valid @RequestBody OfferStatusRequest request
    ) {
        return ResponseEntity.ok(
                priceService.updateOfferStatus(offerId, request.getStatut(), request.getMotif()));
    }

    // --- DTO de requete ---

    @Data
    public static class PriceChangeRequest {
        @NotNull(message = "Le prix est obligatoire")
        private BigDecimal prix;

        @NotNull(message = "Un motif est obligatoire")
        private String motif;
    }

    @Data
    public static class OfferStatusRequest {
        @NotNull(message = "Le statut est obligatoire")
        private OfferStatus statut;

        private String motif;
    }
}
