package com.nexus.portal.shared.config;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.ObjectProvider;
import com.nexus.portal.shared.security.OrigensManualCors;
import java.util.ArrayList;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      GatewayAuthFilter gatewayAuthFilter,
      JwtAuthFilter jwtAuthFilter,
      CorrelationIdFilter correlationIdFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configure(http))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Propaga SecurityContext nos redispatches ASYNC do SSE (SseEmitter). Sem isso, o
        // AuthorizationFilter vê anônimo no 2º evento e loga Access Denied falso-positivo
        // (response already committed), mesmo para admin autenticado.
        .securityContext(sc -> sc.requireExplicitSave(false))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(401);
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.getWriter().write("{\"error\":\"Não autenticado\",\"status\":401}");
            }))
        .authorizeHttpRequests(auth -> auth
            // Redispatch ASYNC/ERROR do Tomcat após SSE já autenticado no REQUEST inicial.
            .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/health/**",
                "/actuator/info", "/actuator/prometheus", "/actuator/metrics/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/release-orchestrator/webhooks/**").permitAll()
            // GitHub → fila de propostas da IA; autenticado pela assinatura HMAC no controller.
            .requestMatchers(HttpMethod.POST, "/api/v1/ai/webhooks/github").permitAll()
            // Manual para agentes (MCP) e perguntas: autenticados pelo token de prévia do cliente.
            .requestMatchers("/api/v1/docflow/mcp").permitAll()
            // Manual vigente para os sistemas do cliente (site, tela, help-bridge): chave no caminho.
            .requestMatchers(HttpMethod.GET, "/api/v1/manual/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/manual/*/eventos").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/v1/ai/manual/*/perguntar").permitAll()
            .requestMatchers("/api/v1/preview/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/public/publicacoes/download").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/docflow/paginas/*/anexos/*/download").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/docflow/clientes/*/logo").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/docflow/empresa/logo").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(gatewayAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(correlationIdFilter, GatewayAuthFilter.class)
        .addFilterAfter(jwtAuthFilter, GatewayAuthFilter.class)
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${docflow.cors.allowed-origins}") String allowedOrigins,
      ObjectProvider<OrigensManualCors> origensManual) {
    List<String> globais = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(origin -> !origin.isBlank())
        .toList();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(globais);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(
        "Authorization", "Content-Type", CorrelationIdFilter.HEADER_NAME,
        GatewayAuthFilter.HEADER_GATEWAY_KEY,
        GatewayAuthFilter.HEADER_GATEWAY_USER,
        GatewayAuthFilter.HEADER_GATEWAY_PERMISSOES));
    config.setExposedHeaders(List.of("Content-Disposition", CorrelationIdFilter.HEADER_NAME));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return request -> {
      String caminho = request.getRequestURI();
      if (!ROTAS_MANUAL.stream().anyMatch(caminho::startsWith)) {
        return source.getCorsConfiguration(request);
      }
      // Sistemas do cliente chamam o manual do navegador: origens das chaves de integração ativas.
      CorsConfiguration manual = new CorsConfiguration();
      List<String> origens = new ArrayList<>(globais);
      OrigensManualCors provedor = origensManual.getIfAvailable();
      if (provedor != null) {
        origens.addAll(provedor.origens());
      }
      manual.setAllowedOrigins(origens);
      manual.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
      manual.setAllowedHeaders(List.of("Authorization", "Content-Type"));
      return manual;
    };
  }

  private static final List<String> ROTAS_MANUAL =
      List.of("/api/v1/manual/", "/api/v1/ai/manual/", "/api/v1/docflow/mcp");
}
