# Como trabalhamos

Somos dois, por isso as regras são poucas.

## Fluxo
1. Cada tarefa é uma issue, com dono. Antes de começar, lê a issue toda e o `docs/CONTEXTO.md`.
2. Cria uma branch a partir de `main`: `feat/3-estado-regras`, `docs/8-relatorio`, `fix/...`.
3. Abre um pedido de integração (PR) com `Closes #3` na descrição.
4. O outro elemento revê. A revisão serve sobretudo para conhecer o código do colega: a defesa é individual e sem IA.
5. Integra com **"Create a merge commit"** (não usar "Squash and merge" nem "Rebase and merge"), como prefere o professor: o histórico mostra cada branch e os seus commits. Só depois apaga a branch.
6. Se um PR partir da branch de outro PR ainda aberto, muda primeiro a base dele para `main` e só depois integra e apaga a branch de que ele parte (senão o GitHub fecha o PR dependente).

Como os commits da branch ficam todos no histórico, cada commit deve compilar e ter uma mensagem que se perceba sozinha.

`main` tem de compilar sempre e o teste de consola (`Run` no NetBeans) tem de passar.

## Commits
`tipo(área): descrição` em português. Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`.
Exemplo: `feat(ledger): rejeitar embalagem acima do balanço de massa`.

## Evitar conflitos
- Cada issue indica os pacotes onde se trabalha. Não se mexe nos pacotes do outro sem combinar.
- `apischain.core` e `apischain.utils` estão feitos e testados: só se alteram se a issue o disser.
- A pasta `data/` (chaves, cadeias, credenciais fora da cadeia) nunca vai para o Git.

## Estilo do código
- Simples e parecido com o do professor (Templar2026, Templar2026_DigitalID, BlockChainGUI).
- Classes e métodos em inglês; comentários em português europeu, passo a passo.
- Campos privados, alterados só por métodos `set`/`add`.
- Código adaptado das aulas indica a origem no cabeçalho da classe e descreve as alterações.
- Sem bibliotecas externas. Testes em classes de consola no pacote `apischain.test`.

## Usar IA na implementação
- Dá ao modelo a issue completa e o `docs/CONTEXTO.md`. Pede só o que a issue pede.
- Antes de abrir o PR, quem implementou tem de conseguir explicar cada linha e alterar o código à mão.
