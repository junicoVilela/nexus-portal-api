# Módulo DocFlow

## Objetivo

O módulo DocFlow é responsável pela criação, organização, versionamento, geração e publicação de documentação dos sistemas internos da nexus.

Abrange: clientes, projetos, módulos, páginas, publicações, preview, auditoria, usuários e empresa.

## Localização Maven

```text
doc-flow/src/main/java/br/com/nexus/portal/docflow/
```

## Pacote base

```text
com.nexus.portal.docflow
```

## Estrutura de pacotes

```text
com.nexus.portal.docflow.controller
com.nexus.portal.docflow.service
com.nexus.portal.docflow.repository
com.nexus.portal.docflow.entity
com.nexus.portal.docflow.dto.request
com.nexus.portal.docflow.dto.response
```

## Estrutura de pastas

```text
doc-flow/src/main/java/br/com/nexus/portal/
└── docflow/
    ├── controller/
    │   ├── ClienteController.java
    │   ├── ModuloController.java
    │   ├── PaginaController.java
    │   ├── ProjetoController.java
    │   ├── PublicacaoController.java
    │   ├── PublicDownloadController.java
    │   ├── PreviewController.java
    │   ├── EmpresaController.java
    │   ├── AuditoriaController.java
    │   ├── AuthController.java
    │   └── UsuarioController.java
    │
    ├── service/
    │   ├── ClienteService.java
    │   ├── ClienteLogoService.java
    │   ├── ModuloService.java
    │   ├── PaginaService.java
    │   ├── PaginaAnexoService.java
    │   ├── ProjetoService.java
    │   ├── PublicacaoService.java
    │   ├── PublicacaoWorkerService.java
    │   ├── PreviewTokenService.java
    │   ├── UsuarioService.java
    │   ├── AuditoriaService.java
    │   ├── EmpresaLogoService.java
    │   ├── GeradorPdfService.java
    │   ├── GeradorPacoteService.java
    │   └── NotificacaoEmailService.java
    │
    ├── repository/
    │   ├── ClienteRepository.java
    │   ├── ClienteModuloRepository.java
    │   ├── ClientePaginaRepository.java
    │   ├── ClienteProjetoRepository.java
    │   ├── ModuloRepository.java
    │   ├── PaginaRepository.java
    │   ├── PaginaAnexoRepository.java
    │   ├── PaginaRevisaoRepository.java
    │   ├── ProjetoRepository.java
    │   ├── PublicacaoRepository.java
    │   ├── PublicacaoChangelogRepository.java
    │   ├── PreviewTokenRepository.java
    │   ├── AuditoriaRepository.java
    │   └── UsuarioRepository.java
    │
    ├── entity/
    │   ├── Cliente.java
    │   ├── ClienteModulo.java
    │   ├── ClientePagina.java
    │   ├── ClienteProjeto.java
    │   ├── Modulo.java
    │   ├── Pagina.java
    │   ├── PaginaTemplate.java
    │   ├── PaginaAnexo.java
    │   ├── PaginaRevisao.java
    │   ├── StatusPagina.java
    │   ├── Projeto.java
    │   ├── Publicacao.java
    │   ├── PublicacaoChangelog.java
    │   ├── StatusPublicacao.java
    │   ├── PreviewToken.java
    │   ├── AuditoriaEvento.java
    │   └── Usuario.java
    │
    └── dto/
        ├── request/
        │   ├── ClienteRequest.java
        │   ├── VinculosRequest.java
        │   ├── CopiarVinculosRequest.java
        │   ├── ModuloRequest.java
        │   ├── PaginaRequest.java
        │   ├── ReordenarRequest.java
        │   ├── ProjetoRequest.java
        │   ├── PublicacaoRequest.java
        │   ├── LoginRequest.java
        │   ├── CriarUsuarioRequest.java
        │   ├── AtualizarUsuarioRequest.java
        │   └── AlterarSenhaRequest.java
        │
        └── response/
            ├── ClienteResponse.java
            ├── ModuloResponse.java
            ├── PaginaResponse.java
            ├── PaginaTemplateResponse.java
            ├── PaginaAnexoResponse.java
            ├── PaginaRevisaoResponse.java
            ├── ProjetoResponse.java
            ├── PublicacaoResponse.java
            ├── ChangelogItemResponse.java
            ├── DownloadTokenResponse.java
            ├── PreviewTokenResponse.java
            ├── AuditoriaResponse.java
            ├── LoginResponse.java
            └── UsuarioResponse.java
```

