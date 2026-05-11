package com.roadmap.securevault;

import com.roadmap.securevault.config.properties.CookieProperties;
import com.roadmap.securevault.config.properties.JwtProperties;
import com.roadmap.securevault.config.properties.OAuth2Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        CookieProperties.class,
        OAuth2Properties.class
})
public class SecurevaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurevaultApplication.class, args);
    }

}
