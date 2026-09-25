package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.ClientDTO;
import com.vollailelink.backend.model.Client;
import org.springframework.stereotype.Component;

@Component
public class ClientMapper {

    public ClientDTO toDto(Client client) {
        if (client == null) return null;
        return ClientDTO.builder()
                .id(client.getId())
                .nom(client.getNom())
                .prenom(client.getPrenom())
                .telephone(client.getTelephone())
                .email(client.getEmail())
                .adresse(client.getAdresse())
                .ville(client.getVille())
                .region(client.getRegion())
                .dateNaissance(client.getDateNaissance())
                .sexe(client.getSexe())
                .professionnel(client.getProfessionnel())
                .entreprise(client.getEntreprise())
                .secteurActivite(client.getSecteurActivite())
                .isActive(client.getIsActive())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    public Client toEntity(ClientDTO dto) {
        if (dto == null) return null;
        return Client.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .telephone(dto.getTelephone())
                .email(dto.getEmail())
                .adresse(dto.getAdresse())
                .ville(dto.getVille())
                .region(dto.getRegion())
                .dateNaissance(dto.getDateNaissance())
                .sexe(dto.getSexe())
                .professionnel(dto.getProfessionnel() != null ? dto.getProfessionnel() : false)
                .entreprise(dto.getEntreprise())
                .secteurActivite(dto.getSecteurActivite())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();
    }
}
