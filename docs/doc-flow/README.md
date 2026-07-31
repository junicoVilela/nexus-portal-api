# DocFlow — Backend (API)

Módulo Maven **`doc-flow/`** do `softon-portal-api`. Gestão de manuais: clientes, projetos, módulos, páginas, publicações, preview, usuários, grupos e auditoria.

> Documentação frontend: `softon-portal-web/docs/docflow/`  
> Jornada integrada (manual + release): [`../jornadas/00-cenario-feliz-acme.md`](../jornadas/00-cenario-feliz-acme.md)

---

## Localização

| Item       | Caminho                                                                                                           |
| ---------- | ----------------------------------------------------------------------------------------------------------------- |
| Código     | `softon-portal-api/doc-flow/src/main/java/br/com/softon/portal/docflow/`                                          |
| Migrations | `application/src/main/resources/db/migration/` (todas centralizadas) — ver [`../MIGRATIONS.md`](../MIGRATIONS.md) |
| Resumo IA  | `softon-portal-api/.ai/modules/docflow.md`                                                                        |

Pacote base: `br.com.softon.portal.docflow.{controller|service|repository|entity|dto}`

---

## Prefixo da API

Todos os controllers de domínio usam:

```text
/api/v1/docflow/{recurso}
```

Exceções (sem prefixo `docflow`):

| Recurso                 | Prefixo                                             |
| ----------------------- | --------------------------------------------------- |
| Login                   | `POST /api/v1/auth/login`                           |
| Usuário atual           | `GET /api/v1/auth/me` (JWT)                         |
| Preview tokens          | `/api/v1/preview-tokens`, `/api/v1/preview/{token}` |
| Download público pacote | `GET /api/v1/public/publicacoes/download`           |

---

## Domínio

```text
Cliente → Projeto → Módulo → Página → Publicação (ZIP/PDF por cliente)
```

| Entidade            | Descrição                                                            |
| ------------------- | -------------------------------------------------------------------- |
| **Cliente**         | Tenant do manual; vínculos a projetos/módulos/páginas; logo          |
| **Projeto**         | Agrupador de módulos (ex.: um sistema)                               |
| **Módulo**          | Agrupador de páginas                                                 |
| **Página**          | HTML + workflow editorial + anexos + revisões                        |
| **PaginaTemplate**  | Estrutura inicial reutilizável para criação de páginas               |
| **Publicacao**      | Job assíncrono: pacote versionado para um cliente                    |
| **PreviewToken**    | Link temporário de visualização                                      |
| **Grupo / Usuario** | RBAC (grupos com permissões string; usuários com roles ADMIN/EDITOR) |
| **AuditoriaEvento** | Log append-only de ações                                             |

### Status da página

```text
RASCUNHO → EM_REVISAO → APROVADO → PUBLICADO
                ↘                    ↘
             ARQUIVADO ←─────────────┘
```

Transições: `POST /paginas/{id}/enviar-revisao`, `/aprovar`, `/publicar`, `/arquivar`.

---

## Controllers principais

| Controller                   | Base path               | Papel                                                     |
| ---------------------------- | ----------------------- | --------------------------------------------------------- |
| `ClienteController`          | `/docflow/clientes`     | CRUD, vínculos, logo, copiar vínculos                     |
| `ProjetoController`          | `/docflow/projetos`     | CRUD                                                      |
| `ModuloController`           | `/docflow/modulos`      | CRUD                                                      |
| `PaginaController`           | `/docflow/paginas`      | CRUD, workflow, anexos, revisões, reordenar               |
| `PublicacaoController`       | `/docflow/publicacoes`  | Criar, listar, excluir, preview, download, PDF, changelog |
| `DocFlowDashboardController` | `/docflow/dashboard`    | Métricas estruturais, operacionais e editoriais           |
| `AjudaController`            | `/docflow/ajuda`        | Conteúdo, eventos e métricas da ajuda interativa           |
| `PreviewController`          | `/api/v1/preview-*`     | Tokens de preview                                         |
| `EmpresaController`          | `/docflow/empresa/logo` | Logo global nos manuais                                   |
| `AuthController`             | `/api/v1/auth`          | Login JWT                                                 |
| `UsuarioController`          | `/docflow/usuarios`     | CRUD (ADMIN)                                              |
| `GrupoController`            | `/rbac/grupos`          | CRUD + membros + permissões (ADMIN) — módulo `dtec-rbac`  |
| `AuditoriaController`        | `/docflow/auditoria`    | Listagem (ADMIN)                                          |

