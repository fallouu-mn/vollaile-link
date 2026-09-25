package com.vollailelink.backend;

import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.exception.BusinessRuleException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.PriceHistory;
import com.vollailelink.backend.model.Product;
import com.vollailelink.backend.model.Producer;
import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.repository.AdministratorRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.PriceHistoryRepository;
import com.vollailelink.backend.repository.ProductRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import com.vollailelink.backend.service.AuditService;
import com.vollailelink.backend.service.PriceService;
import com.vollailelink.backend.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests de la gestion des prix.
 *
 * Verifient que toute variation de prix laisse une trace durable
 * (price_history) ET une trace d'audit (audit_log, CDC 2.5).
 */
@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private AdministratorRepository administratorRepository;

    @Mock
    private StockService stockService;

    @Mock
    private AuditService auditService;

    private PriceService priceService;

    private Product product;
    private Offer offer;

    @BeforeEach
    void setUp() {
        priceService = new PriceService(
                offerRepository,
                productRepository,
                priceHistoryRepository,
                reservationRepository,
                administratorRepository,
                stockService,
                auditService
        );

        product = Product.builder()
                .id(1L)
                .name("Poulet de chair")
                .unitType("piece")
                .defaultUnitPrice(new BigDecimal("3500"))
                .build();

        offer = Offer.builder()
                .id(10L)
                .product(product)
                .producer(Producer.builder().id(1L).name("Ferme").build())
                .status(OfferStatus.ACTIVE)
                .quantityTotal(100)
                .quantityReserved(0)
                .quantitySold(0)
                .unitPrice(new BigDecimal("3500"))
                .startsAt(OffsetDateTime.now().minusDays(1))
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();
    }

    private void stubOffer() {
        when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
    }

    private void stubStockDto() {
        when(stockService.getOfferStock(10L)).thenReturn(
                OfferStockDTO.builder().offerId(10L).quantityAvailable(100).build());
    }

    @Nested
    @DisplayName("Variation du prix d'une offre")
    class VariationOffre {

        @Test
        @DisplayName("Ecrit price_history ET audit_log")
        void ecritLesDeuxTraces() {
            stubOffer();
            stubStockDto();
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(inv -> {
                PriceHistory h = inv.getArgument(0);
                h.setId(1L);
                return h;
            });

            priceService.updateOfferPrice(10L, new BigDecimal("3900"), "Révision tarifaire");

            verify(priceHistoryRepository).save(any(PriceHistory.class));

            ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, Object>> meta = ArgumentCaptor.forClass(Map.class);
            verify(auditService).record(eventType.capture(), anyString(), meta.capture());

            assertEquals(AuditService.EVENT_PRIX_OFFRE, eventType.getValue());
            Map<String, Object> m = meta.getValue();
            assertEquals(10L, m.get("offreId"));
            assertEquals(0, new BigDecimal("3500").compareTo((BigDecimal) m.get("ancienPrix")));
            assertEquals(0, new BigDecimal("3900").compareTo((BigDecimal) m.get("nouveauPrix")));
            assertEquals(0, new BigDecimal("400").compareTo((BigDecimal) m.get("variation")));
        }

        @Test
        @DisplayName("Calcule la variation en pourcentage")
        void calculeLaVariationEnPourcentage() {
            stubOffer();
            stubStockDto();
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            priceService.updateOfferPrice(10L, new BigDecimal("4200"), "Hausse");

            ArgumentCaptor<Map<String, Object>> meta = ArgumentCaptor.forClass(Map.class);
            verify(auditService).record(anyString(), anyString(), meta.capture());

            assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) meta.getValue().get("variationPourcent")),
                    "de 3500 a 4200, la variation est de 20%");
        }

        @Test
        @DisplayName("Un prix identique ne genere aucune trace")
        void prixIdentiqueNeTraceRien() {
            stubOffer();
            stubStockDto();

            priceService.updateOfferPrice(10L, new BigDecimal("3500"), "Rien ne change");

            verify(priceHistoryRepository, never()).save(any());
            verify(auditService, never()).record(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Refuse un prix negatif")
        void refusePrixNegatif() {
            // Pas de stub : la validation doit rejeter AVANT tout acces base,
            // sinon une offre inexistante masquerait le vrai motif du refus.
            assertThrows(BusinessRuleException.class,
                    () -> priceService.updateOfferPrice(10L, new BigDecimal("-100"), "Erreur"));
            verify(priceHistoryRepository, never()).save(any());
            verify(offerRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Refuse une variation sans motif")
        void refuseMotifVide() {
            assertThrows(BusinessRuleException.class,
                    () -> priceService.updateOfferPrice(10L, new BigDecimal("3900"), "  "));
            verify(priceHistoryRepository, never()).save(any());
            verify(offerRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Signale une offre inexistante")
        void offreInexistante() {
            when(offerRepository.findById(99L)).thenReturn(Optional.empty());
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> priceService.updateOfferPrice(99L, new BigDecimal("3900"), "Test"));
            assertTrue(ex.getMessage().contains("99"));
        }

        @Test
        @DisplayName("Accepte un premier prix sur une offre sans historique")
        void acceptePremierPrix() {
            offer.setUnitPrice(null);
            stubOffer();
            stubStockDto();
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(inv -> {
                PriceHistory h = inv.getArgument(0);
                h.setId(1L);
                return h;
            });

            priceService.updateOfferPrice(10L, new BigDecimal("3000"), "Définition initiale");

            assertEquals(0, new BigDecimal("3000").compareTo(offer.getUnitPrice()));
            verify(auditService).record(eq(AuditService.EVENT_PRIX_OFFRE), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Prix par défaut d'un produit")
    class PrixProduit {

        @Test
        @DisplayName("Ecrit l'historique et l'audit avec un evenement distinct")
        void ecritAvecEvenementProduit() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(inv -> {
                PriceHistory h = inv.getArgument(0);
                h.setId(2L);
                return h;
            });

            priceService.updateProductDefaultPrice(1L, new BigDecimal("3200"), "Alignement catalogue");

            verify(auditService).record(eq(AuditService.EVENT_PRIX_PRODUIT), anyString(), any());
            assertEquals(0, new BigDecimal("3200").compareTo(product.getDefaultUnitPrice()));
        }

        @Test
        @DisplayName("Ne modifie pas les offres existantes")
        void neModifiePasLesOffres() {
            BigDecimal prixOffre = offer.getUnitPrice();
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(priceHistoryRepository.save(any(PriceHistory.class))).thenAnswer(inv -> inv.getArgument(0));

            priceService.updateProductDefaultPrice(1L, new BigDecimal("3200"), "Alignement");

            assertEquals(0, prixOffre.compareTo(offer.getUnitPrice()),
                    "chaque offre porte son prix, le prix produit ne doit pas l'ecraser");
        }
    }

    @Nested
    @DisplayName("Cycle de vie d'une offre")
    class CycleOffre {

        @Test
        @DisplayName("Trace le changement de statut")
        void traceChangementStatut() {
            stubOffer();
            stubStockDto();
            when(reservationRepository.findByOfferIdAndStatut(10L, ReservationStatut.ACTIVE))
                    .thenReturn(new ArrayList<>());

            priceService.updateOfferStatus(10L, OfferStatus.WITHDRAWN, "Producteur indisponible");

            verify(auditService).record(eq(AuditService.EVENT_OFFRE_STATUT), anyString(), any());
            assertEquals(OfferStatus.WITHDRAWN, offer.getStatus());
        }

        @Test
        @DisplayName("Refuse de retirer une offre ayant des reservations actives")
        void refuseRetraitAvecReservations() {
            stubOffer();
            StockReservation reservation = StockReservation.builder()
                    .id(1L).offer(offer).quantite(10)
                    .statut(ReservationStatut.ACTIVE)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .build();
            when(reservationRepository.findByOfferIdAndStatut(10L, ReservationStatut.ACTIVE))
                    .thenReturn(new ArrayList<>(List.of(reservation)));

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> priceService.updateOfferStatus(10L, OfferStatus.WITHDRAWN, "Retrait"));

            assertTrue(ex.getMessage().contains("1 réservation"));
            assertEquals(OfferStatus.ACTIVE, offer.getStatus(), "le statut ne doit pas avoir changé");
            verify(auditService, never()).record(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Un statut identique ne genere aucune trace")
        void statutIdentiqueNeTraceRien() {
            stubOffer();
            stubStockDto();

            priceService.updateOfferStatus(10L, OfferStatus.ACTIVE, "Rien ne change");

            verify(auditService, never()).record(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("Lecture des historiques")
    class Lecture {

        @Test
        @DisplayName("L'historique d'un produit est renvoye du plus recent au plus ancien")
        void historiqueProduit() {
            PriceHistory ancien = PriceHistory.builder()
                    .id(1L).product(product).price(new BigDecimal("3500"))
                    .changedAt(OffsetDateTime.now().minusDays(10)).build();
            PriceHistory recent = PriceHistory.builder()
                    .id(2L).product(product).price(new BigDecimal("3900"))
                    .changedAt(OffsetDateTime.now()).build();

            when(priceHistoryRepository.findByProductIdOrderByChangedAtDesc(1L))
                    .thenReturn(List.of(recent, ancien));

            var dtos = priceService.getProductPriceHistory(1L);

            assertEquals(2, dtos.size());
            assertEquals(2L, dtos.get(0).getId());
            assertEquals("Poulet de chair", dtos.get(0).getProductNom());
        }

        @Test
        @DisplayName("L'historique d'une offre porte sur le produit de l'offre")
        void historiqueOffre() {
            stubOffer();
            PriceHistory h = PriceHistory.builder()
                    .id(5L).product(product).price(new BigDecimal("3900"))
                    .changedAt(OffsetDateTime.now()).build();
            when(priceHistoryRepository.findByProductIdOrderByChangedAtDesc(1L))
                    .thenReturn(List.of(h));

            var dtos = priceService.getOfferPriceHistory(10L);

            assertEquals(1, dtos.size());
            assertEquals(1L, dtos.get(0).getProductId());
        }
    }
}
