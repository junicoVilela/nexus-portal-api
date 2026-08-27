package com.nexus.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexus.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import com.nexus.portal.docflow.entity.Cliente;
import com.nexus.portal.docflow.entity.Publicacao;
import com.nexus.portal.shared.exception.BusinessException;
import com.nexus.portal.shared.exception.NotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicacaoConteudoServiceTest {

  @Mock PublicacaoService publicacaoService;

  PublicacaoConteudoService service;

  @TempDir Path storage;

  UUID publicacaoId = UUID.randomUUID();
  UUID paginaId = UUID.randomUUID();
  Publicacao publicacao;

  @BeforeEach
  void setUp() throws Exception {
    service = new PublicacaoConteudoService(publicacaoService);
    publicacao = new Publicacao(new Cliente("ACME", "acme", true), "1.0.0", null);
    setCampo(publicacao, "id", publicacaoId);
    when(publicacaoService.buscar(publicacaoId)).thenReturn(publicacao);
    when(publicacaoService.arvorePaginas(publicacaoId)).thenReturn(List.of(
        new PublicacaoPaginaSnapshotItem(paginaId, null, "Login", "LOG", "login", 0, 0, "hash")));
  }

  @Test
  void htmlDaPagina_extraiSomenteOCorpoDoArtigo() throws Exception {
    Path zip = zipCom("paginas/login.html", """
        <html><body>
          <nav>menu que não interessa</nav>
          <section class="page"><div class="article-content"><p>Conteúdo da versão 1</p></div></section>
        </body></html>
        """);
    publicacao.registrarSucesso(1, 1, "manual.zip", zip.toString(), "sha", "{}");

    String html = service.htmlDaPagina(publicacaoId, paginaId);

    assertThat(html).contains("Conteúdo da versão 1").doesNotContain("menu que não interessa");
  }

  @Test
  void htmlDaPagina_publicacaoNaoConcluida_recusa() {
    assertThatThrownBy(() -> service.htmlDaPagina(publicacaoId, paginaId))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("concluídas");
  }

  @Test
  void htmlDaPagina_paginaForaDaPublicacao_devolve404() throws Exception {
    Path zip = zipCom("paginas/login.html", "<html><body><p>x</p></body></html>");
    publicacao.registrarSucesso(1, 1, "manual.zip", zip.toString(), "sha", "{}");

    assertThatThrownBy(() -> service.htmlDaPagina(publicacaoId, UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("não faz parte");
  }

  @Test
  void htmlDaPagina_pacoteAusenteNoDisco_devolve404() {
    publicacao.registrarSucesso(1, 1, "manual.zip", storage.resolve("sumiu.zip").toString(), "sha", "{}");

    assertThatThrownBy(() -> service.htmlDaPagina(publicacaoId, paginaId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("não encontrado em disco");
  }

  @Test
  void htmlDaPagina_paginaAusenteNoZip_devolve404() throws Exception {
    Path zip = zipCom("paginas/outra.html", "<html><body><p>x</p></body></html>");
    publicacao.registrarSucesso(1, 1, "manual.zip", zip.toString(), "sha", "{}");

    assertThatThrownBy(() -> service.htmlDaPagina(publicacaoId, paginaId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("no pacote");
  }

  private Path zipCom(String entrada, String conteudo) throws IOException {
    Path zip = storage.resolve("manual-" + UUID.randomUUID() + ".zip");
    try (ZipOutputStream saida = new ZipOutputStream(Files.newOutputStream(zip))) {
      saida.putNextEntry(new ZipEntry(entrada));
      saida.write(conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      saida.closeEntry();
    }
    return zip;
  }

  private static void setCampo(Object alvo, String campo, Object valor) throws Exception {
    Field f = alvo.getClass().getDeclaredField(campo);
    f.setAccessible(true);
    f.set(alvo, valor);
  }
}
