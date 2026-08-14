package com.nexus.portal.ai.audit;

/** Constantes de auditoria do módulo AI (listagem RBAC). */
public final class AiAuditoriaAcoes {

  public static final String ENTIDADE_SESSAO = "AI_SESSAO";
  public static final String ENTIDADE_PROPOSTA = "AI_PROPOSTA";
  public static final String ENTIDADE_DOCUMENTO_IMPORTACAO = "AI_DOCUMENTO_IMPORTACAO";

  public static final String SESSAO_CRIADA = "AI_SESSAO_CRIADA";
  public static final String SESSAO_CANCELADA = "AI_SESSAO_CANCELADA";
  public static final String PROPOSTA_GERADA = "AI_PROPOSTA_GERADA";
  public static final String PROPOSTA_ERRO = "AI_PROPOSTA_ERRO";
  public static final String PROPOSTA_ACEITA = "AI_PROPOSTA_ACEITA";
  public static final String PROPOSTA_APLICADA_FORM = "AI_PROPOSTA_APLICADA_FORM";
  public static final String DOCUMENTO_IMPORTADO = "AI_DOCUMENTO_IMPORTADO";
  public static final String DOCUMENTO_ESTRUTURA_CONFIRMADA = "AI_DOCUMENTO_ESTRUTURA_CONFIRMADA";
  public static final String DOCUMENTO_ESTRUTURA_REORDENADA = "AI_DOCUMENTO_ESTRUTURA_REORDENADA";
  public static final String DOCUMENTO_COMPOSICAO_ATUALIZADA = "AI_DOCUMENTO_COMPOSICAO_ATUALIZADA";
  public static final String DOCUMENTO_SUGESTAO_APLICADA = "AI_DOCUMENTO_SUGESTAO_APLICADA";
  public static final String DOCUMENTO_SUGESTAO_IGNORADA = "AI_DOCUMENTO_SUGESTAO_IGNORADA";
  public static final String DOCUMENTO_SUGESTOES_SEGURAS_APLICADAS =
      "AI_DOCUMENTO_SUGESTOES_SEGURAS_APLICADAS";

  private AiAuditoriaAcoes() {}
}
