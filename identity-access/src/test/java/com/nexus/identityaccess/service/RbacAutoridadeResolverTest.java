package com.nexus.identityaccess.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nexus.identityaccess.entity.Usuario;
import com.nexus.identityaccess.repository.UsuarioRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacAutoridadeResolverTest {

  @Mock UsuarioRepository usuarioRepository;
  @Mock RbacService rbacService;

  RbacAutoridadeResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new RbacAutoridadeResolver(usuarioRepository, rbacService);
  }

  @Test
  void devolvePermissoesDoUsuarioAtivo() throws Exception {
    UUID id = UUID.randomUUID();
    Usuario usuario = new Usuario("admin", "hash", "Administrador", "a@x.com");
    Field field = Usuario.class.getDeclaredField("id");
    field.setAccessible(true);
    field.set(usuario, id);

    when(usuarioRepository.findByUsernameAndAtivoTrue("admin")).thenReturn(Optional.of(usuario));
    when(rbacService.permissoesDoUsuario(id)).thenReturn(List.of("HOST:LER"));

    assertThat(resolver.permissoesDoUsername("admin")).containsExactly("HOST:LER");
  }

  @Test
  void devolveNullQuandoUsuarioNaoExisteParaManterJwt() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("ghost")).thenReturn(Optional.empty());

    assertThat(resolver.permissoesDoUsername("ghost")).isNull();
  }

  @Test
  void devolveNullQuandoUsernameVazio() {
    assertThat(resolver.permissoesDoUsername("  ")).isNull();
    assertThat(resolver.permissoesDoUsername(null)).isNull();
  }
}
