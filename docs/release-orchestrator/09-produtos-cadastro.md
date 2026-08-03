# 09 — Produtos — Cadastro

> **MVP**: as seções **Integração GitHub** e **Integração Jenkins** abaixo são **pós-MVP**. No MVP, o produto é cadastrado apenas com identidade e vínculos a módulos/templates. Artefatos chegam via upload manual na release (ver `14-release-orchestrator-detalhe.md` e `10-produtos-modulos-artefatos.md`).

## 1. Papel da tela

Cadastrar e configurar um produto: identidade visual, integrações (GitHub + Jenkins) e regras gerais.

Acessível em:
- `/orchestrator/produtos/novo` (criação).
- `/orchestrator/produtos/:id/editar` (edição).

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Registrar identidade do produto | Seção "Identidade" |
| Configurar repositório GitHub | Seção "Integração GitHub" (pós-MVP) |
| Configurar pipeline Jenkins | Seção "Integração Jenkins" (pós-MVP) |
| Acessar catálogo funcional | Botão "Catálogo funcional" após salvar → `11` |
| Definir status e visibilidade | Status ativo/inativo |
| Vincular templates de documento | Seção "Templates de documento" |
| Acessar configuração de módulos | Botão "Configurar módulos" após salvar |

### 2.1 Relação com o repositório de produto

O cadastro no portal **referencia** o repositório GitHub; o **código de build** (Jenkinsfile, scripts SQL, jobs Kettle) vive **no repo do produto**, não no portal.

| Campo no portal (pós-MVP) | No repositório |
|---|---|
| `repositorioGithub` | URL `owner/repo` onde tags e releases são publicadas |
| `branchPadrao` | Branch de desenvolvimento (ex.: `main`) |
| `padraoTag` | Regex das tags válidas (ex.: `^v\d+\.\d+\.\d+$`) |
| `jenkinsUrl` + `jenkinsJob` | Job que executa o `Jenkinsfile` do repo |

**Documentação externa ao portal** (obrigatória na Fase 2):

- [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) — Jenkinsfile, assets por tipo de módulo, checklist ~2 dias/entregável.
- [`40-guia-versao-tag.md`](40-guia-versao-tag.md) — procedimento de tag, range `FROM..TO`, troubleshooting.

