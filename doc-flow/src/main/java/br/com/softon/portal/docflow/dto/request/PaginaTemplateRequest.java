package br.com.softon.portal.docflow.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PaginaTemplateRequest(
    @NotBlank @Size(max = 120) String nome,
    @Size(max = 300) String descricao,
    @NotBlank String conteudoHtml,
    UUID projetoId,
    UUID clienteId) {

  @AssertTrue(message = "Informe somente um projeto ou um cliente para o modelo.")
  public boolean isEscopoValido() {
    return (projetoId == null) != (clienteId == null);
  }
}
