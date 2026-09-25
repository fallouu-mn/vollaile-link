package com.vollailelink.backend.dto;

import com.vollailelink.backend.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String titre;
    private String message;
    private NotificationType type;
    private Boolean lu;
    private Long relatedEntityId;
    private String relatedEntityType;
    private OffsetDateTime createdAt;
}
