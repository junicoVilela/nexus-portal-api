package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

/**
 * Helper sem estado que monta o ZIP final da entrega (F1.11/F1.14).
 *
 * Layout do ZIP:
 * <pre>
 * /
 * ├── manifest.json
 * ├── SHA256SUMS.txt
 * └── {destinoPacote ou tipo lowercase}/
 *       └── {arquivos}
 * </pre>
 */
@Component
public class EmpacotadorEntrega {

  private final ObjectMapper json = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  /**
   * Monta o pacote no diretório destino. Retorna metadados pra persistir.
   *
   * @param entrega                entrega que está sendo gerada
   * @param linhas                 artefatos uploadados (delta — F1.10)
   * @param virtuais               artefatos sintetizados (FUNC/REGRAS — F1.12)
   * @param diretorioDestino       diretório onde o ZIP final será gravado
   * @return path absoluto do ZIP + sha256 + tamanho
   */
  public PacoteGerado empacotar(Entrega entrega, List<EntregaModuloArtefato> linhas,
      List<VirtualArtefato> virtuais, Path diretorioDestino) throws IOException {
    if (linhas.isEmpty() && virtuais.isEmpty()) {
      throw new BusinessException("Nada a empacotar — delta e renderizados vazios.");
    }

    Files.createDirectories(diretorioDestino);
    String nomeZip = nomeDoPacote(entrega);
    Path zipFinal = diretorioDestino.resolve(nomeZip);

    List<ItemPacote> itens = new ArrayList<>();
    MessageDigest sha256Zip = sha256Digest();

    try (OutputStream fileOut = Files.newOutputStream(zipFinal);
        OutputStream digestOut = new java.security.DigestOutputStream(fileOut, sha256Zip);
        ZipOutputStream zip = new ZipOutputStream(digestOut)) {

      for (EntregaModuloArtefato ema : linhas) {
        EntregaModulo em = ema.getEntregaModulo();
        ModuloProduto modulo = em.getModuloProduto();
        Path artefatoPath = Path.of(ema.getArtefato().getCaminhoArmazenado());
        if (!Files.exists(artefatoPath)) {
          throw new BusinessException(
              "Artefato não encontrado em disco: " + artefatoPath);
        }
        String pastaNoPacote = destinoPacoteOuTipo(modulo);
        String nomeArquivo = ema.getArtefato().getNomeArquivo();
        String entryName = pastaNoPacote + "/" + nomeArquivo;

        ZipEntry entry = new ZipEntry(entryName);
        entry.setSize(ema.getArtefato().getTamanhoBytes());
        zip.putNextEntry(entry);
        try (InputStream in = Files.newInputStream(artefatoPath)) {
          in.transferTo(zip);
        }
        zip.closeEntry();
        itens.add(new ItemPacote(entryName, ema.getArtefato().getSha256(),
            ema.getArtefato().getTamanhoBytes(), modulo.getCodigo(), em.getVersaoTo()));
      }

      // Virtuais (FUNC/REGRAS renderizados)
      for (VirtualArtefato va : virtuais) {
        ModuloProduto modulo = va.entregaModulo().getModuloProduto();
        String pastaNoPacote = destinoPacoteOuTipo(modulo);
        String entryName = pastaNoPacote + "/" + va.nomeArquivo();

        ZipEntry entry = new ZipEntry(entryName);
        entry.setSize(va.conteudo().length);
        zip.putNextEntry(entry);
        zip.write(va.conteudo());
        zip.closeEntry();
        itens.add(new ItemPacote(entryName, va.sha256(),
            va.conteudo().length, modulo.getCodigo(), va.entregaModulo().getVersaoTo()));
      }

      // manifest.json
      String manifestJson = json.writeValueAsString(montarManifest(entrega, itens));
      ZipEntry manifest = new ZipEntry("manifest.json");
      zip.putNextEntry(manifest);
      zip.write(manifestJson.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();

      // SHA256SUMS.txt
      ZipEntry sums = new ZipEntry("SHA256SUMS.txt");
      zip.putNextEntry(sums);
      for (ItemPacote item : itens) {
        zip.write((item.sha256() + "  " + item.entryName() + "\n")
            .getBytes(StandardCharsets.UTF_8));
      }
      zip.closeEntry();
    }

    long tamanho = Files.size(zipFinal);
    String shaZip = HexFormat.of().formatHex(sha256Zip.digest());
    return new PacoteGerado(zipFinal.toString(), shaZip, tamanho, itens.size());
  }

  private Map<String, Object> montarManifest(Entrega entrega, List<ItemPacote> itens) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("entregaId", entrega.getId().toString());
    root.put("cliente", Map.of(
        "id", entrega.getCliente().getId().toString(),
        "sigla", entrega.getCliente().getSigla(),
        "nome", entrega.getCliente().getNome()));
    root.put("produto", Map.of(
        "id", entrega.getProduto().getId().toString(),
        "sigla", entrega.getProduto().getSigla(),
        "nome", entrega.getProduto().getNome()));
    root.put("release", Map.of(
        "id", entrega.getRelease().getId().toString(),
        "versao", entrega.getRelease().getVersao(),
        "titulo", entrega.getRelease().getTitulo()));
    root.put("ambiente", entrega.getAmbiente().name());
    root.put("geradoEm", OffsetDateTime.now().toString());
    List<Map<String, Object>> artefatos = new ArrayList<>();
    for (ItemPacote it : itens) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("caminho", it.entryName());
      m.put("modulo", it.moduloCodigo());
      m.put("versaoTo", it.versaoTo());
      m.put("sha256", it.sha256());
      m.put("tamanhoBytes", it.tamanhoBytes());
      artefatos.add(m);
    }
    root.put("artefatos", artefatos);
    return root;
  }

  private String destinoPacoteOuTipo(ModuloProduto modulo) {
    String config = modulo.getConfigEspecifica();
    if (config != null && !config.isBlank()) {
      try {
        Map<?, ?> map = json.readValue(config, Map.class);
        Object destino = map.get("destinoPacote");
        if (destino instanceof String s && !s.isBlank()) {
          // remove barras finais e iniciais pra entry name limpo
          return s.replaceAll("^/+", "").replaceAll("/+$", "");
        }
      } catch (IOException ignored) {
        // configEspecifica não é JSON válido — cai no fallback
      }
    }
    return modulo.getTipo().name().toLowerCase();
  }

  private String nomeDoPacote(Entrega entrega) {
    String sigla = entrega.getProduto().getSigla().toLowerCase();
    String versao = entrega.getRelease().getVersao();
    String cliente = entrega.getCliente().getSigla().toLowerCase();
    String idCurto = entrega.getId().toString().substring(0, 8);
    return sigla + "-" + versao + "-" + cliente + "-" + idCurto + ".zip";
  }

  private static MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", ex);
    }
  }

  public record PacoteGerado(String caminho, String sha256, long tamanhoBytes, int totalItens) {}

  /**
   * Artefato gerado dinamicamente (FUNCIONALIDADES/REGRAS — F1.12). Não vem
   * de upload, é renderizado a partir da matriz cliente×funcionalidade.
   */
  public record VirtualArtefato(EntregaModulo entregaModulo, String nomeArquivo,
      byte[] conteudo, String sha256) {}

  private record ItemPacote(String entryName, String sha256, long tamanhoBytes,
      String moduloCodigo, String versaoTo) {}
}
