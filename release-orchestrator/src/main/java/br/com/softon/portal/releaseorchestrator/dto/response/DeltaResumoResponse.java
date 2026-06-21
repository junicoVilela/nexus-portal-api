package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import java.util.List;
import java.util.UUID;

/**
 * Resumo do delta calculado para a entrega. Agrupa por módulo selecionado e
 * conta artefatos por tipo. Usado no frontend pra mostrar "X SQL, Y .ktr, …"
 * antes da geração.
 */
public record DeltaResumoResponse(
    UUID entregaId,
    int totalArtefatos,
    long totalTamanhoBytes,
    List<ModuloResumo> modulos) {

  public record ModuloResumo(
      UUID moduloProdutoId,
      String codigo,
      String nome,
      TipoModulo tipo,
      String versaoFrom,
      String versaoTo,
      int quantidadeArtefatos,
      long tamanhoBytes) {}
}
