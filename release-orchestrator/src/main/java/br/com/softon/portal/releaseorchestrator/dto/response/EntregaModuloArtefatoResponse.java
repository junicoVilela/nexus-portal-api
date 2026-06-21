package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import java.util.UUID;

public record EntregaModuloArtefatoResponse(
    UUID id,
    UUID entregaModuloId,
    UUID moduloProdutoId,
    String moduloCodigo,
    String moduloNome,
    TipoModulo moduloTipo,
    UUID artefatoId,
    String nomeArquivo,
    String sha256,
    long tamanhoBytes,
    int ordem) {

  public static EntregaModuloArtefatoResponse from(EntregaModuloArtefato ema) {
    var em = ema.getEntregaModulo();
    var modulo = em.getModuloProduto();
    var artefato = ema.getArtefato();
    return new EntregaModuloArtefatoResponse(
        ema.getId(),
        em.getId(),
        modulo.getId(),
        modulo.getCodigo(),
        modulo.getNome(),
        modulo.getTipo(),
        artefato.getId(),
        artefato.getNomeArquivo(),
        artefato.getSha256(),
        artefato.getTamanhoBytes(),
        ema.getOrdem());
  }
}