As exclusões de conteúdo seguem a ordem segura `publicações → cliente` e
`subpáginas → páginas → módulos → projetos`. O backend retorna uma mensagem de
regra de negócio quando ainda existir uma dependência que precisa ser removida.

Serviços de geração: `GeradorPacoteService`, `GeradorPdfService`, `PublicacaoWorkerService` (@Async).

### Modelos de página

`GET /api/v1/docflow/paginas/templates` lista os modelos ativos ordenados para o editor.
A migration `V17__docflow__05_alter_pagina_templates.sql` cria o catálogo e entrega os
modelos iniciais de funcionalidade, passo a passo, cadastro, consulta, FAQ e solução de problemas.
A migration `V18__docflow__06_update_pagina_templates_visual.sql` evolui esses modelos com
blocos semânticos visuais; o CSS equivalente é incluído no pacote publicado.

A migration `V23__docflow__11_templates_personalizados.sql` adiciona modelos personalizados
com escopo exclusivo de projeto ou cliente. `POST /api/v1/docflow/paginas/templates` copia
e sanitiza o HTML informado; `DELETE /api/v1/docflow/paginas/templates/{templateId}` remove
somente estruturas personalizadas. Modelos do sistema são protegidos e páginas existentes
não dependem do registro após a aplicação.

A migration `V24__docflow__12_templates_contextuais_versionados.sql` acrescenta histórico
imutável, arquivamento e a origem do modelo na página. A página continua armazenando uma
cópia independente do HTML, mas registra `templateOrigemId` e `templateOrigemVersao` para
contabilizar o uso de cada versão.

Variáveis aceitas no conteúdo:

- `{{ cliente.nome }}`;
- `{{ projeto.nome }}`;
- `{{ modulo.nome }}`;
- `{{ pagina.titulo }}`;
- `{{ pagina.codigo }}`;
- `{{ data.atual }}`.

`POST /paginas/templates/{id}/aplicar` resolve os valores disponíveis, preserva marcadores
pendentes e valida se projeto, módulo e cliente são compatíveis com o escopo. A listagem aceita
`projetoId`, `clienteId`, `somenteContexto` e `incluirArquivados`; modelos de cliente são
compatíveis com os projetos vinculados em `tb_cliente_projeto`.

Gestão dos modelos:

- `PUT /paginas/templates/{id}` — edita e cria uma nova versão;
- `POST /paginas/templates/{id}/duplicar` — cria uma cópia personalizada na versão 1;
- `POST /paginas/templates/{id}/arquivar|reativar` — altera disponibilidade com histórico;
- `GET /paginas/templates/{id}/versoes` — lista snapshots e páginas originadas;
- `POST /paginas/templates/{id}/versoes/{numero}/restaurar` — restaura como nova versão.

### Segurança editorial da página

A migration `V19__docflow__07_pagina_seguranca_editorial.sql` adiciona a versão otimista da
página e classifica os eventos de revisão. Atualizações e autosave enviam `version`; uma versão
defasada retorna HTTP 409 sem sobrescrever o conteúdo.

Endpoints adicionais:

- `PUT /paginas/{id}/autosave` — persiste conteúdo de rascunho sem criar revisão;
- `GET /paginas/{id}/qualidade` — checklist editorial autoritativo;
- `GET /paginas/{id}/preview` — HTML da página pelo renderizador do manual.

O envio para revisão é bloqueado enquanto houver erro de qualidade. Avisos, como resumo curto
ou ausência de títulos de seção, permanecem informativos.

O checklist também rejeita imagens sem origem, links vazios/JavaScript e sinaliza saltos na
hierarquia de títulos. Comentários da central de revisão são registrados como eventos
`COMENTARIO` no histórico imutável da página.

### Operação editorial

- `GET /dashboard/resumo` agrega KPIs sem transferir catálogos inteiros ao frontend;
- `GET /paginas/anexos` fornece a biblioteca de mídia paginada e pesquisável;
- `POST /paginas/{id}/revisoes/comentarios` registra discussões da revisão;
- `GET /publicacoes?status=ERRO` filtra no servidor;
- `POST /publicacoes/reprocessar-lote` reenvia até 100 publicações, ignorando jobs em execução.

### Ajuda interativa

As migrations `V25` e `V26` adicionam o catálogo administrável, eventos de uso e permissões
`AJUDA:*`. A leitura é concedida aos grupos base; criação, edição, exclusão e métricas ficam com
ADMIN e EDITOR.

