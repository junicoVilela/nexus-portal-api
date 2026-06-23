package br.com.softon.portal.releaseorchestrator.entity;

/**
 * Estado da publicação remota de uma entrega (F3 P2).
 *
 * <ul>
 *   <li>{@link #NAO_APLICAVEL} — destino PASTA local; nada a publicar.</li>
 *   <li>{@link #PENDENTE} — aguardando próxima tentativa (vide
 *       {@code proxima_tentativa_em}); o retry job está no caso.</li>
 *   <li>{@link #OK} — publicação concluída com sucesso.</li>
 *   <li>{@link #FALHA} — esgotou {@code max-tentativas}; precisa de
 *       intervenção manual (corrigir config ou reprocessar via UI).</li>
 * </ul>
 */
public enum StatusPublicacao {
  NAO_APLICAVEL,
  PENDENTE,
  OK,
  FALHA
}
