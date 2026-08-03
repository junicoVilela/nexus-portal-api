package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.ArtefatoReleaseModulo;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.EntregaModuloArtefato;
import com.nexus.portal.releaseorchestrator.entity.ModuloProduto;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.Release;
import com.nexus.portal.releaseorchestrator.entity.ReleaseStatus;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.entity.TipoRelease;
import com.nexus.portal.shared.exception.BusinessException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmpacotadorEntregaTest {

  @TempDir Path tempDir;

  EmpacotadorEntrega empacotador;
  Cliente cliente;
  ProdutoRh produto;
  Release release;
  Entrega entrega;
  ModuloProduto modPortal;
  ModuloProduto modDb;

  @BeforeEach
  void setUp() throws Exception {
    empacotador = new EmpacotadorEntrega();
    cliente = new Cliente("ACME", "ACME", AmbientePadrao.PROD);
    setId(cliente, UUID.randomUUID());
    produto = new ProdutoRh("NEXUS-LD", "NEXUSLD", null, "#fff", null, true);
    setId(produto, UUID.randomUUID());
    release = new Release(produto, "1.5.0", "Release Junho", TipoRelease.MINOR,
        ReleaseStatus.PUBLICADA, null, null, null, null);
    setId(release, UUID.randomUUID());
    entrega = new Entrega(cliente, produto, release, AmbientePadrao.PROD, null, null);
    setId(entrega, UUID.randomUUID());

    modPortal = new ModuloProduto(produto, "nexus-portal", "Portal", TipoModulo.WEB,
        false, true, 0, "{\"destinoPacote\":\"web/portal\"}");
    setId(modPortal, UUID.randomUUID());
    modDb = new ModuloProduto(produto, "nexus-db", "DB", TipoModulo.BANCO,
        true, true, 1, null);
    setId(modDb, UUID.randomUUID());
  }

  @Test
  void empacotar_geraZipComManifestSha256SumsEArtefatos() throws Exception {
    Path warFile = tempDir.resolve("nexus.war");
    Files.write(warFile, "WAR-BYTES".getBytes(StandardCharsets.UTF_8));
    Path sqlFile = tempDir.resolve("ddl.sql");
    Files.write(sqlFile, "CREATE TABLE x;".getBytes(StandardCharsets.UTF_8));

    EntregaModulo emPortal = newEm(modPortal, "1.4.0", "1.5.0", 0);
    EntregaModulo emDb = newEm(modDb, "1.4.0", "1.5.0", 1);

    ArtefatoReleaseModulo art1 = newArtefato(modPortal, "nexus.war",
        warFile.toString(), "sha-war", Files.size(warFile));
    ArtefatoReleaseModulo art2 = newArtefato(modDb, "ddl.sql",
        sqlFile.toString(), "sha-sql", Files.size(sqlFile));

    List<EntregaModuloArtefato> linhas = List.of(
        new EntregaModuloArtefato(emPortal, art1, 0),
        new EntregaModuloArtefato(emDb, art2, 0));

    Path destino = tempDir.resolve("output");
    var pacote = empacotador.empacotar(entrega, linhas, java.util.List.of(), destino);

    Path zip = Path.of(pacote.caminho());
    assertThat(zip).exists();
    assertThat(pacote.totalItens()).isEqualTo(2);
    assertThat(pacote.tamanhoBytes()).isEqualTo(Files.size(zip));
    assertThat(pacote.sha256()).hasSize(64);
    assertThat(zip.getFileName().toString())
        .startsWith("nexusld-1.5.0-acme-")
        .endsWith(".zip");

    Map<String, String> entries = listarZip(zip);
    assertThat(entries).containsKey("web/portal/nexus.war");
    assertThat(entries).containsKey("banco/ddl.sql");
    assertThat(entries).containsKey("manifest.json");
    assertThat(entries).containsKey("SHA256SUMS.txt");

    assertThat(entries.get("manifest.json"))
        .contains("\"entregaId\"")
        .contains("\"ACME\"")
        .contains("\"NEXUSLD\"")
        .contains("\"versao\" : \"1.5.0\"")
        .contains("web/portal/nexus.war")
        .contains("banco/ddl.sql");

    assertThat(entries.get("SHA256SUMS.txt"))
        .contains("sha-war  web/portal/nexus.war")
        .contains("sha-sql  banco/ddl.sql");

    assertThat(entries.get("web/portal/nexus.war")).isEqualTo("WAR-BYTES");
  }

  @Test
  void empacotar_rejeitaDeltaVazio() {
    assertThatThrownBy(() -> empacotador.empacotar(entrega, List.of(), java.util.List.of(), tempDir))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("vazio");
  }

  @Test
  void empacotar_falhaQuandoArtefatoSumiuDoDisco() {
    EntregaModulo em = newEm(modPortal, "1.4.0", "1.5.0", 0);
    ArtefatoReleaseModulo art = newArtefato(modPortal, "nexus.war",
        "/tmp/inexistente-" + UUID.randomUUID() + ".war", "sha", 100);
    List<EntregaModuloArtefato> linhas = List.of(
        new EntregaModuloArtefato(em, art, 0));

    assertThatThrownBy(() -> empacotador.empacotar(entrega, linhas, java.util.List.of(), tempDir))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("não encontrado");
  }

  private EntregaModulo newEm(ModuloProduto modulo, String from, String to, int ordem) {
    return new EntregaModulo(entrega, modulo, from, to, true, false, ordem);
  }

  private ArtefatoReleaseModulo newArtefato(ModuloProduto modulo, String nome,
      String caminho, String sha, long tamanho) {
    return new ArtefatoReleaseModulo(release, modulo, nome, caminho, sha, tamanho, null);
  }

  private static Map<String, String> listarZip(Path zip) throws IOException {
    Map<String, String> conteudo = new HashMap<>();
    try (ZipFile zf = new ZipFile(zip.toFile())) {
      var entries = zf.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        try (var in = zf.getInputStream(entry)) {
          conteudo.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
      }
    }
    return conteudo;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
