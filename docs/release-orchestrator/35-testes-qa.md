# 35 — Estratégia de Testes e QA

Plano de testes para o módulo Orchestrator. Como o módulo ainda não existe em código, esta spec direciona a implementação.

---

## 1. Princípios

- **Pirâmide de testes**: muito unit, médio integração, pouco E2E.
- **Cobertura mínima**: 70% em service layer; 50% global; críticos (geração de pacote, delta) 90%+.
- **Testes rápidos**: suite unit completa < 30s; integração < 5 min.
- **Sem flakiness**: testes não determinísticos (timing, random) são banidos.
- **Cada bug merece teste**: regressão garantida por teste novo.

---

## 2. Unit tests (JUnit 5 + Mockito + AssertJ)

### O que cobrir
- Services: regras de negócio (criação, validação, transições).
- Strategies: cálculo de delta por tipo de módulo.
- Validators: validação de schema de `configEspecifica`.
- Mappers/utilities.

### Padrão

```java
@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

    @Mock ClienteService clienteService;
    @Mock ReleaseOrchestratorFacade releaseOrchestratorFacade;
    @Mock DeltaCalculator deltaCalculator;
    @Mock PacoteGenerator pacoteGenerator;
    @Mock EntregaRepository entregaRepo;

    @InjectMocks EntregaService service;

    @BeforeEach
    void setup() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("test-user", null));
    }

    @Test
    @DisplayName("deve criar entrega a partir de próxima entrega aprovada")
    void deveCriarEntrega() {
        // arrange
        var prox = ProximaEntregaFactory.aprovada(clienteId, produtoId, releaseId);
        when(clienteService.buscar(any())).thenReturn(ClienteFactory.ativo());
        // ...

        // act
        var entrega = service.criar(prox.getId());

        // assert
        assertThat(entrega.getStatus()).isEqualTo(EntregaStatus.PENDENTE);
        verify(deltaCalculator).calcular(any(), any());
        verify(entregaRepo).save(any());
    }
}
```

### Factories
```java
public class ClienteFactory {
    public static Cliente ativo() {
        return new Cliente("Cliente Teste", "12.345.678/0001-99",
                          "CLI", StatusCliente.ATIVO, /* ... */);
    }
    public static Cliente pausado() { /* ... */ }
}
```

### Cobertura alvo por componente

| Componente | Cobertura |
|---|---|
| `EntregaService` (criar, gerar, reentregar) | 90% |
| `DeltaCalculator` + strategies | 95% |
| `PacoteGenerator` | 80% |
| Repositories (queries customizadas) | 50% (via integration) |
| Controllers | 0% (cobrir em integration) |
| DTOs / records | excluído |
| Entities (sem lógica) | excluído |

---

## 3. Testes de integração (`@SpringBootTest` + Testcontainers)

