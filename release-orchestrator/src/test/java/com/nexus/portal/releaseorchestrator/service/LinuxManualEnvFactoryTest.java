package com.nexus.portal.releaseorchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.portal.releaseorchestrator.entity.AmbientePadrao;
import com.nexus.portal.releaseorchestrator.entity.Cliente;
import com.nexus.portal.releaseorchestrator.entity.ConfiguracaoInstalacao;
import com.nexus.portal.releaseorchestrator.entity.Host;
import com.nexus.portal.releaseorchestrator.entity.InstalacaoCliente;
import com.nexus.portal.releaseorchestrator.entity.ProdutoRh;
import com.nexus.portal.releaseorchestrator.entity.SistemaOperacionalHost;
import com.nexus.portal.releaseorchestrator.entity.TipoBanco;
import com.nexus.portal.releaseorchestrator.entity.TipoConexaoHost;
import com.nexus.portal.releaseorchestrator.entity.TipoImplantacao;
import com.nexus.portal.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinuxManualEnvFactoryTest {

  @Mock EncryptionService encryptionService;
  LinuxManualEnvFactory factory;
  InstalacaoCliente instalacao;
  ConfiguracaoInstalacao cfg;

  @BeforeEach
  void setUp() {
    factory = new LinuxManualEnvFactory(encryptionService, new ObjectMapper());
    Cliente cliente = new Cliente("BMW", "BMW", AmbientePadrao.HOM);
    Host host = new Host("SRV", "Srv", "cli-01", SistemaOperacionalHost.LINUX, TipoConexaoHost.SSH);
    ProdutoRh produto = new ProdutoRh("LD", "LD", null, "#fff", null, true);
    instalacao = new InstalacaoCliente("BMW-LD-01", "LD BMW", cliente, host, produto,
        TipoImplantacao.LINUX_MANUAL, AmbientePadrao.HOM);
    instalacao.setDiretorioInstalacao("/opt/dtec/bmw");
    cfg = new ConfiguracaoInstalacao(instalacao);
    instalacao.setConfiguracao(cfg);
  }

  @Test
  void gerar_sqlserverMontaJdbcEPerfil() {
    cfg.atualizar(TipoBanco.SQLSERVER, "10.10.10.160", 1433, "BMW_HML", "softon", "segredo",
        "http://cli-01:8010", "http://cli-01:4209", null);

    LinuxManualEnvFactory.PacoteConfig p = factory.gerar(instalacao);

    assertThat(p.vendor()).isEqualTo("sqlserver");
    assertThat(p.envFile()).contains("DB_VENDOR=sqlserver");
    assertThat(p.envFile()).contains("SPRING_PROFILES_ACTIVE=prod,sqlserver");
    assertThat(p.envFile()).contains("jdbc:sqlserver://10.10.10.160:1433;databaseName=BMW_HML");
    assertThat(p.envFile()).contains("LEGACY_HTTP_PORT=8010");
    assertThat(p.envFile()).contains("APP_HTTP_PORT=4209");
    assertThat(p.contextXml()).contains("SQLServerDriver");
    assertThat(p.appProperties()).contains("database=sqlserver");
  }

  @Test
  void gerar_oracleUsaServiceName() {
    cfg.atualizar(TipoBanco.ORACLE, "ora.local", 1521, "XEPDB1", "ld", "pw", null, null, null);

    LinuxManualEnvFactory.PacoteConfig p = factory.gerar(instalacao);

    assertThat(p.vendor()).isEqualTo("oracle");
    assertThat(p.envFile()).contains("jdbc:oracle:thin:@//ora.local:1521/XEPDB1");
    assertThat(p.contextXml()).contains("oracle.jdbc.OracleDriver");
    assertThat(p.appProperties()).contains("database=oracle");
  }

  @Test
  void gerar_normalizaDtecUrlQuandoSoHostname() {
    cfg.atualizar(TipoBanco.SQLSERVER, "10.10.10.160", 1433, "BMW_HML", "softon", "segredo",
        "localhost", "localhost", null);

    LinuxManualEnvFactory.PacoteConfig p = factory.gerar(instalacao);

    assertThat(p.envFile()).contains("DTEC_BASE_URL=http://localhost:8010");
    assertThat(p.envFile()).contains("DTEC_LEGACY_PUBLIC_URL=http://localhost:8010");
    assertThat(p.envFile()).contains("DTEC_APP_BASE_URL=http://localhost:4209");
  }

  @Test
  void urlPublica_completaHostnameEPorta() {
    assertThat(LinuxManualEnvFactory.urlPublica(null, "localhost", 8010))
        .isEqualTo("http://localhost:8010");
    assertThat(LinuxManualEnvFactory.urlPublica("localhost", "cli-01", 8010))
        .isEqualTo("http://localhost:8010");
    assertThat(LinuxManualEnvFactory.urlPublica("http://localhost", "cli-01", 8010))
        .isEqualTo("http://localhost:8010");
    assertThat(LinuxManualEnvFactory.urlPublica("http://acme.local:8080", "cli-01", 8010))
        .isEqualTo("http://acme.local:8080");
    assertThat(LinuxManualEnvFactory.urlPublica("4209", "localhost", 4209))
        .isEqualTo("http://localhost:4209");
  }

  @Test
  void contextXmlTemplate_oracleUsaPlaceholdersDoInstallSh() {
    String xml = LinuxManualEnvFactory.contextXmlTemplate(TipoBanco.ORACLE);
    assertThat(xml).contains("oracle.jdbc.OracleDriver");
    assertThat(xml).contains("__LEGACY_DB_HOST__");
    assertThat(xml).contains("__LEGACY_DB_PORT__");
    assertThat(xml).contains("jdbc:oracle:thin:@//__LEGACY_DB_HOST__:__LEGACY_DB_PORT__/__LEGACY_DB_NAME__");
  }

  @Test
  void gerar_rejeitaPostgres() {
    cfg.atualizar(TipoBanco.POSTGRES, "pg", 5432, "db", "u", "p", null, null, null);
    assertThatThrownBy(() -> factory.gerar(instalacao))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Oracle ou SQL Server");
  }
}
