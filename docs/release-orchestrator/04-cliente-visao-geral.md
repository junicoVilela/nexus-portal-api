# 04 — Cliente — Visão Geral

## 1. Papel da tela

**Página de aterrissagem** quando um cliente é selecionado da listagem. Resumo operacional consolidado + ponto de entrada para todas as ações relacionadas ao cliente.

Acessível em `/orchestrator/clientes/:id`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Identificar o cliente rapidamente | Cabeçalho com sigla, nome, status |
| Ver dados-chave de operação | Cards no topo (última entrega, próxima, produtos) |
| Acessar configurações específicas | Abas: Domínios, Produtos, Configurações, Histórico |
| Iniciar ações comuns | Botões: Nova entrega, Editar, Pausar |
| Visualizar saúde da relação com o cliente | Painel de alertas |
| Auditar mudanças recentes | Histórico do cliente |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar para Clientes                                                 │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  🟢 ACME LTDA  (ACME)                          [Nova entrega] [⋮ Mais] │
│  ACME Comércio Ltda • CNPJ 12.345.678/0001-99                          │
│                                                                        │
│  Cliente desde 15/01/2025 • Responsável: Maria Santos                  │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ [⚠️ 1 alerta] Próxima entrega planejada para 20/05/2026 está atrasada  │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Última entrega ─────┐ ┌─ Próxima ──────────┐ ┌─ Produtos ────────┐ │
│ │ DTEC-LD v1.4.0        │ │ DTEC-LD v1.5.0      │ │ 3 ativos          │ │
│ │ 15/05/2026            │ │ Atrasada (5 dias)   │ │ 12 módulos        │ │
│ │ ✅ CONCLUIDA          │ │ 🟡 APROVADA         │ │ Ver detalhes →    │ │
│ │ Ver detalhes →        │ │ Ver agenda →        │ │                   │ │
│ └───────────────────────┘ └─────────────────────┘ └───────────────────┘ │
├────────────────────────────────────────────────────────────────────────┤
│ [Visão Geral] [Domínios] [Produtos] [Configurações] [Histórico]        │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│ ┌─ Próximas entregas ────────────┐ ┌─ Últimas entregas ──────────────┐ │
│ │ DTEC-LD v1.5.0 - hoje (atras.) │ │ DTEC-LD v1.4.0 - 15/05/2026 ✅  │ │
│ │ FOLHA v2.1.0 - +5 dias         │ │ DTEC-LD v1.3.0 - 10/04/2026 ✅  │ │
│ │                                │ │ DTEC-LD v1.2.0 - 02/03/2026 ✅  │ │
│ │ [Ver agenda →]                 │ │ [Ver histórico →]               │ │
│ └────────────────────────────────┘ └─────────────────────────────────┘ │
│                                                                        │
│ ┌─ Produtos contratados ───────────────────────────────────────────┐  │
│ │ DTEC-LD  (PROD)   • 5 módulos    Última: v1.4.0      [Configurar]│  │
│ │ FOLHA    (PROD)   • 4 módulos    Última: v2.0.0      [Configurar]│  │
│ │ CONTAS   (HOM)    • 3 módulos    Última: v0.9.0      [Configurar]│  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Alertas operacionais ──────────────────────────────────────────┐  │
│ │ ⚠️  Próxima entrega DTEC-LD v1.5.0 atrasada (planejada 20/05)   │  │
│ │ ℹ️  Janela de manutenção do cliente: 22h-6h                      │  │
│ └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Cabeçalho

### Identidade
- **Indicador de status** (ícone colorido).
- **Sigla** em destaque + nome.
- **Razão social** + CNPJ formatado.

### Meta-informações
- Cliente desde: data + tempo relativo ("há 1 ano e 4 meses").
- Responsável: nome do responsável comercial + papel.

### Ações primárias (botão direito)
- **Nova entrega**: atalho para `18-nova-entrega-assistente.md` com cliente pré-selecionado.
- **⋮ Mais** (menu): Editar, Pausar/Reativar, Encerrar (com confirm dupla), Exportar dados.

### Permissões
- VIEWER: vê tudo, mas botões de ação ocultos.
- EDITOR: pode editar, criar entrega.
- ADMIN: pode pausar, encerrar.

---

## 5. Banner de alertas

