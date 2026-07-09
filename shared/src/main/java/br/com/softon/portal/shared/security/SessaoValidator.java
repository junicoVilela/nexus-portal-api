package br.com.softon.portal.shared.security;

/**
 * SPI para o JwtAuthFilter validar se o jti de um token ainda está ativo.
 * Implementado pelo módulo dtec-rbac (SessaoService). Presente aqui só para
 * evitar dependência circular shared → dtec-rbac.
 */
public interface SessaoValidator {

  /**
   * True se a sessão associada ao jti está ativa (não revogada, não expirada).
   * Se o validador não estiver disponível ou o token não tem jti,
   * implementações devem retornar true (fail-open pra não bloquear boot
   * antes do dtec-rbac estar pronto).
   */
  boolean sessaoAtiva(String jti);
}
