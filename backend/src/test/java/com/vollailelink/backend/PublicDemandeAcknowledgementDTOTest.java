package com.vollailelink.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vollailelink.backend.publicapi.dto.PublicDemandeAcknowledgementDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicDemandeAcknowledgementDTOTest {

    @Test
    void serializesOnlyPublicAcknowledgementFields() throws Exception {
        PublicDemandeAcknowledgementDTO acknowledgement = PublicDemandeAcknowledgementDTO.builder()
                .reference("DEM-000042")
                .message("Votre demande a bien été reçue.")
                .build();

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(acknowledgement));

        assertEquals(2, json.size());
        assertTrue(json.has("reference"));
        assertTrue(json.has("message"));
        assertFalse(json.has("clientId"));
        assertFalse(json.has("clientNom"));
        assertFalse(json.has("clientTelephone"));
        assertFalse(json.has("commentairesAdmin"));
        assertFalse(json.has("lieuLivraison"));
    }
}
