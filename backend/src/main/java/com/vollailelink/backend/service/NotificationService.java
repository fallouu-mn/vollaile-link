package com.vollailelink.backend.service;

import com.vollailelink.backend.dto.NotificationDTO;
import com.vollailelink.backend.exception.ResourceNotFoundException;
import com.vollailelink.backend.mapper.NotificationMapper;
import com.vollailelink.backend.model.Notification;
import com.vollailelink.backend.model.enums.NotificationType;
import com.vollailelink.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotifications() {
        return notificationRepository.findByLuFalseOrderByCreatedAtDesc().stream()
                .map(notificationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDTO createNotification(String titre, String message, NotificationType type, Long relatedEntityId, String relatedEntityType) {
        Notification notification = Notification.builder()
                .titre(titre)
                .message(message)
                .type(type)
                .lu(false)
                .relatedEntityId(relatedEntityId)
                .relatedEntityType(relatedEntityType)
                .isActive(true)
                .build();
        Notification saved = notificationRepository.save(notification);
        return notificationMapper.toDto(saved);
    }

    @Transactional
    public NotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification non trouvée avec l'id: " + id));
        notification.setLu(true);
        Notification updated = notificationRepository.save(notification);
        return notificationMapper.toDto(updated);
    }
}
