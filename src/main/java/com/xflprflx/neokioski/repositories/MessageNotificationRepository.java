package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.MessageNotification;
import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageNotificationRepository extends JpaRepository<MessageNotification, Long> {
    List<MessageNotification> findByRecipientAndIsReadFalse(User recipient);
    
    @Query("SELECT n FROM MessageNotification n WHERE n.recipient = :recipient AND n.message = :message AND n.isRead = false")
    List<MessageNotification> findByRecipientAndMessageAndIsReadFalse(@Param("recipient") User recipient, @Param("message") ChatMessage message);
}
