package com.nexus.portal.shared.security;

import java.util.List;

/**
 * SPI para o {@code JwtAuthFilter} carregar autoridades atuais do catálogo RBAC.
 * Implementado pelo módulo nexus-identity-access. Presente aqui para evitar
 * dependência circular shared → nexus-identity-access.
 *
 * <p>O JWT continua autenticando a identidade; as permissões vêm do banco para
 * não exigir re-login quando o catálogo cresce (ex.: nova funcionalidade Flyway).
 */
public interface AutoridadeResolver {

  /**
   * Permissões ativas do usuário. {@code null} significa "manter as claims do JWT"
   * (usuário inexistente ou resolução indisponível). Lista vazia é intencional
   * (sem grupos / todas revogadas).
   */
  List<String> permissoesDoUsername(String username);
}
