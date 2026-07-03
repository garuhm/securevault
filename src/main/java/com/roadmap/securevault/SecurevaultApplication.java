package com.roadmap.securevault;

import com.roadmap.securevault.config.properties.KeycloakProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableConfigurationProperties({
        KeycloakProperties.class,
        OAuth2Properties.class
})
@EnableMethodSecurity
public class SecurevaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurevaultApplication.class, args);
    }

}
