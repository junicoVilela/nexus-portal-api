# Prompt - Criar Migration Flyway

Crie uma migration Flyway para o módulo informado.

## Regras

- Usar PostgreSQL.
- Usar snake_case.
- Usar prefixo `tb_`.
- Criar PK.
- Criar FKs com nomes claros.
- Criar índices para FKs.
- Incluir `created_at` e `updated_at` quando for tabela principal.
- Não alterar migration antiga.

## Padrão de arquivo

```text
V{numero}__create_{modulo}_tables.sql
```

## Exemplo de tabela

```sql
create table tb_manual (
    id bigserial primary key,
    titulo varchar(200) not null,
    descricao text,
    status varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp
);
```