Quando o cliente tem **alertas ativos**, banner colorido aparece logo abaixo do cabeçalho.

### Tipos de alerta

| Severidade | Cor | Exemplos |
|---|---|---|
| 🔴 CRITICAL | Vermelho | Última entrega FALHOU; sem produto contratado |
| 🟡 WARN | Amarelo | Entrega atrasada; credencial expirando; janela vencida |
| 🔵 INFO | Azul | Janela próxima; release nova disponível |

Click no alerta → vai para tela relacionada.

Banner agrupa alertas: "1 alerta crítico, 2 avisos" expansível.

---

## 6. Cards superiores (KPIs do cliente)

### Última entrega
- Versão entregue, produto, data, status.
- Status visual (✅ concluída, ❌ falha, ⚠️ pendente).
- Click → vai para `22-detalhes-entrega.md`.

### Próxima entrega
- Versão, produto, data planejada.
- Status (PLANEJADA, APROVADA).
- Indicação de atraso se aplicável.
- Click → vai para `17-proximas-entregas-cadastro.md` modo edit.

### Produtos
- Total ativo + total de módulos.
- Click → vai para aba "Produtos".

---

## 7. Abas

### Aba 1: Visão Geral (default)
Conteúdo descrito acima — painéis sintéticos.

### Aba 2: Domínios e Funcionalidades
- Renderiza `05-cliente-dominios-funcionalidades.md`.

### Aba 3: Produtos
- Renderiza `06-cliente-produtos-contratados.md`.

### Aba 4: Configurações
- Renderiza `07-cliente-configuracoes-entrega.md`.
- Edição de dados gerais também aqui (botão "Editar dados gerais").

### Aba 5: Histórico
- Auditoria do cliente: quem alterou o quê.
- Filtro por tipo de ação, período, usuário.

### Estado da aba
- Aba ativa persistida na URL: `?tab=produtos`.
- Default = visao-geral.

### Lazy loading
- Cada aba carrega seus dados ao ser ativada (não tudo no load inicial).

---

## 8. Painéis (na aba Visão Geral)

### Próximas entregas
- Top 5 próximas entregas planejadas/aprovadas para este cliente.
- Mesma estrutura que dashboard global mas filtrado.
- Click → vai para agenda filtrada.

### Últimas entregas realizadas
- Top 5 entregas concluídas mais recentes.
- Mostra: versão, produto, data, sucesso/falha.
- Click → `22-detalhes-entrega.md`.

### Produtos contratados (resumo)
- Lista todos os produtos contratados ativos.
- Por produto: ambiente, número de módulos, última versão entregue.
- Click "Configurar" → vai para tela de módulos do produto.

### Alertas operacionais
- Conteúdo já mostrado no banner, mas detalhado.
- Cada alerta: causa, impacto, ação sugerida.
- "Ignorar este alerta" (registra que foi reconhecido).

---

## 9. Contratos de API

### Buscar resumo do cliente

```
GET /api/v1/orchestrator/clientes/{id}/visao-geral
```

Retorna **dados agregados** para essa tela em 1 request (evita N chamadas):

```json
{
  "cliente": { ...ClienteResponse... },
  "ultimaEntrega": {
    "id": "uuid",
    "produtoSigla": "DTECLD",
    "versao": "1.4.0",
    "dataPublicacao": "2026-05-15",
    "status": "CONCLUIDO"
  },
  "proximaEntrega": {
    "id": "uuid",
    "produtoSigla": "DTECLD",
    "versao": "1.5.0",
    "dataPlanejada": "2026-05-20",
    "status": "APROVADA",
    "atrasada": true
  },
  "produtosResumo": {
    "totalAtivos": 3,
    "totalModulos": 12
  },
  "proximasEntregas": [ ...5 itens... ],
  "ultimasEntregas": [ ...5 itens... ],
  "produtosContratados": [
    {
      "produtoId": "uuid",
      "produtoSigla": "DTECLD",
      "ambiente": "PROD",
      "totalModulos": 5,
      "ultimaVersaoEntregue": "1.4.0"
    }
  ],
  "alertas": [
    {
      "tipo": "ENTREGA_ATRASADA",
      "severidade": "WARN",
      "mensagem": "Próxima entrega DTEC-LD v1.5.0 atrasada (planejada para 2026-05-20).",
      "entidadeRelacionadaId": "uuid-proxima-entrega"
    }
  ]
}
```

