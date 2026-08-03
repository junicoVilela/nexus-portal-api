package com.nexus.portal.releaseorchestrator.integration.publish;

import com.nexus.portal.releaseorchestrator.entity.ConfigEntrega;
import com.nexus.portal.releaseorchestrator.entity.TipoDestinoEntrega;
import com.nexus.portal.releaseorchestrator.service.EncryptionService;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Publica o pacote em bucket S3-compatível (AWS S3, MinIO, Backblaze B2 etc).
 *
 * <p>Reaproveita os campos da config:
 * <ul>
 *   <li>{@code usuario} → access key ID</li>
 *   <li>{@code senhaCifrada} → secret key (decifrada via EncryptionService)</li>
 *   <li>{@code bucket}, {@code endpoint}, {@code regiao}, {@code pathStyleAccess} → dedicados</li>
 *   <li>{@code caminhoBase} → prefixo (key prefix) dentro do bucket</li>
 * </ul>
 *
 * <p>Política:
 * <ul>
 *   <li>Endpoint vazio → AWS S3 padrão (region obrigatória do request).</li>
 *   <li>Endpoint preenchido → MinIO/Backblaze, default path-style ON.</li>
 *   <li>Sobrescreve key existente (PUT direto sem if-match).</li>
 *   <li>Sem multipart no MVP — ZIPs ficam abaixo do limite single-PUT (5 GB).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3PublishStrategy implements PublishStrategy {

  private static final Region DEFAULT_REGION = Region.US_EAST_1;
  private static final Duration API_TIMEOUT = Duration.ofSeconds(60);

  private final EncryptionService encryptionService;

  @Override
  public TipoDestinoEntrega tipo() {
    return TipoDestinoEntrega.BUCKET;
  }

  @Override
  public PublishResult publicar(ConfigEntrega config, Path pacote) {
    validar(config);
    String key = construirKey(config, pacote.getFileName().toString());
    try (S3Client client = construirClient(config)) {
      long tamanho = Files.size(pacote);
      PutObjectRequest req = PutObjectRequest.builder()
          .bucket(config.getBucket())
          .key(key)
          .contentLength(tamanho)
          .build();
      client.putObject(req, pacote);
      String destino = "s3://" + config.getBucket() + "/" + key;
      log.info("Pacote publicado em {} ({} bytes)", destino, tamanho);
      return new PublishResult(destino, tamanho, OffsetDateTime.now());
    } catch (S3Exception e) {
      throw new PublishException("S3 rejeitou upload: "
          + e.awsErrorDetails().errorMessage(), e);
    } catch (SdkException | java.io.IOException e) {
      throw new PublishException("Falha no upload S3: " + e.getMessage(), e);
    }
  }

  @Override
  public void testarConexao(ConfigEntrega config) {
    validar(config);
    try (S3Client client = construirClient(config)) {
      client.headBucket(HeadBucketRequest.builder().bucket(config.getBucket()).build());
      log.info("Teste S3 OK em bucket={} endpoint={}",
          config.getBucket(), config.getEndpoint() != null ? config.getEndpoint() : "(AWS default)");
    } catch (S3Exception e) {
      throw new PublishException("S3 rejeitou headBucket: "
          + e.awsErrorDetails().errorMessage(), e);
    } catch (SdkException e) {
      throw new PublishException("Falha conectando ao bucket: " + e.getMessage(), e);
    }
  }

  private S3Client construirClient(ConfigEntrega config) {
    String secret = encryptionService.decifrar(config.getSenhaCifrada());
    var creds = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(config.getUsuario(), secret));
    boolean pathStyle = Boolean.TRUE.equals(config.getPathStyleAccess())
        || (config.getEndpoint() != null && !config.getEndpoint().isBlank());

    var builder = S3Client.builder()
        .credentialsProvider(creds)
        .region(resolverRegiao(config))
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(pathStyle)
            .build())
        .overrideConfiguration(c -> c.apiCallTimeout(API_TIMEOUT));

    if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
      builder.endpointOverride(URI.create(config.getEndpoint()));
    }
    return builder.build();
  }

  private Region resolverRegiao(ConfigEntrega config) {
    if (config.getRegiao() != null && !config.getRegiao().isBlank()) {
      return Region.of(config.getRegiao());
    }
    return DEFAULT_REGION;
  }

  /** Concatena {@code caminhoBase} com o nome do arquivo, removendo barras duplicadas. */
  private String construirKey(ConfigEntrega config, String nomeArquivo) {
    String prefixo = config.getCaminhoBase();
    if (prefixo == null || prefixo.isBlank()) {
      return nomeArquivo;
    }
    String limpo = prefixo.replaceAll("^/+", "").replaceAll("/+$", "");
    return limpo.isEmpty() ? nomeArquivo : limpo + "/" + nomeArquivo;
  }

  private void validar(ConfigEntrega c) {
    if (c.getBucket() == null || c.getBucket().isBlank()) {
      throw new PublishException("Bucket obrigatório para destino BUCKET.");
    }
    if (c.getUsuario() == null || c.getUsuario().isBlank()) {
      throw new PublishException("Access key obrigatória para destino BUCKET.");
    }
    if (!c.temSenhaConfigurada()) {
      throw new PublishException("Secret key obrigatória para destino BUCKET.");
    }
  }
}
