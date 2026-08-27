package com.nexus.portal.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexus.portal.shared.security.AutoridadeResolver;
import com.nexus.portal.shared.security.SessaoValidator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

  private static final String SECRET =
      "doc-flow-dev-only-jwt-secret-min-256-bits-do-not-use-in-production-change-me";

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usaPermissoesDoCatalogoQuandoJwtEstaDesatualizado() throws Exception {
    JwtService jwt = new JwtService(SECRET, 86_400_000L, 900_000L);
    String token = jwt.gerarToken("admin", List.of("CLIENTE:LER"));

    JwtAuthFilter filter = new JwtAuthFilter(jwt, sessaoAberta(),
        resolver(username -> List.of("CLIENTE:LER", "HOST:LER")));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

    assertThat(autoridades()).containsExactlyInAnyOrder("CLIENTE:LER", "HOST:LER");
  }

  @Test
  void caiNoJwtQuandoResolverNaoEstaDisponivel() throws Exception {
    JwtService jwt = new JwtService(SECRET, 86_400_000L, 900_000L);
    String token = jwt.gerarToken("admin", List.of("CLIENTE:LER"));

    JwtAuthFilter filter = new JwtAuthFilter(jwt, sessaoAberta(), resolverAusente());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

    assertThat(autoridades()).containsExactly("CLIENTE:LER");
  }

  @Test
  void caiNoJwtQuandoResolverFalha() throws Exception {
    JwtService jwt = new JwtService(SECRET, 86_400_000L, 900_000L);
    String token = jwt.gerarToken("admin", List.of("CLIENTE:LER"));

    JwtAuthFilter filter = new JwtAuthFilter(jwt, sessaoAberta(),
        resolver(username -> {
          throw new IllegalStateException("catalogo indisponivel");
        }));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

    assertThat(autoridades()).containsExactly("CLIENTE:LER");
  }

  private static List<String> autoridades() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<SessaoValidator> sessaoAberta() {
    ObjectProvider<SessaoValidator> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    return provider;
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<AutoridadeResolver> resolver(AutoridadeResolver impl) {
    ObjectProvider<AutoridadeResolver> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(impl);
    return provider;
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<AutoridadeResolver> resolverAusente() {
    ObjectProvider<AutoridadeResolver> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    return provider;
  }
}
