package br.com.softon.portal.releaseorchestrator.dto.response;

import br.com.softon.portal.releaseorchestrator.entity.ProdutoRh;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProdutoRhResponse(
        UUID id,
        String nome,
        String sigla,
        String descricao,
        String cor,
        UUID responsavelId,
        boolean ativo,
        String repositorioGithub,
        String branchPadrao,
        String padraoTag,
        /** True quando há token cadastrado (não devolve o valor por segurança). */
        boolean githubTokenConfigurado,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProdutoRhResponse from(ProdutoRh p) {
        return new ProdutoRhResponse(
                p.getId(), p.getNome(), p.getSigla(), p.getDescricao(),
                p.getCor(), p.getResponsavelId(), p.isAtivo(),
                p.getRepositorioGithub(), p.getBranchPadrao(), p.getPadraoTag(),
                p.getGithubToken() != null && !p.getGithubToken().isBlank(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
