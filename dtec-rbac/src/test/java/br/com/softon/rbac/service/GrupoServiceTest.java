package br.com.softon.rbac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.rbac.dto.request.GrupoPermissoesRequest;
import br.com.softon.rbac.dto.request.GrupoRequest;
import br.com.softon.rbac.dto.request.GrupoUsuariosRequest;
import br.com.softon.rbac.entity.Grupo;
import br.com.softon.rbac.repository.GrupoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GrupoServiceTest {

  @Mock GrupoRepository grupoRepository;
  @Mock RbacService rbacService;
  @Mock AuditoriaService auditoriaService;
  @Mock SecurityContext securityContext;
  @Mock Authentication authentication;

  GrupoService grupoService;
  java.security.Principal principal = () -> "admin";

  @BeforeEach
  void setUp() {
    grupoService = new GrupoService(grupoRepository, rbacService, auditoriaService);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("admin");
    SecurityContextHolder.setContext(securityContext);
    when(grupoRepository.findCodigosComPrefixo(any())).thenReturn(List.of());
  }

  @Test
  void criar_deveSalvarGrupoComDadosCorretos() {
    GrupoRequest request = new GrupoRequest("Editores", "Grupo de editores", true);
    when(grupoRepository.existsByNomeIgnoreCase("Editores")).thenReturn(false);
    when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Grupo resultado = grupoService.criar(request, principal);

    assertThat(resultado.getNome()).isEqualTo("Editores");
    assertThat(resultado.getCodigo()).isEqualTo("EDITORES");
    assertThat(resultado.getDescricao()).isEqualTo("Grupo de editores");
    assertThat(resultado.isAtivo()).isTrue();
    verify(grupoRepository).save(any(Grupo.class));
  }

  @Test
  void criar_deveLancarExcecaoQuandoNomeDuplicado() {
    GrupoRequest request = new GrupoRequest("Editores", null, true);
    when(grupoRepository.existsByNomeIgnoreCase("Editores")).thenReturn(true);

    assertThatThrownBy(() -> grupoService.criar(request, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("nome informado");
  }

  @Test
  void criar_deveUsarAtivoTrueQuandoNaoInformado() {
    GrupoRequest request = new GrupoRequest("Admins", null, null);
    when(grupoRepository.existsByNomeIgnoreCase("Admins")).thenReturn(false);
    when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Grupo resultado = grupoService.criar(request, principal);

    assertThat(resultado.isAtivo()).isTrue();
  }

  @Test
  void atualizar_deveAlterarDadosDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "Antigo", null, true);
    GrupoRequest request = new GrupoRequest("Novo Nome", "nova desc", false);

    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(grupoRepository.existsByNomeIgnoreCaseAndIdNot("Novo Nome", id)).thenReturn(false);

    Grupo resultado = grupoService.atualizar(id, request, principal);

    assertThat(resultado.getNome()).isEqualTo("Novo Nome");
    assertThat(resultado.getDescricao()).isEqualTo("nova desc");
    assertThat(resultado.isAtivo()).isFalse();
  }

  @Test
  void atualizar_deveLancarExcecaoQuandoNomeDuplicadoEmOutroGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "Original", null, true);
    GrupoRequest request = new GrupoRequest("Duplicado", null, true);

    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(grupoRepository.existsByNomeIgnoreCaseAndIdNot("Duplicado", id)).thenReturn(true);

    assertThatThrownBy(() -> grupoService.atualizar(id, request, principal))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void alterarStatus_deveAtualizarStatusDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    Grupo resultado = grupoService.alterarStatus(id, false, principal);

    assertThat(resultado.isAtivo()).isFalse();
  }

  @Test
  void excluir_deveDeletarGrupoExistente() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    grupoService.excluir(id, principal);

    verify(grupoRepository).delete(grupo);
  }

  @Test
  void buscar_deveLancarNotFoundQuandoNaoExistir() {
    UUID id = UUID.randomUUID();
    when(grupoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> grupoService.buscar(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Grupo não encontrado");
  }

  @Test
  void salvarPermissoes_deveAtualizarListaDePermissoes() {
    UUID id = UUID.randomUUID();
    UUID permId = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(rbacService.idsPorCodigos(List.of("CLIENTE:LER", "CLIENTE:EDITAR")))
        .thenReturn(List.of(permId));

    grupoService.salvarPermissoes(id, new GrupoPermissoesRequest(
        List.of("CLIENTE:LER", "CLIENTE:EDITAR")), principal);

    assertThat(grupo.getPermissaoIds()).containsExactly(permId);
  }

  @Test
  void listarPermissoes_deveRetornarPermissoesDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(rbacService.codigosPermissoesDoGrupo(grupo)).thenReturn(List.of("USUARIO:LER"));

    List<String> permissoes = grupoService.listarPermissoes(id);

    assertThat(permissoes).containsExactly("USUARIO:LER");
  }

  @Test
  void salvarMembros_deveAtualizarListaDeUsuarios() {
    UUID id = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    grupoService.salvarMembros(id, new GrupoUsuariosRequest(List.of(usuarioId)), principal);

    assertThat(grupo.getUsuarios()).containsExactly(usuarioId);
  }

  @Test
  void listarMembros_deveRetornarIdsDeUsuariosDoGrupo() {
    UUID id = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", "G1", null, true);
    grupo.atualizarUsuarios(List.of(usuarioId));
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    List<UUID> membros = grupoService.listarMembros(id);

    assertThat(membros).containsExactly(usuarioId);
  }
}
