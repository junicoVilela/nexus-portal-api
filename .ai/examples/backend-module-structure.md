# Exemplo — Estrutura de Módulo Backend

Estrutura padrão de um módulo de negócio do Nexus Portal API. Cada módulo vive em seu próprio submódulo Maven (`doc-flow/`, `release-orchestrator/`, etc.) e seu próprio sub-pacote direto sob `com.nexus.portal`. **Nunca usar `modules` no caminho do pacote.**

```text
doc-flow/src/main/java/br/com/nexus/portal/docflow/
├── controller/
│   └── ManualController.java
├── service/
│   └── ManualService.java
├── repository/
│   └── ManualRepository.java
├── entity/
│   ├── Manual.java
│   ├── ManualCapitulo.java
│   └── ManualSecao.java
├── dto/
│   ├── request/
│   │   ├── CriarManualRequest.java
│   │   └── AtualizarManualRequest.java
│   └── response/
│       └── ManualResponse.java
├── mapper/                          (opcional — adicionar quando necessário)
│   └── ManualMapper.java
└── exception/                       (opcional — adicionar quando necessário)
    └── ManualNaoEncontradoException.java
```

## Observações

- Não usar `service/impl/` — Service é classe concreta direta (não interface + impl).
- `mapper/` e `exception/` só quando realmente necessários.
- DTOs preferencialmente como `record` com Bean Validation nos requests.
- Não expor Entity diretamente na API.
- Endpoints sob `/api/v1/{recurso}`.
- Tabelas com prefixo `tb_{recurso}`.
