package com.nexus.portal.shared.security;

/**
 * Expressões SpEL para uso em {@code @PreAuthorize("…")} baseadas em permissões
 * granulares (catálogo {@code tb_permissao}). Cada constante mapeia para um
 * código {@code FUNCIONALIDADE:ACAO} provido pelos seeds em
 * {@code V5/V8/V9__rbac*.sql}.
 *
 * <p>Use estas constantes em vez de digitar a string SpEL nos controllers —
 * elimina typos e centraliza a evolução do catálogo. Quando uma ação especial
 * for adicionada ao seed (ex.: {@code RELEASE:PUBLICAR}), adicione a constante
 * aqui também.
 *
 * <p>Endpoints públicos (webhooks com chave compartilhada, downloads
 * autenticados por token, previews) usam {@code permitAll} no {@code
 * SecurityConfig} em vez de {@code @PreAuthorize}.
 */
public final class Permissoes {

  private Permissoes() {}

  // ---------------------------------------------------------------------------
  // SEGURANCA (V5)
  // ---------------------------------------------------------------------------
  public static final String USUARIO_LER          = "hasAuthority('USUARIO:LER')";
  public static final String USUARIO_CRIAR        = "hasAuthority('USUARIO:CRIAR')";
  public static final String USUARIO_EDITAR       = "hasAuthority('USUARIO:EDITAR')";
  public static final String USUARIO_EXCLUIR      = "hasAuthority('USUARIO:EXCLUIR')";
  public static final String USUARIO_RESETAR      = "hasAuthority('USUARIO:RESETAR_SENHA')";
  public static final String USUARIO_BLOQUEAR     = "hasAuthority('USUARIO:BLOQUEAR')";

  public static final String GRUPO_ACESSO_LER     = "hasAuthority('GRUPO_ACESSO:LER')";
  public static final String GRUPO_ACESSO_CRIAR   = "hasAuthority('GRUPO_ACESSO:CRIAR')";
  public static final String GRUPO_ACESSO_EDITAR  = "hasAuthority('GRUPO_ACESSO:EDITAR')";
  public static final String GRUPO_ACESSO_EXCLUIR = "hasAuthority('GRUPO_ACESSO:EXCLUIR')";
  public static final String GRUPO_ACESSO_VINCULAR_PERMISSAO =
      "hasAuthority('GRUPO_ACESSO:VINCULAR_PERMISSAO')";

  public static final String AUDITORIA_LER        = "hasAuthority('AUDITORIA:LER')";
  public static final String AUDITORIA_VISUALIZAR = "hasAuthority('AUDITORIA:VISUALIZAR')";

  // ---------------------------------------------------------------------------
  // DOC_FLOW (V8 + V9)
  // ---------------------------------------------------------------------------
  public static final String CLIENTE_LER          = "hasAuthority('CLIENTE:LER')";
  public static final String CLIENTE_CRIAR        = "hasAuthority('CLIENTE:CRIAR')";
  public static final String CLIENTE_EDITAR       = "hasAuthority('CLIENTE:EDITAR')";
  public static final String CLIENTE_EXCLUIR      = "hasAuthority('CLIENTE:EXCLUIR')";

  public static final String PROJETO_LER          = "hasAuthority('PROJETO:LER')";
  public static final String PROJETO_CRIAR        = "hasAuthority('PROJETO:CRIAR')";
  public static final String PROJETO_EDITAR       = "hasAuthority('PROJETO:EDITAR')";
  public static final String PROJETO_EXCLUIR      = "hasAuthority('PROJETO:EXCLUIR')";

  public static final String MODULO_LER           = "hasAuthority('MODULO:LER')";
  public static final String MODULO_CRIAR         = "hasAuthority('MODULO:CRIAR')";
  public static final String MODULO_EDITAR        = "hasAuthority('MODULO:EDITAR')";
  public static final String MODULO_EXCLUIR       = "hasAuthority('MODULO:EXCLUIR')";

