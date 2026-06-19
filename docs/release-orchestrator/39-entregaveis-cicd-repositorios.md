# 39 — Entregáveis de CI/CD nos Repositórios

> **Estado**: 📋 Especificado — entregáveis nos **repositórios de produto** (GitHub), não no portal.  
> **Código portal**: integração consumida na Fase 2 (`09`, `ROADMAP` F2.*).  
> **Relacionado**: [`40-guia-versao-tag.md`](40-guia-versao-tag.md), [`09-produtos-cadastro.md`](09-produtos-cadastro.md), [`21-geracao-pacote.md`](21-geracao-pacote.md).

---

## 1. Contexto

A história **Releases (GitHub + Jenkins)** inclui trabalho em **dois lugares**:

| Onde | O quê |
|---|---|
| **Repositórios de produto** (ex.: `softon/dtec-ld`) | Jenkinsfile, pipelines, scripts de banco/kettle/func/regras, publicação de assets no GitHub Release |
| **Portal Release Orchestrator** | Cadastro de produto/módulos, releases, entregas a clientes, montagem do pacote final por cliente |

Este documento cobre o **primeiro bloco** — o que cada repositório deve entregar para o orchestrator consumir na Fase 2+.

---

## 2. Catálogo de entregáveis

### 2.1 Release Java Web

**Resultado**: aplicação web compilada publicada como **asset** no GitHub Release da tag.

| Item | Detalhe |
|---|---|
| Artefato | `.war`, `.jar`, `.zip`, `.tar.gz` ou outro conforme `extensoesAceitas` do módulo `WEB` |
| Trigger | Tag `v*.*.*` (regex configurável no portal — ver `09`) |
| Jenkins | Job `build-on-tag` — ver [`40`](40-guia-versao-tag.md) |
| Jenkinsfile | No repositório: `Jenkinsfile` na raiz ou `ci/Jenkinsfile` |
| GitHub Release | Asset nomeado conforme convenção do produto (ex.: `{modulo}-{versao}.war`) |
| Guia | [`40-guia-versao-tag.md`](40-guia-versao-tag.md) no repo ou wiki do produto |
| Repo | Um repo por app **ou** monorepo com vários assets na mesma release — ver `repositorioGithub` por módulo em [`10`](10-produtos-modulos-artefatos.md) §6 |

**Produtos de referência (estimativa história: 2 dias cada)**:

| Produto | Sigla | Repositório (exemplo) | Módulo portal |
|---|---|---|---|
| Configuração base | — | `softon/release-pipeline-base` | template Jenkins compartilhado |
| DTEC-LD | `DTECLD` | `softon/dtec-ld` | `WEB` |
| DTEC-CR | `DTECCR` | `softon/dtec-cr` | `WEB` |
| DTEC-ONLINE | `DTECONLINE` | `softon/dtec-online` | `WEB` |

### 2.2 Release Java Batch

**Resultado**: processos em lote publicados no GitHub Releases.

| Item | Detalhe |
|---|---|
| Artefato | `.jar` |
| Jenkinsfile | Mesmo padrão `build-on-tag` |
| Prazo história | 2 dias (por produto batch) |
| Módulo portal | `BATCH` |

### 2.3 Pacote de Banco (DDL/DML unificados)

**Resultado**: pacote único de banco com mudanças entre versões.

| Item | Detalhe |
|---|---|
| Conteúdo no repo | Scripts em `db/` ou `scripts/banco/` (convenção por produto) |
| Ordenação | DDL → DML (numeração `DDL_###`, `DML_###`) |
| Saída no release | `DDL.sql` + `DML.sql` unificados por dialeto |
| Dialetos | Oracle e SQL Server (subpastas ou sufixo no nome) |
| Publicação | Assets no GitHub Release da tag |
| Prazo história | Pacote geral — 2 dias |
| Módulo portal | `BANCO` |

**Estratégia de delta (Fase 2)**: diff entre commits de `FROM_TAG` e `TO_TAG` — ver [`20-range-manual-delta.md`](20-range-manual-delta.md).

### 2.4 Pacote Funcionalidades

**Resultado**: scripts de habilitação de funcionalidades aplicáveis por cliente.

| Item | Detalhe |
|---|---|
| Conteúdo no repo | DDL + scripts MERGE: catálogo legado + **`TB_CLIENTE_FUNCIONALIDADE`** (nova) — ver [`11` §17](11-produtos-catalogo-funcional.md) |
| No orchestrator | Gerado na entrega: catálogo + subset cliente (`ClienteFuncionalidade`) — [`21` etapa 6](21-geracao-pacote.md) |
| Prazo história | 2 dias |
| Módulo portal | `FUNCIONALIDADES` (auto-gerado; **sem upload** na release) |

### 2.5 Pacote Regras

**Resultado**: scripts de regras de negócio por cliente.

