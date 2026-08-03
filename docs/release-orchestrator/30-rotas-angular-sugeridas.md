# 30 — Rotas Angular Sugeridas

## 1. Convenção

- Prefixo do módulo orchestrator: `/orchestrator`.
- Prefixo do release flow (existente): `/release-orchestrator`.
- Snake-kebab nas rotas.
- IDs sempre `:id` (UUID).
- Sub-recursos com path completo.

---

## 2. Rotas principais do Orchestrator

```text
/orchestrator                                            → redirect /orchestrator/dashboard
/orchestrator/dashboard                                  → 01 Dashboard

/orchestrator/clientes                                   → 02 Listagem
/orchestrator/clientes/novo                              → 03 Cadastro
/orchestrator/clientes/:id                               → 04 Visão geral
/orchestrator/clientes/:id/editar                        → 03 Cadastro (edit)
/orchestrator/clientes                                   → 02 Listagem
/orchestrator/clientes/resumo-funcionalidades             → 05 §8 Visão consolidada por produto
/orchestrator/clientes/:id/dominios-funcionalidades      → 05 Matriz do cliente
/orchestrator/clientes/:id/produtos                      → 06
/orchestrator/clientes/:id/configuracao-entrega          → 07

/orchestrator/produtos                                   → 08 Listagem
/orchestrator/produtos/novo                              → 09 Cadastro
/orchestrator/produtos/:id                               → (detalhe → tabs)
/orchestrator/produtos/:id/editar                        → 09 Cadastro (edit)
/orchestrator/produtos/:id/modulos                       → 10 Catálogo de módulos
/orchestrator/produtos/:id/catalogo-funcional            → 11 Catálogo domínios/funcionalidades

/orchestrator/proximas-entregas                          → 16 Agenda
/orchestrator/proximas-entregas/nova                     → 17 Cadastro
/orchestrator/proximas-entregas/:id                      → 17 Detalhe
/orchestrator/proximas-entregas/:id/editar               → 17 Cadastro (edit)

/orchestrator/entregas                                   → 23 Histórico
/orchestrator/entregas/nova                              → 18 Wizard
/orchestrator/entregas/:id                               → 22 Detalhes
/orchestrator/entregas/:id/modulos                       → 19 Seleção
/orchestrator/entregas/:id/delta                         → 20 Delta
/orchestrator/entregas/:id/geracao                       → 21 Geração
/orchestrator/entregas/:id/documento                     → 25 Documento

/orchestrator/documentos/templates                       → 24 Templates
/orchestrator/documentos/templates/novo                  → form
/orchestrator/documentos/templates/:id                   → detalhe

/orchestrator/relatorios                                 → 26 Relatórios
/orchestrator/relatorios/:tipo                           → tipo específico

/orchestrator/suporte                                    → 27 Suporte
/orchestrator/suporte/falhas                             → aba
/orchestrator/suporte/logs                               → aba

/orchestrator/configuracoes                              → 28 Configurações
/orchestrator/configuracoes/:aba                         → aba específica
```

---

## 3. Rotas Release Orchestrator existentes

```text
/release-orchestrator                                            → Dashboard
/release-orchestrator/builder                                    → Release Builder
/release-orchestrator/releases                                   → Listagem
/release-orchestrator/releases/nova                              → Form criação
/release-orchestrator/releases/:id                               → Detalhe
/release-orchestrator/releases/:id/editar                        → Form edição
/release-orchestrator/releases/:id/revisao                       → Tela de revisão
/release-orchestrator/produtos                                   → Listagem produtos
/release-orchestrator/templates                                  → Listagem templates
/release-orchestrator/guia                                       → Guia
```

> Sugestão: renomear `RELEASE_ORCHESTRATOR_ROUTES` → `RELEASE_ORCHESTRATOR_ROUTES` (ver `../naming-suggestions.md`).

---

## 4. Estrutura no Angular

### Arquivo principal
`frontend/src/app/app.routes.ts`:

