package br.com.softon.portal.docflow.dto.request;

import br.com.softon.portal.docflow.entity.TipoAjudaConteudo;
import br.com.softon.portal.docflow.entity.TipoAjudaMedia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AjudaConteudoRequest(
    @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Z0-9_]+") String codigo,
    @NotNull TipoAjudaConteudo tipo,
    @Size(max = 80) String jornadaCodigo,
    @NotBlank @Size(max = 160) String titulo,
    @Size(max = 400) String resumo,
    String conteudo,
    @Size(max = 220) String rotaContexto,
    @Size(max = 220) String rotaAcao,
    @Size(max = 80) String rotuloAcao,
    @Size(max = 50) String icone,
    @Size(max = 200) String seletorAlvo,
    TipoAjudaMedia mediaTipo,
    @Size(max = 8) List<@Size(max = 500) String> mediaUrls,
    @Size(max = 240) String mediaAlt,
    Integer ordem,
    Boolean ativo) {
}
