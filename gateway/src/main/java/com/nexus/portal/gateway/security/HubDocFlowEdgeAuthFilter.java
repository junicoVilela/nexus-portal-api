package com.nexus.portal.gateway.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Equivalente em servlet ao antigo {@code GlobalFilter} do Spring Cloud Gateway: para {@code /api/doc-flow/**}
 * valida JWT e injeta {@code X-Gateway-*}, alinhado ao {@code GatewayAuthFilter} do Doc Flow.
 */
public class HubDocFlowEdgeAuthFilter extends OncePerRequestFilter {

  public static final String HEADER_GATEWAY_KEY = "X-Gateway-Key";
  public static final String HEADER_GATEWAY_USER = "X-Gateway-User";
  public static final String HEADER_GATEWAY_PERMISSOES = "X-Gateway-Permissoes";

  private static final String DOC_FLOW_API_PREFIX = "/api/doc-flow";

  private static final List<String> PUBLIC_PATHS = List.of(
      "/api/doc-flow/auth/",
      "/api/doc-flow/preview/",
      "/api/doc-flow/public/"
  );

  private static final List<String> PUBLIC_GET_PATHS = List.of(
      "/api/doc-flow/clientes/",
      "/api/doc-flow/empresa/"
  );

  private final GatewayEdgeJwtService jwtService;
  private final String gatewayApiKey;

  public HubDocFlowEdgeAuthFilter(GatewayEdgeJwtService jwtService,
      @Value("${docflow.security.gateway-api-key:}") String gatewayApiKey) {
    this.jwtService = jwtService;
    this.gatewayApiKey = gatewayApiKey;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain) throws ServletException, IOException {

    String contextPath = request.getContextPath() != null ? request.getContextPath() : "";
    String uri = request.getRequestURI();
    if (!uri.startsWith(contextPath)) {
      filterChain.doFilter(request, response);
      return;
    }
    String pathWithinContext = uri.substring(contextPath.length());

    if (!pathWithinContext.startsWith(DOC_FLOW_API_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (HttpMethod.OPTIONS.matches(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    if (PUBLIC_PATHS.stream().anyMatch(pathWithinContext::startsWith)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (HttpMethod.GET.matches(request.getMethod())) {
      boolean isPublicGetPath = PUBLIC_GET_PATHS.stream()
          .anyMatch(p -> pathWithinContext.startsWith(p) && pathWithinContext.endsWith("/logo"));
      if (isPublicGetPath) {
        filterChain.doFilter(request, response);
        return;
      }
    }

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      writeUnauthorized(response);
      return;
    }

    String token = authHeader.substring(7);
    if (!jwtService.isValido(token)) {
      writeUnauthorized(response);
      return;
    }

    Claims claims = jwtService.validar(token);
    String username = claims.getSubject();
    @SuppressWarnings("unchecked")
    List<String> permissoes = claims.get("permissoes", List.class);
    String permissoesHeader = (permissoes == null || permissoes.isEmpty()) ? "" : String.join(",", permissoes);

    HeaderMergingRequest wrapped =
        new HeaderMergingRequest(request, Map.of(
            HEADER_GATEWAY_KEY, gatewayApiKey,
            HEADER_GATEWAY_USER, username != null ? username : "",
            HEADER_GATEWAY_PERMISSOES, permissoesHeader));
    filterChain.doFilter(wrapped, response);
  }

  private static void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(401);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getOutputStream().write("{\"error\":\"Não autenticado\",\"status\":401}".getBytes());
  }

  static final class HeaderMergingRequest extends HttpServletRequestWrapper {

    private final Map<String, String> extra;

    HeaderMergingRequest(HttpServletRequest request, Map<String, String> extra) {
      super(request);
      this.extra = Map.copyOf(extra);
    }

    @Override
    public String getHeader(String name) {
      if (name == null) {
        return super.getHeader(name);
      }
      for (Map.Entry<String, String> e : extra.entrySet()) {
        if (e.getKey().equalsIgnoreCase(name)) {
          return e.getValue();
        }
      }
      return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      if (name == null) {
        return super.getHeaders(name);
      }
      for (Map.Entry<String, String> e : extra.entrySet()) {
        if (e.getKey().equalsIgnoreCase(name)) {
          return Collections.enumeration(List.of(e.getValue()));
        }
      }
      return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      Set<String> names = new HashSet<>(Collections.list(super.getHeaderNames()));
      names.addAll(extra.keySet());
      return Collections.enumeration(new ArrayList<>(names));
    }
  }
}