## Endpoints atuais

```text
GET    /api/v1/clientes
POST   /api/v1/clientes
PUT    /api/v1/clientes/{id}
DELETE /api/v1/clientes/{id}

GET    /api/v1/projetos
POST   /api/v1/projetos
PUT    /api/v1/projetos/{id}
DELETE /api/v1/projetos/{id}

GET    /api/v1/modulos
POST   /api/v1/modulos
PUT    /api/v1/modulos/{id}
DELETE /api/v1/modulos/{id}

GET    /api/v1/paginas
GET    /api/v1/paginas/templates
GET    /api/v1/docflow/paginas/blocos
GET    /api/v1/docflow/paginas/blueprints
GET    /api/v1/docflow/paginas/biblioteca
POST   /api/v1/paginas
PUT    /api/v1/paginas/{id}
DELETE /api/v1/paginas/{id}

POST   /api/v1/publicacoes
GET    /api/v1/publicacoes/{id}
DELETE /api/v1/publicacoes/{id}
POST   /api/v1/docflow/publicacoes/{id}/cancelar
GET    /api/v1/docflow/publicacoes/{id}/diff?comparadaCom={id}
GET    /api/v1/docflow/publicacoes/eventos (SSE autenticado, filtrado por escopo de cliente)

POST   /api/v1/docflow/paginas/{id}/revisor
GET    /api/v1/docflow/paginas/minhas-revisoes
GET    /api/v1/docflow/paginas/snippets
POST   /api/v1/docflow/paginas/snippets
PUT    /api/v1/docflow/paginas/snippets/{id}
DELETE /api/v1/docflow/paginas/snippets/{id}

POST   /api/v1/auth/login
GET    /api/v1/usuarios
POST   /api/v1/usuarios

GET    /api/v1/auditoria
```

## Regras de negócio principais

- Comunicação entre contextos internos (clientes, projetos, módulos, páginas) sempre via Service, nunca via Repository direto.
- Publicação gera snapshot do conteúdo.
- Publicações concluídas ou com erro podem ser excluídas; o histórico e o pacote ZIP são removidos. Publicações em geração são protegidas contra exclusão.
- Preview usa token temporário para acesso público.
- Auditoria registra eventos de criação e alteração.
- `Pagina.version` protege atualizações concorrentes; divergências retornam HTTP 409.
- Autosave atualiza somente rascunhos e não cria revisões intermediárias.
- Revisões registram o tipo e a descrição do evento editorial.
- O checklist de qualidade é obrigatório antes do envio para revisão.
- A biblioteca possui 20 templates visuais, 45 componentes canônicos e 10 blueprints
  declarativos. Templates oferecem uma página pronta; blueprints permitem recombinar apenas os
  componentes necessários sem duplicar HTML.
- A migração V21 atualiza somente o catálogo de templates; páginas existentes não têm
  seu `conteudo_html` sobrescrito.
- Modelos personalizados possuem escopo de projeto ou cliente, aplicação contextual com
  variáveis, edição, duplicação, arquivamento e histórico imutável. A página registra a origem
  e a versão aplicada sem manter dependência de conteúdo com o modelo.
- Prévia individual, prévia do manual e pacote ZIP compartilham o layout `layout-v12`,
  incluindo objetivo, captura, marcações numeradas, grids, fluxo e badges responsivos.
