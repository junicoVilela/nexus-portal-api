# 20 — Range Manual / Cálculo de Delta

> **MVP**: esta tela é **pós-MVP**. No MVP não há cálculo de delta via Git — o operador é responsável por anexar na release apenas os artefatos que devem ser entregues (ver `14-release-orchestrator-detalhe.md`). A pré-visualização do conteúdo, no MVP, apenas lista os artefatos já uploadados, sem comparação entre versões nem ajuste de FROM/TO.

## 1. Papel da tela

**Detalhar e ajustar FROM/TO por módulo** e **revisar o conteúdo do delta** antes da geração do pacote.

Acessível ao clicar "Manual" no passo de seleção de módulos (`19`), ou em `/orchestrator/entregas/:id/delta`.

---

## 2. Fonte da verdade

- **Tags** vêm do **GitHub** (release tags do repositório configurado no produto — ver `09`).
- **Versão atual no cliente** vem do **histórico de entregas** (`23`).
- **Conteúdo do delta** vem da **comparação entre commits** das duas tags.

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Range Manual / Cálculo de Delta                                        │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ dtec-db-ddl (BANCO) ───────────────────────────────────────────┐  │
│ │ FROM_TAG: [v1.4.0 ▼]     TO_TAG: [v1.5.0 ▼]                      │  │
│ │ Origem: última entrega ao cliente em 15/04/2026 — tag v1.4.0    │  │
│ │ Justificativa (se alterou): [_______________________________]   │  │
│ │                                                                  │  │
│ │ Conteúdo previsto:                                               │  │
│ │   • DDL_001_users.sql (novo)                                     │  │
│ │   • DDL_002_grupos.sql (novo)                                    │  │
│ │   • DDL_010_index_relatorio.sql (modificado)                     │  │
│ │   • DML_001_grupos_iniciais.sql (novo)                           │  │
│ │   Total: 4 arquivos                                              │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ dtec-web (WEB) ────────────────────────────────────────────────┐  │
│ │ FROM_TAG: [v1.4.0 ▼]     TO_TAG: [v1.5.0 ▼]                      │  │
│ │ Conteúdo: asset dtec-web-1.5.0.war (47MB estimado)               │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ⚠️  Avisos:                                                            │
│ • Range "pula" v1.4.1 e v1.4.2 publicadas. Confirmar?                  │
│                                                                        │
│              [Voltar]    [Recalcular]    [Confirmar →]                 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Campos por módulo

| Campo | Descrição |
|---|---|
| **FROM_TAG** | Tag de origem. Default = última enviada ao cliente. |
| **TO_TAG** | Tag de destino. Default = tag da release-alvo. |
| **Origem calculada** | Texto explicativo do default ("última entrega ao cliente X em DD/MM/AAAA — tag vY"). |
| **Justificativa** | Obrigatória se FROM/TO foram alterados manualmente. |
| **Conteúdo previsto** | Pré-visualização do que entra no pacote (lista de arquivos). |

---

## 5. Cálculo por tipo de módulo

### 5.1 WEB / BATCH
- **Não há delta de conteúdo**.
- Sistema baixa o **asset completo** da `TO_TAG`:
  - WEB → `.war`.
  - BATCH → `.jar`.
- `FROM_TAG` é apenas registro histórico (não afeta o pacote).
- Preview: nome do asset + tamanho estimado.

### 5.2 BANCO

#### Coleta
- Coleta arquivos `.sql` adicionados ou modificados no diretório configurado (ver `10` §config específica) entre os commits de `FROM_TAG` e `TO_TAG`.

#### Classificação
- Separa por prefixo configurado:
  - `DDL_*.sql` → bloco DDL (estrutura).
  - `DML_*.sql` → bloco DML (dados).

#### Ordenação
- Dentro de cada bloco, ordena pela convenção configurada:
  - `alfabetica`: ordem do nome do arquivo.
  - `numerica`: extrai número do nome.
  - `data-commit`: data do commit que adicionou/modificou.

#### Geração
- Dois arquivos unificados: `DDL.sql` e `DML.sql`.
- Cada arquivo concatena os scripts em ordem.

#### Multi-dialeto
- Se módulo configurado com `dialeto: ambos` (oracle + sqlserver):
  - Gera subdiretórios `banco/oracle/` e `banco/sqlserver/`.
  - Coleta scripts em diretórios distintos.

### 5.3 KETTLE
- Coleta arquivos `.ktr` e `.kjb` adicionados ou modificados entre `FROM_TAG` e `TO_TAG` no diretório configurado.
- Se `incluirDependencias = true`: resolve referências entre transformações/jobs e inclui dependentes.

### 5.4 FUNCIONALIDADES / REGRAS
- **Não usa FROM/TO de tag**.
- Lê configuração atual do cliente (ver `05-cliente-dominios-funcionalidades.md`) e gera scripts no momento da geração do pacote.
- Sempre considerado "full" (substitui o estado anterior).

---

## 6. Pré-visualização do conteúdo

Antes de avançar para a geração (`21`), a tela exibe:

| Tipo | Pré-visualização |
|---|---|
| WEB/BATCH | Nome do asset que será baixado + tamanho estimado |
| BANCO | Lista de scripts SQL que entrarão (ordenados), totais por bloco DDL/DML, por dialeto |
| KETTLE | Lista de transformações afetadas |
| FUNCIONALIDADES/REGRAS | Lista de funcionalidades habilitadas que vão virar scripts |