| Item | Detalhe |
|---|---|
| Conteúdo no repo | Templates em `regras/` |
| No orchestrator | Gerado na entrega — `21` etapa 6 |
| Prazo história | 2 dias |
| Módulo portal | `REGRAS` (auto-gerado) |

### 2.6 Kettle (PDI)

**Resultado**: ZIP do **delta** de jobs (`.ktr`/`.kjb`) entre duas versões, com dependências.

| Item | Detalhe |
|---|---|
| Conteúdo no repo | Jobs em `kettle/` ou `pdi/` |
| Saída no release | ZIP com arquivos alterados entre tags |
| Publicação | Asset no GitHub Release |
| Prazo história | DTEC-LD — 2 dias; DTEC-CR — 2 dias |
| Módulo portal | `KETTLE` |

---

## 3. Jenkinsfile padrão (build-on-tag)

Contrato mínimo que todos os repos WEB/BATCH devem seguir:

```groovy
// Jenkinsfile (resumo — ver repo release-pipeline-base)
pipeline {
  agent any
  triggers {
    // Disparo por tag Git v*.*.*
  }
  stages {
    stage('Checkout') { /* tag exata */ }
    stage('Build')    { /* mvn package / gradle */ }
    stage('Package')  { /* war ou jar */ }
    stage('Publish')  {
      // gh release upload $TAG ./target/*.war
    }
  }
}
```

**Checklist por repositório novo**:

- [ ] `Jenkinsfile` commitado na branch padrão
- [ ] Job Jenkins criado e apontando para o repo
- [ ] Credencial GitHub (PAT) com escopo `repo` + upload release
- [ ] Credencial Jenkins registrada no portal (`09` — pós-MVP)
- [ ] Primeira tag de teste `v0.0.1` gera release com asset
- [ ] Guia versão/tag publicado — [`40`](40-guia-versao-tag.md)

---

## 4. Integração com o portal

O portal **não contém** o Jenkinsfile; referencia repositórios no **produto** e, quando necessário, **por módulo**:

| Campo | Onde | Uso |
|---|---|---|
| `repositorioGithub` | Produto (`09`) | Default para tags e download de assets |
| `repositorioGithub` | Módulo `WEB`/`BATCH` (`10` §6) | Override quando cada app tem repo próprio |
| `branchPadrao` | Produto (`09`) | Branch para comparar tags |
| `padraoTag` | Produto (`09`) | Regex de tags válidas (ex.: `^v\d+\.\d+\.\d+$`) |
| `jenkinsUrl` + `jenkinsJob` | Produto / módulo | Disparo manual ou webhook pós-tag |
| `triggerMode` | Produto (`09`) | `BUILD_ON_TAG` |
| `padraoAsset` | Módulo (`10` §6) | Glob do asset na GitHub Release da `TO_TAG` |

Fluxo Fase 2:

```text
Tag v1.5.0 no GitHub
  → Jenkins build-on-tag
  → Assets no GitHub Release v1.5.0
  → Portal: release PUBLICADA com ReleaseModuloVersao = v1.5.0
  → Orchestrator: download assets na geração do pacote (21 etapa 3)
```

No **MVP** (Fase 0–1), o operador faz **upload manual** dos mesmos artefatos na aba Artefatos (`14`).

---

## 5. Mapa entregável → spec do portal

| Entregável (história) | Spec portal | Fase implementação |
|---|---|---|
| WAR/JAR no GitHub | `21` § etapa 3, `09` GitHub | Fase 2 |
| DDL/DML unificado | `20` § BANCO, `21` § etapa 4 | MVP upload / Fase 2 delta |
| Kettle delta ZIP | `20` § KETTLE, `21` § etapa 5 | MVP upload / Fase 2 delta |
| Funcionalidades/regras | `05`, `21` § etapa 6 | Fase 1 |
| Bundle por cliente | `18`–`23`, `21` | Fase 1 |
| FTP / pasta destino | `07`, `21` § etapa 10 | MVP pasta / Fase 3 FTP |
| Jenkinsfile + guia tag | **este doc** + `40` | Repos (paralelo Fase 2) |

---

## 6. Critérios de aceite (por produto)

Para considerar um produto **pronto para automação** (Fase 2):

1. Módulos cadastrados no portal (`10`) com tipos corretos.
2. Pelo menos um ciclo MVP completo com upload manual e entrega a um cliente piloto.
3. Jenkinsfile + job funcionando; tag de teste publica assets.
4. Guia versão/tag revisado pela equipe de release.
5. Portal com integração GitHub testada (`Testar conexão` em `09`).

---

## Cross-reference

- [`40-guia-versao-tag.md`](40-guia-versao-tag.md) — Procedimento operacional de versionamento.
- [`00-visao-geral-fluxo-integrado.md`](00-visao-geral-fluxo-integrado.md) — Mapa história → documentação.
- [`../ROADMAP.md`](../ROADMAP.md) — Fase 2 (automação).
