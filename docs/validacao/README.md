# Validação do Release Orchestrator

Documentação de **validação funcional e técnica** do módulo Release Orchestrator. Serve como guia compartilhado entre QA, dev e usuário de negócio para conferir, tela por tela, se o sistema está operando como esperado antes de cada release do produto.

---

## 1. Objetivo da documentação

Garantir que, ao final do roteiro, é possível:

1. Demonstrar todas as **configurações iniciais** necessárias para o sistema operar (cliente, produto, módulos, catálogo funcional, integração GitHub/Jenkins, destino de entrega).
2. Validar **cada tela** do módulo: campos, mensagens, persistência, comportamento esperado.
3. Executar **um fluxo completo de entrega** ponta a ponta — do cadastro do cliente até o pacote ZIP publicado no destino — e confirmar que os artefatos chegam corretamente.
4. Provar que a **documentação da release** e os **scripts de banco** são gerados de acordo com a especificação.
5. Confirmar que o **histórico de entregas** preserva auditoria de tudo que foi enviado para cada cliente.
6. Cobrir os principais **cenários de erro** e suas mensagens.

Esta doc complementa (não substitui) os guias operacionais já existentes:

- [`docs/jornadas/09-passo-a-passo-local-tela-a-tela.md`](../jornadas/09-passo-a-passo-local-tela-a-tela.md) — "tour" do portal com upload manual.
- [`docs/jornadas/10-fluxo-local-completo-com-jenkins.md`](../jornadas/10-fluxo-local-completo-com-jenkins.md) — fluxo end-to-end com CI real.
- [`docs/release-orchestrator/*`](../release-orchestrator/) — spec de cada tela.

A diferença é que aqui o foco é **validar**: critérios de aceite explícitos, dados a verificar no banco, mensagens esperadas, checklist marcável.

---

## 2. Como ler / estrutura de arquivos

```text
docs/validacao/
  README.md                              ← você está aqui
  01-configuracoes-iniciais.md           ← o que precisa estar cadastrado antes do fluxo
  02-validacao-tela-por-tela.md          ← campos, mensagens e persistência de cada tela
  03-fluxo-completo-entrega.md           ← XPTO / DTEC v1.0.0 → v1.1.0 ponta a ponta
  04-checklist-final.md                  ← lista marcável para fechar a validação
  05-cenarios-de-erro.md                 ← falhas esperadas e como reproduzir
```

Ordem de leitura recomendada na primeira validação:

1. `README.md` (este arquivo) — entender escopo e premissas.
2. `01-configuracoes-iniciais.md` — fazer todos os cadastros base.
3. `02-validacao-tela-por-tela.md` — passar tela por tela conferindo campos e mensagens.
4. `03-fluxo-completo-entrega.md` — executar a jornada de XPTO / DTEC.
5. `05-cenarios-de-erro.md` — provocar as falhas para validar reação do sistema.
6. `04-checklist-final.md` — marcar tudo que foi validado.

---

## 3. Premissas importantes (leia antes de começar)

Algumas convenções do projeto que diferem do que se costuma assumir em um cadastro "tradicional":

| Item | Como funciona no Release Orchestrator |
|---|---|
| **Ambiente** | Não tem tela própria. É um **enum** (`PROD`, `HOM`, `DEV`, `TEST`) escolhido como atributo do **cliente** (ambiente padrão) e da **entrega**. |
| **Domínio funcional** | Pertence **ao produto**, não é global. Cadastra-se dentro do detalhe do produto (catálogo funcional). |
| **Funcionalidade** | Pertence a um domínio do produto. Mesmo padrão acima. |
| **Cliente** | Existem dois tipos no sistema: `tb_cliente` (DocFlow — manuais) e `tb_cliente_orchestrator` (este módulo). Esta doc trata exclusivamente do **segundo**. |
| **Caminhos de armazenamento** | Não são cadastros de UI. São variáveis do backend (`application-dev.yml` ou env). Veja seção 3 da `01-configuracoes-iniciais.md`. |
| **Repositório de artefatos** | Configurado **por produto** na aba GitHub (não é cadastro global). |
| **Permissões** | Gerenciadas em `/seguranca` (módulo separado). Esta doc usa o `admin` seed por padrão. |
| **Módulo** | Pertence **ao produto** (tipos `WEB`, `BATCH`, `BANCO`, `KETTLE`, `FUNCIONALIDADES`, `REGRAS`). |
| **Release** | Pertence ao produto e tem versão única por produto. Só releases `PUBLICADA` entram em entregas. |
| **Entrega** | É a "saída para um cliente específico" derivada de uma release publicada. Estados: `RASCUNHO → EM_GERACAO → CONCLUIDA/FALHA`. |

