package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.dto.response.MeResponse;
import br.com.softon.rbac.entity.Usuario;
import br.com.softon.rbac.repository.UsuarioRepository;
import br.com.softon.portal.shared.config.JwtService;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

  @Mock UsuarioRepository usuarioRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtService jwtService;
  @Mock RbacService rbacService;
  @Mock AuditoriaService auditoriaService;

  UsuarioService service;

  UUID userId;
  Usuario usuario;
  java.security.Principal principal = () -> "admin";

  @BeforeEach
  void setUp() throws Exception {
    service = new UsuarioService(usuarioRepository, passwordEncoder, jwtService, rbacService, auditoriaService);
    userId = UUID.randomUUID();
    usuario = new Usuario("admin", "hash", "Administrador", "a@x.com");
    setId(usuario, userId);
    when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void autenticar_geraJwtComPermissoesDoUsuario() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("admin")).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("plain", "hash")).thenReturn(true);
    when(rbacService.permissoesDoUsuario(userId)).thenReturn(List.of("CLIENTE:LER", "RELEASE:CRIAR"));
    when(jwtService.gerarToken("admin", List.of("CLIENTE:LER", "RELEASE:CRIAR"))).thenReturn("tok");

    String jwt = service.autenticar("admin", "plain");

    assertThat(jwt).isEqualTo("tok");
  }

  @Test
  void autenticar_falhaSeUsuarioInexistenteOuInativo() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("x")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.autenticar("x", "y"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("inválidos");
    verifyNoInteractions(jwtService, rbacService);
  }

  @Test
  void autenticar_falhaSeSenhaNaoBate() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("admin")).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

    assertThatThrownBy(() -> service.autenticar("admin", "wrong"))
        .isInstanceOf(BusinessException.class);
    verifyNoInteractions(jwtService);
  }

  @Test
  void me_agregaGruposEPermissoesDoRbac() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("admin")).thenReturn(Optional.of(usuario));
    when(rbacService.gruposDoUsuario(userId)).thenReturn(List.of());
    when(rbacService.permissoesDoUsuario(userId)).thenReturn(List.of("CLIENTE:LER"));

    MeResponse me = service.me("admin");

    assertThat(me.id()).isEqualTo(userId);
    assertThat(me.username()).isEqualTo("admin");
    assertThat(me.permissoes()).containsExactly("CLIENTE:LER");
  }

  @Test
  void me_falhaSeUsuarioInexistente() {
    when(usuarioRepository.findByUsernameAndAtivoTrue("x")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.me("x"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void criar_bloqueiaSeUsernameJaExiste() {
    when(usuarioRepository.existsByUsername("admin")).thenReturn(true);

    assertThatThrownBy(() -> service.criar("admin", "x", "y", "z@x.com", principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Já existe");
  }

  @Test
  void criar_criptografaSenhaAntesDeSalvar() {
    when(usuarioRepository.existsByUsername("novo")).thenReturn(false);
    when(passwordEncoder.encode("plain")).thenReturn("hashed");

    Usuario u = service.criar("novo", "plain", "Novo", "n@x.com", principal);

    assertThat(u.getPassword()).isEqualTo("hashed");
    assertThat(u.getUsername()).isEqualTo("novo");
  }

  @Test
  void atualizar_delegaAoAgregado() {
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));

    Usuario u = service.atualizar(userId, "Novo Nome", "novo@x.com", false, principal);

    assertThat(u.getNome()).isEqualTo("Novo Nome");
    assertThat(u.getEmail()).isEqualTo("novo@x.com");
    assertThat(u.isAtivo()).isFalse();
  }

  @Test
  void atualizar_falhaSeInexistente() {
    when(usuarioRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atualizar(userId, "x", "y", true, principal))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void alterarSenha_criptografaAntes() {
    when(usuarioRepository.findById(userId)).thenReturn(Optional.of(usuario));
    when(passwordEncoder.encode("nova")).thenReturn("hash-nova");

    service.alterarSenha(userId, "nova", principal);

    assertThat(usuario.getPassword()).isEqualTo("hash-nova");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
