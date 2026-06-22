package br.com.softon.portal.releaseorchestrator.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Configuração específica de módulo BANCO armazenada como JSON no campo
 * {@code ModuloProduto.configEspecifica}.
 *
 * Exemplo mono-dialeto:
 * <pre>{
 *   "caminhoRepo": "db/oracle",
 *   "prefixoDDL": "DDL_",
 *   "prefixoDML": "DML_"
 * }</pre>
 *
 * Exemplo multi-dialeto:
 * <pre>{
 *   "prefixoDDL": "DDL_",
 *   "prefixoDML": "DML_",
 *   "dialetos": [
 *     { "nome": "oracle",    "caminhoRepo": "db/oracle" },
 *     { "nome": "sqlserver", "caminhoRepo": "db/sqlserver" }
 *   ]
 * }</pre>
 *
 * Defaults quando campos não preenchidos:
 * <ul>
 *   <li>caminhoRepo: "" (raiz do repo)</li>
 *   <li>prefixoDDL: "DDL_"</li>
 *   <li>prefixoDML: "DML_"</li>
 *   <li>dialetos: lista vazia (single dialeto sem nome usando caminhoRepo)</li>
 * </ul>
 *
 * <p>{@link #dialetosResolvidos()} concilia o formato legado (apenas
 * {@code caminhoRepo}) com o novo (lista {@code dialetos}): se a lista
 * for vazia, devolve um dialeto único sem nome usando o {@code caminhoRepo}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigBancoModulo(
    String caminhoRepo,
    String prefixoDDL,
    String prefixoDML,
    List<Dialeto> dialetos) {

  public ConfigBancoModulo {
    if (caminhoRepo == null) caminhoRepo = "";
    if (prefixoDDL == null || prefixoDDL.isBlank()) prefixoDDL = "DDL_";
    if (prefixoDML == null || prefixoDML.isBlank()) prefixoDML = "DML_";
    if (dialetos == null) dialetos = List.of();
  }

  public static ConfigBancoModulo padrao() {
    return new ConfigBancoModulo("", "DDL_", "DML_", List.of());
  }

  public static ConfigBancoModulo de(String json, ObjectMapper mapper) {
    if (json == null || json.isBlank()) return padrao();
    try {
      return mapper.readValue(json, ConfigBancoModulo.class);
    } catch (JsonProcessingException e) {
      return padrao();
    }
  }

  /**
   * Devolve a lista efetiva de dialetos a processar. Sempre retorna ao menos
   * um — fallback para um dialeto único sem nome usando {@code caminhoRepo}
   * (compat com configs antigas mono-dialeto).
   */
  public List<Dialeto> dialetosResolvidos() {
    if (dialetos.isEmpty()) {
      return List.of(new Dialeto(null, caminhoRepo));
    }
    return dialetos;
  }

  public boolean ehDDL(String nomeArquivo) {
    return nomeArquivo.startsWith(prefixoDDL);
  }

  public boolean ehDML(String nomeArquivo) {
    return nomeArquivo.startsWith(prefixoDML);
  }

  /** Caminho do arquivo combina com o prefixo do dialeto? */
  public boolean dentroDoEscopo(String filename, Dialeto dialeto) {
    String caminho = dialeto.caminhoRepo();
    if (caminho == null || caminho.isEmpty()) return true;
    String prefixo = caminho.endsWith("/") ? caminho : caminho + "/";
    return filename.startsWith(prefixo);
  }

  /**
   * @param nome opcional. Quando presente, é usado como subpasta nos artefatos
   *             ({@code oracle/DDL.sql}, {@code sqlserver/DDL.sql}); quando
   *             null/vazio, os artefatos saem na raiz ({@code DDL.sql}).
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Dialeto(String nome, String caminhoRepo) {
    public Dialeto {
      if (caminhoRepo == null) caminhoRepo = "";
    }

    /** True se este dialeto deve produzir artefatos com prefixo de pasta. */
    public boolean ehNomeado() {
      return nome != null && !nome.isBlank();
    }
  }
}
