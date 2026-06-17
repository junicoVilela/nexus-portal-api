package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.service.GeradorPacoteService;
import br.com.softon.portal.docflow.service.GeradorPacoteService.ResultadoGeracao;
import br.com.softon.portal.docflow.service.NotificacaoEmailService;
import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.entity.PublicacaoChangelog;
import br.com.softon.portal.docflow.repository.PublicacaoChangelogRepository;
import br.com.softon.portal.docflow.repository.PublicacaoRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PublicacaoWorkerService {

  private final PublicacaoRepository publicacaoRepository;
  private final PublicacaoChangelogRepository changelogRepository;
  private final GeradorPacoteService geradorPacoteService;
  private final NotificacaoEmailService notificacaoEmailService;

  @Async
  @Transactional
  public void processar(UUID publicacaoId, String username) {
    publicacaoRepository.findById(publicacaoId).ifPresent(publicacao -> {
      List<PaginaResponse> paginasAtuais = geradorPacoteService
          .selecionarPaginas(publicacao.getCliente().getId())
          .stream().map(PaginaResponse::from).toList();
      try {
        ResultadoGeracao resultado = geradorPacoteService.gerar(publicacao.getCliente(),
            publicacao.getVersao());
        publicacao.registrarSucesso(resultado.quantidadePaginas(), resultado.quantidadeModulos(),
            resultado.arquivoZipNome(), resultado.arquivoZipCaminho(), resultado.hashPacote(),
            resultado.relatorioValidacaoJson());
        gerarChangelog(publicacao, paginasAtuais);
        notificacaoEmailService.notificarPublicacaoGerada(publicacao);
      } catch (RuntimeException | java.io.IOException ex) {
        publicacao.registrarErro(ex.getMessage());
        notificacaoEmailService.notificarPublicacaoGerada(publicacao);
      }
    });
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
