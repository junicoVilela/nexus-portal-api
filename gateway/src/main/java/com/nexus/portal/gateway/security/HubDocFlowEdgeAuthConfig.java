package com.nexus.portal.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class HubDocFlowEdgeAuthConfig {

  @Bean
  public FilterRegistrationBean<HubDocFlowEdgeAuthFilter> hubDocFlowEdgeAuthFilterRegistration(
      GatewayEdgeJwtService jwtService,
      @Value("${docflow.security.gateway-api-key:}") String gatewayApiKey) {
    FilterRegistrationBean<HubDocFlowEdgeAuthFilter> reg =
        new FilterRegistrationBean<>(new HubDocFlowEdgeAuthFilter(jwtService, gatewayApiKey));
    reg.addUrlPatterns("/*");
    reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return reg;
  }
}
