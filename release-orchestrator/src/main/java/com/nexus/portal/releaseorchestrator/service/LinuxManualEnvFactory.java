package com.nexus.portal.releaseorchestrator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.releaseorchestrator.entity.ConfiguracaoInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.PapelPorta;
import com.nexus.portal.releaseorchestrator.entity.ReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.StatusReservaPorta;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import com.nexus.portal.shared.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gera o {@code .env} da pasta instalador Softon V5 (Oracle ou SQL Server) a
 * partir da configuração da instalação. Senhas não vão para o log.
 */
@Component
@RequiredArgsConstructor
public class LinuxManualEnvFactory {

  static final String CATALINA_REL = "./runtime/apache-tomcat-9.0.98";

  private final EncryptionService encryptionService;
  private final ObjectMapper objectMapper;

  public PacoteConfig gerar(InstalacaoCliente inst) {
    ConfiguracaoInstalacao cfg = inst.getConfiguracao();
    if (cfg == null || cfg.getTipoBanco() == null) {
      throw new BusinessException("Instalação Linux manual precisa do tipo de banco (Oracle ou SQL Server).");
    }
    TipoBanco banco = cfg.getTipoBanco();
    if (banco != TipoBanco.ORACLE && banco != TipoBanco.SQLSERVER) {
      throw new BusinessException("Linux manual só configura Oracle ou SQL Server (não Postgres).");
    }
    if (blank(cfg.getBancoHost()) || blank(cfg.getBancoNome()) || blank(cfg.getBancoUsuario())) {
      throw new BusinessException("Informe host, nome e usuário do banco na configuração da instalação.");
    }
    int appPort = porta(inst, PapelPorta.FRONTEND, 4209);
    int legacyPort = porta(inst, PapelPorta.BACKEND, 8010);
    int dbPort = cfg.getBancoPorta() != null
        ? cfg.getBancoPorta()
        : (banco == TipoBanco.ORACLE ? 1521 : 1433);
    String senha = senhaBanco(cfg.getBancoCredencialRef());
    String vendor = banco == TipoBanco.ORACLE ? "oracle" : "sqlserver";
    String jdbc = jdbcUrl(banco, cfg.getBancoHost(), dbPort, cfg.getBancoNome());
    Host host = inst.getHost();
    String publicHost = host.getHostname();
    String urlLegacy = urlPublica(cfg.getUrlBackend(), publicHost, legacyPort);
    String urlApp = urlPublica(cfg.getUrlFrontend(), publicHost, appPort);

    Map<String, String> env = new LinkedHashMap<>();
    env.put("CATALINA_HOME", CATALINA_REL);
    env.put("JAVA8_HOME", "./jdk/temurin-8");
    env.put("JAVA17_HOME", "./jdk/temurin-17");
    env.put("APP_HTTP_PORT", String.valueOf(appPort));
    env.put("LEGACY_HTTP_PORT", String.valueOf(legacyPort));
    env.put("TZ", "America/Sao_Paulo");
    env.put("DB_VENDOR", vendor);
    env.put("SPRING_PROFILES_ACTIVE", "prod," + vendor);
    env.put("JAVA_OPTS", "\"-Xms512m -Xmx1024m\"");
    env.put("DATABASE_URL", jdbc);
    env.put("DATABASE_USERNAME", cfg.getBancoUsuario());
    env.put("DATABASE_PASSWORD", senha == null ? "" : senha);
    env.put("LEGACY_DB_HOST", cfg.getBancoHost());
    env.put("LEGACY_DB_PORT", String.valueOf(dbPort));
    env.put("LEGACY_DB_NAME", cfg.getBancoNome());
    env.put("LEGACY_DB_USER", cfg.getBancoUsuario());
    env.put("LEGACY_DB_PASSWORD", senha == null ? "" : senha);
    env.put("DTEC_BASE_URL", urlLegacy);
    env.put("DTEC_LEGACY_PUBLIC_URL", urlLegacy);
    env.put("DTEC_APP_BASE_URL", urlApp);
    env.put("LEGACY_BACKEND_WAR", "ldv4.war");
    env.put("LEGACY_FRONTEND_WAR", "ldv4-frontend.war");
    env.put("HEALTH_CHECK_TIMEOUT", "120");
    env.put("HEALTH_CHECK_INTERVAL", "5");
    mesclarParametros(env, cfg.getParametros());

    String schema = banco == TipoBanco.SQLSERVER ? "dbo" : cfg.getBancoUsuario();
    String contextXml = contextXml(banco, cfg.getBancoHost(), dbPort, cfg.getBancoNome(),
        cfg.getBancoUsuario(), senha == null ? "" : senha);
    String appProperties = appProperties(schema, vendor, legacyPort);
    return new PacoteConfig(renderEnv(env), contextXml, appProperties, CATALINA_REL, vendor);
  }

