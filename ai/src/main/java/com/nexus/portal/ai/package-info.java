/**
 * Módulo Maven {@code nexus-ai} — assistente de IA do portal.
 *
 * <p><b>Fronteira de extração:</b> API pública em {@code /api/v1/ai/**}, schema Flyway
 * {@code V*__ai__*}, configuração {@code nexus.ai.*}. Dependência do DocFlow concentrada em
 * {@link com.nexus.portal.ai.integration.docflow} — único pacote a trocar por cliente HTTP
 * se o módulo virar serviço separado.
 *
 * <p>Camadas: {@code controller → service → repository/provider}.
 */
package com.nexus.portal.ai;
