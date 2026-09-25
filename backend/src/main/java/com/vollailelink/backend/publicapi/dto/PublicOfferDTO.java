package com.vollailelink.backend.publicapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOfferDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productDescription;
    private String categoryName;
    private String unitType;
    private Integer quantityAvailable;
    private BigDecimal unitPrice;
    private OffsetDateTime startsAt;
    private OffsetDateTime expiresAt;
    private Boolean isNegotiable;
    private String description;
}
