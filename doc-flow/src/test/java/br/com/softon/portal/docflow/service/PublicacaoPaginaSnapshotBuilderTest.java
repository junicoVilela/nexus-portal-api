package br.com.softon.portal.docflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.softon.portal.docflow.dto.response.PaginaResponse;
import br.com.softon.portal.docflow.dto.response.PublicacaoPaginaSnapshotItem;
import br.com.softon.portal.docflow.entity.Modulo;
import br.com.softon.portal.docflow.entity.Pagina;
import br.com.softon.portal.docflow.entity.Projeto;
import br.com.softon.portal.docflow.entity.StatusPagina;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicacaoPaginaSnapshotBuilderTest {

  @Test
  void build_comHierarquia_calculaNivelEPreservaOrdem() throws Exception {
    Projeto projeto = new Projeto("Projeto", "projeto", null, true);
    Modulo modulo = new Modulo("Módulo", "modulo", null, 0, true, projeto);
    Pagina pai = pagina("Pai", "pai", "PAI", null, 0, modulo);
    UUID paiId = UUID.randomUUID();
    setId(pai, paiId);
    Pagina filho = pagina("Filho", "filho", "FILHO", pai, 1, modulo);
    UUID filhoId = UUID.randomUUID();
    setId(filho, filhoId);

    List<PublicacaoPaginaSnapshotItem> snapshot = PublicacaoPaginaSnapshotBuilder.build(
        List.of(PaginaResponse.from(pai), PaginaResponse.from(filho)));

    assertThat(snapshot).hasSize(2);
    assertThat(snapshot.get(0).id()).isEqualTo(paiId);
    assertThat(snapshot.get(0).parentId()).isNull();
    assertThat(snapshot.get(0).nivel()).isZero();
    assertThat(snapshot.get(1).id()).isEqualTo(filhoId);
    assertThat(snapshot.get(1).parentId()).isEqualTo(paiId);
    assertThat(snapshot.get(1).nivel()).isEqualTo(1);
    assertThat(snapshot.get(1).ordem()).isEqualTo(1);
  }

  private static Pagina pagina(String titulo, String slug, String codigo, Pagina parent, int ordem,
      Modulo modulo) {
    Pagina pagina = new Pagina(titulo, slug, codigo, "Resumo editorial com texto suficiente.",
        "<p>Conteúdo útil com texto suficiente para passar na validação editorial.</p>",
        ordem, true, modulo, parent);
    pagina.publicar();
    return pagina;
  }

  private static void setId(Object entity, UUID id) throws Exception {
    Field f = entity.getClass().getDeclaredField("id");
    f.setAccessible(true);
    f.set(entity, id);
  }
}
