# 99 — Padrões Gerais de Tela

Critérios que se aplicam a **todas** as telas do orchestrator. Cada spec de tela (01–28) assume estes critérios sem repetir.

> Pareado com a documentação do frontend em `nexus-portal-web/docs/release-orchestrator/` (por tela).

---

## 1. Estados obrigatórios

Toda tela com dados assíncronos deve tratar explicitamente:

| Estado | Quando aparece | Como mostrar |
|---|---|---|
| **Carregando** | Buscando dados pela primeira vez | Skeleton (preferível) ou spinner |
| **Vazio (sem dados)** | Recurso não tem nenhum registro | Ícone + texto + ação principal |
| **Vazio (filtro sem match)** | Filtro aplicado, nenhum resultado | Texto explicativo + "Limpar filtros" |
| **Erro** | Request falhou (4xx, 5xx, network) | Mensagem + retry + correlation ID |
| **Erro de permissão (403)** | Sem role para a tela | Tela "sem permissão" + botão "Voltar" |
| **Sucesso (com dados)** | Render normal | A tela funcional |
| **Salvando** | Ação em curso (mutação) | Botão `loading` + form `disabled` |

### Exemplo — listagem
```
┌──────────────────────────────────┐
│  Filtros                          │
├──────────────────────────────────┤
│  [estado renderizado:]            │
│   - skeleton (carregando)         │
│   - tabela (sucesso)              │
│   - empty state (vazio)           │
│   - error state (erro)            │
├──────────────────────────────────┤
│  Paginação (só se sucesso)        │
└──────────────────────────────────┘
```

---

## 2. Filtros

### Comportamento
- Todos os filtros podem ser limpos individualmente (ícone X).
- Botão **"Limpar tudo"** quando houver ≥ 2 filtros aplicados.
- Filtro aplicado fica visível na tela (chip ou indicador).
- Filtros persistem em **querystring** — refresh mantém estado.
- Mudar filtro reseta a paginação para página 1.

### Filtros multi-valor
- Dropdowns com seleção múltipla quando aplicável.
- Chips visuais para cada valor selecionado.

### Datas
- Date picker para data única.
- Range picker (de/até) para intervalos.
- Atalhos: "Hoje", "Últimos 7 dias", "Este mês", "Mês passado", "Este ano".

### Busca livre
- `q` na querystring.
- Debounce de 400ms.
- Resetar para página 1 ao mudar.

---

## 3. Formulários

### Regras gerais
- Campos obrigatórios sinalizados visualmente (asterisco vermelho).
- Validação no submit + por campo onChange (apenas touched).
- Mensagens de erro em linguagem simples (PT-BR).
- Credenciais e segredos nunca exibidos após salvos (`****` + botão "alterar").
- Submit desabilitado durante request.
- Erro do servidor mapeado para campo via `code` quando possível.

### Layout
- 1 coluna em mobile/tablet.
- 1 ou 2 colunas em desktop, dependendo da densidade.
- Labels acima dos campos (não inline).
- Ação primária à direita, secundária à esquerda.

### Ações
- **Salvar / Salvar e continuar**: ação principal.
- **Cancelar**: volta sem salvar (com confirm se form sujo).
- **Excluir**: destrutivo, em cor de alerta, com confirm.

### Discard guard
- Sair de form com mudanças → confirmação "Descartar mudanças?".
- Aplicar em: cadastro de cliente, produto, configuração.

---

## 4. Auditoria

Toda ação crítica deve registrar histórico com **usuário, data/hora, e diff/contexto**:

| Ação | Registrado em |
|---|---|
| Criar cliente | `orchestrator_auditoria` (acao=CLIENTE_CRIADO) |
| Editar cliente | `orchestrator_auditoria` (acao=CLIENTE_EDITADO) |
| Excluir / inativar | `orchestrator_auditoria` |
| Configurar entrega | `orchestrator_auditoria` (acao=CONFIG_ENTREGA_ALTERADA) |
| Habilitar funcionalidade | `orchestrator_auditoria` |
| Criar próxima entrega | `orchestrator_auditoria` |
| Aprovar próxima entrega | `orchestrator_auditoria` |
| Cancelar próxima entrega | `orchestrator_auditoria` |
| Gerar entrega (criar Entrega) | `orchestrator_auditoria` + entrada de Entrega |
| Reentregar | `orchestrator_auditoria` |
| Reprocessar | `orchestrator_auditoria` |
| Publicar pacote | `orchestrator_auditoria` |
| Alterar credencial | `orchestrator_auditoria` (sem expor valor) |

