package com.nexus.portal.shared.security;

import java.util.Set;

/**
 * Origens extras liberadas no CORS das rotas do manual para os sistemas do cliente
 * ({@code /api/v1/manual/**}, {@code /api/v1/ai/manual/**}, MCP). Implementado pelo DocFlow a
 * partir das chaves de integração ativas (INT-405); o {@code SecurityConfig} só consulta.
 */
public interface OrigensManualCors {

  Set<String> origens();
}
