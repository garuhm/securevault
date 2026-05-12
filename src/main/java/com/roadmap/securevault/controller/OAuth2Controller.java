package com.roadmap.securevault.controller;

import com.roadmap.securevault.service.OAuth2Service;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/oauth2")

@RequiredArgsConstructor
public class OAuth2Controller {
    private final OAuth2Service oAuth2Service;

//    get, not post bc its a redirect
    @GetMapping("/link/{provider}")
    public ResponseEntity<Void> linkProvider(@PathVariable String provider,
                                             HttpServletResponse response) {
        oAuth2Service.initiateLink(provider, response);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/oauth2/authorization/" + provider)
                .build();
    }

    @DeleteMapping("/link/{provider}")
    public ResponseEntity<Void> unlinkProvider(@PathVariable String provider) {
        oAuth2Service.unlink(provider);
        return ResponseEntity.noContent().build();
    }
}