- Exclusões estruturais são feitas de baixo para cima: cliente exige publicações removidas; projeto exige módulos removidos; módulo exige páginas removidas; página exige subpáginas removidas.
- Exclusões removem vínculos e registros técnicos em cascata, limpam arquivos após o commit e registram evento de auditoria.
- A geração publica eventos SSE ao concluir e registra `docflow.publicacao.duracao` e
  `docflow.publicacao.resultado{status=sucesso|erro|cancelada}` no Micrometer/Prometheus.
- A geração do pacote roda **fora de transação**: o banco só é tocado em transações curtas
  no início (carregar contexto) e no fim (registrar desfecho).
- `PublicacaoWatchdogJob` reconcilia publicações presas em `GERANDO` além de
  `docflow.publicacao.timeout-minutos` (padrão 30), marcando-as como ERRO.
- Cancelamento é cooperativo: `cancelamento_solicitado` é verificado antes de persistir o
  resultado; o ZIP já escrito é descartado e o status vira `CANCELADA`.
- O stream SSE de publicação respeita o escopo de cliente do assinante, fotografado na
  inscrição. O stream de página não é de cliente e vai para todos os assinantes.
- Anexos: o `Content-Type` do cliente não basta — o arquivo precisa decodificar como imagem
  (SVG é recusado). A remoção do arquivo acontece após o commit.
- A busca de páginas usa a coluna gerada `search_vector` (índice GIN) combinada com LIKE,
  que cobre termos parciais.
- Duplicar página copia subpáginas, anexos (arquivo em disco) e reaponta os links do HTML.
- Reordenar respeita a ordem dos ids enviados e só grava `ordem`, sem incrementar a `version`
  de quem não mudou.
- Revisão pode ter responsável e prazo. Com responsável definido, só ele aprova a página.
- Decidir sobre a revisão (aprovar, devolver, assumir, comentar) exige `PAGINA:APROVAR`,
  ação própria do catálogo RBAC concedida a ADMIN, EDITOR e REVISOR. Editar, criar, excluir
  e publicar continuam sob `PAGINA:EDITAR`/`PAGINA:CRIAR`/`PAGINA:EXCLUIR` — o revisor decide
  sem poder alterar o conteúdo.
- Snippets (`{{snippet:CODIGO}}`) são resolvidos na geração do pacote, no preview e no PDF —
  nunca ficam no conteúdo salvo. Código inexistente vira aviso visível.
- O checklist de qualidade bloqueia o envio para revisão quando a página cita um trecho
  inexistente ou inativo; a listagem de trechos informa quantas páginas citam cada um.
- O watchdog também remove diretórios em `publicacoes/tmp` deixados por geração interrompida.
- O preview público é limitado por token (`docflow.preview.limite-por-minuto`, padrão 30) e o
  download de anexo é registrado na auditoria.
- `GET /publicacoes/{id}/paginas/{paginaId}/html` devolve o HTML arquivado no pacote, usado
  para comparar o conteúdo de duas publicações.
- O diff entre publicações compara o hash do conteúdo por página gravado no snapshot
  (ADICIONADA / REMOVIDA / ALTERADA / MOVIDA / INALTERADA / INDETERMINADA).
- O PDF normaliza HTML5 para XHTML com Jsoup antes do OpenHTMLtoPDF. O teste integrado
  `PublicacaoDownloadIntegrationTest` sobe PostgreSQL real, gera a publicação e valida ZIP e PDF.

## Pacote AI-ready e base RAG

- O ZIP da publicação inclui `llms.txt`, `llms-full.txt`, `rag/<projeto>/<CODIGO>.md` (frontmatter
  YAML) e `rag/index.json` (sha256 por arquivo). Implementação em `ManualRagService` e
  `PaginaMarkdownConverter`.
- Base RAG por projeto: `GET /api/v1/docflow/projetos/{id}/rag.zip` (`ProjetoRagService`).
- Deep link `?tela=CODIGO` no pacote (via `assets/routes.js`, funciona em `file://`) e na prévia
  por token.
