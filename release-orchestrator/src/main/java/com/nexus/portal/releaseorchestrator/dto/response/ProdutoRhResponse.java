package com.nexus.portal.releaseorchestrator.dto.response;

import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
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
        String jenkinsUrl,
        String jenkinsJob,
        String jenkinsUser,
        String jenkinsTriggerMode,
        /** True quando há token Jenkins cadastrado. */
        boolean jenkinsTokenConfigurado,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ProdutoRhResponse from(ProdutoRh p) {
        return new ProdutoRhResponse(
                p.getId(), p.getNome(), p.getSigla(), p.getDescricao(),
                p.getCor(), p.getResponsavelId(), p.isAtivo(),
                p.getRepositorioGithub(), p.getBranchPadrao(), p.getPadraoTag(),
                p.getGithubToken() != null && !p.getGithubToken().isBlank(),
                p.getJenkinsUrl(), p.getJenkinsJob(), p.getJenkinsUser(),
                p.getJenkinsTriggerMode(),
                p.getJenkinsToken() != null && !p.getJenkinsToken().isBlank(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
