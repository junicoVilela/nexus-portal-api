package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.dto.response.PublicacaoDiffItemResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoDiffResponse;
import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.docflow.entity.StatusPublicacao;
import com.nexus.portal.docflow.repository.PublicacaoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Compara duas publicações do mesmo cliente página a página.
 *
 * <p>O changelog existente marca como ATUALIZADO tudo o que aparece nas duas
 * publicações, mesmo sem mudança nenhuma. Aqui a comparação usa o hash do
 * conteúdo gravado no snapshot, então "alterada" quer dizer alterada.
 */
@Service
@RequiredArgsConstructor
public class PublicacaoDiffService {

  private final PublicacaoService publicacaoService;
  private final PublicacaoRepository publicacaoRepository;

  public PublicacaoDiffResponse comparar(UUID publicacaoId, UUID comparadaComId) {
    Publicacao atual = publicacaoService.buscar(publicacaoId);
    Publicacao anterior = comparadaComId == null
        ? anteriorBemSucedida(atual)
        : publicacaoService.buscar(comparadaComId);

    if (anterior == null) {
      throw new BusinessException(
          "Não há publicação anterior concluída deste cliente para comparar.");
    }
    if (!anterior.getCliente().getId().equals(atual.getCliente().getId())) {
      throw new BusinessException("Só é possível comparar publicações do mesmo cliente.");
    }
    if (anterior.getId().equals(atual.getId())) {
      throw new BusinessException("Selecione duas publicações diferentes.");
    }

    Map<UUID, PublicacaoPaginaSnapshotItem> antes = porId(publicacaoService.arvorePaginas(anterior.getId()));
    Map<UUID, PublicacaoPaginaSnapshotItem> depois = porId(publicacaoService.arvorePaginas(atual.getId()));

    List<PublicacaoDiffItemResponse> itens = new ArrayList<>();
    for (PublicacaoPaginaSnapshotItem item : depois.values()) {
      PublicacaoPaginaSnapshotItem correspondente = antes.get(item.id());
      itens.add(new PublicacaoDiffItemResponse(item.id(), item.titulo(), item.codigoTela(),
          correspondente == null ? Mudanca.ADICIONADA.name() : classificar(correspondente, item).name()));
    }
    antes.values().stream()
        .filter(item -> !depois.containsKey(item.id()))
        .forEach(item -> itens.add(new PublicacaoDiffItemResponse(item.id(), item.titulo(),
            item.codigoTela(), Mudanca.REMOVIDA.name())));

    Map<String, Long> totais = itens.stream()
        .collect(Collectors.groupingBy(PublicacaoDiffItemResponse::mudanca,
            LinkedHashMap::new, Collectors.counting()));

    return new PublicacaoDiffResponse(
        atual.getId(), atual.getVersao(),
        anterior.getId(), anterior.getVersao(),
        totais, List.copyOf(itens));
  }

  /**
   * Sem hash nos dois lados (publicação anterior ao campo) não dá para afirmar
   * que nada mudou — nesses casos o resultado fica como indeterminado.
   */
  private Mudanca classificar(PublicacaoPaginaSnapshotItem antes, PublicacaoPaginaSnapshotItem depois) {
    if (antes.conteudoHash() == null || depois.conteudoHash() == null) {
      return Mudanca.INDETERMINADA;
    }
    if (!antes.conteudoHash().equals(depois.conteudoHash())) {
      return Mudanca.ALTERADA;
    }
    return antes.ordem() == depois.ordem() && java.util.Objects.equals(antes.parentId(), depois.parentId())
        ? Mudanca.INALTERADA
        : Mudanca.MOVIDA;
  }

  private Publicacao anteriorBemSucedida(Publicacao atual) {
    return publicacaoRepository
        .findByCliente_IdOrderByCreatedAtDesc(atual.getCliente().getId()).stream()
        .filter(p -> !p.getId().equals(atual.getId())
            && p.getStatus() == StatusPublicacao.SUCESSO
            && p.getCreatedAt().isBefore(atual.getCreatedAt()))
        .findFirst()
        .orElse(null);
  }

  private Map<UUID, PublicacaoPaginaSnapshotItem> porId(List<PublicacaoPaginaSnapshotItem> itens) {
    return itens.stream().collect(Collectors.toMap(PublicacaoPaginaSnapshotItem::id,
        Function.identity(), (a, b) -> a, LinkedHashMap::new));
  }

  public enum Mudanca {
    ADICIONADA,
    REMOVIDA,
    ALTERADA,
    MOVIDA,
    INALTERADA,
    INDETERMINADA
  }
}
