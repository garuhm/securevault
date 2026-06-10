package com.roadmap.securevault;

import com.roadmap.securevault.common.config.properties.CookieProperties;
import com.roadmap.securevault.common.config.properties.JwtProperties;
import com.roadmap.securevault.common.config.properties.RedisProperties;
import com.roadmap.securevault.common.config.properties.SendGridProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        CookieProperties.class,
        SendGridProperties.class,
        RedisProperties.class
})
@EnableMethodSecurity
@EnableJpaAuditing
@EnableScheduling
public class SecurevaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurevaultApplication.class, args);
    }

}
