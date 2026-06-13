package com.roadmap.securevault.service;

import com.roadmap.securevault.config.KafkaTopics;
import com.roadmap.securevault.dto.UserEvent;
import com.roadmap.securevault.entity.User;
import com.roadmap.securevault.repo.UserRepository;
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
            case CREATED, UPDATED -> userRepository.save(
                    User.builder()
                            .id(event.id())
                            .username(event.username())
                            .email(event.email())
                            .build()
            );
            case DELETED -> userRepository.deleteById(event.id());
        }
    }
}