package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.dto.response.PublicacaoDiffItemResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoDiffResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.portal.docflow.service.PublicacaoDiffService.Mudanca;
import com.nexus.portal.shared.exception.BusinessException;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
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
class PublicacaoDiffServiceTest {

  @Mock PublicacaoService publicacaoService;
  @Mock PublicacaoRepository publicacaoRepository;

  PublicacaoDiffService service;

  Cliente cliente;
  Publicacao atual;
  Publicacao anterior;
  UUID paginaMantida = UUID.randomUUID();
  UUID paginaAlterada = UUID.randomUUID();
  UUID paginaNova = UUID.randomUUID();
  UUID paginaRemovida = UUID.randomUUID();

  @BeforeEach
  void setUp() throws Exception {
    service = new PublicacaoDiffService(publicacaoService, publicacaoRepository);
    cliente = new Cliente("ACME", "acme", true);
    setCampo(cliente, "id", UUID.randomUUID());

    anterior = new Publicacao(cliente, "1.0.0", null);
    setCampo(anterior, "id", UUID.randomUUID());
    setCampo(anterior, "createdAt", OffsetDateTime.now().minusDays(2));
    anterior.registrarSucesso(3, 1, "v1.zip", "/tmp/v1.zip", "sha1", "{}");

    atual = new Publicacao(cliente, "2.0.0", null);
    setCampo(atual, "id", UUID.randomUUID());
    setCampo(atual, "createdAt", OffsetDateTime.now());
    atual.registrarSucesso(3, 1, "v2.zip", "/tmp/v2.zip", "sha2", "{}");

    when(publicacaoService.buscar(atual.getId())).thenReturn(atual);
    when(publicacaoService.buscar(anterior.getId())).thenReturn(anterior);
    when(publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(cliente.getId()))
        .thenReturn(List.of(atual, anterior));
  }

  @Test
  void comparar_classificaAdicionadaRemovidaAlteradaEInalterada() {
    when(publicacaoService.arvorePaginas(anterior.getId())).thenReturn(List.of(
        item(paginaMantida, "Login", "hash-igual", 0),
        item(paginaAlterada, "Cadastro", "hash-antigo", 1),
        item(paginaRemovida, "Obsoleta", "hash-x", 2)));
    when(publicacaoService.arvorePaginas(atual.getId())).thenReturn(List.of(
        item(paginaMantida, "Login", "hash-igual", 0),
        item(paginaAlterada, "Cadastro", "hash-novo", 1),
        item(paginaNova, "Relatórios", "hash-y", 2)));

    PublicacaoDiffResponse diff = service.comparar(atual.getId(), anterior.getId());

    assertThat(mudanca(diff, paginaMantida)).isEqualTo(Mudanca.INALTERADA.name());
    assertThat(mudanca(diff, paginaAlterada)).isEqualTo(Mudanca.ALTERADA.name());
    assertThat(mudanca(diff, paginaNova)).isEqualTo(Mudanca.ADICIONADA.name());
    assertThat(mudanca(diff, paginaRemovida)).isEqualTo(Mudanca.REMOVIDA.name());
    assertThat(diff.totaisPorMudanca()).containsEntry(Mudanca.ALTERADA.name(), 1L);
  }

  @Test
  void comparar_mesmoConteudoEmOutraPosicao_ehMovida() {
    when(publicacaoService.arvorePaginas(anterior.getId()))
        .thenReturn(List.of(item(paginaMantida, "Login", "hash-igual", 0)));
    when(publicacaoService.arvorePaginas(atual.getId()))
        .thenReturn(List.of(item(paginaMantida, "Login", "hash-igual", 5)));

    PublicacaoDiffResponse diff = service.comparar(atual.getId(), anterior.getId());

    assertThat(mudanca(diff, paginaMantida)).isEqualTo(Mudanca.MOVIDA.name());
  }

  /** Publicações anteriores ao campo de hash não permitem afirmar que nada mudou. */
  @Test
  void comparar_semHashDeUmDosLados_ficaIndeterminada() {
    when(publicacaoService.arvorePaginas(anterior.getId()))
        .thenReturn(List.of(item(paginaMantida, "Login", null, 0)));
    when(publicacaoService.arvorePaginas(atual.getId()))
        .thenReturn(List.of(item(paginaMantida, "Login", "hash-novo", 0)));

    PublicacaoDiffResponse diff = service.comparar(atual.getId(), anterior.getId());

    assertThat(mudanca(diff, paginaMantida)).isEqualTo(Mudanca.INDETERMINADA.name());
  }

  @Test
  void comparar_semParametro_usaAPublicacaoAnteriorDoMesmoCliente() {
    when(publicacaoService.arvorePaginas(anterior.getId())).thenReturn(List.of());
    when(publicacaoService.arvorePaginas(atual.getId())).thenReturn(List.of());

    PublicacaoDiffResponse diff = service.comparar(atual.getId(), null);

    assertThat(diff.comparadaComId()).isEqualTo(anterior.getId());
    assertThat(diff.versaoComparada()).isEqualTo("1.0.0");
  }

  @Test
  void comparar_semPublicacaoAnterior_recusa() {
    when(publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(cliente.getId()))
        .thenReturn(List.of(atual));

    assertThatThrownBy(() -> service.comparar(atual.getId(), null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Não há publicação anterior");
  }

  @Test
  void comparar_aMesmaPublicacao_recusa() {
    assertThatThrownBy(() -> service.comparar(atual.getId(), atual.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("diferentes");
  }

  @Test
  void comparar_publicacoesDeClientesDiferentes_recusa() throws Exception {
    Cliente outro = new Cliente("Outro", "outro", true);
    setCampo(outro, "id", UUID.randomUUID());
    Publicacao deOutroCliente = new Publicacao(outro, "9.0.0", null);
    setCampo(deOutroCliente, "id", UUID.randomUUID());
    when(publicacaoService.buscar(deOutroCliente.getId())).thenReturn(deOutroCliente);

    assertThatThrownBy(() -> service.comparar(atual.getId(), deOutroCliente.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("mesmo cliente");
  }

  private static String mudanca(PublicacaoDiffResponse diff, UUID paginaId) {
    return diff.itens().stream()
        .filter(item -> item.paginaId().equals(paginaId))
        .map(PublicacaoDiffItemResponse::mudanca)
        .findFirst()
        .orElseThrow();
  }

  private static PublicacaoPaginaSnapshotItem item(UUID id, String titulo, String hash, int ordem) {
    return new PublicacaoPaginaSnapshotItem(id, null, titulo, "COD-" + ordem, "slug-" + ordem,
        ordem, 0, hash);
  }

  private static void setCampo(Object alvo, String campo, Object valor) throws Exception {
    Class<?> tipo = alvo.getClass();
    while (tipo != null) {
      try {
        Field f = tipo.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(alvo, valor);
        return;
      } catch (NoSuchFieldException ex) {
        tipo = tipo.getSuperclass();
      }
    }
    throw new NoSuchFieldException(campo);
  }
}