---

## 4. Cenário de exemplo usado nos guias

Para tornar concreto, o roteiro de validação usa um cenário fictício consistente em todos os arquivos:

| Item | Valor |
|---|---|
| Cliente | `XPTO` — Banco XPTO S.A. |
| Produto | `DTEC` — Suite DTEC |
| Módulos | `DTEC-LD` (WEB), `DTEC-CR` (WEB), `POWERMATCH` (BATCH), `DTEC-BANCO` (BANCO) |
| Ambiente do contrato | `HOM` (Homologação) |
| Versão atual instalada no XPTO | `1.0.0` |
| Nova release | `1.1.0` |
| Pacote final esperado | `XPTO_DTEC_1.0.0_1.1.0.zip` |

> O monorepo já documenta um cenário canônico com `ACME` / `DTECLD`. Os dois são equivalentes — qualquer um serve de smoke. Use `XPTO`/`DTEC` se quiser seguir esta doc de validação literalmente, ou `ACME`/`DTECLD` se preferir alinhar com [`docs/jornadas/00-cenario-feliz-acme.md`](../jornadas/00-cenario-feliz-acme.md).

---

## 5. Pré-requisitos do ambiente

Antes de abrir qualquer tela, garanta:

| Item | Como verificar |
|---|---|
| Postgres rodando | `docker compose -f infra/docker/docker-compose.yml ps postgres` → `healthy` |
| Backend Spring Boot rodando | `curl -fs http://localhost:8080/actuator/health` → `{"status":"UP"}` |
| Migrations aplicadas (V1–V7) | `psql … -c "SELECT version FROM flyway_schema_history ORDER BY installed_rank"` → 7 linhas |
| Seeds RBAC + usuário admin | login com `admin / admin` no `/login` |
| Frontend Angular rodando | `http://localhost:4200` abre a tela de login |
| Pastas de storage existentes | `ls softon-portal-api/storage/{artefatos,entregas,publicacoes}` |
| Encryption key (se for usar SFTP/FTP/BUCKET) | `echo $RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` ≠ vazio |

O passo a passo completo de subida do ambiente está em [`docs/jornadas/09-passo-a-passo-local-tela-a-tela.md`](../jornadas/09-passo-a-passo-local-tela-a-tela.md) §1–§3.

---

## 6. Perfis envolvidos na validação

| Papel | O que valida | Onde |
|---|---|---|
| **Negócio** | Conteúdo do release-notes, lista de funcionalidades por cliente, ordem da entrega | `02` (visão geral) + `03` (fluxo XPTO) |
| **QA** | Mensagens, validações de campo, estado dos botões, persistência | `02` (tela por tela) + `05` (cenários de erro) |
| **Dev/DevOps** | Persistência no banco, logs, integrações GitHub/Jenkins, retry job de publicação | `02` + `03` + `05` |
| **Todos** | Que o checklist final está 100% marcado | `04-checklist-final.md` |

---

## 7. Onde reportar falhas

- **Spec divergente da implementação:** abrir PR atualizando [`docs/release-orchestrator/`](../release-orchestrator/) (spec é a fonte da verdade).
- **Implementação divergente da spec:** abrir issue no repo `softon-portal-api` (back) ou `softon-portal-web` (front), referenciando o item desta doc.
- **Gap funcional:** registrar em [`docs/jornadas/06-gaps-jornada-acme.md`](../jornadas/06-gaps-jornada-acme.md).
