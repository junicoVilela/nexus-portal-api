package com.nexus.portal.docflow.service;

import com.nexus.identityaccess.service.EscopoResolver;
import com.nexus.portal.docflow.entity.ManualAcesso;
import com.nexus.portal.docflow.repository.ClienteRepository;
import com.nexus.portal.docflow.repository.ManualAcessoRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import com.nexus.portal.shared.security.OrigensManualCors;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chaves de integração do manual (Onda D, INT-403). O token ({@code nxm_…}) aparece uma vez, na
 * criação; o banco guarda só o sha256. Cada chave tem limite de acessos por minuto próprio — um
 * sistema do cliente atende muitos usuários, diferente do link de prévia.
 */
@Service
public class ManualAcessoService implements OrigensManualCors {

  public static final String PREFIXO_TOKEN = "nxm_";
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Duration CACHE_ORIGENS = Duration.ofMinutes(1);

  private final ManualAcessoRepository repository;
  private final ClienteRepository clienteRepository;
  private final EscopoResolver escopoResolver;
  private final int limitePorMinuto;
  private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
  private volatile OrigensEmCache origensEmCache;

  public ManualAcessoService(
      ManualAcessoRepository repository,
      ClienteRepository clienteRepository,
      EscopoResolver escopoResolver,
      @Value("${docflow.manual.limite-por-minuto:600}") int limitePorMinuto) {
    this.repository = repository;
    this.clienteRepository = clienteRepository;
    this.escopoResolver = escopoResolver;
    this.limitePorMinuto = limitePorMinuto;
  }

  /** Chave criada: o {@code token} só existe aqui. */
  public record Criada(ManualAcesso acesso, String token) {}

  @Transactional
  public Criada criar(UUID clienteId, String nome, List<String> origens, Integer diasValidade, Principal principal) {
    clienteRepository.findById(clienteId).orElseThrow(() -> new NotFoundException("Cliente não encontrado."));
    escopoResolver.assertPodeEscreverEmCliente(clienteId);
    if (nome == null || nome.isBlank()) {
      throw new BusinessException("Dê um nome à chave (ex.: \"NEXUS-LD produção\").");
    }
    List<String> normalizadas = (origens == null ? List.<String>of() : origens).stream()
        .map(ManualAcessoService::normalizarOrigem)
        .distinct()
        .toList();
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    String token = PREFIXO_TOKEN + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    OffsetDateTime expira = diasValidade == null || diasValidade <= 0 ? null : OffsetDateTime.now().plusDays(diasValidade);
    ManualAcesso acesso = repository.save(new ManualAcesso(clienteId, nome.strip(), token.substring(0, 12),
        hash(token), normalizadas, expira, principal == null ? null : principal.getName()));
    origensEmCache = null;
    return new Criada(acesso, token);
  }

  @Transactional(readOnly = true)
  public List<ManualAcesso> listar(UUID clienteId) {
    if (!escopoResolver.podeAcessarCliente(clienteId)) {
      throw new NotFoundException("Cliente não encontrado.");
    }
    return repository.findByClienteIdOrderByCreatedAtDesc(clienteId);
  }

  @Transactional
  public void revogar(UUID id, Principal principal) {
    ManualAcesso acesso = repository.findById(id).orElseThrow(() -> new NotFoundException("Chave não encontrada."));
    escopoResolver.assertPodeEscreverEmCliente(acesso.getClienteId());
    acesso.revogar(principal == null ? null : principal.getName());
    origensEmCache = null;
  }

  /**
   * Cliente da chave, conferindo validade, origem do navegador e limite de acessos.
   *
   * @param origem cabeçalho {@code Origin}; nulo em chamadas servidor a servidor
   */
  @Transactional
  public UUID clienteDaChave(String token, String origem) {
    ManualAcesso acesso = repository.findByTokenHash(hash(token))
        .filter(a -> a.valido(OffsetDateTime.now()))
        .orElseThrow(() -> new NotFoundException("Chave do manual inválida, revogada ou expirada."));
    if (!acesso.aceitaOrigem(origem)) {
      throw new NotFoundException("Origem " + origem + " não está liberada para esta chave do manual.");
    }
    limitar(acesso.getId().toString());
    OffsetDateTime agora = OffsetDateTime.now();
    if (acesso.getUltimoUsoEm() == null || acesso.getUltimoUsoEm().isBefore(agora.minusMinutes(5))) {
      acesso.registrarUso(agora); // sem escrever a cada requisição
    }
    return acesso.getClienteId();
  }

  /** Origens de todas as chaves ativas, para o CORS das rotas do manual (cache de 1 min). */
  @Override
  @Transactional(readOnly = true)
  public Set<String> origens() {
    OrigensEmCache atual = origensEmCache;
    if (atual != null && atual.expira().isAfter(Instant.now())) {
      return atual.origens();
    }
    OffsetDateTime agora = OffsetDateTime.now();
    Set<String> origens = repository.findByAtivoTrue().stream()
        .filter(a -> a.valido(agora))
        .flatMap(a -> a.getOrigens().stream())
        .collect(Collectors.toUnmodifiableSet());
    origensEmCache = new OrigensEmCache(origens, Instant.now().plus(CACHE_ORIGENS));
    return origens;
  }

  public static boolean ehChave(String token) {
    return token != null && token.startsWith(PREFIXO_TOKEN);
  }

  /** {@code https://App.Cliente.com/} → {@code https://app.cliente.com}; recusa o que não é origem. */
  static String normalizarOrigem(String origem) {
    try {
      URI uri = URI.create(origem.strip());
      if (uri.getScheme() == null || uri.getHost() == null
          || !(uri.getScheme().equals("https") || uri.getScheme().equals("http"))
          || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))) {
        throw new IllegalArgumentException();
      }
      return uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT)
          + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("Origem inválida: \"" + origem + "\". Use o formato https://app.cliente.com.");
    }
  }

  static String hash(String token) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void limitar(String chave) {
    if (limitePorMinuto <= 0) {
      return;
    }
    Instant agora = Instant.now();
    Janela janela = janelas.compute(chave, (k, atual) ->
        atual == null || atual.expira().isBefore(agora) ? new Janela(agora.plusSeconds(60), new AtomicInteger()) : atual);
    if (janela.acessos().incrementAndGet() > limitePorMinuto) {
      throw new BusinessException("Muitos acessos ao manual com esta chave. Tente novamente em instantes.");
    }
    if (janelas.size() > 1_000) {
      janelas.entrySet().removeIf(e -> e.getValue().expira().isBefore(agora));
    }
  }

  private record Janela(Instant expira, AtomicInteger acessos) {}

  private record OrigensEmCache(Set<String> origens, Instant expira) {}
}