```ts
import { Routes } from '@angular/router';
import { authGuard } from '@core/auth/guards/auth.guard';
import { roleGuard } from '@core/auth/guards/role.guard';

export const APP_ROUTES: Routes = [
  // ... outras rotas
  {
    path: 'release-orchestrator',
    canActivate: [authGuard],
    loadChildren: () => import('@modules/release-orchestrator/release-orchestrator.routes')
      .then(m => m.RELEASE_ORCHESTRATOR_ROUTES)
  },
  {
    path: 'orchestrator',
    canActivate: [authGuard],
    loadChildren: () => import('@modules/orchestrator/orchestrator.routes')
      .then(m => m.ORCHESTRATOR_ROUTES)
  }
];
```

### Módulo orchestrator
`frontend/src/app/modules/orchestrator/orchestrator.routes.ts`:

```ts
export const ORCHESTRATOR_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./shell/orchestrator-shell.component')
      .then(m => m.OrchestratorShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component')
          .then(m => m.DashboardComponent) },

      // Clientes
      { path: 'clientes',
        loadComponent: () => import('./pages/clientes/lista/clientes-lista.component')
          .then(m => m.ClientesListaComponent) },
      { path: 'clientes/novo',
        canActivate: [roleGuard('ADMIN', 'EDITOR')],
        loadComponent: () => import('./pages/clientes/form/cliente-form.component')
          .then(m => m.ClienteFormComponent) },
      { path: 'clientes/:id',
        loadComponent: () => import('./pages/clientes/visao-geral/cliente-visao-geral.component')
          .then(m => m.ClienteVisaoGeralComponent) },
      { path: 'clientes/:id/editar',
        canActivate: [roleGuard('ADMIN', 'EDITOR')],
        loadComponent: () => import('./pages/clientes/form/cliente-form.component')
          .then(m => m.ClienteFormComponent) },
      { path: 'clientes/:id/dominios-funcionalidades',
        loadComponent: () => import('./pages/clientes/dominios/dominios-funcionalidades.component')
          .then(m => m.DominiosFuncionalidadesComponent) },
      { path: 'clientes/:id/produtos',
        loadComponent: () => import('./pages/clientes/produtos/cliente-produtos.component')
          .then(m => m.ClienteProdutosComponent) },
      { path: 'clientes/:id/configuracao-entrega',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () => import('./pages/clientes/configuracao/configuracao-entrega.component')
          .then(m => m.ConfiguracaoEntregaComponent) },

      // Produtos
      { path: 'produtos',
        loadComponent: () => import('./pages/produtos/lista/produtos-lista.component')
          .then(m => m.ProdutosListaComponent) },
      { path: 'produtos/:id/catalogo-funcional',
        canActivate: [roleGuard('ADMIN', 'EDITOR', 'LEITOR')],
        loadComponent: () => import('./pages/produtos/catalogo/catalogo-funcional.component')
          .then(m => m.CatalogoFuncionalComponent) },
      // ... resto

      // Próximas entregas
      // Entregas (wizard + detalhes)
      // Documentos
      // Relatórios
      // Suporte
      // Configurações
    ]
  }
];
```

---

## 5. Lazy loading hierárquico

```text
app.routes.ts
  ├─ release-orchestrator.routes.ts        (lazy)
  └─ orchestrator.routes.ts        (lazy)
       ├─ dashboard                (lazy via loadComponent)
       ├─ clientes/*               (lazy)
       ├─ produtos/*               (lazy)
       ├─ entregas/*               (lazy)
       └─ ...
```

Cada `loadComponent` baixa só o chunk necessário.

---

## 6. Guards

### authGuard (já existe)
Bloqueia rotas sem token.

### roleGuard (sugerido)
```ts
export function roleGuard(...rolesPermitidas: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    if (rolesPermitidas.some(r => auth.temRole(r))) return true;
    return inject(Router).parseUrl('/orchestrator/dashboard');
  };
}
```

### discardChangesGuard (CanDeactivate)
Para forms com mudanças não salvas.

```ts
export const discardChangesGuard: CanDeactivateFn<HasDirtyForm> = component => {
  return component.podemSair() ? true : confirm('Descartar mudanças?');
};
```

---

## 7. Resolvers

### clienteResolver
Pré-carrega cliente antes de mostrar detalhe (evita flash).

