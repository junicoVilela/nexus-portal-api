package com.nexus.portal.docflow.service;

import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.service.GeradorPacoteService;
import com.nexus.portal.docflow.entity.PreviewToken;
import com.nexus.portal.docflow.repository.PreviewTokenRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.identityaccess.service.EscopoResolver;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PreviewTokenService {

  private final PreviewTokenRepository previewTokenRepository;
  private final ClienteRepository clienteRepository;
  private final GeradorPacoteService geradorPacoteService;
  private static final Duration CACHE_TTL = Duration.ofSeconds(60);

  private final EscopoResolver escopoResolver;
  private final PreviewRateLimiter rateLimiter;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Map<UUID, HtmlEmCache> cachePorCliente = new ConcurrentHashMap<>();

  @Transactional
  public PreviewToken gerar(UUID clienteId, int horasValidade, Principal principal) {
    clienteRepository.findById(clienteId)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    String token = gerarToken();
    OffsetDateTime expiracao = OffsetDateTime.now().plusHours(horasValidade <= 0 ? 24 : horasValidade);
    return previewTokenRepository.save(
        new PreviewToken(clienteId, token, expiracao, username(principal)));
  }

  public List<PreviewToken> listar(UUID clienteId) {
    if (!escopoResolver.podeAcessarCliente(clienteId)) {
      return List.of();
    }
    return previewTokenRepository.findByClienteIdAndAtivoTrue(clienteId);
  }

  @Transactional
  public void revogar(UUID tokenId, Principal principal) {
    PreviewToken pt = previewTokenRepository.findById(tokenId)
        .orElseThrow(() -> new NotFoundException("Token não encontrado."));
    escopoResolver.assertPodeEscreverEmCliente(pt.getClienteId());
    pt.revogar();
  }

  public String renderizarPreview(String token) {
    rateLimiter.registrarAcesso(token);
    PreviewToken pt = previewTokenRepository.findByTokenAndAtivoTrue(token)
        .orElseThrow(() -> new NotFoundException("Token inválido ou expirado."));
    if (!pt.estaValido()) {
      throw new BusinessException("Token expirado.");
    }
    if (!escopoResolver.podeAcessarCliente(pt.getClienteId())) {
      throw new NotFoundException("Token inválido ou expirado.");
    }
    return htmlDoCliente(pt.getClienteId());
  }

  /**
   * O endpoint do preview é público e remontar o manual inteiro a cada acesso
   * sai caro. O HTML fica em cache por {@link #CACHE_TTL}, janela curta o
   * bastante para o editor ver a alteração em seguida.
   */
  private String htmlDoCliente(UUID clienteId) {
    HtmlEmCache emCache = cachePorCliente.get(clienteId);
    if (emCache != null && emCache.valido()) {
      return emCache.html();
    }
    var cliente = clienteRepository.findById(clienteId)
        .orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    String html = geradorPacoteService.previewHtml(cliente, "preview");
    cachePorCliente.put(clienteId, new HtmlEmCache(html, Instant.now().plus(CACHE_TTL)));
    return html;
  }

  private record HtmlEmCache(String html, Instant expiraEm) {
    boolean valido() {
      return Instant.now().isBefore(expiraEm);
    }
  }

  private String gerarToken() {
    byte[] bytes = new byte[36];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String username(Principal principal) {
    return principal == null ? "system" : principal.getName();
  }
}
