//package com.roadmap.securevault.scheduling;
//
//import com.roadmap.securevault.service.helper.RefreshJwtService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class RefreshTokenCleaner {
//
//    private final RefreshJwtService refreshJwtService;
//
//    @Scheduled(fixedRate = 900000) // 15 minutes
//    public void cleanUpRevokedAndExpired() {
//        refreshJwtService.cleanUpRevokedAndExpired();
//    }
//}
