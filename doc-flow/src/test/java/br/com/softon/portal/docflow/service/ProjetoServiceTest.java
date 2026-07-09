package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.softon.portal.docflow.dto.request.ProjetoRequest;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.repository.ProjetoRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjetoServiceTest {

  @Mock ProjetoRepository projetoRepository;

  ProjetoService service;

  @BeforeEach
  void setUp() {
    service = new ProjetoService(projetoRepository);
    when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void criar_normalizaSlugAPartirDoNomeQuandoNaoInformado() {
    Projeto p = service.criar(new ProjetoRequest("Meu Projeto  ", null, "desc", true));

    assertThat(p.getNome()).isEqualTo("Meu Projeto");
    assertThat(p.getSlug()).isEqualTo("meu-projeto");
    assertThat(p.isAtivo()).isTrue();
  }

  @Test
  void criar_defaultAtivoTrueQuandoNull() {
    Projeto p = service.criar(new ProjetoRequest("X", "x", null, null));

    assertThat(p.isAtivo()).isTrue();
  }

  @Test
  void criar_bloqueiaSlugDuplicado() {
    when(projetoRepository.existsBySlug("existente")).thenReturn(true);

    assertThatThrownBy(() -> service.criar(new ProjetoRequest("X", "existente", null, true)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("slug");
  }

  @Test
  void atualizar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(projetoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.atualizar(id, new ProjetoRequest("X", "x", "", true)))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void atualizar_bloqueiaSlugConflitanteEmOutroRegistro() throws Exception {
    UUID id = UUID.randomUUID();
    Projeto existente = new Projeto("A", "a", null, true);
    setId(existente, id);
    when(projetoRepository.findById(id)).thenReturn(Optional.of(existente));
    when(projetoRepository.existsBySlugAndIdNot("b", id)).thenReturn(true);

    assertThatThrownBy(() -> service.atualizar(id, new ProjetoRequest("B", "b", null, true)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void buscar_falhaSeInexistente() {
    UUID id = UUID.randomUUID();
    when(projetoRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(id)).isInstanceOf(NotFoundException.class);
  }

  @Test
  void buscarTodos_falhaSeAlgumIdNaoExiste() {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    Projeto p1 = new Projeto("A", "a", null, true);
    when(projetoRepository.findAllById(List.of(a, b))).thenReturn(List.of(p1));

    assertThatThrownBy(() -> service.buscarTodos(List.of(a, b)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não existem");
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
