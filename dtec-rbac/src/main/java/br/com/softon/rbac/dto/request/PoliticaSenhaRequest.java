package br.com.softon.rbac.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PoliticaSenhaRequest(
    @NotNull @Min(4) Integer tamanhoMinimo,
    @NotNull Boolean exigirMaiuscula,
    @NotNull Boolean exigirMinuscula,
    @NotNull Boolean exigirNumero,
    @NotNull Boolean exigirEspecial,
    Integer expiraSenhaDias,
    @NotNull @Min(0) Integer quantidadeHistorico,
    @NotNull @Min(1) Integer maxTentativasInvalidas) {
}
