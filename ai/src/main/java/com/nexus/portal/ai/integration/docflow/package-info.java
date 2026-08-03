/**
 * Única fronteira de acoplamento com o DocFlow.
 *
 * <p>Coloque aqui facades/adapters que chamam {@code PaginaService},
 * {@code PaginaTemplateService}, {@code PaginaQualidadeService}, etc.
 *
 * <p>Na extração do {@code nexus-ai} para um serviço separado, este pacote é
 * substituído por um cliente HTTP contra {@code /api/v1/docflow/**}.
 */
package com.nexus.portal.ai.integration.docflow;