  public static final String PAGINA_LER           = "hasAuthority('PAGINA:LER')";
  public static final String PAGINA_CRIAR         = "hasAuthority('PAGINA:CRIAR')";
  public static final String PAGINA_EDITAR        = "hasAuthority('PAGINA:EDITAR')";
  public static final String PAGINA_EXCLUIR       = "hasAuthority('PAGINA:EXCLUIR')";

  public static final String PUBLICACAO_LER       = "hasAuthority('PUBLICACAO:LER')";
  public static final String PUBLICACAO_CRIAR     = "hasAuthority('PUBLICACAO:CRIAR')";
  public static final String PUBLICACAO_EDITAR    = "hasAuthority('PUBLICACAO:EDITAR')";
  public static final String PUBLICACAO_EXCLUIR   = "hasAuthority('PUBLICACAO:EXCLUIR')";

  public static final String EMPRESA_LER          = "hasAuthority('EMPRESA:LER')";
  public static final String EMPRESA_EDITAR       = "hasAuthority('EMPRESA:EDITAR')";

  public static final String AJUDA_LER            = "hasAuthority('AJUDA:LER')";
  public static final String AJUDA_CRIAR          = "hasAuthority('AJUDA:CRIAR')";
  public static final String AJUDA_EDITAR         = "hasAuthority('AJUDA:EDITAR')";
  public static final String AJUDA_EXCLUIR        = "hasAuthority('AJUDA:EXCLUIR')";

  // ---------------------------------------------------------------------------
  // RELEASE_ORCHESTRATOR (V8 + V9)
  // ---------------------------------------------------------------------------
  public static final String RELEASE_LER          = "hasAuthority('RELEASE:LER')";
  public static final String RELEASE_CRIAR        = "hasAuthority('RELEASE:CRIAR')";
  public static final String RELEASE_EDITAR       = "hasAuthority('RELEASE:EDITAR')";
  public static final String RELEASE_EXCLUIR      = "hasAuthority('RELEASE:EXCLUIR')";

  public static final String PRODUTO_LER          = "hasAuthority('PRODUTO:LER')";
  public static final String PRODUTO_CRIAR        = "hasAuthority('PRODUTO:CRIAR')";
  public static final String PRODUTO_EDITAR       = "hasAuthority('PRODUTO:EDITAR')";
  public static final String PRODUTO_EXCLUIR      = "hasAuthority('PRODUTO:EXCLUIR')";

  public static final String TEMPLATE_LER         = "hasAuthority('TEMPLATE:LER')";
  public static final String TEMPLATE_CRIAR       = "hasAuthority('TEMPLATE:CRIAR')";
  public static final String TEMPLATE_EDITAR      = "hasAuthority('TEMPLATE:EDITAR')";
  public static final String TEMPLATE_EXCLUIR     = "hasAuthority('TEMPLATE:EXCLUIR')";

  public static final String CLIENTE_RO_LER       = "hasAuthority('CLIENTE_RO:LER')";
  public static final String CLIENTE_RO_CRIAR     = "hasAuthority('CLIENTE_RO:CRIAR')";
  public static final String CLIENTE_RO_EDITAR    = "hasAuthority('CLIENTE_RO:EDITAR')";
  public static final String CLIENTE_RO_EXCLUIR   = "hasAuthority('CLIENTE_RO:EXCLUIR')";

  public static final String ENTREGA_LER          = "hasAuthority('ENTREGA:LER')";
  public static final String ENTREGA_CRIAR        = "hasAuthority('ENTREGA:CRIAR')";
  public static final String ENTREGA_EDITAR       = "hasAuthority('ENTREGA:EDITAR')";
  public static final String ENTREGA_EXCLUIR      = "hasAuthority('ENTREGA:EXCLUIR')";

  public static final String PROXIMA_ENTREGA_LER     = "hasAuthority('PROXIMA_ENTREGA:LER')";
  public static final String PROXIMA_ENTREGA_CRIAR   = "hasAuthority('PROXIMA_ENTREGA:CRIAR')";
  public static final String PROXIMA_ENTREGA_EDITAR  = "hasAuthority('PROXIMA_ENTREGA:EDITAR')";
  public static final String PROXIMA_ENTREGA_EXCLUIR = "hasAuthority('PROXIMA_ENTREGA:EXCLUIR')";
}
