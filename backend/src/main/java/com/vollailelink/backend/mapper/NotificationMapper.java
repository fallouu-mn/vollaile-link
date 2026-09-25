package com.vollailelink.backend.mapper;

import com.vollailelink.backend.dto.NotificationDTO;
import com.vollailelink.backend.model.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDTO toDto(Notification n) {
        if (n == null) return null;
        return NotificationDTO.builder()
                .id(n.getId())
                .titre(n.getTitre())
                .message(n.getMessage())
                .type(n.getType())
                .lu(n.getLu())
                .relatedEntityId(n.getRelatedEntityId())
                .relatedEntityType(n.getRelatedEntityType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
