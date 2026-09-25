package com.vollailelink.backend.publicapi.mapper;

import com.vollailelink.backend.model.Offer;
import com.vollailelink.backend.publicapi.dto.PublicOfferDTO;
import org.springframework.stereotype.Component;

@Component
public class PublicOfferMapper {

    public PublicOfferDTO toDto(Offer offer) {
        if (offer == null) return null;
        return PublicOfferDTO.builder()
                .id(offer.getId())
                .productId(offer.getProduct() != null ? offer.getProduct().getId() : null)
                .productName(offer.getProduct() != null ? offer.getProduct().getName() : null)
                .productDescription(offer.getProduct() != null ? offer.getProduct().getDescription() : null)
                .categoryName(offer.getProduct() != null && offer.getProduct().getCategory() != null ? offer.getProduct().getCategory().getName() : null)
                .unitType(offer.getProduct() != null ? offer.getProduct().getUnitType() : null)
                .quantityAvailable(offer.getQuantityAvailable())
                .unitPrice(offer.getUnitPrice())
                .startsAt(offer.getStartsAt())
                .expiresAt(offer.getExpiresAt())
                .isNegotiable(offer.getIsNegotiable())
                .description(offer.getDescription())
                .build();
    }
}
