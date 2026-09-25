package com.vollailelink.backend.publicapi.controller;

import com.vollailelink.backend.publicapi.dto.PublicDemandeAcknowledgementDTO;
import com.vollailelink.backend.publicapi.dto.PublicDemandeRequestDTO;
import com.vollailelink.backend.service.DemandeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/demandes")
@RequiredArgsConstructor
public class PublicDemandeController {

    private final DemandeService demandeService;

    @PostMapping
    public ResponseEntity<PublicDemandeAcknowledgementDTO> submitDemande(
            @Valid @RequestBody PublicDemandeRequestDTO dto) {
        PublicDemandeAcknowledgementDTO acknowledgement = demandeService.createPublicDemande(dto);
        return new ResponseEntity<>(acknowledgement, HttpStatus.CREATED);
    }
}
