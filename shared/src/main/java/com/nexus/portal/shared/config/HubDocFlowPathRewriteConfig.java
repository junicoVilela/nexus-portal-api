package com.nexus.portal.shared.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class HubDocFlowPathRewriteConfig {

  @Bean
  public FilterRegistrationBean<HubDocFlowPathRewriteFilter> hubDocFlowPathRewriteFilterRegistration() {
    FilterRegistrationBean<HubDocFlowPathRewriteFilter> reg =
        new FilterRegistrationBean<>(new HubDocFlowPathRewriteFilter());
    reg.addUrlPatterns("/*");
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return reg;
  }
}
