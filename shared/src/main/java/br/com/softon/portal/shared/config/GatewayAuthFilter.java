package br.com.softon.portal.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Confere headers injetados pelo módulo {@code softon-portal-gateway} (filtro edge) após validação JWT.
 * Quando o X-Gateway-Key bate com o segredo configurado, o usuário e roles do header
 * são usados para popular o SecurityContext, dispensando nova validação de JWT.
 * Sem o header (acesso direto em dev), o JwtAuthFilter assume como fallback.
 */
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_GATEWAY_KEY = "X-Gateway-Key";
    public static final String HEADER_GATEWAY_USER = "X-Gateway-User";
    public static final String HEADER_GATEWAY_ROLES = "X-Gateway-Roles";

    private final String gatewayApiKey;

    public GatewayAuthFilter(
            @Value("${docflow.security.gateway-api-key:}") String gatewayApiKey) {
        this.gatewayApiKey = gatewayApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!gatewayApiKey.isBlank()) {
            String receivedKey = request.getHeader(HEADER_GATEWAY_KEY);
            if (gatewayApiKey.equals(receivedKey)) {
                String username = request.getHeader(HEADER_GATEWAY_USER);
                String rolesHeader = request.getHeader(HEADER_GATEWAY_ROLES);

                if (username != null && !username.isBlank()) {
                    List<SimpleGrantedAuthority> authorities =
                            (rolesHeader == null || rolesHeader.isBlank())
                                    ? List.of()
                                    : Arrays.stream(rolesHeader.split(","))
                                            .map(String::trim)
                                            .filter(r -> !r.isBlank())
                                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
