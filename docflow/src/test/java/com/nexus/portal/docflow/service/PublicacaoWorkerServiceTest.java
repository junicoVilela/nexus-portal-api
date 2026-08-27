package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.PublicacaoChangelog;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoChangelogRepository;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.portal.docflow.service.GeradorPacoteService.ResultadoGeracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacaoWorkerServiceTest {

  @Mock PublicacaoRepository publicacaoRepository;
  @Mock PublicacaoChangelogRepository changelogRepository;
  @Mock GeradorPacoteService geradorPacoteService;
  @Mock NotificacaoEmailService notificacaoEmailService;
  @Mock PublicacaoEventService publicacaoEventService;

  PublicacaoWorkerService service;

  UUID publicacaoId;
  UUID clienteId;
  Cliente cliente;
  Publicacao publicacao;

  @BeforeEach
  void setUp() throws Exception {
    service = new PublicacaoWorkerService(publicacaoRepository, changelogRepository,
        geradorPacoteService, notificacaoEmailService, publicacaoEventService,
        new SimpleMeterRegistry(), new ObjectMapper(), transactionTemplateDireto());

    clienteId = UUID.randomUUID();
    cliente = new Cliente("ACME", "acme", true);
    setId(cliente, clienteId);

    publicacaoId = UUID.randomUUID();
    publicacao = new Publicacao(cliente, "1.0.0", null);
    setId(publicacao, publicacaoId);

    when(geradorPacoteService.selecionarPaginas(clienteId)).thenReturn(List.of());
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.of(publicacao));
  }

  @Test
  void processar_sucesso_registraDadosDoResultadoENotifica() throws IOException {
    when(geradorPacoteService.gerar(cliente, "1.0.0", publicacaoId))
        .thenReturn(new ResultadoGeracao("manual.zip", "/tmp/manual.zip", "sha", 5, 2, "{}"));
    when(publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(clienteId))
        .thenReturn(List.of(publicacao));

    service.processar(publicacaoId, "admin");

    assertThat(publicacao.getStatus()).isEqualTo(StatusPublicacao.SUCESSO);
    assertThat(publicacao.getArquivoZipNome()).isEqualTo("manual.zip");
    assertThat(publicacao.getArquivoZipCaminho()).isEqualTo("/tmp/manual.zip");
    assertThat(publicacao.getHashPacote()).isEqualTo("sha");
    assertThat(publicacao.getArvorePaginas()).isEqualTo("[]");
    verify(notificacaoEmailService).notificarPublicacaoGerada(publicacao);
    verify(publicacaoEventService).publicar(publicacao);
  }

  @Test
  void processar_gerarLancaIOException_registraErroENotifica() throws IOException {
    when(geradorPacoteService.gerar(cliente, "1.0.0", publicacaoId))
        .thenThrow(new IOException("falha de disco"));

    service.processar(publicacaoId, "admin");

    assertThat(publicacao.getStatus()).isEqualTo(StatusPublicacao.ERRO);
    verify(notificacaoEmailService).notificarPublicacaoGerada(publicacao);
    verify(changelogRepository, never()).save(any());
  }

  @Test
  void processar_gerarLancaRuntimeException_registraErroENotifica() throws IOException {
    when(geradorPacoteService.gerar(cliente, "1.0.0", publicacaoId))
        .thenThrow(new RuntimeException("boom"));

    service.processar(publicacaoId, "admin");

    assertThat(publicacao.getStatus()).isEqualTo(StatusPublicacao.ERRO);
    verify(notificacaoEmailService).notificarPublicacaoGerada(publicacao);
  }

  @Test
  void processar_publicacaoInexistente_ehNoOp() {
    when(publicacaoRepository.findById(publicacaoId)).thenReturn(Optional.empty());

    service.processar(publicacaoId, "admin");

    verifyNoInteractions(geradorPacoteService, notificacaoEmailService, changelogRepository);
  }

  @Test
  void processar_cancelamentoSolicitado_descartaPacoteEMarcaCancelada(@TempDir Path dir)
      throws Exception {
    Path zip = Files.createFile(dir.resolve("manual.zip"));
    when(geradorPacoteService.gerar(cliente, "1.0.0", publicacaoId))
        .thenAnswer(invocation -> {
          // O pedido de cancelamento chega enquanto o pacote está sendo escrito.
          publicacao.solicitarCancelamento();
          return new ResultadoGeracao("manual.zip", zip.toString(), "sha", 5, 2, "{}");
        });

    service.processar(publicacaoId, "admin");

    assertThat(publicacao.getStatus()).isEqualTo(StatusPublicacao.CANCELADA);
    assertThat(publicacao.isCancelamentoSolicitado()).isFalse();
    assertThat(zip).doesNotExist();
    verify(changelogRepository, never()).save(any());
    verify(publicacaoEventService).publicar(publicacao);
  }

  @Test
  void processar_paginasAnterioresAusentesGeramRemovido() throws Exception {
    UUID anteriorId = UUID.randomUUID();
    Publicacao anterior = new Publicacao(cliente, "0.9.0", null);
    setId(anterior, anteriorId);
    anterior.registrarSucesso(3, 1, "old.zip", "/tmp/old.zip", "sha-old", "{}");

    UUID pagRemovidaId = UUID.randomUUID();
    PublicacaoChangelog changelogAnterior =
        new PublicacaoChangelog(anteriorId, pagRemovidaId, "Página X", "ADICIONADO");

    when(geradorPacoteService.gerar(cliente, "1.0.0", publicacaoId))
        .thenReturn(new ResultadoGeracao("v1.zip", "/tmp/v1.zip", "sha", 0, 0, "{}"));
    when(publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(clienteId))
        .thenReturn(List.of(publicacao, anterior));
    when(changelogRepository.findByPublicacaoIdOrderByCreatedAtAsc(anteriorId))
        .thenReturn(List.of(changelogAnterior));

    service.processar(publicacaoId, "admin");

    ArgumentCaptor<PublicacaoChangelog> captor = ArgumentCaptor.forClass(PublicacaoChangelog.class);
    verify(changelogRepository, times(1)).save(captor.capture());
    PublicacaoChangelog salvo = captor.getValue();
    assertThat(salvo.getPublicacaoId()).isEqualTo(publicacaoId);
    assertThat(salvo.getPaginaId()).isEqualTo(pagRemovidaId);
    assertThat(salvo.getTipoMudanca()).isEqualTo("REMOVIDO");
    assertThat(salvo.getPaginaTitulo()).isEqualTo("Página X");
  }

  /** Executa o callback direto, sem transação real — o worker só precisa do enquadramento. */
  private static TransactionTemplate transactionTemplateDireto() {
    return new TransactionTemplate(new PlatformTransactionManager() {
      @Override
      public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
      }

      @Override
      public void commit(TransactionStatus status) {
      }

      @Override
      public void rollback(TransactionStatus status) {
      }
    });
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
