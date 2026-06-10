package com.roadmap.securevault.common.service;

public interface NotificationService {
    void sendBootstrapLink(String toEmail, String token);
    void sendInviteLink(String toEmail, String token);
}