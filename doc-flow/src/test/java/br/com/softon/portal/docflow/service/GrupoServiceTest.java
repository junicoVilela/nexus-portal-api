package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.dto.request.GrupoPermissoesRequest;
import br.com.softon.portal.docflow.dto.request.GrupoRequest;
import br.com.softon.portal.docflow.dto.request.GrupoUsuariosRequest;
import br.com.softon.portal.docflow.entity.Grupo;
import br.com.softon.portal.docflow.repository.GrupoRepository;
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
  @Mock SecurityContext securityContext;
  @Mock Authentication authentication;

  GrupoService grupoService;

  @BeforeEach
  void setUp() {
    grupoService = new GrupoService(grupoRepository);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("admin");
    SecurityContextHolder.setContext(securityContext);
  }

  // -------------------------------------------------------------------------
  // criar
  // -------------------------------------------------------------------------

  @Test
  void criar_deveSalvarGrupoComDadosCorretos() {
    GrupoRequest request = new GrupoRequest("Editores", "Grupo de editores", true);
    when(grupoRepository.existsByNomeIgnoreCase("Editores")).thenReturn(false);
    when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Grupo resultado = grupoService.criar(request);

    assertThat(resultado.getNome()).isEqualTo("Editores");
    assertThat(resultado.getDescricao()).isEqualTo("Grupo de editores");
    assertThat(resultado.isAtivo()).isTrue();
    verify(grupoRepository).save(any(Grupo.class));
  }

  @Test
  void criar_deveLancarExcecaoQuandoNomeDuplicado() {
    GrupoRequest request = new GrupoRequest("Editores", null, true);
    when(grupoRepository.existsByNomeIgnoreCase("Editores")).thenReturn(true);

    assertThatThrownBy(() -> grupoService.criar(request))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("nome informado");
  }

  @Test
  void criar_deveUsarAtivoTrueQuandoNaoInformado() {
    GrupoRequest request = new GrupoRequest("Admins", null, null);
    when(grupoRepository.existsByNomeIgnoreCase("Admins")).thenReturn(false);
    when(grupoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Grupo resultado = grupoService.criar(request);

    assertThat(resultado.isAtivo()).isTrue();
  }

  // -------------------------------------------------------------------------
  // atualizar
  // -------------------------------------------------------------------------

  @Test
  void atualizar_deveAlterarDadosDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("Antigo", null, true);
    GrupoRequest request = new GrupoRequest("Novo Nome", "nova desc", false);

    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(grupoRepository.existsByNomeIgnoreCaseAndIdNot("Novo Nome", id)).thenReturn(false);

    Grupo resultado = grupoService.atualizar(id, request);

    assertThat(resultado.getNome()).isEqualTo("Novo Nome");
    assertThat(resultado.getDescricao()).isEqualTo("nova desc");
    assertThat(resultado.isAtivo()).isFalse();
  }

  @Test
  void atualizar_deveLancarExcecaoQuandoNomeDuplicadoEmOutroGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("Original", null, true);
    GrupoRequest request = new GrupoRequest("Duplicado", null, true);

    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));
    when(grupoRepository.existsByNomeIgnoreCaseAndIdNot("Duplicado", id)).thenReturn(true);

    assertThatThrownBy(() -> grupoService.atualizar(id, request))
        .isInstanceOf(BusinessException.class);
  }

  // -------------------------------------------------------------------------
  // alterarStatus
  // -------------------------------------------------------------------------

  @Test
  void alterarStatus_deveAtualizarStatusDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    Grupo resultado = grupoService.alterarStatus(id, false);

    assertThat(resultado.isAtivo()).isFalse();
  }

  // -------------------------------------------------------------------------
  // excluir
  // -------------------------------------------------------------------------

  @Test
  void excluir_deveDeletarGrupoExistente() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    grupoService.excluir(id);

    verify(grupoRepository).delete(grupo);
  }

  // -------------------------------------------------------------------------
  // buscar
  // -------------------------------------------------------------------------

  @Test
  void buscar_deveLancarNotFoundQuandoNaoExistir() {
    UUID id = UUID.randomUUID();
    when(grupoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> grupoService.buscar(id))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Grupo não encontrado");
  }

  // -------------------------------------------------------------------------
  // permissoes
  // -------------------------------------------------------------------------

  @Test
  void salvarPermissoes_deveAtualizarListaDePermissoes() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    GrupoPermissoesRequest request = new GrupoPermissoesRequest(
        List.of("docflow.visualizar", "docflow.editar"));

    grupoService.salvarPermissoes(id, request);

    assertThat(grupo.getPermissoes()).containsExactlyInAnyOrder("docflow.visualizar", "docflow.editar");
  }

  @Test
  void listarPermissoes_deveRetornarPermissoesDoGrupo() {
    UUID id = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    grupo.atualizarPermissoes(List.of("admin.usuarios"));
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    List<String> permissoes = grupoService.listarPermissoes(id);

    assertThat(permissoes).containsExactly("admin.usuarios");
  }

  // -------------------------------------------------------------------------
  // membros
  // -------------------------------------------------------------------------

  @Test
  void salvarMembros_deveAtualizarListaDeUsuarios() {
    UUID id = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    GrupoUsuariosRequest request = new GrupoUsuariosRequest(List.of(usuarioId));
    grupoService.salvarMembros(id, request);

    assertThat(grupo.getUsuarios()).containsExactly(usuarioId);
  }

  @Test
  void listarMembros_deveRetornarIdsDeUsuariosDoGrupo() {
    UUID id = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    Grupo grupo = new Grupo("G1", null, true);
    grupo.atualizarUsuarios(List.of(usuarioId));
    when(grupoRepository.findById(id)).thenReturn(Optional.of(grupo));

    List<UUID> membros = grupoService.listarMembros(id);

    assertThat(membros).containsExactly(usuarioId);
  }
}
