package com.longobuccodev.app_adm_obras;

import com.longobuccodev.app_adm_obras.infra.bootstrap.BootstrapProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BootstrapProperties.class)
public class AppAdmObrasApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppAdmObrasApplication.class, args);
    }
}