```ts
export const clienteResolver: ResolveFn<Cliente> = route => {
  const id = route.paramMap.get('id')!;
  return inject(ClienteService).buscar(id);
};
```

Aplicado:
```ts
{
  path: 'clientes/:id',
  resolve: { cliente: clienteResolver },
  loadComponent: ...
}
```

### entregaResolver
Idem para entregas.

---

## 8. Query state

Filtros e paginação preservados em querystring.

```ts
// Exemplo: rota /orchestrator/entregas?status=CONCLUIDA&periodo=ULTIMOS_30
this.route.queryParams.subscribe(qp => {
  this.filtros.set({
    status: qp['status'],
    periodo: qp['periodo']
  });
});

// Ao mudar filtro:
this.router.navigate([], {
  queryParams: { status: 'EM_GERACAO' },
  queryParamsHandling: 'merge'
});
```

---

## 9. Títulos

```ts
{
  path: 'clientes',
  title: 'Clientes — Orchestrator',
  loadComponent: ...
}
```

Para títulos dinâmicos: `TitleStrategy` custom.

---

## 10. Estrutura de pastas Angular sugerida

```text
frontend/src/app/modules/orchestrator/
├── orchestrator.routes.ts
├── shell/
│   └── orchestrator-shell.component.ts
├── pages/
│   ├── dashboard/
│   ├── clientes/
│   │   ├── lista/
│   │   ├── form/
│   │   ├── visao-geral/
│   │   ├── dominios/
│   │   ├── produtos/
│   │   └── configuracao/
│   ├── produtos/
│   │   ├── lista/
│   │   ├── form/
│   │   └── modulos/
│   ├── proximas-entregas/
│   ├── entregas/
│   │   ├── wizard/
│   │   ├── selecao-modulos/
│   │   ├── delta/
│   │   ├── geracao/
│   │   ├── detalhes/
│   │   └── documento/
│   ├── documentos/
│   ├── relatorios/
│   ├── suporte/
│   └── configuracoes/
├── services/
│   ├── cliente.service.ts
│   ├── produto.service.ts
│   ├── entrega.service.ts
│   ├── proxima-entrega.service.ts
│   ├── relatorio.service.ts
│   └── ...
├── models/
│   ├── cliente.model.ts
│   ├── produto.model.ts
│   ├── entrega.model.ts
│   └── ...
├── guards/
│   ├── role.guard.ts
│   └── discard-changes.guard.ts
├── resolvers/
│   ├── cliente.resolver.ts
│   └── entrega.resolver.ts
└── components/
    └── (componentes específicos do módulo)
```

---

## 11. Boas práticas

- **Standalone components** sempre.
- **loadComponent** em vez de loadChildren onde possível (mais granular).
- **Resolvers** para evitar flash de loading em telas críticas.
- **Guards** para autorização e proteção de dados.
- **Title** em cada rota para SEO/UX.
- **Query state** preservado.

---

## 12. Integração com release flow

O orchestrator referencia muito o release flow. Rotas relevantes:

```text
Orchestrator → Release Orchestrator (navegação)
├─ /orchestrator/entregas/nova step 3 → /release-orchestrator/releases/:id (preview da release)
├─ /orchestrator/produtos/:id → /release-orchestrator/releases?produtoId=X (releases do produto)
└─ /orchestrator/entregas/:id → /release-orchestrator/releases/:releaseId (release de origem)
```

Atalhos via botões. Não nest rotas.

---

## 13. Mobile (futuro)

Hoje desktop-first. Quando mobile entrar:
- Sidebar vira drawer.
- Tabelas viram cards.
- Wizard fullscreen.

---

## 14. Cross-reference

- [`../../nexus-portal-web/docs/release-orchestrator/01-shell-navegacao.md`](../../nexus-portal-web/docs/release-orchestrator/01-shell-navegacao.md) — Shell e navegação do release flow.
- [`../../nexus-portal-web/docs/release-orchestrator/`](../../nexus-portal-web/docs/release-orchestrator/README.md) — Documentação do frontend (por tela).
- [`../naming-suggestions.md`](../naming-suggestions.md) — Renomeações.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
