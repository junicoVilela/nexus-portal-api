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
POST   /api/v1/paginas
PUT    /api/v1/paginas/{id}
DELETE /api/v1/paginas/{id}

POST   /api/v1/publicacoes
GET    /api/v1/publicacoes/{id}
DELETE /api/v1/publicacoes/{id}
GET    /api/v1/docflow/publicacoes/eventos (SSE autenticado)

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
- O catálogo possui 12 templates visuais. Além de funcionalidade, passo a passo, cadastro,
  consulta, dicionário, processo, FAQ e solução, inclui central de ajuda, relatório,
  índice de categoria e primeiros passos.
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
- O PDF normaliza HTML5 para XHTML com Jsoup antes do OpenHTMLtoPDF. O teste integrado
  `PublicacaoDownloadIntegrationTest` sobe PostgreSQL real, gera a publicação e valida ZIP e PDF.

## Contrato OpenAPI

- Documento JSON: `GET /v3/api-docs`.
- Interface Swagger: `/swagger-ui.html`.
- Ambos ficam habilitados por padrão em desenvolvimento e desabilitados por padrão no
  profile `prod`; use `SPRINGDOC_ENABLED=true` para habilitação controlada.
- A autenticação Bearer JWT é declarada globalmente no contrato.
- O snapshot consumido pelo frontend fica em
  `nexus-portal-web/frontend/openapi/nexus-portal-api.json`.

## Assistente Nexus AI (módulo irmão)

O assistente de páginas **não** vive em `docflow/` — módulo Maven `ai/` (`nexus-ai`):

- Pacote: `com.nexus.portal.ai.*`
- API: `/api/v1/ai/**`
- Config: `nexus.ai.*` / `NEXUS_AI_*` (OpenRouter por padrão)
- Acoplamento DocFlow só em `com.nexus.portal.ai.integration.docflow.DocFlowAiBridge`
- Docs: `docs/ai/README.md`, `docs/ai/RUNBOOK-LOCAL.md`, `docs/doc-flow/10-assistente-ia-paginas.md`
- Auditoria: entidades `AI_SESSAO` / `AI_PROPOSTA`
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
