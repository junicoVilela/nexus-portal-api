package br.com.softon.portal.docflow.service;

import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import br.com.softon.portal.docflow.entity.Cliente;
import br.com.softon.portal.docflow.entity.Publicacao;
import br.com.softon.portal.docflow.entity.PublicacaoChangelog;
import br.com.softon.portal.docflow.entity.StatusPublicacao;
import br.com.softon.portal.docflow.repository.PublicacaoChangelogRepository;
import br.com.softon.portal.docflow.repository.PublicacaoRepository;
import br.com.softon.portal.shared.exception.BusinessException;
import br.com.softon.portal.shared.exception.NotFoundException;
import br.com.softon.rbac.service.AuditoriaService;
import br.com.softon.rbac.service.EscopoResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@RequiredArgsConstructor
@Service
public class PublicacaoService {

  private final PublicacaoRepository publicacaoRepository;
  private final ClienteService clienteService;
  private final GeradorPacoteService geradorPacoteService;
  private final PublicacaoWorkerService publicacaoWorkerService;
  private final PublicacaoChangelogRepository changelogRepository;
  private final EscopoResolver escopoResolver;
  private final AuditoriaService auditoriaService;
  private final ArquivoRemocaoService arquivoRemocaoService;
  private final ObjectMapper objectMapper;

  @Transactional
  public Publicacao gerar(UUID clienteId, String versao, String observacao, Principal principal) {
    Cliente cliente = clienteService.buscar(clienteId);
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    if (!cliente.isAtivo()) {
      throw new BusinessException("Não é possível gerar publicação para cliente inativo.");
    }
    if (publicacaoRepository.existsByCliente_IdAndVersao(clienteId, versao.trim())) {
      throw new BusinessException("Já existe uma publicação para esta versão e cliente.");
    }
    String usuario = username(principal);
    Publicacao publicacao = publicacaoRepository.save(new Publicacao(cliente, versao.trim(), observacao));
    agendarProcessamento(publicacao.getId(), usuario);
    return publicacao;
  }

  @Transactional
  public Publicacao reprocessar(UUID id, Principal principal) {
    Publicacao publicacao = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(publicacao.getCliente().getId());
    if (publicacao.getStatus() == StatusPublicacao.GERANDO) {
      throw new BusinessException("A publicação já está em geração.");
    }
    String usuario = username(principal);
    publicacao.prepararGeracao();
    agendarProcessamento(publicacao.getId(), usuario);
    return publicacao;
  }

