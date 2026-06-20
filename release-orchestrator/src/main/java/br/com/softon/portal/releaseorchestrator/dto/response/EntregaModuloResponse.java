package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.EntregaModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import java.util.UUID;

public record EntregaModuloResponse(
    UUID id,
    UUID entregaId,
    UUID moduloProdutoId,
    String moduloCodigo,
    String moduloNome,
    TipoModulo moduloTipo,
    String versaoFrom,
    String versaoTo,
    boolean selecionado,
    boolean foraContrato,
    boolean mudancaDetectada,
    int ordem) {

  public static EntregaModuloResponse from(EntregaModulo em) {
    var mod = em.getModuloProduto();
    boolean mudou = em.getVersaoFrom() == null
        || !em.getVersaoFrom().equalsIgnoreCase(em.getVersaoTo());
    return new EntregaModuloResponse(
        em.getId(),
        em.getEntrega().getId(),
        mod.getId(),
        mod.getCodigo(),
        mod.getNome(),
        mod.getTipo(),
        em.getVersaoFrom(),
        em.getVersaoTo(),
        em.isSelecionado(),
        em.isForaContrato(),
        mudou,
        em.getOrdem());
  }
}
