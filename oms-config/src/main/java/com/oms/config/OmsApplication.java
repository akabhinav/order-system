package com.oms.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.oms.config",
    "com.oms.application",
    "com.oms.infrastructure",
    "com.oms.api"
})
@EntityScan("com.oms.infrastructure.persistence.jpa.entity")
@EnableJpaRepositories("com.oms.infrastructure.persistence.jpa.repository")
@EnableKafka
@EnableScheduling
@EnableConfigurationProperties(OmsProperties.class)
public class OmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(OmsApplication.class, args);
    }
}
