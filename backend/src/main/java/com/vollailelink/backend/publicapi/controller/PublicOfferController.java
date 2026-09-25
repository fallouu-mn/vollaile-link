package com.vollailelink.backend.publicapi.controller;

import com.vollailelink.backend.publicapi.dto.PublicOfferDTO;
import com.vollailelink.backend.service.OfferPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/offers")
@RequiredArgsConstructor
public class PublicOfferController {

    private final OfferPublicService offerPublicService;

    @GetMapping
    public ResponseEntity<List<PublicOfferDTO>> getActiveOffers() {
        return ResponseEntity.ok(offerPublicService.getActivePublicOffers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicOfferDTO> getOfferById(@PathVariable Long id) {
        return ResponseEntity.ok(offerPublicService.getPublicOfferById(id));
    }
}
