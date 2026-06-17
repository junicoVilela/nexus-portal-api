package br.com.softon.portal.releaseorchestrator.dto.request;

import br.com.softon.portal.releaseorchestrator.entity.ReleaseStatus;
import br.com.softon.portal.releaseorchestrator.entity.TipoRelease;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record ReleaseRequest(
        @NotNull  UUID produtoId,
        @NotBlank @Size(max = 50)
        @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?(-\\w+)?$",
                 message = "Versão inválida. Use o formato semântico: 1.0.0")
                  String versao,
        @NotBlank @Size(max = 200) String titulo,
        @NotNull  TipoRelease tipo,
        ReleaseStatus status,
        LocalDate dataPrevista,
        UUID responsavelId,
        String resumo,
        String observacoes
) {}
