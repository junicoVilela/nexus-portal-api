package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.dto.request.ModuloRequest;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.ModuloRepository;
import br.com.softon.portal.docflow.repository.PaginaRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModuloServiceTest {

  @Mock ModuloRepository moduloRepository;
  @Mock ProjetoService projetoService;
  @Mock PaginaRepository paginaRepository;
  @Mock br.com.softon.rbac.service.AuditoriaService auditoriaService;

  ModuloService service;

  UUID projetoId;
  Projeto projeto;

  @BeforeEach
  void setUp() throws Exception {
    service = new ModuloService(moduloRepository, projetoService, paginaRepository, auditoriaService);
    projetoId = UUID.randomUUID();
    projeto = new Projeto("Suite", "suite", null, true);
    setId(projeto, projetoId);
    when(projetoService.buscar(projetoId)).thenReturn(projeto);
    when(moduloRepository.save(any(Modulo.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_normalizaSlugEDefaultsOrdemZeroEAtivoTrue() {
    Modulo m = service.criar(new ModuloRequest("Portal Web", null, "d", null, null, projetoId));

    assertThat(m.getSlug()).isEqualTo("portal-web");
    assertThat(m.getOrdem()).isZero();
    assertThat(m.isAtivo()).isTrue();
    assertThat(m.getProjeto()).isEqualTo(projeto);
  }

  @Test
  void criar_bloqueiaSlugDuplicadoNoMesmoProjeto() {
    when(moduloRepository.existsBySlugAndProjeto_Id("portal", projetoId)).thenReturn(true);

    assertThatThrownBy(() -> service.criar(new ModuloRequest("Portal", "portal", null, 1, true, projetoId)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
  }

  @Test
  void atualizar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(moduloRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atualizar(id, new ModuloRequest("X", "x", null, 1, true, projetoId)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void atualizar_bloqueiaColisaoComOutroModuloDoMesmoProjeto() throws Exception {
    UUID id = UUID.randomUUID();
    Modulo existente = new Modulo("A", "a", null, 1, true, projeto);
    setId(existente, id);
    when(moduloRepository.findById(id)).thenReturn(Optional.of(existente));
    when(moduloRepository.existsBySlugAndProjeto_IdAndIdNot("b", projetoId, id)).thenReturn(true);

    assertThatThrownBy(() -> service.atualizar(id, new ModuloRequest("B", "b", null, 1, true, projetoId)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void buscar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(moduloRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void excluir_semPaginas_removeModuloEAudita() {
    UUID id = UUID.randomUUID();
    Modulo modulo = new Modulo("Cadastros", "cadastros", null, 1, true, projeto);
    when(moduloRepository.findById(id)).thenReturn(Optional.of(modulo));

    service.excluir(id, null);

    verify(moduloRepository).delete(modulo);
    verify(auditoriaService).registrar("MODULO", id, "EXCLUIR", "Módulo excluído: Cadastros", null);
  }

  @Test
  void excluir_comPaginasOrientaRemocaoPrevia() {
    UUID id = UUID.randomUUID();
    when(moduloRepository.findById(id))
        .thenReturn(Optional.of(new Modulo("Cadastros", "cadastros", null, 1, true, projeto)));
    when(paginaRepository.existsByModulo_Id(id)).thenReturn(true);

    assertThatThrownBy(() -> service.excluir(id, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("páginas");

    verify(moduloRepository, never()).delete(any(Modulo.class));
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
