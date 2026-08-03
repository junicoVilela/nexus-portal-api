package com.nexus.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.nexus")
@ConfigurationPropertiesScan(basePackages = "com.nexus")
@EntityScan(basePackages = "com.nexus")
@EnableJpaRepositories(basePackages = "com.nexus")
@EnableAsync
@EnableScheduling
public class NexusPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(NexusPortalApplication.class, args);
  }
}
