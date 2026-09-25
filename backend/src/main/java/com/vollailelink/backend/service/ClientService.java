package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.ClientDTO;
import com.vollailelink.backend.exception.DuplicateResourceException;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.mapper.ClientMapper;
import com.vollailelink.backend.model.Client;
import com.vollailelink.backend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll().stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id: " + id));
        return clientMapper.toDto(client);
    }

    @Transactional(readOnly = true)
    public ClientDTO getClientByTelephone(String telephone) {
        Client client = clientRepository.findByTelephone(telephone)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec le téléphone: " + telephone));
        return clientMapper.toDto(client);
    }

    @Transactional(readOnly = true)
    public List<ClientDTO> searchClients(String query) {
        return clientRepository.searchClients(query).stream()
                .map(clientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientDTO createClient(ClientDTO dto) {
        if (clientRepository.existsByTelephone(dto.getTelephone())) {
            throw new DuplicateResourceException("Un client avec le numéro de téléphone " + dto.getTelephone() + " existe déjà.");
        }
        Client client = clientMapper.toEntity(dto);
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    @Transactional
    public ClientDTO getOrCreateClient(String nom, String prenom, String telephone, String email) {
        return clientRepository.findByTelephone(telephone)
                .map(clientMapper::toDto)
                .orElseGet(() -> {
                    ClientDTO newClient = ClientDTO.builder()
                            .nom(nom)
                            .prenom(prenom)
                            .telephone(telephone)
                            .email(email)
                            .professionnel(false)
                            .isActive(true)
                            .build();
                    return createClient(newClient);
                });
    }

    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'id: " + id));

        if (!client.getTelephone().equals(dto.getTelephone()) && clientRepository.existsByTelephone(dto.getTelephone())) {
            throw new DuplicateResourceException("Un client avec le numéro de téléphone " + dto.getTelephone() + " existe déjà.");
        }

        client.setNom(dto.getNom());
        client.setPrenom(dto.getPrenom());
        client.setTelephone(dto.getTelephone());
        client.setEmail(dto.getEmail());
        client.setAdresse(dto.getAdresse());
        client.setVille(dto.getVille());
        client.setRegion(dto.getRegion());
        client.setDateNaissance(dto.getDateNaissance());
        client.setSexe(dto.getSexe());
        client.setProfessionnel(dto.getProfessionnel() != null ? dto.getProfessionnel() : false);
        client.setEntreprise(dto.getEntreprise());
        client.setSecteurActivite(dto.getSecteurActivite());

        Client updated = clientRepository.save(client);
        return clientMapper.toDto(updated);
    }
}
