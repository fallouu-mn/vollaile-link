package com.vollailelink.backend;

import com.vollailelink.backend.dto.OfferStockDTO;
import com.vollailelink.backend.dto.StockReservationDTO;
import com.vollailelink.backend.exception.InsufficientStockException;
import com.vollailelink.backend.exception.InvalidStatusTransitionException;
import com.vollailelink.backend.dto.MouvementStockDTO;
import com.vollailelink.backend.mapper.MouvementStockMapper;
import com.vollailelink.backend.model.Commande;
import com.vollailelink.backend.model.MouvementStock;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.Product;
import com.vollailelink.backend.model.Producer;
import com.vollailelink.backend.model.StockReservation;
import com.vollailelink.backend.model.enums.CommandeStatut;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.model.enums.ReservationStatut;
import com.vollailelink.backend.model.enums.TypeMouvementStock;
import com.vollailelink.backend.repository.MouvementStockRepository;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.StockReservationRepository;
import com.vollailelink.backend.service.AuditService;
import com.vollailelink.backend.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests du moteur de stock.
 *
 * Ils verrouillent la regle du CDC : disponible = total - reserve - vendu.
 * L'ancien modele decrementait le disponible a la reservation PUIS a la
 * vente : ces tests verrouillent l'absence de ce double decrement.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private MouvementStockRepository mouvementStockRepository;

    @Mock
    private MouvementStockMapper mouvementStockMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectProvider<StockService> selfProvider;

    @InjectMocks
    private StockService stockService;

    private Product product;
    private Producer producer;
    private Offer offer;
    private Commande commande;
    private long reservationIdSeq = 100L;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("Poulet de chair").unitType("piece").build();
        producer = Producer.builder().id(1L).name("Ferme Dakar").build();

        offer = Offer.builder()
                .id(10L)
                .product(product)
                .producer(producer)
                .status(OfferStatus.ACTIVE)
                .quantityTotal(100)
                .quantityReserved(0)
                .quantitySold(0)
                .unitPrice(java.math.BigDecimal.valueOf(3500))
                .startsAt(OffsetDateTime.now().minusDays(1))
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .build();

        commande = Commande.builder().id(500L).client(null).produit(product).offre(offer)
                .quantite(10).build();
    }

    private void stubLockedOffer() {
        when(offerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(offer));
    }

    private StockReservation persistReservation(ReservationStatut statut, int quantite) {
        StockReservation r = StockReservation.builder()
                .id(reservationIdSeq++)
                .offer(offer)
                .commande(commande)
                .quantite(quantite)
                .statut(statut)
                .expiresAt(OffsetDateTime.now().plusHours(48))
                .idempotencyKey("CMD-500:RESERVE")
                .createdAt(OffsetDateTime.now())
                .build();
        return r;
    }

    @Nested
    @DisplayName("Lecture de l'etat du stock")
    class Lecture {

        @Test
        @DisplayName("Disponible = total - reserve - vendu")
        void disponibleSuitLaRegleDuCDC() {
            offer.setQuantityTotal(100);
            offer.setQuantityReserved(30);
            offer.setQuantitySold(20);

            when(offerRepository.findById(10L)).thenReturn(Optional.of(offer));
            when(reservationRepository.findByOfferIdAndStatut(10L, ReservationStatut.ACTIVE))
                    .thenReturn(new ArrayList<>());

            OfferStockDTO dto = stockService.getOfferStock(10L);

            assertEquals(100, dto.getQuantityTotal());
            assertEquals(30, dto.getQuantityReserved());
            assertEquals(20, dto.getQuantitySold());
            assertEquals(50, dto.getQuantityAvailable());
        }
    }

    @Nested
    @DisplayName("Reservation")
    class Reservation {

        @Test
        @DisplayName("Reserve : reserve augmente, total inchange, disponible baisse")
        void reserveBaisseLeDisponibleSansToucherAuTotal() {
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
                StockReservation r = inv.getArgument(0);
                r.setId(reservationIdSeq++);
                return r;
            });
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.reserve(10L, 10, commande, "CMD-500:RESERVE");

            assertEquals(100, offer.getQuantityTotal(), "le total ne doit pas bouger lors d'une reservation");
            assertEquals(10, offer.getQuantityReserved());
            assertEquals(0, offer.getQuantitySold());
            assertEquals(90, offer.getDisponible());
        }

        @Test
        @DisplayName("Reserve : refuses si le disponible est insuffisant")
        void refuseReservationSuperieureAuDisponible() {
            offer.setQuantityTotal(5);
            offer.setQuantityReserved(0);
            offer.setQuantitySold(0);

            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();

            assertThrows(InsufficientStockException.class,
                    () -> stockService.reserve(10L, 10, commande, "CMD-500:RESERVE"));

            assertEquals(0, offer.getQuantityReserved(), "aucune reservation ne doit etre creee");
        }

        @Test
        @DisplayName("Reserve : ne peut pas depasser le total meme si le disponible est incoherent")
        void refuseDepassementDuTotal() {
            // disponible incoherent simule une donnee corrompue : le total prime
            offer.setQuantityTotal(10);
            offer.setQuantityReserved(8);
            offer.setQuantitySold(5); // reserved + sold > total

            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();

            assertThrows(InsufficientStockException.class,
                    () -> stockService.reserve(10L, 5, commande, "CMD-500:RESERVE"));
        }

        @Test
        @DisplayName("Reserve : refuses une offre non active")
        void refuseOffreInactive() {
            offer.setStatus(OfferStatus.DRAFT);

            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();

            assertThrows(InvalidStatusTransitionException.class,
                    () -> stockService.reserve(10L, 1, commande, "CMD-500:RESERVE"));
        }

        @Test
        @DisplayName("Reserve : rejette une quantite nulle ou negative")
        void refuseQuantiteInvalide() {
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> stockService.reserve(10L, 0, commande, "CMD-500:RESERVE"));
        }

        @Test
        @DisplayName("Reserve : la meme cle d'idempotence ne reserve pas deux fois")
        void estIdempotente() {
            StockReservation existante = persistReservation(ReservationStatut.ACTIVE, 10);
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.of(existante));

            StockReservationDTO dto = stockService.reserve(10L, 10, commande, "CMD-500:RESERVE");

            assertEquals(existante.getId(), dto.getId());
            assertEquals(0, offer.getQuantityReserved(),
                    "une relecture idempotente ne doit pas modifier les compteurs");
            verify(reservationRepository, never()).save(any());
            verify(offerRepository, never()).findByIdForUpdate(any());
        }
    }

    @Nested
    @DisplayName("Vente : reservation -> vendu")
    class Vente {

        @Test
        @DisplayName("Vente : le disponible ne bouge pas (pas de double decrement)")
        void venteNeDoublePasLeDecrement() {
            offer.setQuantityTotal(100);
            offer.setQuantityReserved(10);
            offer.setQuantitySold(0);

            StockReservation r = persistReservation(ReservationStatut.ACTIVE, 10);
            when(reservationRepository.findById(r.getId())).thenReturn(Optional.of(r));
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.consume(r.getId());

            assertEquals(100, offer.getQuantityTotal());
            assertEquals(0, offer.getQuantityReserved(), "la reservation doit etre debitee");
            assertEquals(10, offer.getQuantitySold());
            assertEquals(90, offer.getDisponible(),
                    "le disponible doit rester a 90 : 100 total - 0 reserve - 10 vendu");
        }

        @Test
        @DisplayName("Vente : est idempotente")
        void venteEstIdempotente() {
            offer.setQuantityTotal(100);
            offer.setQuantityReserved(0);
            offer.setQuantitySold(10);

            StockReservation dejaConsommee = persistReservation(ReservationStatut.CONSUMED, 10);
            when(reservationRepository.findById(dejaConsommee.getId()))
                    .thenReturn(Optional.of(dejaConsommee));

            StockReservationDTO dto = stockService.consume(dejaConsommee.getId());

            assertEquals(ReservationStatut.CONSUMED, dto.getStatut());
            assertEquals(10, offer.getQuantitySold(),
                    "une vente rejouee ne doit pas incrementer quantity_sold");
            verify(offerRepository, never()).findByIdForUpdate(any());
        }

        @Test
        @DisplayName("Vente : refuse une reservation deja liberee")
        void refuseVenteSurReservationLiberee() {
            StockReservation liberee = persistReservation(ReservationStatut.RELEASED, 10);
            when(reservationRepository.findById(liberee.getId())).thenReturn(Optional.of(liberee));

            assertThrows(InvalidStatusTransitionException.class,
                    () -> stockService.consume(liberee.getId()));
        }
    }

    @Nested
    @DisplayName("Liberation : reservation -> disponible")
    class Liberation {

        @Test
        @DisplayName("Liberation : le disponible remonte, le total ne bouge pas")
        void liberationRestaureLeDisponible() {
            offer.setQuantityTotal(100);
            offer.setQuantityReserved(10);
            offer.setQuantitySold(0);

            StockReservation r = persistReservation(ReservationStatut.ACTIVE, 10);
            when(reservationRepository.findById(r.getId())).thenReturn(Optional.of(r));
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.release(r.getId(), "Annulation", ReservationStatut.RELEASED);

            assertEquals(100, offer.getQuantityTotal());
            assertEquals(0, offer.getQuantityReserved());
            assertEquals(100, offer.getDisponible());
        }

        @Test
        @DisplayName("Liberation : est idempotente")
        void liberationEstIdempotente() {
            offer.setQuantityReserved(0);
            StockReservation dejaLiberee = persistReservation(ReservationStatut.RELEASED, 10);
            when(reservationRepository.findById(dejaLiberee.getId()))
                    .thenReturn(Optional.of(dejaLiberee));

            StockReservationDTO dto = stockService.release(
                    dejaLiberee.getId(), "Annulation", ReservationStatut.RELEASED);

            assertEquals(ReservationStatut.RELEASED, dto.getStatut());
            assertEquals(100, offer.getDisponible(),
                    "une double liberation ne doit pas restoration le disponible au-dela du total");
            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Mouvements physiques")
    class MouvementsPhysiques {

        @Test
        @DisplayName("Entree : le total et le disponible augmentent")
        void entreeAugmenteLeTotal() {
            stubLockedOffer();
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OfferStockDTO dto = stockService.entree(10L, 50, "Reception produit");

            assertEquals(150, dto.getQuantityTotal());
            assertEquals(150, dto.getQuantityAvailable());
        }

        @Test
        @DisplayName("Sortie : refuse de descendre sous le volume engage")
        void sortieRefuseeSousVolumeEngage() {
            offer.setQuantityTotal(100);
            offer.setQuantityReserved(80);
            offer.setQuantitySold(15);

            stubLockedOffer();

            assertThrows(InsufficientStockException.class,
                    () -> stockService.sortie(10L, 10, "Casse"));

            assertEquals(100, offer.getQuantityTotal(), "le total ne doit pas avoir bouge");
        }

        @Test
        @DisplayName("Sortie : refuse un total negatif")
        void sortieRefuseeSurTotalNegatif() {
            stubLockedOffer();
            assertThrows(InsufficientStockException.class,
                    () -> stockService.sortie(10L, 500, "Casse"));
            assertEquals(100, offer.getQuantityTotal());
        }

        @Test
        @DisplayName("Ajustement : accepte un delta signe")
        void ajustementAccepteDeltaSigne() {
            stubLockedOffer();
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OfferStockDTO dto = stockService.ajuster(10L, -10, "Correction inventaire");

            assertEquals(90, dto.getQuantityTotal());
            assertEquals(90, dto.getQuantityAvailable());
        }
    }

    @Nested
    @DisplayName("Workflow de commande")
    class WorkflowCommande {

        @Test
        @DisplayName("Workflow nominal : reservation puis vente")
        void workflowNominal() {
            // Creation : 100 total, 0 reserve, 0 vendu
            assertEquals(100, offer.getDisponible());

            // Reservation de 10
            offer.setQuantityReserved(10);
            assertEquals(90, offer.getDisponible());

            // Livraison : convertit reserve -> vendu
            offer.setQuantityReserved(0);
            offer.setQuantitySold(10);
            assertEquals(90, offer.getDisponible(), "la livraison ne doit pas reaffecter le disponible");

            // Le stock total a bien baisse de 10 par rapport au depart
            assertEquals(100 - 10, 100 - offer.getQuantitySold());
        }

        @Test
        @DisplayName("Workflow d'annulation : reservation puis liberation")
        void workflowAnnulation() {
            offer.setQuantityReserved(10);
            assertEquals(90, offer.getDisponible());

            offer.setQuantityReserved(0);
            assertEquals(100, offer.getDisponible(), "l'annulation rend la totalite du lot");
        }
    }

    @Nested
    @DisplayName("Transitions de statut de commande")
    class Transitions {

        @Test
        @DisplayName("Workflow nominal autorise")
        void transitionsNominalesAutorisees() {
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EN_ATTENTE_PAIEMENT, CommandeStatut.PAYEE));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.PAYEE, CommandeStatut.EN_PREPARATION));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EN_PREPARATION, CommandeStatut.EXPEDIEE));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EXPEDIEE, CommandeStatut.LIVREE));
        }

        @Test
        @DisplayName("Saut de statut interdit")
        void sautDeStatutInterdit() {
            assertFalse(StockService.transitionAutorisee(
                    CommandeStatut.EN_ATTENTE_PAIEMENT, CommandeStatut.LIVREE),
                    "on ne peut pas livrer une commande non payee");
        }

        @Test
        @DisplayName("Retour en arriere interdit")
        void retourEnArriereInterdit() {
            assertFalse(StockService.transitionAutorisee(
                    CommandeStatut.EN_PREPARATION, CommandeStatut.PAYEE));
        }

        @Test
        @DisplayName("Statuts terminaux : aucune sortie")
        void statutsTerminaux() {
            assertFalse(StockService.transitionAutorisee(
                    CommandeStatut.LIVREE, CommandeStatut.ANNULEE));
            assertFalse(StockService.transitionAutorisee(
                    CommandeStatut.ANNULEE, CommandeStatut.PAYEE));
            assertFalse(StockService.transitionAutorisee(
                    CommandeStatut.EXPIREE, CommandeStatut.PAYEE));
        }

        @Test
        @DisplayName("Annulation possible depuis les etats non livres")
        void annulationPossibleAvantLivraison() {
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EN_ATTENTE_PAIEMENT, CommandeStatut.ANNULEE));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.PAYEE, CommandeStatut.ANNULEE));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EN_PREPARATION, CommandeStatut.ANNULEE));
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.EXPEDIEE, CommandeStatut.ANNULEE));
        }

        @Test
        @DisplayName("Meme statut autorise (idempotence)")
        void memeStatutAutorise() {
            assertTrue(StockService.transitionAutorisee(
                    CommandeStatut.PAYEE, CommandeStatut.PAYEE));
        }
    }

    @Nested
    @DisplayName("Expiration automatique")
    class Expiration {

        @Test
        @DisplayName("Ne libere que les reservations echues")
        void neLibereQueLesEchues() {
            StockReservation echue = StockReservation.builder()
                    .id(1L).offer(offer).quantite(5)
                    .statut(ReservationStatut.ACTIVE)
                    .expiresAt(OffsetDateTime.now().minusHours(1))
                    .build();
            StockReservation echue2 = StockReservation.builder()
                    .id(3L).offer(offer).quantite(2)
                    .statut(ReservationStatut.ACTIVE)
                    .expiresAt(OffsetDateTime.now().minusMinutes(5))
                    .build();
            StockReservation encoreValide = StockReservation.builder()
                    .id(2L).offer(offer).quantite(5)
                    .statut(ReservationStatut.ACTIVE)
                    .expiresAt(OffsetDateTime.now().plusHours(5))
                    .build();

            when(reservationRepository.findByStatutAndExpiresAtBefore(
                    eq(ReservationStatut.ACTIVE), any(OffsetDateTime.class)))
                    .thenReturn(List.of(echue, echue2));

            // Le proxy injecte sert a ouvrir une transaction isolee par reservation.
            StockService proxy = mock(StockService.class);
            when(selfProvider.getObject()).thenReturn(proxy);
            when(proxy.expireOne(1L)).thenReturn(true);
            when(proxy.expireOne(3L)).thenReturn(false);

            int resultat = stockService.expireOverdueReservations();

            assertEquals(1, resultat, "seule la reservation traitee avec succes est comptee");
            verify(proxy, never()).expireOne(encoreValide.getId());
        }

        @Test
        @DisplayName("Ne traite aucune reservation si rien n'est echu")
        void neTraiteRienSiRienNestEchu() {
            when(reservationRepository.findByStatutAndExpiresAtBefore(
                    eq(ReservationStatut.ACTIVE), any(OffsetDateTime.class)))
                    .thenReturn(List.of());

            assertEquals(0, stockService.expireOverdueReservations());
            verify(selfProvider, never()).getObject();
        }
    }

    @Nested
    @DisplayName("Journal des mouvements")
    class Journal {

        @Test
        @DisplayName("Le mouvement conserve le lien vers l'offre et la reservation")
        void leMouvementConserveLesLiens() {
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
                StockReservation r = inv.getArgument(0);
                r.setId(777L);
                return r;
            });
            ArgumentCaptor<MouvementStock> captor = ArgumentCaptor.forClass(MouvementStock.class);
            when(mouvementStockRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            stockService.reserve(10L, 10, commande, "CMD-500:RESERVE");

            MouvementStock ecrit = captor.getValue();
            assertEquals(10L, ecrit.getOffer().getId(),
                    "sans le lien offre, on ne sait pas quel lot a bouge");
            assertNotNull(ecrit.getReservation());
            assertEquals(777L, ecrit.getReservation().getId());
            assertEquals(TypeMouvementStock.RESERVATION, ecrit.getTypeMouvement());
            assertEquals(90, ecrit.getQuantiteApresMouvement(),
                    "le disponible apres reservation = total - reserve - vendu");
        }

        @Test
        @DisplayName("Le mapper expose offerId et reservationId dans le JSON")
        void leMapperExposeLesLiens() {
            StockReservation reservation = StockReservation.builder()
                    .id(42L).offer(offer).commande(commande).quantite(10)
                    .statut(ReservationStatut.ACTIVE)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .build();

            MouvementStock mouvement = MouvementStock.builder()
                    .id(7L)
                    .produit(product)
                    .offer(offer)
                    .reservation(reservation)
                    .typeMouvement(TypeMouvementStock.VENTE)
                    .quantite(10)
                    .quantiteApresMouvement(90)
                    .isActive(true)
                    .build();

            MouvementStockDTO dto = new MouvementStockMapper().toDto(mouvement);

            assertEquals(10L, dto.getOfferId(),
                    "l'API doit exposer l'offre : le stock est porte par l'offre, pas par le produit");
            assertEquals(42L, dto.getReservationId());
            assertEquals(1L, dto.getProduitId());
        }
    }

    @Nested
    @DisplayName("Piste d'audit (CDC 2.5)")
    class Audit {

        @Test
        @DisplayName("La reservation est tracee avec les compteurs apres coup")
        void reservationEstTracee() {
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> {
                StockReservation r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> {
                MouvementStock m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });

            stockService.reserve(10L, 10, commande, "CMD-500:RESERVE");

            ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, Object>> meta = ArgumentCaptor.forClass(Map.class);
            verify(auditService).record(type.capture(), desc.capture(), meta.capture());

            assertEquals(AuditService.EVENT_STOCK_RESERVATION, type.getValue());
            Map<String, Object> m = meta.getValue();
            assertEquals(10L, m.get("offreId"));
            assertEquals(100, m.get("quantityTotal"));
            assertEquals(10, m.get("quantityReserved"));
            assertEquals(0, m.get("quantitySold"));
            assertEquals(90, m.get("quantityAvailable"),
                    "l'audit doit conserver l'etat du stock apres le mouvement");
        }

        @Test
        @DisplayName("La vente est tracee comme STOCK_VENTE")
        void venteEstTracee() {
            offer.setQuantityReserved(10);
            StockReservation r = persistReservation(ReservationStatut.ACTIVE, 10);

            when(reservationRepository.findById(r.getId())).thenReturn(Optional.of(r));
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.consume(r.getId());

            verify(auditService).record(eq(AuditService.EVENT_STOCK_VENTE), anyString(), any());
        }

        @Test
        @DisplayName("La liberation est tracee comme STOCK_LIBERATION")
        void liberationEstTracee() {
            offer.setQuantityReserved(10);
            StockReservation r = persistReservation(ReservationStatut.ACTIVE, 10);

            when(reservationRepository.findById(r.getId())).thenReturn(Optional.of(r));
            stubLockedOffer();
            when(reservationRepository.save(any(StockReservation.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.release(r.getId(), "Annulation", ReservationStatut.RELEASED);

            verify(auditService).record(eq(AuditService.EVENT_STOCK_LIBERATION), anyString(), any());
        }

        @Test
        @DisplayName("L'entree de stock est tracee comme STOCK_ENTREE")
        void entreeEstTracee() {
            stubLockedOffer();
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.entree(10L, 50, "Reception");

            verify(auditService).record(eq(AuditService.EVENT_STOCK_ENTREE), anyString(), any());
        }

        @Test
        @DisplayName("L'ajustement est tracee comme STOCK_AJUSTEMENT")
        void ajustementEstTracee() {
            stubLockedOffer();
            when(mouvementStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            stockService.ajuster(10L, -5, "Correction");

            verify(auditService).record(eq(AuditService.EVENT_STOCK_AJUSTEMENT), anyString(), any());
        }

        @Test
        @DisplayName("Une operation refusee ne laisse aucune trace d'audit")
        void operationRefuseeNeTracePas() {
            offer.setQuantityTotal(5);
            when(reservationRepository.findByIdempotencyKey("CMD-500:RESERVE"))
                    .thenReturn(Optional.empty());
            stubLockedOffer();

            assertThrows(InsufficientStockException.class,
                    () -> stockService.reserve(10L, 10, commande, "CMD-500:RESERVE"));

            verify(auditService, never()).record(anyString(), anyString(), any());
        }
    }
}
