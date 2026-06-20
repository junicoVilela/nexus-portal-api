package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.ClienteProdutoModulo;
import br.com.softon.portal.releaseorchestrator.entity.TipoModulo;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteProdutoModuloResponse(
    UUID id,
    UUID clienteProdutoId,
    UUID moduloProdutoId,
    String moduloCodigo,
    String moduloNome,
    TipoModulo moduloTipo,
    String versaoAtual,
    boolean ativo,
    OffsetDateTime updatedAt) {

  public static ClienteProdutoModuloResponse from(ClienteProdutoModulo cpm) {
    return new ClienteProdutoModuloResponse(
        cpm.getId(),
        cpm.getClienteProduto().getId(),
        cpm.getModuloProduto().getId(),
        cpm.getModuloProduto().getCodigo(),
        cpm.getModuloProduto().getNome(),
        cpm.getModuloProduto().getTipo(),
        cpm.getVersaoAtual(),
        cpm.isAtivo(),
        cpm.getUpdatedAt());
  }
}
