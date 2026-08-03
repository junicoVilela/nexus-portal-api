package com.nexus.portal.releaseorchestrator.dto.request;

import com.nexus.portal.releaseorchestrator.entity.PapelContato;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContatoRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotNull PapelContato papel,
    @NotBlank @Email @Size(max = 200) String email,
    @Size(max = 40) String telefone) {}
