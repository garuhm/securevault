package com.roadmap.securevault;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        CookieProperties.class,
        OAuth2Properties.class
})
@EnableScheduling
public class SecurevaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurevaultApplication.class, args);
    }

}
