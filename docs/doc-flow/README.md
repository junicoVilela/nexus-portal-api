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

| Controller             | Base path               | Papel                                                     |
| ---------------------- | ----------------------- | --------------------------------------------------------- |
| `ClienteController`    | `/docflow/clientes`     | CRUD, vínculos, logo, copiar vínculos                     |
| `ProjetoController`    | `/docflow/projetos`     | CRUD                                                      |
| `ModuloController`     | `/docflow/modulos`      | CRUD                                                      |
| `PaginaController`     | `/docflow/paginas`      | CRUD, workflow, anexos, revisões, reordenar               |
| `PublicacaoController` | `/docflow/publicacoes`  | Criar, listar, excluir, preview, download, PDF, changelog |
| `PreviewController`    | `/api/v1/preview-*`     | Tokens de preview                                         |
| `EmpresaController`    | `/docflow/empresa/logo` | Logo global nos manuais                                   |
| `AuthController`       | `/api/v1/auth`          | Login JWT                                                 |
| `UsuarioController`    | `/docflow/usuarios`     | CRUD (ADMIN)                                              |
| `GrupoController`      | `/rbac/grupos`          | CRUD + membros + permissões (ADMIN) — módulo `dtec-rbac`  |
| `AuditoriaController`  | `/docflow/auditoria`    | Listagem (ADMIN)                                          |

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

---

## Autorização (backend)

- Controllers de conteúdo: `@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")`
- Usuários, grupos, auditoria, logo empresa: `@PreAuthorize("hasRole('ADMIN')")`
- Endpoints públicos: preview HTML, download anexo/logo, download pacote com token

Grupos (`tb_grupo`, `tb_grupo_permissao`) existem na API; permissões vêm do **catálogo RBAC** (`tb_dominio`, `tb_funcionalidade`, `tb_permissao`) e são expostas em `GET /auth/me`. `@PreAuthorize` nos controllers ainda usa roles `ADMIN`/`EDITOR`.

---

## Integração frontend

| Aspecto               | Frontend                                         | Backend                                | Status                                |
| --------------------- | ------------------------------------------------ | -------------------------------------- | ------------------------------------- |
| Base URL services     | `environment.apiUrl` = `/api/doc-flow`           | `/api/v1/docflow/...`                  | ✅ Proxy dev reescreve para `docflow` |
| Auth                  | `AuthApiService` → `POST/GET /api/v1/auth/*`     | JWT real                               | ✅ S0                                 |
| Permissões menu       | `GET /auth/me` → `permissoes[]` do catálogo RBAC | `tb_dominio` → `tb_permissao` + grupos | ✅ S0.5                               |
| Logo empresa          | `ConfiguracaoService` → HttpClient               | `EmpresaController`                    | ✅ S0                                 |
| Preview tokens        | Service existe                                   | API existe                             | ⚠️ Sem UI                             |
| Grupos/usuários admin | Módulo `seguranca` mock                          | API docflow                            | ⚠️ Conectar ou separar                |

Proxy dev (`softon-portal-web/frontend/proxy.conf.json`): reescreve `/api/doc-flow` → `/api/v1` (falta segmento `docflow` nos paths atuais).

---

## Testes

Cobertura parcial em `doc-flow/src/test/...`: `ClienteServiceTest`, `PaginaServiceTest`, `GrupoServiceTest`.

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
