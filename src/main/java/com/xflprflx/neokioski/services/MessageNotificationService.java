package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.MessageNotification;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.NotificationType;
import com.xflprflx.neokioski.repositories.MessageNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageNotificationService {

    private final MessageNotificationRepository repository;

    public MessageNotificationService(MessageNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void createNotification(User recipient, ChatMessage message, NotificationType type) {
        MessageNotification notification = new MessageNotification(recipient, message, type);
        repository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    @Transactional
    public void markMessageNotificationsAsReadForUser(ChatMessage message, User user) {
        List<MessageNotification> notifications = repository.findByRecipientAndMessageAndIsReadFalse(user, message);
        for (MessageNotification n : notifications) {
            n.setRead(true);
        }
        repository.saveAll(notifications);
    }

    public List<MessageNotification> getUnreadNotificationsForUser(User recipient) {
        return repository.findByRecipientAndIsReadFalse(recipient);
    }
}
