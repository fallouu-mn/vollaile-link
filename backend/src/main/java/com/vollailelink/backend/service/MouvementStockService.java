package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.MouvementStockDTO;
import com.vollailelink.backend.mapper.MouvementStockMapper;
import com.vollailelink.backend.model.MouvementStock;
import com.vollailelink.backend.repository.MouvementStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lecture du journal des mouvements de stock.
 *
 * ATTENTION : cette classe n'ecrit plus rien sur le stock.
 * Toute mutation passe par {@link StockService}, qui applique la regle
 * disponible = total - reserve - vendu avec verrouillage.
 * Le calcul du stock a partir du dernier mouvement (ancien
 * {@code getStockActuel}) a ete supprime : il etait faux, car il ignorait
 * la distinction reservation / vente et le perimetre de l'offre.
 */
@Service
@RequiredArgsConstructor
public class MouvementStockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final MouvementStockMapper mouvementStockMapper;

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getMouvementsByProduit(Long produitId) {
        return mouvementStockRepository.findByProduitIdOrderByCreatedAtDesc(produitId).stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getMouvementsByOffer(Long offerId) {
        return mouvementStockRepository.findByOfferIdOrderByIdDesc(offerId).stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getMouvementsByReservation(Long reservationId) {
        return mouvementStockRepository.findByReservationIdOrderByIdAsc(reservationId).stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MouvementStockDTO> getAllMouvements() {
        return mouvementStockRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(mouvementStockMapper::toDto)
                .collect(Collectors.toList());
    }
}
