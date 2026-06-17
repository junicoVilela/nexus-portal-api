# 31 — Guia para Figma / Handoff Visual

## 1. Objetivo

Orientar a **criação das telas no Figma** (ou outra ferramenta visual) para o orchestrator. Garantir consistência visual entre release flow e orchestrator, e facilitar handoff design → desenvolvimento.

---

## 2. Padrão visual geral

### Layout
- **Desktop-first**: design pensado para 1920×1080, deve funcionar em 1366×768.
- **Layout 4:3** preferível para apresentação em telas operacionais.
- **Sidebar fixa** à esquerda com navegação.
- **Topbar** com identificação do usuário, logout, busca global (futuro).
- **Área principal** ocupa o restante.

### Visual style
- **Sidebar escura** (`#0f172a` ou similar) com texto claro.
- **Área principal clara** (`#f9fafb` background, `#ffffff` cards).
- **Cards** com sombra leve (`box-shadow: 0 1px 3px rgba(0,0,0,0.1)`).
- **Bordas arredondadas** (4-8px).
- **Espaçamento generoso** (16-24px entre seções).

### Cores semânticas
| Cor | Hex | Uso |
|---|---|---|
| Primary | `#2563eb` | Ações principais, links, badge default |
| Success | `#16a34a` | Status concluído, sucesso |
| Warning | `#d97706` | Avisos, atrasos, pendências |
| Danger | `#dc2626` | Erros, ações destrutivas, falhas |
| Info | `#0891b2` | Mensagens informativas |
| Text | `#0f172a` | Texto principal |
| Muted | `#6b7280` | Texto secundário, labels |
| Border | `#e5e7eb` | Bordas, separadores |

### Tipografia
- **Sans-serif** moderna (Inter, Roboto, ou similar).
- **Tamanhos**:
  - h1: 32px / 700
  - h2: 24px / 600
  - h3: 20px / 600
  - h4: 16px / 600
  - body: 14px / 400
  - caption: 12px / 400
- **Line height**: 1.5 para body.
- **Letter spacing**: 0 para body, negativo leve em headings grandes.

### Ícones
- **PrimeIcons** (já usado no release flow).
- Tamanho default: 16px.
- Cor herda do contexto.

---

## 3. Componentes principais (Figma)

Criar como **componentes reutilizáveis** no Figma:

| Componente | Variantes | Uso |
|---|---|---|
| **Sidebar** | Expandida/Colapsada/Dark | Layout |
| **Topbar** | Default/Com busca | Layout |
| **Card KPI** | Default/Com trend/Com alerta | Dashboard |
| **Tabela** | Default/Com filtros/Vazia/Loading | Listagens |
| **Wizard de etapas** | 3 etapas/5 etapas/Com sub-etapa | Nova entrega |
| **Timeline de progresso** | Vertical/Horizontal | Geração |
| **Modal de confirmação** | Default/Destrutivo/Com input | Confirmações |
| **Drawer de detalhes** | Estreito/Largo | Inspeção rápida |
| **Badge de status** | 8+ variantes (por cor) | Indicadores |
| **Toggle de funcionalidade** | On/Off/Disabled | Matriz |
| **Editor Markdown** | Editor/Preview/Split | Documentos |
| **Preview PDF** | Loading/Renderizado/Erro | Documentos |
| **Form field** | Input/Select/Date/Textarea | Forms |
| **Botão** | Primary/Secondary/Danger/Ghost | Ações |
| **Toast/Snackbar** | Success/Error/Warning/Info | Feedback |
| **Empty state** | Genérico/Por tipo | Listas vazias |
| **Loading state** | Skeleton/Spinner | Carregando |
| **Error state** | Inline/Página inteira | Erros |

---

## 4. Telas prioritárias para desenho (sequência de criação)

### Fase A — Foundations
1. **Sidebar + Topbar** (layout shell global).
2. **Componentes base** (botões, inputs, badges).

### Fase B — Telas centrais
3. **Dashboard** (`01`).
4. **Clientes — Listagem** (`02`).
5. **Cadastro de Cliente** (`03`).
6. **Cliente — Visão Geral** (`04`).
7. **Domínios e Funcionalidades** (`05`).

### Fase C — Fluxo do produto
8. **Produtos — Listagem** (`08`).
9. **Cadastro de Produto** (`09`).
10. **Catálogo de Módulos** (`10`).

### Fase D — Fluxo de entrega
11. **Próximas Entregas — Agenda** (`16`).
12. **Cadastro de Próxima Entrega** (`17`).
13. **Nova Entrega — Wizard** (`18`).
14. **Seleção de Módulos** (`19`).
15. **Geração de Pacote** (`21`).
16. **Detalhes da Entrega** (`22`).

