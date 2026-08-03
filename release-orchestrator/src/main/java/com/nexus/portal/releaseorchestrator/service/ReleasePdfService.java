package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.CategoriaItem;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseItem;
import com.nexus.portal.releaseorchestrator.entity.TipoPdfRelease;
import com.nexus.portal.releaseorchestrator.repository.ReleaseItemRepository;
import com.nexus.portal.releaseorchestrator.repository.ReleaseRepository;
import com.nexus.portal.shared.exception.NotFoundException;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Geração de PDF da release (F0.8). Carrega Release + ReleaseItems, filtra
 * itens pelo {@link TipoPdfRelease} (CLIENTE/SUPORTE/INTERNO) e delega a
 * renderização ao {@link MarkdownPdfRenderer}.
 *
 * Template default embarcado na classe; F0.11 vai permitir override por
 * produto via banco.
 */
@Service
@RequiredArgsConstructor
public class ReleasePdfService {

  private final ReleaseRepository releaseRepository;
  private final ReleaseItemRepository releaseItemRepository;
  private final MarkdownPdfRenderer renderer;

  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public byte[] gerar(UUID releaseId, TipoPdfRelease tipo) {
    Release release = releaseRepository.findById(releaseId)
        .orElseThrow(() -> new NotFoundException("Release não encontrada."));
    List<ReleaseItem> itens = releaseItemRepository.findByReleaseIdOrderByOrdemAsc(releaseId)
        .stream().filter(i -> tipo.inclui(i.getVisibilidade())).toList();

    Map<String, Object> contexto = montarContexto(release, itens, tipo);
    return renderer.renderToPdf(TEMPLATE_DEFAULT, contexto);
  }

  private Map<String, Object> montarContexto(Release release, List<ReleaseItem> itens,
      TipoPdfRelease tipo) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("produto", release.getProduto().getNome());
    ctx.put("sigla", release.getProduto().getSigla());
    ctx.put("versao", release.getVersao());
    ctx.put("titulo", release.getTitulo());
    ctx.put("tipoRelease", release.getTipo().name());
    ctx.put("status", release.getStatus().name());
    ctx.put("dataPrevista", release.getDataPrevista() != null
        ? release.getDataPrevista().format(DATA) : "—");
    ctx.put("dataPublicacao", release.getDataPublicacao() != null
        ? release.getDataPublicacao().format(DATA) : "—");
    ctx.put("resumo", release.getResumo() != null ? release.getResumo() : "");
    ctx.put("observacoes", release.getObservacoes() != null ? release.getObservacoes() : "");
    ctx.put("tipoPdf", tipo.name());
    ctx.put("itensPorCategoria", agruparPorCategoria(itens));
    ctx.put("totalItens", itens.size());
    return ctx;
  }

  private Map<String, List<ReleaseItem>> agruparPorCategoria(List<ReleaseItem> itens) {
    Map<CategoriaItem, List<ReleaseItem>> agrupado = new EnumMap<>(CategoriaItem.class);
    for (ReleaseItem item : itens) {
      agrupado.computeIfAbsent(item.getCategoria(), c -> new java.util.ArrayList<>()).add(item);
    }
    // converte chaves Enum → String para o Thymeleaf renderizar com nome amigável
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

  // Mantido inline pra esse commit; F0.11 move pra banco com override por produto.
  private static final String TEMPLATE_DEFAULT = """
      # Release [(${sigla})] [(${versao})]

      **Título:** [(${titulo})]
      **Tipo:** [(${tipoRelease})]
      **Status:** [(${status})]
      **Data prevista:** [(${dataPrevista})]
      **Data de publicação:** [(${dataPublicacao})]

      ## Resumo

      [(${resumo})]

      ## Itens

      Total: [(${totalItens})] item(s)

      [# th:each="grupo : ${itensPorCategoria}"]
      ### [(${grupo.key})]

      [# th:each="item : ${grupo.value}"]
      - **[(${item.titulo})]** — [(${item.descricao})]
      [/]
      [/]

      ## Observações

      [(${observacoes})]

      ---

      _Documento gerado automaticamente (tipo PDF: [(${tipoPdf})])_
      """;
}
