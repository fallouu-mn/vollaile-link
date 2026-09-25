package com.vollailelink.backend.service;

import com.vollailelink.backend.model.enums.OfferStatus;
import com.vollailelink.backend.publicapi.dto.PublicOfferDTO;
import com.vollailelink.backend.publicapi.mapper.PublicOfferMapper;
import com.vollailelink.backend.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferPublicService {

    private final OfferRepository offerRepository;
    private final PublicOfferMapper publicOfferMapper;

    @Transactional(readOnly = true)
    public List<PublicOfferDTO> getActivePublicOffers() {
        return offerRepository.findByStatus(OfferStatus.ACTIVE).stream()
                .map(publicOfferMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicOfferDTO getPublicOfferById(Long id) {
        return offerRepository.findById(id)
                .filter(o -> o.getStatus() == OfferStatus.ACTIVE)
                .map(publicOfferMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Offre publique non trouvée avec l'id: " + id));
    }
}