### Fase E — Documentos e suporte
17. **Documento MD → PDF** (`25`).
18. **Histórico de Entregas** (`23`).
19. **Suporte Operacional** (`27`).
20. **Relatórios** (`26`).

### Fase F — Configurações
21. **Configurações** (`28`).
22. **Templates** (`24`).

> **Importante**: telas devem ser desenhadas na **sequência real de uso** para facilitar validação com usuários e operadores.

---

## 5. Estrutura de páginas no Figma

### Páginas
```text
1. Foundations
   ├─ Cores
   ├─ Tipografia
   ├─ Ícones
   ├─ Espaçamento
   └─ Sombras

2. Components
   ├─ Sidebar
   ├─ Topbar
   ├─ Botões
   ├─ Inputs
   ├─ Tabelas
   ├─ Cards
   ├─ Modais
   └─ Feedback

3. Layouts
   ├─ Shell padrão
   ├─ Shell com sub-shell
   └─ Mobile (futuro)

4. Telas — Orchestrator
   ├─ Dashboard
   ├─ Clientes
   ├─ Produtos
   ├─ Próximas Entregas
   ├─ Entregas
   ├─ Documentos
   ├─ Relatórios
   ├─ Suporte
   └─ Configurações

5. Telas — Release Orchestrator (referência)
   └─ Já existentes

6. Estados especiais
   ├─ Loading
   ├─ Empty
   ├─ Error
   └─ Sem permissão

7. Mobile (futuro)

8. Archive
```

---

## 6. Convenções de nomenclatura

### Componentes
- `Btn/Primary`, `Btn/Secondary` — prefixo por categoria.
- `Badge/Status/Success`, `Badge/Status/Error` — hierarquia.
- `Form/Field/Input`, `Form/Field/Select` — agrupado.

### Variantes
- Usar `Property Name=Value` (ex: `Type=Primary`, `Size=Medium`, `State=Hover`).

### Frames
- Cada tela = 1 frame nomeado conforme o número da spec: `01-Dashboard`, `02-Clientes-Lista`.

---

## 7. Estados a desenhar por tela

Para cada tela principal, desenhar:
- **Default**: estado normal com dados.
- **Loading**: skeleton.
- **Empty**: sem dados.
- **Filtered Empty**: filtro sem match.
- **Error**: erro de carregamento.
- **No permission**: sem acesso (se aplicável).

---

## 8. Auto layout

Usar **Auto Layout** do Figma sempre:
- Espaçamento vertical e horizontal consistente.
- Resizing inteligente.
- Facilita atualizações.

---

## 9. Handoff

### Tokens de design
Exportar como JSON ou direto para CSS:

```json
{
  "color": {
    "primary": "#2563eb",
    "success": "#16a34a"
  },
  "spacing": {
    "xs": 4, "sm": 8, "md": 16, "lg": 24, "xl": 32
  },
  "radius": {
    "sm": 4, "md": 8, "lg": 12
  }
}
```

### Documentação inline
Cada componente do Figma deve ter:
- Descrição do propósito.
- Variantes e quando usar cada uma.
- Estados (hover, focus, disabled).
- Linkagem com a spec (`02-clientes-lista.md`).

---

## 10. Validação com usuários

Antes de desenvolver:
- **Apresentar telas em sequência** simulando fluxo real.
- **Validar com operador** que vai usar.
- **Validar com administrador** que vai configurar.
- **Capturar feedback** e iterar.

---

## 11. Consistência com Release Orchestrator

Telas do orchestrator devem **espelhar visualmente** as do release flow:
- Mesma sidebar.
- Mesmas badges de status.
- Mesma estrutura de tabelas.
- Mesma estrutura de forms.

Diferenças justificadas:
- Sidebar do orchestrator tem itens próprios.
- Wizard de nova entrega é tela diferente.

---

## 12. Acessibilidade no design

- **Contraste**: garantir 4.5:1 mínimo.
- **Focus indicators**: visíveis e consistentes.
- **Touch targets**: mínimo 44px em mobile.
- **Não depender só de cor** para significado.

---

## 13. Responsividade

### Desktop (default)
- 3-4 colunas em listas/cards.
- Sidebar fixa.

### Notebook (1366px)
- 2 colunas.
- Sidebar fixa.

### Tablet (768-1024px)
- 1-2 colunas.
- Sidebar colapsável.

### Mobile (<768px) — futuro
- 1 coluna.
- Sidebar como drawer.

---

## 14. Cross-reference

- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões UX.
- [`30-rotas-angular-sugeridas.md`](30-rotas-angular-sugeridas.md) — Rotas.
- [`../../softon-portal-web/docs/release-orchestrator/`](../../softon-portal-web/docs/release-orchestrator/README.md) — Documentação do frontend (componentes, estados de UI, por tela).
