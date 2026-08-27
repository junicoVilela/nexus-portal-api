package com.nexus.portal.shared.config;

import com.nexus.portal.shared.security.AutoridadeResolver;
import com.nexus.portal.shared.security.SessaoValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ObjectProvider<SessaoValidator> sessaoValidator;
  private final ObjectProvider<AutoridadeResolver> autoridadeResolver;

  public JwtAuthFilter(JwtService jwtService, ObjectProvider<SessaoValidator> sessaoValidator,
      ObjectProvider<AutoridadeResolver> autoridadeResolver) {
    this.jwtService = jwtService;
    this.sessaoValidator = sessaoValidator;
    this.autoridadeResolver = autoridadeResolver;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (jwtService.isValido(token)) {
        Claims claims = jwtService.validar(token);
        boolean tokenPacoteParaDownloadPublico =
            JwtService.TYPE_PACOTE_DOWNLOAD.equals(claims.get(JwtService.CLAIM_TOKEN_TYPE));
        if (!tokenPacoteParaDownloadPublico) {
          String jti = claims.getId();
          SessaoValidator validador = sessaoValidator.getIfAvailable();
          if (validador != null && !validador.sessaoAtiva(jti)) {
            // Sessão foi revogada — token JWT ainda válido, mas rejeitamos.
            filterChain.doFilter(request, response);
            return;
          }
          String username = claims.getSubject();
          @SuppressWarnings("unchecked")
          List<String> permissoes = claims.get("permissoes", List.class);
          permissoes = autoridadesAtuais(username, permissoes);
          List<SimpleGrantedAuthority> authorities = new ArrayList<>();
          if (permissoes != null) {
            permissoes.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
          }
          var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Prefere o catálogo RBAC ao claim {@code permissoes} do JWT, para novas
   * funcionalidades (ex.: HOST) valerem na sessão já aberta.
   */
  private List<String> autoridadesAtuais(String username, List<String> claimsDoJwt) {
    AutoridadeResolver resolver = autoridadeResolver.getIfAvailable();
    if (resolver == null || username == null || username.isBlank()) {
      return claimsDoJwt;
    }
    try {
      List<String> atuais = resolver.permissoesDoUsername(username);
      return atuais != null ? atuais : claimsDoJwt;
    } catch (RuntimeException ignored) {
      return claimsDoJwt;
    }
  }
}
