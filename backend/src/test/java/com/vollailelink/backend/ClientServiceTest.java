package com.vollailelink.backend;

import com.vollailelink.backend.dto.ClientDTO;
import com.vollailelink.backend.exception.DuplicateResourceException;
import com.vollailelink.backend.mapper.ClientMapper;
import com.vollailelink.backend.model.Client;
import com.vollailelink.backend.repository.ClientRepository;
import com.vollailelink.backend.service.ClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private Client client;
    private ClientDTO clientDTO;

    @BeforeEach
    void setUp() {
        client = Client.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Moussa")
                .telephone("+221770000000")
                .email("moussa@example.com")
                .professionnel(false)
                .isActive(true)
                .build();

        clientDTO = ClientDTO.builder()
                .id(1L)
                .nom("Diop")
                .prenom("Moussa")
                .telephone("+221770000000")
                .email("moussa@example.com")
                .professionnel(false)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Devrait créer un client avec succès si le téléphone est unique")
    void testCreateClientSuccess() {
        when(clientRepository.existsByTelephone("+221770000000")).thenReturn(false);
        when(clientMapper.toEntity(any(ClientDTO.class))).thenReturn(client);
        when(clientRepository.save(any(Client.class))).thenReturn(client);
        when(clientMapper.toDto(any(Client.class))).thenReturn(clientDTO);

        ClientDTO created = clientService.createClient(clientDTO);

        assertNotNull(created);
        assertEquals("+221770000000", created.getTelephone());
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    @DisplayName("Devrait lever DuplicateResourceException si le numéro de téléphone existe déjà (§13)")
    void testCreateClientDuplicatePhone() {
        when(clientRepository.existsByTelephone("+221770000000")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> clientService.createClient(clientDTO));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait récupérer un client existant sans dupliquer lors du getOrCreate")
    void testGetOrCreateExistingClient() {
        when(clientRepository.findByTelephone("+221770000000")).thenReturn(Optional.of(client));
        when(clientMapper.toDto(client)).thenReturn(clientDTO);

        ClientDTO result = clientService.getOrCreateClient("Diop", "Moussa", "+221770000000", "moussa@example.com");

        assertNotNull(result);
        assertEquals("Diop", result.getNom());
        verify(clientRepository, never()).save(any());
    }
}
