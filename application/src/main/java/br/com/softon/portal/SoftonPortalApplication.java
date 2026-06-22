package br.com.softon.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "br.com.softon.portal")
@ConfigurationPropertiesScan(basePackages = "br.com.softon.portal")
@EntityScan(basePackages = "br.com.softon.portal")
@EnableJpaRepositories(basePackages = "br.com.softon.portal")
@EnableAsync
@EnableScheduling
public class SoftonPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(SoftonPortalApplication.class, args);
  }
}
