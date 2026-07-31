package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.service.GeradorPacoteService;
import br.com.softon.portal.docflow.service.GeradorPacoteService.ResultadoGeracao;
import br.com.softon.portal.docflow.service.NotificacaoEmailService;
import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.entity.PublicacaoChangelog;
import br.com.softon.portal.docflow.repository.PublicacaoChangelogRepository;
import br.com.softon.portal.docflow.repository.PublicacaoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublicacaoWorkerService {

  private final PublicacaoRepository publicacaoRepository;
  private final PublicacaoChangelogRepository changelogRepository;
  private final GeradorPacoteService geradorPacoteService;
  private final NotificacaoEmailService notificacaoEmailService;
  private final PublicacaoEventService publicacaoEventService;
  private final MeterRegistry meterRegistry;
  private final ObjectMapper objectMapper;

  @Async
  @Transactional
  public void processar(UUID publicacaoId, String username) {
    Timer.Sample tempoGeracao = Timer.start(meterRegistry);
    Publicacao publicacao = publicacaoRepository.findById(publicacaoId).orElse(null);
    if (publicacao == null) {
      meterRegistry.counter("docflow.publicacao.resultado", "status", "cancelada").increment();
      tempoGeracao.stop(meterRegistry.timer("docflow.publicacao.duracao"));
      log.warn("Publicação {} não encontrada antes do processamento assíncrono", publicacaoId);
      return;
    }
    List<PaginaResponse> paginasAtuais = geradorPacoteService
        .selecionarPaginas(publicacao.getCliente().getId())
        .stream().map(PaginaResponse::from).toList();
    try {
      ResultadoGeracao resultado = geradorPacoteService.gerar(publicacao.getCliente(),
          publicacao.getVersao(), publicacao.getId());
      publicacao.registrarSucesso(resultado.quantidadePaginas(), resultado.quantidadeModulos(),
          resultado.arquivoZipNome(), resultado.arquivoZipCaminho(), resultado.hashPacote(),
          resultado.relatorioValidacaoJson());
      publicacao.definirArvorePaginas(serializarArvorePaginas(paginasAtuais));
      gerarChangelog(publicacao, paginasAtuais);
      notificacaoEmailService.notificarPublicacaoGerada(publicacao);
      meterRegistry.counter("docflow.publicacao.resultado", "status", "sucesso").increment();
      log.info("Publicação {} concluída: cliente={}, versao={}, paginas={}, modulos={}",
          publicacaoId, publicacao.getCliente().getId(), publicacao.getVersao(),
          resultado.quantidadePaginas(), resultado.quantidadeModulos());
    } catch (RuntimeException | java.io.IOException ex) {
      publicacao.registrarErro(ex.getMessage());
      notificacaoEmailService.notificarPublicacaoGerada(publicacao);
      meterRegistry.counter("docflow.publicacao.resultado", "status", "erro").increment();
      log.error("Falha ao gerar publicação {}: cliente={}, versao={}", publicacaoId,
          publicacao.getCliente().getId(), publicacao.getVersao(), ex);
    } finally {
      tempoGeracao.stop(meterRegistry.timer("docflow.publicacao.duracao"));
      publicacaoEventService.publicar(publicacao);
    }
  }

  private String serializarArvorePaginas(List<PaginaResponse> paginasAtuais) {
    try {
      return objectMapper.writeValueAsString(PublicacaoPaginaSnapshotBuilder.build(paginasAtuais));
    } catch (JsonProcessingException ex) {
      log.warn("Falha ao serializar árvore de páginas da publicação: {}", ex.getMessage());
      return null;
    }
  }

  private void gerarChangelog(Publicacao publicacao, List<PaginaResponse> paginasAtuais) {
    List<Publicacao> anteriores = publicacaoRepository
        .findByCliente_IdOrderByCreatedAtDesc(publicacao.getCliente().getId())
        .stream()
        .filter(p -> !p.getId().equals(publicacao.getId())
            && p.getStatus() == br.com.softon.portal.docflow.entity.StatusPublicacao.SUCESSO)
        .limit(1)
        .toList();

    if (anteriores.isEmpty()) {
      paginasAtuais.forEach(p -> changelogRepository.save(
          new PublicacaoChangelog(publicacao.getId(), p.id(), p.titulo(), "ADICIONADO")));
      return;
    }

    Publicacao anterior = anteriores.get(0);
    List<PublicacaoChangelog> changelogAnterior = changelogRepository
        .findByPublicacaoIdOrderByCreatedAtAsc(anterior.getId());
    Set<UUID> idsAnteriores = changelogAnterior.stream()
        .map(PublicacaoChangelog::getPaginaId)
        .filter(id -> id != null)
        .collect(Collectors.toSet());
    Set<UUID> idsAtuais = paginasAtuais.stream().map(PaginaResponse::id).collect(Collectors.toSet());

    paginasAtuais.forEach(p -> {
      String tipo = idsAnteriores.contains(p.id()) ? "ATUALIZADO" : "ADICIONADO";
      changelogRepository.save(new PublicacaoChangelog(publicacao.getId(), p.id(), p.titulo(), tipo));
    });
    idsAnteriores.stream()
        .filter(id -> !idsAtuais.contains(id))
        .forEach(id -> {
          String titulo = changelogAnterior.stream()
              .filter(c -> id.equals(c.getPaginaId()))
              .map(PublicacaoChangelog::getPaginaTitulo)
              .findFirst().orElse("Página removida");
          changelogRepository.save(new PublicacaoChangelog(publicacao.getId(), id, titulo, "REMOVIDO"));
        });
  }
}