  static String jdbcUrl(TipoBanco banco, String host, int porta, String nome) {
    if (banco == TipoBanco.ORACLE) {
      return "jdbc:oracle:thin:@//" + host + ":" + porta + "/" + nome;
    }
    return "jdbc:sqlserver://" + host + ":" + porta
        + ";databaseName=" + nome + ";encrypt=false;trustServerCertificate=true";
  }

  private String senhaBanco(String credencialRef) {
    if (credencialRef == null || credencialRef.isBlank()) {
      return "";
    }
    try {
      String dec = encryptionService.decifrar(credencialRef.trim());
      return dec != null ? dec : credencialRef.trim();
    } catch (RuntimeException ignored) {
      return credencialRef.trim();
    }
  }

  private void mesclarParametros(Map<String, String> env, String parametros) {
    if (parametros == null || parametros.isBlank()) {
      return;
    }
    String raw = parametros.trim();
    if (raw.startsWith("{")) {
      try {
        Map<String, String> extra = objectMapper.readValue(raw, new TypeReference<>() {});
        extra.forEach((k, v) -> {
          if (k != null && !k.isBlank() && v != null) {
            env.put(k.trim(), v);
          }
        });
        return;
      } catch (Exception ignored) {
        // cai no parser linha a linha
      }
    }
    for (String line : raw.split("\n")) {
      String t = line.trim();
      if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) {
        continue;
      }
      int eq = t.indexOf('=');
      env.put(t.substring(0, eq).trim(), t.substring(eq + 1).trim());
    }
  }

  static String renderEnv(Map<String, String> env) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Gerado pelo Nexus Portal — não editar senhas em texto se puder usar credencialRef\n");
    env.forEach((k, v) -> sb.append(k).append('=').append(v == null ? "" : v).append('\n'));
    return sb.toString();
  }

  /** Template com placeholders do {@code install.sh} (Oracle ausente na cópia SQL Server). */
  static String contextXmlTemplate(TipoBanco banco) {
    return contextXml(banco, "__LEGACY_DB_HOST__", -1, "__LEGACY_DB_NAME__",
        "__LEGACY_DB_USER__", "__LEGACY_DB_PASSWORD__");
  }

  static String contextXml(TipoBanco banco, String host, int porta, String nome, String user, String senha) {
    String driver;
    String jdbc;
    if (banco == TipoBanco.ORACLE) {
      driver = "oracle.jdbc.OracleDriver";
      jdbc = porta < 0
          ? "jdbc:oracle:thin:@//" + host + ":__LEGACY_DB_PORT__/" + nome
          : "jdbc:oracle:thin:@//" + host + ":" + porta + "/" + nome;
    } else {
      driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
      jdbc = porta < 0
          ? "jdbc:sqlserver://" + host + ":__LEGACY_DB_PORT__;databaseName=" + nome
              + ";encrypt=false;trustServerCertificate=true"
          : "jdbc:sqlserver://" + host + ":" + porta
              + ";databaseName=" + nome + ";encrypt=false;trustServerCertificate=true";
    }
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <Context>
            <WatchedResource>WEB-INF/web.xml</WatchedResource>
            <Resource
                    name="jdbc/ds"
                    auth="Container"
                    driverClass="%s"
                    jdbcUrl="%s"
                    user="%s"
                    password="%s"
                    factory="org.apache.naming.factory.BeanFactory"
                    type="com.mchange.v2.c3p0.ComboPooledDataSource"
                    maxPoolSize="4"
                    minPoolSize="2"
                    acquireIncrement="2"
                    breakAfterAcquireFailure="false"
                    maxConnectionAge="60"
                    maxIdleTime="30"
                    maxIdleTimeExcessConnections="10"
                    idleConnectionTestPeriod="15"
                    testConnectionOnCheckout="true"
                    preferredTestQuery="SELECT 1"
            />
        </Context>
        """.formatted(driver, xml(jdbc), xml(user), xml(senha));
  }

  static String appProperties(String schema, String vendor, int legacyPort) {
    return """
        ldap.check=false
        default-schema =%s
        database=%s
        ocorrencia.tipoDocumento=3
        ocorrencia.cdCpfcnpjComumic=
        nome-arquivo-logo=
        queue-check.interval=3600
        cron.disable=true
        report-location=./runtime/apache-tomcat-9.0.98/relatorios/
        rest-base.url=http://localhost:%d/ldv4/rest/
        help-base.url=http://localhost:%d/help-ld/
        session.expiration.minutes=1000
        """.formatted(schema, vendor, legacyPort, legacyPort);
  }

  private static int porta(InstalacaoCliente inst, PapelPorta papel, int padrao) {
    if (inst.getPortas() == null) {
      return padrao;
    }
    return inst.getPortas().stream()
        .filter(p -> p.getPapel() == papel)
        .filter(p -> p.getStatus() == StatusReservaPorta.RESERVADA
            || p.getStatus() == StatusReservaPorta.EM_USO)
        .map(ReservaPorta::getPorta)
        .findFirst()
        .orElse(padrao);
  }

  private static String xml(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  /**
   * URL pública no .env: sempre {@code http(s)://host:porta}. Aceita override
   * só com hostname ({@code localhost}) ou só com a porta ({@code 4209}).
   */
  static String urlPublica(String informada, String host, int porta) {
    String h = hostParaUrl(host);
    if (blank(informada)) {
      return "http://" + h + ":" + porta;
    }
    String t = informada.trim();
    if (t.matches("\\d{2,5}")) {
      return "http://" + h + ":" + Integer.parseInt(t);
    }
    if (!t.contains("://")) {
      t = "http://" + t;
    }
    return garantirPorta(t, porta);
  }

  static String garantirPorta(String url, int porta) {
    try {
      java.net.URI u = java.net.URI.create(url);
      if (u.getHost() == null) {
        return url;
      }
      if (u.getPort() > 0) {
        return url;
      }
      String scheme = u.getScheme() == null ? "http" : u.getScheme();
      String path = u.getRawPath() == null || "/".equals(u.getRawPath()) ? "" : u.getRawPath();
      if (path == null) {
        path = "";
      }
      String query = u.getRawQuery() == null ? "" : "?" + u.getRawQuery();
      return scheme + "://" + hostParaUrl(u.getHost()) + ":" + porta + path + query;
    } catch (IllegalArgumentException e) {
      return "http://" + hostParaUrl(url) + ":" + porta;
    }
  }

  static String hostParaUrl(String host) {
    String h = blank(host) ? "localhost" : host.trim();
    if ("::1".equals(h) || (h.contains(":") && !h.startsWith("["))) {
      return "[" + h.replace("[", "").replace("]", "") + "]";
    }
    return h;
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  public record PacoteConfig(
      String envFile,
      String contextXml,
      String appProperties,
      String catalinaRel,
      String vendor) {}
}
