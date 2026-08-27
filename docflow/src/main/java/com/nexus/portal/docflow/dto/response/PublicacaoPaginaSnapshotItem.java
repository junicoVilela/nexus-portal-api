package com.nexus.portal.docflow.dto.response;

import java.util.UUID;

/**
 * @param conteudoHash resumo do HTML no momento da publicação; é o que permite
 *     dizer se a página realmente mudou entre duas versões do manual. Vem nulo
 *     em publicações geradas antes deste campo existir.
 */
public record PublicacaoPaginaSnapshotItem(
    UUID id,
    UUID parentId,
    String titulo,
    String codigoTela,
    String slug,
    int ordem,
    int nivel,
    String conteudoHash) {
}