- Perguntar ao manual publicado e MCP (Onda E): `ManualCorpusService` (snapshot do ZIP),
  `ManualBusca` (código + BM25), `ManualMcpController` (`/api/v1/docflow/mcp`, token de prévia);
  a resposta com IA fica no módulo `ai` (`AiManualPerguntaService`). Guia em
  `docs/ai/PERGUNTAS-MANUAL.md`.
- Manual nos sistemas do cliente (Onda D): chave `nxm_…` (`ManualAcessoService`, V46),
  `ManualPublicoController` (`/api/v1/manual/{chave}/vigente|tela|site/**`, `help-bridge.js`),
  `ManualLeitorService` (chave ou link de prévia) e CORS por origem das chaves
  (`OrigensManualCors`).
- Página `tipo` ARTIGO ou MENU (V47): menu é pasta/capítulo, tem checklist próprio (mínimo 20),
  ganha a lista "Nesta seção" no pacote e não entra nas respostas da IA (`ManualCorpusService`).
- Tema do cliente: o padrão do produto é aplicado no construtor; cor não faz parte do contrato.
- Anexo guarda `codigo_tela` e `seletor` (V48): a Mídia filtra por tela e a fila de release diz
  quantas capturas podem ter envelhecido.
- Eventos do leitor (Onda F): `POST /api/v1/manual/{token}/eventos` grava BUSCA,
  BUSCA_SEM_RESULTADO e PAGINA_ABERTA em `tb_manual_evento` (V49), sem PII; o dashboard
  (`/dashboard/resumo`) mostra as lacunas dos últimos 30 dias.
- Release publicada no Orchestrator emite `ReleasePublicadaEvento` (shared): a página da tela
  citada é marcada `desatualizada_por` (V50, limpa ao publicar) e a fila da IA ganha um item
  `RELEASE` em `PARA_REVISAR` (V51) — a IA só gera o ajuste se alguém pedir.
- Detalhes em `docs/doc-flow/12-proximos-passos-integracoes.md` (Ondas A–F).

## Contrato OpenAPI

- Documento JSON: `GET /v3/api-docs`.
- Interface Swagger: `/swagger-ui.html`.
- Ambos ficam habilitados por padrão em desenvolvimento e desabilitados por padrão no
  profile `prod`; use `SPRINGDOC_ENABLED=true` para habilitação controlada.
- A autenticação Bearer JWT é declarada globalmente no contrato.
- O snapshot consumido pelo frontend fica em
  `nexus-portal-web/frontend/openapi/nexus-portal-api.json`.

## Assistente Nexus AI

**Backend** em módulo Maven `ai/` (`nexus-ai`); **UI** no DocFlow web (`modules/docflow/`):

- Pacote: `com.nexus.portal.ai.*`
- API: `/api/v1/ai/**`
- Config: `nexus.ai.*` / `NEXUS_AI_*` (OpenRouter por padrão)
- Acoplamento DocFlow só em `com.nexus.portal.ai.integration.docflow.DocFlowAiBridge`
- Front: `/doc-flow/assistente`, `/doc-flow/propostas-ia` (não há módulo Angular separado)
- Docs: `docs/ai/README.md`, `docs/ai/RUNBOOK-LOCAL.md`, `docs/doc-flow/10-assistente-ia-paginas.md`
- Auditoria: entidades `AI_SESSAO` / `AI_PROPOSTA`
- A `PageSpec` v2 registra `blueprintId`; a seleção mantém componentes obrigatórios/recomendados
  e inclui opcionais somente quando o briefing fornece evidência.
- A IA **nunca publica** — só rascunho/proposta

## Observações para IA

Ao gerar código para este módulo:

- Usar pacote `com.nexus.portal.docflow.{camada}`.
- Não criar sub-pacotes por contexto dentro das camadas.
- Não usar `modules` no caminho do pacote.
- Priorizar simplicidade.
- Não criar arquitetura hexagonal.
- Usar services claros.
- Usar DTOs.
- Não expor Entity diretamente.
- Código do assistente → módulo `ai/`, não `docflow/`.