### Endpoint por aba (carregamento sob demanda)

```
GET /api/v1/orchestrator/clientes/{id}/produtos
GET /api/v1/orchestrator/clientes/{id}/dominios-funcionalidades
GET /api/v1/orchestrator/clientes/{id}/configuracao-entrega
GET /api/v1/orchestrator/clientes/{id}/historico?page=1&size=20
```

Detalhes nos specs respectivos (05, 06, 07).

---

## 10. Regras de navegação

### Click em produto contratado
- Vai para `06-cliente-produtos-contratados.md` filtrado por esse produto.
- Botão "Configurar módulos" → tela específica.

### "Nova entrega"
- Atalho para `18-nova-entrega-assistente.md`.
- Pré-preenche `clienteId`.
- Se cliente tem 1 produto: pré-preenche produto também.
- Se múltiplos produtos: passa para passo de seleção de produto no wizard.

### "Editar"
- Vai para `03-clientes-cadastro.md` modo edit.
- Após salvar, volta para visão geral.

### "Pausar"
- Confirm modal: "Pausar Cliente ACME?".
- Pede motivo.
- Atualiza status. Banner muda para "Cliente pausado. Não receberá novas entregas."

### "Encerrar"
- Confirm dupla: modal pedindo para digitar a sigla do cliente.
- Pede motivo + data efetiva.
- Status muda para ENCERRADO (terminal).
- Cliente some das listagens default mas continua acessível por busca.

---

## 11. Performance

### Endpoint agregado
- `/visao-geral` é otimizado para 1 round-trip.
- Caches internos (`@Cacheable`) com TTL 30s.

### Carregamento de abas
- Lazy: cada aba só busca quando ativada.
- Mantém dados em memória para alternar abas sem re-fetch.

### Skeleton
- Layout completo carrega skeleton enquanto dados vêm.
- Cabeçalho aparece com dados parciais (cliente já em cache de listagem).

---

## 12. Estados e edge cases

### Cliente recém-cadastrado (EM_ANALISE)
- Banner indicando: "Cliente em análise. Complete o cadastro para ativar."
- Lista de pendências: "Faltam: produtos contratados, configuração de entrega."
- Botão "Nova entrega" desabilitado com tooltip.

### Cliente PAUSADO
- Banner: "Cliente pausado em 2026-04-01 por Maria. Motivo: Revisão contratual."
- Botão "Nova entrega" desabilitado.
- Botão "Reativar" disponível.

### Cliente ENCERRADO
- Banner cinza: "Cliente encerrado em 2026-04-01."
- Todas as ações disabled.
- Aba histórico funcional.

### Cliente sem entregas ainda
- Card "Última entrega" mostra: "Nenhuma entrega ainda."
- Card "Próxima entrega" mostra: "Sem agenda. [Planejar primeira]."
- Painéis vazios com CTA.

### Cliente com erro de carga
- Skeleton + erro inline em cada bloco.
- Painéis independentes.

---

## 13. Auditoria

Acessos ao visualizar dados sensíveis (`AUDITORIA_CONSULTADA`) podem ser registrados se compliance exigir.

Ações via tela:
- Mudança de status → `CLIENTE_STATUS_ALTERADO`.
- Editar → `CLIENTE_EDITADO`.
- Encerrar → `CLIENTE_ENCERRADO`.

---

## 14. Acessibilidade

- Abas com `role="tablist"` + `aria-selected`.
- Banner de alerta com `role="alert"` + `aria-live="polite"`.
- Cards de KPI com `<article>` + `aria-label`.
- Foco mantido ao trocar de aba.

---

## 15. Cross-reference

- [`02-clientes-lista.md`](02-clientes-lista.md) — Listagem.
- [`03-clientes-cadastro.md`](03-clientes-cadastro.md) — Edição.
- [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md) — Aba Domínios.
- [`06-cliente-produtos-contratados.md`](06-cliente-produtos-contratados.md) — Aba Produtos.
- [`07-cliente-configuracoes-entrega.md`](07-cliente-configuracoes-entrega.md) — Aba Configurações.
- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Início de entrega.
- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Detalhe de entrega.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Histórico global.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões aplicáveis.
