package com.roadmap.securevault;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.KeycloakProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        KeycloakProperties.class,
        CookieProperties.class
})
@EnableScheduling
public class SecurevaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurevaultApplication.class, args);
    }

}
