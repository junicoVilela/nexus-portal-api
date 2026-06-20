package br.com.softon.portal.shared.security;

/**
 * Constantes de expressão SpEL para uso em {@code @PreAuthorize}. Compile-time
 * inlining garante que as anotações fiquem com strings literais. Centraliza a
 * lista de roles que mudam (LEITOR adicionado em F0.9) e evita typos espalhados.
 */
public final class SecurityRoles {

  /** Endpoints de escrita: ADMIN e EDITOR. */
  public static final String WRITE = "hasAnyRole('ADMIN','EDITOR')";

  /** Endpoints de leitura: ADMIN, EDITOR e LEITOR. */
  public static final String READ = "hasAnyRole('ADMIN','EDITOR','LEITOR')";

  /** Apenas ADMIN. */
  public static final String ADMIN_ONLY = "hasRole('ADMIN')";

  private SecurityRoles() {}
}
