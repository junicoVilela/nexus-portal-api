package br.com.softon.portal.shared.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
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
          String username = claims.getSubject();
          @SuppressWarnings("unchecked")
          List<String> permissoes = claims.get("permissoes", List.class);
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
}
