package com.nexus.portal.ai.integration.github;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Assinatura dos webhooks do GitHub: {@code X-Hub-Signature-256: sha256=<HMAC-SHA256(secret, corpo)>}.
 * O GitHub não envia cabeçalhos customizados — o "Secret" do webhook vira esta assinatura.
 *
 * @see <a href="https://docs.github.com/webhooks/using-webhooks/validating-webhook-deliveries">Validating deliveries</a>
 */
public final class AiGithubAssinatura {

  private static final String PREFIXO = "sha256=";

  private AiGithubAssinatura() {}

  public static String assinar(String secret, byte[] corpo) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return PREFIXO + HexFormat.of().formatHex(mac.doFinal(corpo));
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new IllegalStateException("HMAC-SHA256 indisponível", ex);
    }
  }

  /** Comparação em tempo constante. Falso para cabeçalho ausente ou fora do formato. */
  public static boolean valida(String secret, byte[] corpo, String cabecalho) {
    if (secret == null || secret.isBlank() || cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
      return false;
    }
    byte[] esperado = assinar(secret, corpo).getBytes(StandardCharsets.US_ASCII);
    byte[] recebido = cabecalho.strip().toLowerCase().getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(esperado, recebido);
  }
}