  public Page<Publicacao> listar(UUID clienteId, StatusPublicacao status, Pageable pageable) {
    Optional<Set<UUID>> permitidos = escopoResolver.clientesPermitidosDoUsuarioAtual();
    if (clienteId != null) {
      if (permitidos.isPresent() && !permitidos.get().contains(clienteId)) {
        return new PageImpl<>(List.of(), pageable, 0);
      }
      return status == null
          ? publicacaoRepository.findByCliente_Id(clienteId, pageable)
          : publicacaoRepository.findByCliente_IdAndStatus(clienteId, status, pageable);
    }
    if (permitidos.isEmpty()) {
      return status == null
          ? publicacaoRepository.findAll(pageable)
          : publicacaoRepository.findByStatus(status, pageable);
    }
    Set<UUID> ids = permitidos.get();
    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, 0);
    }
    return status == null
        ? publicacaoRepository.findByCliente_IdIn(ids, pageable)
        : publicacaoRepository.findByCliente_IdInAndStatus(ids, status, pageable);
  }

  @Transactional
  public List<Publicacao> reprocessarLote(List<UUID> ids, Principal principal) {
    List<UUID> idsUnicos = ids.stream().distinct().toList();
    String usuario = username(principal);
    List<Publicacao> reprocessadas = new java.util.ArrayList<>();
    for (UUID id : idsUnicos) {
      Publicacao publicacao = buscar(id);
      escopoResolver.assertPodeEscreverEmCliente(publicacao.getCliente().getId());
      if (publicacao.getStatus() == StatusPublicacao.GERANDO) {
        continue;
      }
      publicacao.prepararGeracao();
      agendarProcessamento(publicacao.getId(), usuario);
      reprocessadas.add(publicacao);
    }
    return List.copyOf(reprocessadas);
  }

  public List<Publicacao> listar(UUID clienteId) {
    Optional<Set<UUID>> permitidos = escopoResolver.clientesPermitidosDoUsuarioAtual();
    if (clienteId != null) {
      if (permitidos.isPresent() && !permitidos.get().contains(clienteId)) {
        return List.of();
      }
      return publicacaoRepository.findByCliente_IdOrderByCreatedAtDesc(clienteId);
    }
    if (permitidos.isEmpty()) {
      return publicacaoRepository.findAllByOrderByCreatedAtDesc();
    }
    Set<UUID> ids = permitidos.get();
    if (ids.isEmpty()) {
      return List.of();
    }
    return publicacaoRepository.findByCliente_IdInOrderByCreatedAtDesc(ids);
  }

  public Publicacao buscar(UUID id) {
    Publicacao publicacao = publicacaoRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Publicação não encontrada."));
    if (!escopoResolver.podeAcessarCliente(publicacao.getCliente().getId())) {
      throw new NotFoundException("Publicação não encontrada.");
    }
    return publicacao;
  }

  @Transactional
  public void excluir(UUID id, Principal principal) {
    Publicacao publicacao = buscar(id);
    escopoResolver.assertPodeEscreverEmCliente(publicacao.getCliente().getId());
    if (publicacao.getStatus() == StatusPublicacao.GERANDO) {
      throw new BusinessException("Aguarde a geração terminar antes de excluir a publicação.");
    }

    Path arquivo = caminhoArquivo(publicacao);
    publicacaoRepository.delete(publicacao);
    auditoriaService.registrar("PUBLICACAO", id, "EXCLUIR",
        "Publicação " + publicacao.getVersao() + " de " + publicacao.getCliente().getNome(), principal);
    arquivoRemocaoService.removerAposCommit(arquivo);
  }

  public List<PaginaResponse> preverPaginas(UUID clienteId) {
    clienteService.buscar(clienteId);
    return geradorPacoteService.selecionarPaginas(clienteId).stream()
        .map(PaginaResponse::from)
        .toList();
  }

  public String previewHtml(UUID clienteId, String versao) {
    Cliente cliente = clienteService.buscar(clienteId);
    return geradorPacoteService.previewHtml(cliente, versao == null || versao.isBlank() ? "prévia" : versao.trim());
  }

  public List<DiagnosticoPublicacao> diagnosticar(UUID clienteId) {
    clienteService.buscar(clienteId);
    List<PaginaResponse> paginas = preverPaginas(clienteId);
    if (paginas.isEmpty()) {
      return List.of(new DiagnosticoPublicacao("ERRO",
          "Não há páginas publicadas elegíveis para este cliente.", null, null));
    }
    List<DiagnosticoPublicacao> diagnosticos = new java.util.ArrayList<>();
    for (PaginaResponse pagina : paginas) {
      if (pagina.conteudoHtml() == null || org.jsoup.Jsoup.parse(pagina.conteudoHtml()).text().isBlank()) {
        diagnosticos.add(new DiagnosticoPublicacao("ERRO",
            "Página publicada sem conteúdo útil.", pagina.id(), pagina.titulo()));
      }
      if (pagina.resumo() == null || pagina.resumo().isBlank()) {
        diagnosticos.add(new DiagnosticoPublicacao("AVISO",
            "Página sem resumo cadastrado.", pagina.id(), pagina.titulo()));
      }
      if (pagina.parentId() != null && paginas.stream().noneMatch(item -> item.id().equals(pagina.parentId()))) {
        diagnosticos.add(new DiagnosticoPublicacao("AVISO",
            "Subpágina sem página pai no pacote (verifique publicação e vínculos do pai).",
            pagina.id(), pagina.titulo()));
      }
    }
    return diagnosticos;
  }

  public List<PublicacaoChangelog> listarChangelog(UUID publicacaoId) {
    buscar(publicacaoId);
    return changelogRepository.findByPublicacaoIdOrderByCreatedAtAsc(publicacaoId);
  }

  public List<PublicacaoPaginaSnapshotItem> arvorePaginas(UUID id) {
    Publicacao publicacao = buscar(id);
    String json = publicacao.getArvorePaginas();
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<PublicacaoPaginaSnapshotItem>>() {});
    } catch (Exception ex) {
      return List.of();
    }
  }

  public Resource arquivo(UUID id) {
    Publicacao publicacao = buscar(id);
    if (publicacao.getArquivoZipCaminho() == null) {
      throw new BusinessException("Publicação ainda não possui arquivo disponível.");
    }
    Path path = Path.of(publicacao.getArquivoZipCaminho());
    if (!Files.exists(path)) {
      throw new NotFoundException("Arquivo da publicação não encontrado em disco.");
    }
    return new PathResource(path);
  }

  /** Download autenticado ou via token público: exige publicação concluída com sucesso. */
  public Resource recursoPacoteDownloadPublico(UUID publicacaoId) {
    Publicacao publicacao = buscar(publicacaoId);
    if (publicacao.getStatus() != StatusPublicacao.SUCESSO) {
      throw new BusinessException("Download disponível apenas para publicações concluídas.");
    }
    return arquivo(publicacaoId);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }

  private Path caminhoArquivo(Publicacao publicacao) {
    String caminho = publicacao.getArquivoZipCaminho();
    return caminho == null || caminho.isBlank() ? null : Path.of(caminho);
  }

  private void agendarProcessamento(UUID publicacaoId, String usuario) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publicacaoWorkerService.processar(publicacaoId, usuario);
        }
      });
      return;
    }
    publicacaoWorkerService.processar(publicacaoId, usuario);
  }

  public record DiagnosticoPublicacao(String severidade, String mensagem, UUID paginaId, String paginaTitulo) {
  }
}