### Regras
- Append-only — registros nunca são editados ou excluídos individualmente.
- Histórico nunca é apagado fisicamente. Em caso de necessidade (LGPD): anonimizar.
- Sempre captura o usuário autenticado (fallback "system").
- Diff resumido para campos críticos (não logar dados pessoais completos).

### Schema (resumo)
Ver `34-observabilidade.md` §9 para schema completo de `orchestrator_auditoria`.

---

## 5. Responsividade

### Targets
- **Desktop**: 1920×1080 — uso primário.
- **Notebook**: 1366×768 — uso comum, deve funcionar bem.
- **Tablet**: 1024×768 — degradação graciosa.
- **Mobile**: < 768px — não prioritário (operação típica é desktop).

### Padrão de degradação
- Sidebar → drawer overlay em < 1024px.
- Tabelas → cards em < 768px.
- Modais → fullscreen em < 768px.

---

## 6. Autorização

### Princípios
- Toda tela respeita o perfil do usuário autenticado.
- Ações fora do escopo do perfil ficam **ocultas ou desabilitadas com tooltip explicativo**.
- Backend é fonte de verdade — frontend só ergonomia.

### Roles
- `ROLE_ADMIN` — tudo.
- `ROLE_EDITOR` — operação diária (criar/editar/aprovar/gerar).
- `ROLE_LEITOR` — só leitura.

### Mapeamento por tela

| Tela | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Listagens de leitura | ✅ | ✅ | ✅ |
| Cadastros (criar/editar) | ✅ | ✅ | ❌ |
| Cancelar / excluir | ✅ | ❌ | ❌ |
| Publicar pacote | ✅ | ❌ | ❌ |
| Configurações gerais | ✅ | ❌ | ❌ |
| Credenciais de cliente | ✅ | ❌ | ❌ |
| Auditoria detalhada | ✅ | ❌ | ✅ |

Detalhamento por endpoint em `33-decisoes-tecnicas.md` §Permissões.

---

## 7. Acessibilidade

### Requisitos
- Todo `<input>` tem `<label>` ou `aria-label`.
- Foco visível ao navegar por teclado.
- Contraste mínimo **AA** (4.5:1 texto normal, 3:1 texto grande).
- Mensagens de erro com `aria-live="polite"`.
- Modais com `role="dialog"` + `aria-modal="true"` + foco trapped.
- ESC fecha modal.
- Enter submete form (a menos que esteja em textarea).
- Tab navega em ordem lógica.

### Auditoria
- Rodar Lighthouse a11y por tela. Meta: ≥ 95.
- Auditar com `axe-core` em testes automatizados.

---

## 8. Feedback ao usuário (toast / inline / modal)

### Quando usar cada um

| Tipo | Usar quando |
|---|---|
| **Toast efêmero** (3-5s) | Confirmação de ação simples (salvou, copiou, atualizou) |
| **Toast sticky** | Erro importante que merece atenção |
| **Inline na tela** | Estado persistente (lista vazia, erro de carregamento) |
| **Modal confirmação** | Ação destrutiva ou irreversível |
| **Modal info** | Conteúdo grande (preview, detalhe) |

### Severidades

| Severidade | Cor | Uso |
|---|---|---|
| Success | Verde | Salvar, publicar, concluir |
| Info | Azul | Informações neutras |
| Warning | Amarelo | Alertas não-bloqueantes |
| Error | Vermelho | Falhas |

### Toast com correlation ID
Em erros, mostrar correlation ID para o usuário poder reportar:
```
Erro ao gerar pacote.
Tente novamente em alguns instantes.
ID: abc-123-def-456
```

---

## 9. Confirmações destrutivas

Sempre que a ação não puder ser desfeita ou afetar outros:

### Padrão
```
┌─ Modal ─────────────────────────────┐
│ ⚠️  Confirmar cancelamento           │
├─────────────────────────────────────┤
│ Cancelar a próxima entrega para o   │
│ cliente Cliente Teste LTDA?         │
│                                     │
│ Esta ação não pode ser desfeita.    │
├─────────────────────────────────────┤
│        [Cancelar]   [Confirmar]     │
└─────────────────────────────────────┘
```