### Estatísticas no resumo
- Total de arquivos por módulo.
- Tamanho estimado por módulo.
- Tamanho total estimado do pacote.

---

## 7. Regras

### 7.1 Range manual exige justificativa
- Texto livre obrigatório.
- Vai para audit log.

### 7.2 Avisos visuais
- **Pula versões**: ex.: FROM = v1.0, TO = v3.0, e existem v2.0 e v2.1 publicadas entre elas. Aviso amarelo.
- **Rollback**: FROM > TO cronologicamente. Bloqueio + confirmação explícita marcada como "rollback".
- **Tag inexistente**: TO_TAG não encontrada no GitHub. Erro vermelho.

### 7.3 Persistência da pré-visualização
- Sistema persiste a pré-visualização junto com a entrega.
- Permite auditar "o que era esperado" vs "o que foi gerado".

### 7.4 Tag deletada
- Se a TO_TAG for deletada do GitHub após pré-visualização mas antes da geração: falha com mensagem clara.

### 7.5 Cache do delta
- Cache por (FROM, TO, módulo) com TTL 30 min.
- Botão "Recalcular" força refresh.

---

## 8. Contratos de API

### Calcular delta

```
POST /api/v1/orchestrator/entregas/{id}/delta/calcular
```

```json
{
  "modulos": [
    {
      "moduloProdutoId": "uuid",
      "fromTag": "v1.4.0",
      "toTag": "v1.5.0"
    }
  ]
}
```

Response:
```json
{
  "modulos": [
    {
      "moduloProdutoId": "uuid",
      "codigo": "dtec-db-ddl",
      "tipo": "BANCO",
      "fromTag": "v1.4.0",
      "toTag": "v1.5.0",
      "conteudo": {
        "arquivos": [
          { "nome": "DDL_001_users.sql", "tipo": "NOVO", "tamanhoBytes": 1234 },
          { "nome": "DDL_002_grupos.sql", "tipo": "NOVO", "tamanhoBytes": 567 },
          { "nome": "DDL_010_index_relatorio.sql", "tipo": "MODIFICADO", "tamanhoBytes": 234 },
          { "nome": "DML_001_grupos_iniciais.sql", "tipo": "NOVO", "tamanhoBytes": 5678 }
        ],
        "totalArquivos": 4,
        "tamanhoTotalBytes": 7713
      },
      "avisos": [
        { "tipo": "PULA_VERSAO", "mensagem": "Range pula v1.4.1 e v1.4.2." }
      ]
    }
  ],
  "tamanhoTotalEstimado": 49283746
}
```

### Salvar customização manual

```
PUT /api/v1/orchestrator/entregas/{id}/delta
```

```json
{
  "modulos": [
    {
      "moduloProdutoId": "uuid",
      "fromTag": "v1.3.5",
      "toTag": "v1.5.0",
      "justificativa": "Cliente está em 1.3.5 segundo confirmação manual."
    }
  ]
}
```

---

## 9. DTOs

```java
public record DeltaResponse(
    List<ModuloDelta> modulos,
    long tamanhoTotalEstimado
) {
    public record ModuloDelta(
        UUID moduloProdutoId, String codigo, TipoModulo tipo,
        String fromTag, String toTag,
        Conteudo conteudo,
        List<AvisoDelta> avisos
    ) {}

    public record Conteudo(
        List<Arquivo> arquivos, int totalArquivos, long tamanhoTotalBytes
    ) {}

    public record Arquivo(String nome, TipoArquivo tipo, long tamanhoBytes) {}
    public record AvisoDelta(TipoAvisoDelta tipo, String mensagem) {}
}

public enum TipoArquivo { NOVO, MODIFICADO, REMOVIDO }
public enum TipoAvisoDelta { PULA_VERSAO, ROLLBACK, TAG_INEXISTENTE, DELTA_VAZIO }
```

---

## 10. Performance

### Cálculo de delta
- Pode demorar (Git diff entre tags, listagem de arquivos).
- Cache 30 min.
- Estimativa rápida (count) primeiro, detalhe sob demanda.

### Repositório grande
- Limite de profundidade de busca (configurável).
- Timeout: 30s. Se excede, falha com sugestão de range menor.

---

## 11. Segurança

- Sem expor credencial Git em response.
- Validar `from/to` para evitar injection.
- Logs sem incluir token.

---

## 12. Estados e edge cases

### Delta vazio
- "Nenhum arquivo modificado entre as tags."
- Operador pode desmarcar módulo ou prosseguir vazio.

### Tag não existe
- "Tag v1.5.0 não encontrada no repositório."
- Atualizar lista de tags do GitHub.

### Falha de comunicação GitHub
- Banner: "GitHub temporariamente indisponível. Tente novamente."
- Modo fallback: usar versões anteriores ou cache.

### Rollback intencional
- Toggle "Confirmar como rollback".
- Reverso lógico: scripts de "down" gerados.

---

## 13. Acessibilidade

- Formulário com labels.
- Lista de arquivos com `<ul>` semântico.
- Avisos com `aria-live`.

---

## 14. Auditoria

| Ação | Detalhes |
|---|---|
| `DELTA_CALCULADO` | módulos, totais |
| `DELTA_MANUAL_AJUSTADO` | de → para + justificativa |
| `DELTA_RECALCULADO` | quem, quando |

---

## 15. Cross-reference

- [`19-selecao-modulos.md`](19-selecao-modulos.md) — Origem.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Próximo passo.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Config dos módulos.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Origem da "versão atual cliente".
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack de geração.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
