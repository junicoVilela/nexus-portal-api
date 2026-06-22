package br.com.softon.portal.releaseorchestrator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProdutoRhRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 20)  String sigla,
        @Size(max = 500)           String descricao,
        @NotBlank @Size(max = 20)  String cor,
        UUID responsavelId,
        Boolean ativo,
        /* --- Integração GitHub (Fase 2, opcional) --- */
        @Size(max = 200) String repositorioGithub,
        @Size(max = 80)  String branchPadrao,
        @Size(max = 200) String padraoTag,
        /** PAT. Enviar em branco em PUT preserva o token atual. */
        @Size(max = 500) String githubToken,
        /* --- Integração Jenkins (Fase 2, opcional) --- */
        @Size(max = 300) String jenkinsUrl,
        @Size(max = 200) String jenkinsJob,
        @Size(max = 120) String jenkinsUser,
        /** API token Jenkins. Enviar em branco em PUT preserva o atual. */
        @Size(max = 500) String jenkinsToken,
        @Size(max = 30)  String jenkinsTriggerMode
) {}
