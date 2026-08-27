package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.PreviewToken;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.PreviewTokenRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.identityaccess.service.EscopoResolver;
import java.security.Principal;
import java.time.OffsetDateTime;
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
class PreviewTokenServiceTest {

  @Mock PreviewTokenRepository previewTokenRepository;
  @Mock ClienteRepository clienteRepository;
  @Mock GeradorPacoteService geradorPacoteService;
  @Mock EscopoResolver escopoResolver;

  PreviewTokenService service;

  UUID clienteId;
  Cliente cliente;
  Principal principal;

  @BeforeEach
  void setUp() {
    service = new PreviewTokenService(previewTokenRepository, clienteRepository, geradorPacoteService, escopoResolver, new PreviewRateLimiter());
    clienteId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    principal = () -> "admin";

    when(previewTokenRepository.save(any(PreviewToken.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(escopoResolver.podeAcessarCliente(any(UUID.class))).thenReturn(true);
  }

  @Test
  void gerar_usa24hQuandoHorasInvalidas() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

    PreviewToken token = service.gerar(clienteId, 0, principal);

    assertThat(token.getClienteId()).isEqualTo(clienteId);
    assertThat(token.getToken()).isNotBlank();
    assertThat(token.getExpiresAt()).isAfter(OffsetDateTime.now().plusHours(23))
        .isBefore(OffsetDateTime.now().plusHours(25));
    assertThat(token.isAtivo()).isTrue();
    verify(previewTokenRepository).save(any(PreviewToken.class));
  }

  @Test
  void gerar_respeitaHorasCustomizadas() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

    PreviewToken token = service.gerar(clienteId, 72, principal);

    assertThat(token.getExpiresAt()).isAfter(OffsetDateTime.now().plusHours(71))
        .isBefore(OffsetDateTime.now().plusHours(73));
  }

  @Test
  void gerar_usaSystemQuandoPrincipalNulo() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));

    PreviewToken token = service.gerar(clienteId, 1, null);

    assertThat(token.getCreatedBy()).isEqualTo("system");
  }

  @Test
  void gerar_falhaSeClienteNaoExiste() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.gerar(clienteId, 24, principal))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cliente");
  }

  @Test
  void listar_retornaSomenteAtivosDoCliente() {
    var t1 = new PreviewToken(clienteId, "a", OffsetDateTime.now().plusHours(1), "admin");
    var t2 = new PreviewToken(clienteId, "b", OffsetDateTime.now().plusHours(2), "admin");
    when(previewTokenRepository.findByClienteIdAndAtivoTrue(clienteId))
        .thenReturn(List.of(t1, t2));

    List<PreviewToken> lista = service.listar(clienteId);

    assertThat(lista).containsExactly(t1, t2);
  }

  @Test
  void revogar_marcaTokenComoInativo() {
    UUID tokenId = UUID.randomUUID();
    PreviewToken pt = new PreviewToken(clienteId, "abc", OffsetDateTime.now().plusHours(1), "admin");
    when(previewTokenRepository.findById(tokenId)).thenReturn(Optional.of(pt));

    service.revogar(tokenId, principal);

    assertThat(pt.isAtivo()).isFalse();
  }

  @Test
  void revogar_falhaSeTokenNaoExiste() {
    UUID tokenId = UUID.randomUUID();
    when(previewTokenRepository.findById(tokenId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.revogar(tokenId, principal))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Token");
  }

  @Test
  void renderizarPreview_retornaHtmlDoGerador() {
    PreviewToken pt = new PreviewToken(clienteId, "tok", OffsetDateTime.now().plusHours(1), "admin");
    when(previewTokenRepository.findByTokenAndAtivoTrue("tok")).thenReturn(Optional.of(pt));
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
    when(geradorPacoteService.previewHtml(cliente, "preview")).thenReturn("<html>ok</html>");

    String html = service.renderizarPreview("tok");

    assertThat(html).isEqualTo("<html>ok</html>");
  }

  @Test
  void renderizarPreview_falhaSeTokenInexistenteOuInativo() {
    when(previewTokenRepository.findByTokenAndAtivoTrue("x")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.renderizarPreview("x"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Token inválido");
  }

  @Test
  void renderizarPreview_falhaSeTokenExpirado() {
    PreviewToken pt = new PreviewToken(clienteId, "old", OffsetDateTime.now().minusHours(1), "admin");
    when(previewTokenRepository.findByTokenAndAtivoTrue("old")).thenReturn(Optional.of(pt));

    assertThatThrownBy(() -> service.renderizarPreview("old"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("expirado");
  }

  @Test
  void gerar_bloqueiaQuandoEscopoNegaEscrita() {
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
    doThrow(new BusinessException("Acesso somente leitura para este cliente."))
        .when(escopoResolver).assertPodeEscreverEmCliente(clienteId);

    assertThatThrownBy(() -> service.gerar(clienteId, 24, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("somente leitura");
  }

  @Test
  void listar_retornaVazioSeEscopoBloqueiaAcesso() {
    when(escopoResolver.podeAcessarCliente(clienteId)).thenReturn(false);

    List<PreviewToken> lista = service.listar(clienteId);

    assertThat(lista).isEmpty();
  }

  @Test
  void revogar_bloqueiaQuandoEscopoNegaEscritaNoClienteDoToken() {
    UUID tokenId = UUID.randomUUID();
    PreviewToken pt = new PreviewToken(clienteId, "abc", OffsetDateTime.now().plusHours(1), "admin");
    when(previewTokenRepository.findById(tokenId)).thenReturn(Optional.of(pt));
    doThrow(new BusinessException("Acesso somente leitura para este cliente."))
        .when(escopoResolver).assertPodeEscreverEmCliente(clienteId);

    assertThatThrownBy(() -> service.revogar(tokenId, principal))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("somente leitura");
    assertThat(pt.isAtivo()).isTrue();
  }

  @Test
  void renderizarPreview_bloqueiaSeEscopoRestringeAcessoAoCliente() {
    PreviewToken pt = new PreviewToken(clienteId, "tok", OffsetDateTime.now().plusHours(1), "admin");
    when(previewTokenRepository.findByTokenAndAtivoTrue("tok")).thenReturn(Optional.of(pt));
    when(escopoResolver.podeAcessarCliente(clienteId)).thenReturn(false);

    assertThatThrownBy(() -> service.renderizarPreview("tok"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Token inválido");
  }

  @Test
  void renderizarPreview_falhaSeClienteNaoEncontrado() {
    PreviewToken pt = new PreviewToken(clienteId, "tok", OffsetDateTime.now().plusHours(1), "admin");
    when(previewTokenRepository.findByTokenAndAtivoTrue("tok")).thenReturn(Optional.of(pt));
    when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.renderizarPreview("tok"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Cliente");
  }
}