### Setup

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class EntregaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("test_orchestrator");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;

    @Test
    void deveCriarEntregaCompleta() throws Exception {
        // setup: cria cliente, produto, release com artefatos (via DB)
        // act: POST entrega
        // assert: status code, response shape, DB state, FS state
    }
}
```

### Cenários obrigatórios
1. **Fluxo end-to-end**: criar cliente → planejar entrega → gerar pacote → publicar.
2. **Reentrega**: gera novamente o pacote, verifica imutabilidade do original.
3. **Falha de download** durante geração: rollback, status ERRO.
4. **Cancelamento mid-flight**: thread pode interrompido, estado consistente.
5. **Próxima entrega no passado**: rejeita ou avisa.
6. **Cliente pausado**: bloqueia criação.
7. **Concorrência**: 2 entregas para mesmo cliente simultaneamente — comportamento esperado.

### Migrations
- Test runs Flyway automaticamente.
- Validar que migrações aplicam em base vazia E em base com dados (idempotência).

---

## 4. Testes de Controller (MockMvc)

### Foco
- Bean Validation (campos inválidos → 400).
- Status HTTP correto.
- Serialização JSON do response.
- Autorização (sem token → 401; sem role → 403).

```java
@Test
void deveRejeitarCnpjInvalido() throws Exception {
    var payload = "{\"nome\":\"X\",\"cnpj\":\"123\"}";  // CNPJ inválido
    mockMvc.perform(post("/api/v1/orchestrator/clientes")
                    .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .contentType(APPLICATION_JSON)
                    .content(payload))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("REQUEST_INVALIDO"))
           .andExpect(jsonPath("$.fields[?(@.field=='cnpj')]").exists());
}
```

---

## 5. Testes de delta (strategy por tipo)

### `DeltaBancoStrategyTest`
- Cenário: cliente está em v1.0, novo target é v1.5.
- Strategy deve retornar scripts DDL/DML existentes nas releases v1.1, v1.2, ..., v1.5 que não estavam em v1.0.
- Ordem alfabética, sem duplicação, sem scripts já aplicados.

### `DeltaKettleStrategyTest`
- Hash de cada job; só inclui se hash mudou entre v1.0 e v1.5.

### `DeltaWebStrategyTest`
- Sempre inclui o artefato da release alvo (não há delta de WAR — substitui inteiro).

### Edge cases
- Cliente nunca recebeu (versão atual = null) → delta = tudo.
- Target = versão atual → delta = vazio (deve avisar).
- Versão regredindo (target < atual) → bloquear ou avisar.

---

## 6. Testes de geração de pacote

### Foco
- Estrutura do ZIP final.
- SHA-256 e manifest corretos.
- Empacotamento paralelo não corrompe.
- Cancelamento limpo (sem arquivos parciais).

```java
@Test
void deveGerarPacoteComEstruturaCorreta() {
    var entrega = EntregaFactory.comModulosCompletos();

    Path pacote = pacoteGenerator.gerar(entrega);

    try (ZipFile zip = new ZipFile(pacote.toFile())) {
        assertThat(zip.entries()).hasSize(expectedSize);
        assertThat(zip.getEntry("manifest.json")).isNotNull();
        assertThat(zip.getEntry("checksums.sha256")).isNotNull();
        assertThat(zip.getEntry("release-notes.pdf")).isNotNull();
        assertThat(zip.getEntry("web/dtec-web-1.5.0.war")).isNotNull();
        assertThat(zip.getEntry("db/DDL_001.sql")).isNotNull();
        // ...
    }
}
```

---

## 7. Testes de publicação

### MVP (PASTA local)
- Verifica arquivo no path correto.
- Permissões.

### Pós-MVP (FTP/SFTP/Bucket)
- Mock server (`MockSftpServer`, `Localstack` para S3).
- Casos: sucesso, credencial inválida, host inacessível, espaço cheio.

---

## 8. Testes E2E (Cypress/Playwright)

### Stack sugerida
- Playwright (TypeScript, mesma stack do front).
- Roda contra ambiente staging com dados seed.

### Cenários
1. Cadastrar novo cliente.
2. Habilitar funcionalidades.
3. Cadastrar produto contratado e módulos.
4. Planejar próxima entrega.
5. Aprovar e gerar entrega.
6. Verificar polling de status.
7. Baixar pacote gerado.
8. Verificar manifest no ZIP baixado.

### Frequência
- Por PR no frontend.
- Diariamente em staging (cron).
- Antes de deploy prod.

---

## 9. Testes de carga (k6 ou Gatling)

### Cenários
- 100 entregas concorrentes geradas (release pequena).
- 10 entregas concorrentes (release grande, 500MB cada).
- Listagem com 100k registros + filtros.

### Métricas alvo
- p95 < 2s em endpoints CRUD.
- Throughput de geração: 5 entregas/min sustentado.
- Sem OOM, sem deadlock.

### Quando rodar
- Antes de release importante.
- Após mudança estrutural (DB, async pool, storage).

---

## 10. Testes de migração

### Validações
1. Migrations aplicam clean em DB vazio.
2. Migrations aplicam em DB com dados realistas (seed).
3. Rollback (manual via SQL — Flyway não suporta) testado em script `rollback/V?__schema.sql`.

```java
@Test
void migracaoAplicaEmBaseVazia() {
    var flyway = Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .load();
    var result = flyway.migrate();
    assertThat(result.success).isTrue();
}
```

---

## 11. Property-based testing (sugestão)

### Onde aplicar
- `VersaoSemver`: comparação, ordenação, incremento.
- `DeltaCalculator`: dadas N releases aleatórias, delta entre duas é subconjunto consistente.

### Stack
- `jqwik` (Java).

```java
@Property
void deltaContemSomenteScriptsEntreVersoes(@ForAll("releases") List<Release> releases) {
    // ...
}
```

---

## 12. Qualidade contínua

### Em cada PR
- Lint (`checkstyle` / `spotless`).
- Unit tests.
- Coverage report.
- Static analysis (`SonarQube` / `SpotBugs`).

### Em main
- Integration tests.
- Build Docker image.
- Deploy automático em staging.

### Diariamente
- E2E em staging.
- Cobertura tracked em dashboard.

### Pre-release
- Carga.
- Smoke tests em staging.

---

## 13. Mock vs real

### Mock (unit)
- Repositories, services externos, time (Clock).

### Real (integration)
- DB via Testcontainers.
- Filesystem temporário (`@TempDir`).
- HTTP server mock (`WireMock`) para GitHub/Jenkins.

### Real (E2E)
- DB persistente em staging.
- Storage real (não temp).
- Sem mocks.

---

## 14. Ferramentas e versões sugeridas

| Ferramenta | Uso |
|---|---|
| JUnit 5 | Test runner |
| Mockito | Mocking |
| AssertJ | Assertions fluentes |
| Testcontainers | DB + serviços externos |
| MockMvc | Controller tests |
| Awaitility | Esperar condições assíncronas |
| WireMock | HTTP mock |
| RestAssured | (alternativa MockMvc com sintaxe BDD) |
| jqwik | Property-based |
| Playwright | E2E |
| k6 | Carga |

---

## 15. Naming

```java
@Test
@DisplayName("deve gerar entrega quando próxima entrega está aprovada")
void deveGerarEntrega() { ... }

@Test
@DisplayName("deve rejeitar geração quando cliente está pausado")
void deveRejeitarGeracaoClientePausado() { ... }
```

Padrão: `deve{açao}{contexto}` em camelCase + `@DisplayName` descritivo em português.

---

## 16. Cross-reference

- Estratégia de testes do backend — consolidada neste documento.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Seção E.
