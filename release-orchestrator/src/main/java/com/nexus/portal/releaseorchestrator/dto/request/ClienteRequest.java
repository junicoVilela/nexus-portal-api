package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ClienteRequest(
    @NotBlank @Size(max = 200) String nome,
    @Size(max = 300) String razaoSocial,
    @Pattern(regexp = "\\d{14}|", message = "CNPJ deve ter 14 dígitos (sem máscara)")
    @Size(max = 18) String cnpj,
    @NotBlank @Size(max = 20) String sigla,
    UUID responsavelComercialId,
    @NotNull AmbientePadrao ambientePadrao,
    TipoBanco tipoBanco,
    @Size(max = 30) String codificacao,
    @Size(max = 60) String fusoHorario,
    @Size(max = 4000) String observacoes,
    Boolean ativo) {}