### Regras
- Botão de confirmação na cor do tipo (`danger` para destrutivo).
- Foco default no botão **Cancelar** (não no destrutivo).
- Texto explícito sobre o impacto.
- Em ações **muito críticas** (publicar para produção): exigir digitar nome do recurso ou marcar checkbox de confirmação.

---

## 10. Paginação

### Padrão
- 15-20 itens por página (default por tela).
- Controle no rodapé da tabela.
- Indicador "Mostrando X-Y de Z resultados".
- Botões: primeira, anterior, número da página, próxima, última.

### Otimizações
- Botão "exportar todos os resultados" disponível em listagens longas (CSV/Excel).
- Para tabelas > 100k registros: virtual scroll ou cursor-based pagination.

---

## 11. Ordenação

### Padrão
- Click no cabeçalho ordena por essa coluna.
- 1° click: ASC. 2°: DESC. 3°: limpa.
- Indicador visual (seta).
- Ordenação preservada em querystring.

### Default por tela
| Tela | Sort default |
|---|---|
| Lista de clientes | `nome ASC` |
| Lista de produtos | `nome ASC` |
| Lista de releases | `dataPublicacao DESC` ou `updatedAt DESC` |
| Lista de entregas | `dataCriacao DESC` |
| Próximas entregas | `dataPlanejada ASC` |

---

## 12. Atalhos de teclado

### Globais (sugestão futura)
- `?` — abre cheat-sheet.
- `/` — foco na busca.
- `g` `h` — vai para home.
- `g` `c` — vai para clientes.
- `g` `e` — vai para entregas.
- `Esc` — fecha modal/popover.

### Em formulários
- `Ctrl+S` / `Cmd+S` — salva.
- `Ctrl+Enter` — salva e continua.
- `Esc` — cancela.

---

## 13. Estados de loading detalhados

### Curtos (<200ms)
- Não mostrar nada (evita flicker).

### Médios (200-1500ms)
- Spinner inline ou skeleton.

### Longos (1500ms-10s)
- Mensagem do que está fazendo: "Calculando delta...", "Empacotando módulos...".
- Skeleton específico da estrutura da página.

### Muito longos (>10s)
- Progress bar com etapas.
- Estimativa de tempo restante (se possível).
- Botão "Cancelar" se cancelável.
- Notificação no final (toast + mudança visual da página).

### Geração assíncrona (entrega)
Estado próprio: ver `21-geracao-pacote.md` e `34-observabilidade.md`.

---

## 14. Identificadores e códigos

### Mostrar IDs
- UUIDs **não** são exibidos por padrão na UI (são longos e sem significado).
- Em vez disso: sigla (Cliente.sigla, Produto.sigla) + nome + versão.
- ID completo mostrado em **tooltip** ou em URL ao expandir um detalhe.

### Códigos curtos (humanos)
- Releases: `NEXUSLD v1.5.0` (sigla + versão).
- Entregas: `#1234` (sequencial por cliente) ou `#ENTR-2026-001` (formato com ano).
- Decisão: sequencial humano-amigável + UUID interno para FKs.

---

## 15. Performance percebida

### Princípios
- Page transition < 200ms.
- First contentful paint < 1s.
- Data render < 500ms.
- Save ação < 1s percebida (com optimistic UI se possível).

### Técnicas
- Skeletons para reduzir percepção de espera.
- Optimistic UI em ações simples (toggle ativo, marcar funcionalidade).
- Cache de dados estáticos (Produtos, Templates).
- Pre-fetch de telas adjacentes quando o usuário hover botão.

---

## 16. Cross-reference

- [`34-observabilidade.md`](34-observabilidade.md) — Métricas de comportamento.
- [`35-testes-qa.md`](35-testes-qa.md) — Testes E2E e a11y.
- [`38-glossario.md`](38-glossario.md) — Termos do domínio.
- Padrões de erro e Problem Details — consolidados neste documento.
- [`../../nexus-portal-web/docs/release-orchestrator/`](../../nexus-portal-web/docs/release-orchestrator/README.md) — Documentação do frontend (componentes, estados, por tela).
