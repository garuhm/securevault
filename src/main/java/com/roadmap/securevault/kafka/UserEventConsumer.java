package com.roadmap.securevault.kafka;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.kafka.events.UserEvent;
import com.roadmap.securevault.mapper.UserMapper;
import com.roadmap.securevault.repo.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserRepository userRepository;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onUserEvent(UserEvent event) {
        switch (event.type()) {
            case CREATED -> userRepository.save(
                    User.builder()
                            .id(event.id())
                            .username(event.username())
                            .email(event.email())
                            .build()
            );
            case UPDATED -> {
                User user = userRepository.findById(event.id())
                        .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + event.id()));
                UserMapper.updateUser(user, event.username(), event.email());

                userRepository.save(user);
            }
            case DELETED -> userRepository.deleteById(event.id());
        }
    }
}