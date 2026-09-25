package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.ProductDTO;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.model.Product;
import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.repository.OfferRepository;
import com.vollailelink.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catalogue produits (back-office).
 *
 * Le service Angular appelait /api/admin/products qui n'existait pas :
 * les ecrans clients, demandes et stock echouaient au chargement des
 * libelles produit.
 *
 * Le stock n'etant plus porte par le produit mais par les offres, on
 * expose un agregat explicite plutot qu'un total unique trompeur.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;

    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        Map<Long, List<Offer>> offresParProduit = offresActivesParProduit(products);

        return products.stream()
                .map(p -> toDto(p, offresParProduit.getOrDefault(p.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'id: " + id));

        List<Offer> offres = offerRepository.findByProductIdAndStatus(id, OfferStatus.ACTIVE);
        return toDto(product, offres);
    }

    /**
     * Charge en une seule requete les offres actives de tous les produits
     * demandes : evite un N+1 sur le catalogue.
     */
    private Map<Long, List<Offer>> offresActivesParProduit(List<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<Long> productIds = products.stream().map(Product::getId).toList();
        return offerRepository.findByProductIdInAndStatus(productIds, OfferStatus.ACTIVE)
                .stream()
                .collect(Collectors.groupingBy(o -> o.getProduct().getId()));
    }

    private ProductDTO toDto(Product p, List<Offer> offres) {
        long total = offres.stream().mapToInt(o -> nz(o.getQuantityTotal())).sum();
        long reserved = offres.stream().mapToInt(o -> nz(o.getQuantityReserved())).sum();
        long sold = offres.stream().mapToInt(o -> nz(o.getQuantitySold())).sum();

        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .unitType(p.getUnitType())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryNom(p.getCategory() != null ? p.getCategory().getName() : null)
                .defaultUnitPrice(p.getDefaultUnitPrice())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .activeOfferCount(offres.size())
                .stock(ProductDTO.Stock.builder()
                        .total(total)
                        .reserved(reserved)
                        .sold(sold)
                        .available(total - reserved - sold)
                        .build())
                .build();
    }

    private static int nz(Integer value) {
        return value == null ? 0 : value;
    }
}
