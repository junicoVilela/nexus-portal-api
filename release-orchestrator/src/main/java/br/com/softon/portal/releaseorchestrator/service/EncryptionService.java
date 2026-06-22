package br.com.softon.portal.releaseorchestrator.service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cifragem simétrica para segredos de cadastro (senhas FTP/SFTP). AES-GCM
 * com IV aleatório por payload — o IV é prepended ao ciphertext e o
 * resultado final é codificado em base64 (URL-safe).
 *
 * <p>A chave vem de {@code release-orchestrator.encryption.key} (env var
 * {@code RELEASE_ORCHESTRATOR_ENCRYPTION_KEY}). Deve ser exatamente
 * <strong>32 bytes em base64</strong> (256 bits) — gere com
 * {@code openssl rand -base64 32}.
 *
 * <p>Quando a key não está configurada, o service entra em <em>modo dev</em>
 * usando uma chave fixa de desenvolvimento — assim o backend sobe em dev
 * sem precisar configurar o ambiente. Em produção, configurar a env é
 * obrigatório; um log.warn é emitido no startup quando estamos no fallback.
 *
 * <p>Formato do payload codificado: {@code base64(iv || ciphertext+tag)}
 * com IV de 12 bytes (recomendado para GCM) e tag de 128 bits.
 */
@Slf4j
@Service
public class EncryptionService {

  private static final String ALGO = "AES";
  private static final String TRANSFORM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH_BYTES = 12;
  private static final int TAG_LENGTH_BITS = 128;

  /**
   * Dev-only fallback (32 bytes em base64). NÃO usar em produção — em prod
   * o backend deve falhar se a env var não estiver configurada. Esta chave
   * é literal e propositalmente sem segredo: serve só pra testes locais e
   * pipeline de CI.
   */
  private static final String DEV_KEY_BASE64 =
      "ATot//U/MD4/dpS/+5bd6OrVlDhA6fC02R39f0WsCU8=";

  private final String keyConfig;
  private SecretKey chave;

  public EncryptionService(@Value("${release-orchestrator.encryption.key:}") String keyConfig) {
    this.keyConfig = keyConfig;
  }

  @PostConstruct
  void inicializar() {
    String base64;
    if (keyConfig == null || keyConfig.isBlank()) {
      log.warn("release-orchestrator.encryption.key NÃO configurada — usando key DEV. "
          + "Não use isso em produção.");
      base64 = DEV_KEY_BASE64;
    } else {
      base64 = keyConfig;
    }
    byte[] raw = Base64.getDecoder().decode(base64);
    if (raw.length != 32) {
      throw new IllegalStateException(
          "release-orchestrator.encryption.key deve ter 32 bytes em base64 (gere com "
              + "openssl rand -base64 32). Recebido: " + raw.length + " bytes.");
    }
    this.chave = new SecretKeySpec(raw, ALGO);
  }

  /**
   * Cifra um texto plano. Retorna {@code base64(iv || ciphertext)} pronto
   * para persistir em coluna {@code VARCHAR}.
   *
   * @return null se {@code plaintext} for null/vazio (caller decide se mantém
   *         o valor anterior — útil para o padrão "senha em branco preserva
   *         a atual" no PUT).
   */
  public String cifrar(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) return null;
    try {
      byte[] iv = new byte[IV_LENGTH_BYTES];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao cifrar payload.", e);
    }
  }

  /**
   * Decifra um payload produzido por {@link #cifrar(String)}.
   *
   * @throws IllegalStateException se o payload for inválido (tag inválida =
   *         chave errada ou conteúdo adulterado).
   */
  public String decifrar(String payloadBase64) {
    if (payloadBase64 == null || payloadBase64.isBlank()) return null;
    try {
      byte[] combined = Base64.getDecoder().decode(payloadBase64);
      if (combined.length < IV_LENGTH_BYTES + 1) {
        throw new IllegalStateException("Payload cifrado muito curto.");
      }
      byte[] iv = new byte[IV_LENGTH_BYTES];
      byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
      System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
      System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);
      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao decifrar payload.", e);
    }
  }
}
