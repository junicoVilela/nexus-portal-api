package br.com.softon.portal.releaseorchestrator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuração específica de módulo BANCO armazenada como JSON no campo
 * {@code ModuloProduto.configEspecifica}.
 *
 * Exemplo:
 * <pre>{
 *   "caminhoRepo": "db/oracle",
 *   "prefixoDDL": "DDL_",
 *   "prefixoDML": "DML_"
 * }</pre>
 *
 * Defaults quando campos não preenchidos:
 * <ul>
 *   <li>caminhoRepo: "" (raiz do repo)</li>
 *   <li>prefixoDDL: "DDL_"</li>
 *   <li>prefixoDML: "DML_"</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigBancoModulo(
    String caminhoRepo,
    String prefixoDDL,
    String prefixoDML) {

  public ConfigBancoModulo {
    if (caminhoRepo == null) caminhoRepo = "";
    if (prefixoDDL == null || prefixoDDL.isBlank()) prefixoDDL = "DDL_";
    if (prefixoDML == null || prefixoDML.isBlank()) prefixoDML = "DML_";
  }

  public static ConfigBancoModulo padrao() {
    return new ConfigBancoModulo("", "DDL_", "DML_");
  }

  /** Lê o JSON do campo configEspecifica; falha silenciosa cai no default. */
  public static ConfigBancoModulo de(String json, ObjectMapper mapper) {
    if (json == null || json.isBlank()) return padrao();
    try {
      return mapper.readValue(json, ConfigBancoModulo.class);
    } catch (JsonProcessingException e) {
      return padrao();
    }
  }

  /** Caminho do arquivo combina com o módulo? Compara com prefix path/. */
  public boolean dentroDoEscopo(String filename) {
    if (caminhoRepo.isEmpty()) return true;
    String prefixo = caminhoRepo.endsWith("/") ? caminhoRepo : caminhoRepo + "/";
    return filename.startsWith(prefixo);
  }

  public boolean ehDDL(String nomeArquivo) {
    return nomeArquivo.startsWith(prefixoDDL);
  }

  public boolean ehDML(String nomeArquivo) {
    return nomeArquivo.startsWith(prefixoDML);
  }
}
