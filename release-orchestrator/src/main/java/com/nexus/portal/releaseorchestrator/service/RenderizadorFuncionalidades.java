package com.nexus.portal.releaseorchestrator.service;

import com.nexus.portal.releaseorchestrator.entity.ClienteFuncionalidadeProduto;
import com.nexus.portal.releaseorchestrator.entity.Entrega;
import com.nexus.portal.releaseorchestrator.entity.EntregaModulo;
import com.nexus.portal.releaseorchestrator.entity.FuncionalidadeProduto;
import com.nexus.portal.releaseorchestrator.entity.TipoModulo;
import com.nexus.portal.releaseorchestrator.repository.OrchestratorClienteFuncionalidadeRepository;
import com.nexus.portal.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gera o conteúdo dos módulos {@link TipoModulo#FUNCIONALIDADES} e
 * {@link TipoModulo#REGRAS} a partir da matriz
 * {@link ClienteFuncionalidadeProduto} (F1.12).
 *
 * Estes módulos não aceitam upload — o conteúdo é sempre regerado por
 * cliente, sincronizando o subset habilitado.
 */
@Component
@RequiredArgsConstructor
public class RenderizadorFuncionalidades {

  private final OrchestratorClienteFuncionalidadeRepository matrizRepository;
  private final ObjectMapper json = new ObjectMapper()
      .enable(SerializationFeature.INDENT_OUTPUT);

  public List<EmpacotadorEntrega.VirtualArtefato> renderizar(Entrega entrega,
      List<EntregaModulo> linhas) {
    List<EmpacotadorEntrega.VirtualArtefato> resultado = new ArrayList<>();
    List<ClienteFuncionalidadeProduto> matriz = null;

    for (EntregaModulo em : linhas) {
      if (!em.isSelecionado()) continue;
      TipoModulo tipo = em.getModuloProduto().getTipo();
      if (tipo != TipoModulo.FUNCIONALIDADES && tipo != TipoModulo.REGRAS) continue;

      // Lazy load — só busca matriz se algum módulo FUNC/REGRAS estiver selecionado
      if (matriz == null) {
        matriz = matrizRepository.findByClienteEProduto(
            entrega.getCliente().getId(), entrega.getProduto().getId());
      }

      byte[] conteudo = (tipo == TipoModulo.FUNCIONALIDADES)
          ? renderSqlFuncionalidades(entrega, matriz)
          : renderJsonRegras(entrega, matriz);
      String nomeArquivo = (tipo == TipoModulo.FUNCIONALIDADES)
          ? "funcionalidades-" + entrega.getCliente().getSigla().toLowerCase() + ".sql"
          : "regras-" + entrega.getCliente().getSigla().toLowerCase() + ".json";

      String sha256 = sha256Hex(conteudo);
      resultado.add(new EmpacotadorEntrega.VirtualArtefato(em, nomeArquivo, conteudo, sha256));
    }

    return resultado;
  }

  private byte[] renderSqlFuncionalidades(Entrega entrega, List<ClienteFuncionalidadeProduto> matriz) {
    String clienteSigla = entrega.getCliente().getSigla();
    StringBuilder sb = new StringBuilder();
    sb.append("-- =============================================================================\n");
    sb.append("-- Funcionalidades por cliente — gerado automaticamente pelo Nexus Portal\n");
    sb.append("-- Cliente: ").append(entrega.getCliente().getNome()).append(" (").append(clienteSigla).append(")\n");
    sb.append("-- Produto: ").append(entrega.getProduto().getNome())
        .append(" (").append(entrega.getProduto().getSigla()).append(")\n");
    sb.append("-- Release: ").append(entrega.getRelease().getVersao()).append("\n");
    sb.append("-- =============================================================================\n\n");

    sb.append("DELETE FROM TB_CLIENTE_FUNCIONALIDADE\n");
    sb.append(" WHERE CD_CLIENTE = '").append(escape(clienteSigla)).append("';\n\n");

    for (ClienteFuncionalidadeProduto cf : matriz) {
      if (!cf.isHabilitada()) continue;
      FuncionalidadeProduto fp = cf.getFuncionalidade();
      String codigoLegado = fp.getCodigoLegado() != null ? fp.getCodigoLegado() : fp.getCodigo();
      String codigoOperacao = fp.getCodigoOperacao() != null ? fp.getCodigoOperacao() : "DEFAULT";
      sb.append("INSERT INTO TB_CLIENTE_FUNCIONALIDADE (CD_CLIENTE, CD_FUNCIONALIDADE, CD_OPERACAO)\n");
      sb.append("VALUES ('").append(escape(clienteSigla))
          .append("', '").append(escape(codigoLegado))
          .append("', '").append(escape(codigoOperacao)).append("');\n");
    }
    sb.append("\nCOMMIT;\n");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private byte[] renderJsonRegras(Entrega entrega, List<ClienteFuncionalidadeProduto> matriz) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("cliente", entrega.getCliente().getSigla());
    root.put("produto", entrega.getProduto().getSigla());
    root.put("release", entrega.getRelease().getVersao());

    List<Map<String, Object>> regras = new ArrayList<>();
    for (ClienteFuncionalidadeProduto cf : matriz) {
      if (!cf.isHabilitada()) continue;
      FuncionalidadeProduto fp = cf.getFuncionalidade();
      Map<String, Object> r = new LinkedHashMap<>();
      r.put("dominio", fp.getDominio().getCodigo());
      r.put("codigo", fp.getCodigo());
      r.put("codigoLegado", fp.getCodigoLegado());
      r.put("codigoOperacao", fp.getCodigoOperacao());
      r.put("critica", fp.isCritica());
      r.put("origem", cf.getOrigem().name());
      regras.add(r);
    }
    root.put("regras", regras);

    try {
      return json.writeValueAsBytes(root);
    } catch (Exception ex) {
      throw new BusinessException("Falha ao serializar regras: " + ex.getMessage());
    }
  }

  private String escape(String s) {
    if (s == null) return "";
    return s.replace("'", "''");
  }

  private static String sha256Hex(byte[] conteudo) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(conteudo));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 indisponível na JVM.", ex);
    }
  }
}
