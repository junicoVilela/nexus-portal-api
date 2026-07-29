package br.com.softon.portal.releaseorchestrator.service;

import br.com.softon.portal.releaseorchestrator.entity.CategoriaItem;
import br.com.softon.portal.releaseorchestrator.entity.Entrega;
import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.ReleaseItem;
import br.com.softon.portal.releaseorchestrator.entity.TipoPdfRelease;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaModuloRepository;
import br.com.softon.portal.releaseorchestrator.repository.OrchestratorEntregaRepository;
import br.com.softon.portal.releaseorchestrator.repository.ReleaseItemRepository;
import br.com.softon.portal.shared.exception.NotFoundException;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Gera o PDF do "Documento de Entrega" — release-notes contextualizado pro
 * cliente (F1.13). Distinto do PDF da Release (F0.8):
 * <ul>
 *   <li>Cabeçalho inclui cliente + ambiente alvo da entrega.</li>
 *   <li>Itens filtrados pela visibilidade {@link TipoPdfRelease#CLIENTE}.</li>
 *   <li>Lista de módulos do pacote com versão from/to.</li>
 * </ul>
 *
 * Spec: docs/release-orchestrator/25-documento-release-md-pdf.md §2
 */
@Service("orchestratorDocumentoEntregaService")
@RequiredArgsConstructor
public class DocumentoEntregaService {

  private final OrchestratorEntregaRepository entregaRepository;
  private final OrchestratorEntregaModuloRepository entregaModuloRepository;
  private final ReleaseItemRepository releaseItemRepository;
  private final MarkdownPdfRenderer renderer;

  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DATA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  @Transactional
  public byte[] gerar(UUID entregaId) {
    Entrega entrega = entregaRepository.findById(entregaId)
        .orElseThrow(() -> new NotFoundException("Entrega não encontrada."));

    List<ReleaseItem> itens = releaseItemRepository
        .findByReleaseIdOrderByOrdemAsc(entrega.getRelease().getId())
        .stream()
        .filter(i -> TipoPdfRelease.CLIENTE.inclui(i.getVisibilidade()))
        .toList();

    List<EntregaModulo> modulos = entregaModuloRepository
        .findByEntrega_IdOrderByOrdemAscModuloProduto_NomeAsc(entregaId).stream()
        .filter(EntregaModulo::isSelecionado)
        .toList();

    Map<String, Object> contexto = montarContexto(entrega, itens, modulos);
    return renderer.renderToPdf(TEMPLATE_DEFAULT, contexto);
  }

  private Map<String, Object> montarContexto(Entrega entrega, List<ReleaseItem> itens,
      List<EntregaModulo> modulos) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("cliente", entrega.getCliente().getNome());
    ctx.put("clienteSigla", entrega.getCliente().getSigla());
    ctx.put("produto", entrega.getProduto().getNome());
    ctx.put("produtoSigla", entrega.getProduto().getSigla());
    ctx.put("releaseVersao", entrega.getRelease().getVersao());
    ctx.put("releaseTitulo", entrega.getRelease().getTitulo());
    ctx.put("ambiente", entrega.getAmbiente().name());
    ctx.put("status", entrega.getStatus().name());
    ctx.put("dataGeracao", entrega.getDataInicioGeracao() != null
        ? entrega.getDataInicioGeracao().format(DATA_HORA) : "—");
    ctx.put("dataConclusao", entrega.getDataConclusao() != null
        ? entrega.getDataConclusao().format(DATA_HORA) : "—");
    ctx.put("dataPrevista", entrega.getRelease().getDataPrevista() != null
        ? entrega.getRelease().getDataPrevista().format(DATA) : "—");
    ctx.put("resumo", entrega.getRelease().getResumo() != null
        ? entrega.getRelease().getResumo() : "");
    ctx.put("observacoes", entrega.getObservacoes() != null
        ? entrega.getObservacoes() : "");
    ctx.put("itensPorCategoria", agruparPorCategoria(itens));
    ctx.put("totalItens", itens.size());
    ctx.put("modulos", modulos);
    ctx.put("totalModulos", modulos.size());
    return ctx;
  }

  private Map<String, List<ReleaseItem>> agruparPorCategoria(List<ReleaseItem> itens) {
    Map<CategoriaItem, List<ReleaseItem>> agrupado = new EnumMap<>(CategoriaItem.class);
    for (ReleaseItem item : itens) {
      agrupado.computeIfAbsent(item.getCategoria(), c -> new ArrayList<>()).add(item);
    }
    Map<String, List<ReleaseItem>> porNome = new TreeMap<>();
    agrupado.forEach((cat, lista) -> porNome.put(rotuloCategoria(cat), lista));
    return porNome;
  }

  private String rotuloCategoria(CategoriaItem cat) {
    return switch (cat) {
      case NOVIDADE -> "Novidades";
      case MELHORIA -> "Melhorias";
      case CORRECAO -> "Correções";
      case SEGURANCA -> "Segurança";
      case PERFORMANCE -> "Performance";
      case DOCUMENTACAO -> "Documentação";
      case AJUSTE_TECNICO -> "Ajustes técnicos";
      case IMPACTO_OPERACIONAL -> "Impacto operacional";
      case IMPORTANTE -> "Importantes";
    };
  }

  /** Template Markdown embutido. F0.11 vai mover para banco com override por produto. */
  private static final String TEMPLATE_DEFAULT = """
      # Documento de Entrega

      **Cliente:** [(${cliente})] ([(${clienteSigla})])
      **Produto:** [(${produto})] ([(${produtoSigla})])
      **Release:** [(${releaseVersao})] — [(${releaseTitulo})]
      **Ambiente:** [(${ambiente})]
      **Status:** [(${status})]
      **Início da geração:** [(${dataGeracao})]
      **Conclusão:** [(${dataConclusao})]
      **Data prevista da release:** [(${dataPrevista})]

      ## Resumo

      [(${resumo})]

      ## Módulos entregues

      Total: [(${totalModulos})] módulo(s)

      | Código | Nome | De | Para |
      | --- | --- | --- | --- |
      [# th:each="m : ${modulos}"]
      | [(${m.moduloProduto.codigo})] | [(${m.moduloProduto.nome})] | [(${m.versaoFrom != null ? m.versaoFrom : '—'})] | [(${m.versaoTo != null ? m.versaoTo : '—'})] |
      [/]

      ## Novidades para o cliente

      Total: [(${totalItens})] item(ns)

      [# th:each="grupo : ${itensPorCategoria}"]
      ### [(${grupo.key})]

      [# th:each="item : ${grupo.value}"]
      - **[(${item.titulo})]** — [(${item.descricao})]
      [/]
      [/]

      ## Observações

      [(${observacoes})]

      ---

      _Documento gerado automaticamente pelo Softon Portal._
      """;
}
