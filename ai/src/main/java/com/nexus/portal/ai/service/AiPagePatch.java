package com.nexus.portal.ai.service;

import java.util.List;
import java.util.Map;

/**
 * Ajuste proposto pela IA para uma página existente (Fase B): operações pontuais sobre as
 * unidades do {@link AiPaginaEsboco}. Persistido em {@code tb_ai_proposta.patch_json} com o texto
 * anterior de cada unidade, para o diff da UI e para reaplicar só as operações aceitas.
 */
public record AiPagePatch(String resumoDaMudanca, List<Operacao> operacoes) {

  /** Pseudo-unidades para o título e o resumo da página (não fazem parte do HTML). */
  public static final String UNIDADE_TITULO = "titulo";
  public static final String UNIDADE_RESUMO = "resumo";

  public AiPagePatch {
    operacoes = operacoes == null ? List.of() : List.copyOf(operacoes);
  }

  public enum Tipo {
    ALTERAR_TEXTO,
    INSERIR_BLOCO,
    REMOVER_UNIDADE
  }

  /**
   * {@code textoAntes}: texto da unidade quando o patch foi gerado (diff na UI).
   * {@code textos}: slots do componente em {@code INSERIR_BLOCO}.
   */
  public record Operacao(
      String id,
      Tipo tipo,
      String unidadeId,
      String textoAntes,
      String novoTexto,
      String aposSecaoId,
      String componenteId,
      Map<String, String> textos,
      String motivo) {

    public Operacao {
      textos = textos == null ? Map.of() : Map.copyOf(textos);
    }
  }
}