A migration `V27` protege o vínculo entre etapas e jornadas com chave estrangeira e restrição de
tipo. O serviço também impede excluir ou alterar uma jornada que ainda possua etapas.

- `GET /ajuda/conteudos` — jornadas, etapas, FAQs, artigos, onboarding e passos do tour;
- `POST|PUT|DELETE /ajuda/conteudos` — gestão dos textos, rotas, seletores e mídias;
- `POST /ajuda/eventos` — telemetria de busca, acesso, progresso e conclusão;
- `GET /ajuda/metricas` — totais, taxa de conclusão e rankings dos últimos 30 dias.

Mídias aceitam caminhos internos ou URLs HTTP/HTTPS e podem ser imagem, GIF, vídeo ou galeria.
Eventos idênticos da mesma sessão são deduplicados em uma janela de 10 segundos. Um job diário
remove eventos antigos; a retenção padrão é de 180 dias e pode ser ajustada por
`docflow.ajuda.retencao-eventos-dias`. O cron usa `docflow.ajuda.retencao-cron` e, por padrão,
executa às 03:30 do horário do servidor.

---

## Autorização (backend)

- Controllers de conteúdo: `@PreAuthorize(Permissoes.*)` com `hasAuthority('DOMINIO:ACAO')` (ex.: `PAGINA:LER`, `PUBLICACAO:CRIAR`).
- Usuários, grupos, auditoria, logo empresa: permissões granulares do catálogo RBAC (`USUARIO:*`, `GRUPO_ACESSO:*`, `AUDITORIA:*`, …).
- Endpoints públicos: preview HTML, download anexo/logo, download pacote com token

Grupos (`tb_grupo`, `tb_grupo_permissao`) existem na API; permissões vêm do **catálogo RBAC** (`tb_dominio`, `tb_funcionalidade`, `tb_permissao`) e são expostas em `GET /auth/me`. Os seeds base concedem permissões editoriais aos grupos `ADMIN` e `EDITOR`, mas os controllers validam **autoridade** (`DOMINIO:ACAO`), não role diretamente.

---

## Integração frontend

| Aspecto               | Frontend                                         | Backend                                | Status                                |
| --------------------- | ------------------------------------------------ | -------------------------------------- | ------------------------------------- |
| Base URL services     | `environment.apiUrl` = `/api/doc-flow`           | `/api/v1/docflow/...`                  | ✅ Proxy dev reescreve para `docflow` |
| Auth                  | `AuthApiService` → `POST/GET /api/v1/auth/*`     | JWT real                               | ✅ S0                                 |
| Permissões menu       | `GET /auth/me` → `permissoes[]` do catálogo RBAC | `tb_dominio` → `tb_permissao` + grupos | ✅ S0.5                               |
| Logo empresa          | `ConfiguracaoService` → HttpClient               | `EmpresaController`                    | ✅ S0                                 |
| Preview tokens        | UI no painel de vínculos de clientes             | API existe                             | ✅ Gerar, listar, copiar URL, revogar |
| Grupos/usuários admin | Módulo `seguranca` mock                          | API docflow                            | ⚠️ Conectar ou separar                |

Proxy dev (`softon-portal-web/frontend/proxy.conf.json`): reescreve `/api/doc-flow` → `/api/v1/docflow`.

---

## Testes

Cobertura em `doc-flow/src/test/...`, incluindo serviços de página, publicação, dashboard e
qualidade editorial, além das integrações de geração e download.

---

## Documentação relacionada

| Documento                                                                      | Conteúdo                         |
| ------------------------------------------------------------------------------ | -------------------------------- |
| [`../jornadas/00-cenario-feliz-acme.md`](../jornadas/00-cenario-feliz-acme.md) | Jornada manual + release         |
| `softon-portal-web/docs/docflow/`                                              | Specs frontend                   |
| [`../release-orchestrator/README.md`](../release-orchestrator/README.md)       | Módulo irmão (entregas técnicas) |
| [`../ROADMAP.md`](../ROADMAP.md)                                               | Prioridades gerais do portal     |

### Specs detalhadas por tela (futuro)

Espelhar o padrão `release-orchestrator/` conforme necessidade (ex.: `01-paginas-workflow.md`). Por ora, contratos estão nos controllers + `softon-portal-web/docs/docflow/04-services-e-models.md`.