No **MVP**, integrações GitHub/Jenkins ficam ocultas; artefatos entram por upload na release (`14`).

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar           Novo Produto                          [Salvar]      │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Identidade ────────────────────────────────────────────────────┐  │
│ │ Nome*               Sigla*           Tipo*                       │  │
│ │ [_______________]   [______]         [Sistema ▼]                 │  │
│ │                                                                   │  │
│ │ Descrição                                                        │  │
│ │ [_____________________________________________________________]  │  │
│ │                                                                   │  │
│ │ Cor visual*       Status*         Responsável                    │  │
│ │ 🎨 [#2563eb]      [Ativo ▼]       [Select ▼]                     │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Integração GitHub (Pós-MVP) ───────────────────────────────────┐  │
│ │ URL do repositório*                                              │  │
│ │ [https://github.com/nexus/nexus-ld                  ]            │  │
│ │ Branch padrão           Padrão de tag                            │  │
│ │ [main]                  [^v\d+\.\d+\.\d+$]                       │  │
│ │ Credencial: [Token GitHub: ghp_•••••••••••• Alterar]             │  │
│ │ [Testar conexão]                                                 │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Integração Jenkins (Pós-MVP) ──────────────────────────────────┐  │
│ │ URL do Jenkins                                                   │  │
│ │ [https://jenkins.nexus.internal                    ]            │  │
│ │ Nome do job                                                      │  │
│ │ [nexus-ld-build]                                                  │  │
│ │ Trigger:  ◉ build-on-tag  ○ manual                               │  │
│ │ Credencial: [User: deploy / Token: ••••• Alterar]                │  │
│ │ [Testar conexão]                                                 │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Templates de Documento ────────────────────────────────────────┐  │
│ │ Template padrão para PDF de release (do produto):                │  │
│ │ [Template NEXUS-LD Padrão ▼]                                      │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│              [Cancelar]   [Salvar e configurar módulos]   [Salvar]     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Seções e campos

### 4.1 Identidade

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Nome | text | ✅ | max 200 |
| Sigla | text | ✅ | max 20, [A-Z0-9-]+, UNIQUE, **imutável após criação** |
| Tipo | select | ✅ | SISTEMA, INTEGRACAO, LIBRARY, OUTRO |
| Descrição | textarea | ❌ | max 500 |
| Cor visual | color picker | ✅ | hex 6 chars |
| Status | select | ✅ | ATIVO (default), INATIVO |
| Responsável | select usuário | ❌ | FK opcional |

### 4.2 Integração GitHub (pós-MVP)

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| URL do repositório | text | ✅ | URL válida do GitHub |
| Branch padrão | text | ✅ | default `main` |
| Padrão de tag | text (regex) | ✅ | regex válida, default `^v\d+\.\d+\.\d+$` |
| Credencial | reference + senha | ✅ | token PAT GitHub com escopo `repo` |

**Campos derivados:**
- `repositorioGithub`: armazenado como `owner/repo` ou URL completa.
- Validação: backend testa conexão em background ao salvar.

### 4.3 Integração Jenkins (pós-MVP)

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| URL do Jenkins | text | ❌ | URL válida |
| Nome do job | text | ✅ se URL preenchida | max 200 |
| Trigger | radio | ✅ | `BUILD_ON_TAG` ou `MANUAL` |
| Credencial | user + token | ✅ se URL preenchida | API token Jenkins |

### 4.4 Templates de Documento

- Select para template padrão de PDF (consumido pelo renderer).
- Lista templates ativos do catálogo (ver `24-documentos-templates.md`).
- Opcional: produto pode usar template global.

---

## 5. Botões

### Cancelar
- Volta para `08-produtos-lista.md`.
- Discard guard se sujo.

### Salvar
- Persiste. Toast "Produto salvo".
- Redirect para detalhe do produto (ou listagem).

### Salvar e configurar módulos
- Persiste + redirect para `10-produtos-modulos-artefatos.md`.

### Testar conexão GitHub
- Testa sem salvar.
- Backend usa credencial fornecida (ou existente).
- Response: OK + última tag visível, ou erro descritivo.

### Testar conexão Jenkins
- Idem para Jenkins.
- Valida URL + autenticação + existência do job.

---

## 6. Regras de negócio

### 6.1 Criação
- **Sigla obrigatória, única (case-insensitive), imutável**.
- Sigla normalizada para uppercase.
- Cor obrigatória (default `#2563eb`).
- Status default ATIVO.

### 6.2 Edição
- **Sigla**: ignora se enviada.
- Demais campos editáveis.

### 6.3 Status
- Produto ATIVO aparece como produto-alvo de novas releases.
- INATIVO bloqueia criação de release.
- INATIVO **não** quebra histórico nem clientes contratantes.
- Inativação exige confirm.

### 6.4 Validação para ATIVO
- Produto ATIVO precisa ter pelo menos **1 módulo cadastrado** (ver tela 10).
- Aviso na hora de salvar se ATIVO mas sem módulo: "Produto ativo precisa ter ao menos 1 módulo. Configurar agora?"

### 6.5 Integrações (pós-MVP)
- Teste de conexão obrigatório antes de salvar configuração de integração nova.
- Token Jenkins/GitHub armazenado cifrado.
- Token mascarado em GETs.

### 6.6 Sigla — exemplos válidos
- `NEXUSLD`, `Nexus-CR`, `FOLHA`, `CONTAS-V2`.
- Inválidos: `nexus ld` (espaço), `nexus_ld` (underscore), `nexus/ld` (slash).

---

## 7. Contratos de API

### Criar produto

```
POST /api/v1/release-orchestrator/produtos
```

Existing (ver `37-contratos-openapi.md`).

Para pós-MVP, body amplia com campos GitHub/Jenkins:

```json
{
  "nome": "NEXUS-LD",
  "sigla": "NEXUSLD",
  "tipo": "SISTEMA",
  "descricao": "Sistema de legislação digital",
  "cor": "#2563eb",
  "ativo": true,
  "responsavelId": "uuid",
  "github": {
    "repositorioUrl": "https://github.com/nexus/nexus-ld",
    "branchPadrao": "main",
    "padraoTag": "^v\\d+\\.\\d+\\.\\d+$",
    "credencialId": "uuid"
  },
  "jenkins": {
    "url": "https://jenkins.nexus.internal",
    "job": "nexus-ld-build",
    "triggerMode": "BUILD_ON_TAG",
    "credencialId": "uuid"
  },
  "templatePdfId": "uuid"
}
```

### Atualizar

```
PUT /api/v1/release-orchestrator/produtos/{id}
```

`sigla` ignorada se enviada.

### Testar GitHub

```
POST /api/v1/release-orchestrator/produtos/{id}/integracao/github/testar
```

Body opcional com credenciais provisórias.

Response:
```json
{
  "ok": true,
  "detalhes": {
    "repoEncontrado": "nexus/nexus-ld",
    "ultimaTag": "v1.5.0",
    "ultimoCommitData": "2026-05-30T18:00:00Z"
  }
}
```

### Testar Jenkins

```
POST /api/v1/release-orchestrator/produtos/{id}/integracao/jenkins/testar
```

Response:
```json
{
  "ok": true,
  "detalhes": {
    "jobEncontrado": "nexus-ld-build",
    "ultimoBuild": "#234",
    "ultimoBuildStatus": "SUCCESS"
  }
}
```

---

## 8. DTOs

```java
public record ProdutoRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Pattern(regexp = "^[A-Z0-9-]+$") @Size(max = 20) String sigla,
    @NotNull TipoProduto tipo,
    @Size(max = 500) String descricao,
    @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String cor,
    Boolean ativo,
    UUID responsavelId,
    @Valid IntegracaoGithub github,
    @Valid IntegracaoJenkins jenkins,
    UUID templatePdfId
) {
    public record IntegracaoGithub(
        @NotBlank String repositorioUrl,
        @NotBlank String branchPadrao,
        @NotBlank String padraoTag,
        UUID credencialId
    ) {}

    public record IntegracaoJenkins(
        @NotBlank String url,
        @NotBlank String job,
        @NotNull TriggerMode triggerMode,
        UUID credencialId
    ) {}
}
```

---

## 9. Performance

- Cadastro leve.
- Teste de conexão tem rate limit (10/min por usuário).

---

## 10. Segurança

- Credenciais GitHub/Jenkins cifradas em repouso.
- Nunca expostas em GETs.
- Auditadas como severidade ALTA.
- Token nunca em log.

---

## 11. Estados e edge cases

### Sigla duplicada
- Erro inline + sugestão.

### Token inválido
- Erro de teste: "Token inválido ou sem permissão."

### Padrão de tag inválido
- Validação regex client-side antes de submit.

### Produto sem módulos sendo ativado
- Aviso: "Produto ATIVO precisa de ao menos 1 módulo. [Cadastrar agora]".

### Mudança de credencial
- Confirm: "Substituir credencial atual?".

---

## 12. Acessibilidade

- Labels associados.
- Mensagens de erro com `aria-live`.
- Color picker com input numérico alternativo.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `PRODUTO_CRIADO` | snapshot inicial |
| `PRODUTO_EDITADO` | diff |
| `PRODUTO_INTEGRACAO_ALTERADA` | tipo + indicador (sem token) |
| `PRODUTO_STATUS_ALTERADO` | de → para |

---

## 14. Exemplos

| Produto | Sigla | Tipo |
|---|---|---|
| NEXUS-LD | NEXUSLD | SISTEMA |
| Nexus-CR | NEXUSCR | SISTEMA |
| FOLHA-WEB | FOLHA | SISTEMA |
| Integração Pagamentos | INTPAG | INTEGRACAO |
| Lib Util Comum | LIBUTIL | LIBRARY |

---

## 15. Cross-reference

- [`08-produtos-lista.md`](08-produtos-lista.md) — Listagem.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Próximo passo (módulos).
- [`24-documentos-templates.md`](24-documentos-templates.md) — Templates de PDF.
- [`28-configuracoes.md`](28-configuracoes.md) — Credenciais globais.
- Integrações GitHub/Jenkins — spec técnica descrita neste documento (seções acima) e em [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md).
- [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) — Jenkinsfile e assets nos repos.
- [`40-guia-versao-tag.md`](40-guia-versao-tag.md) — Guia de versionamento.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
