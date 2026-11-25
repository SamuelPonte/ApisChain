# ApisChain · contexto do projeto

Trabalho prático de Computação Distribuída (3.º ano, Eng. Informática, IPT, 2026/2027, Prof. António Manso).
Grupo: Samuel Ponte (27227) e André Vassalo (27454).
Ideia: Blockchain ID para certificar a origem do mel. Apicultores, apiários, lotes e frascos têm identificadores; DGAV, Laboratório e Certificador DOP emitem credenciais assinadas; o consumidor verifica o QR do frasco.

## Fases e prazos
- Fase 1 (outubro): relatório inicial de 1 página. Entregue.
- **Fase 2: relatório até 1 dez 2026; pitch de ~5 min com demonstração a 2 dez.** PoC da blockchain a funcionar num só computador.
- Fase 3 (janeiro): relatório final, sistema em rede P2P distribuída (sockets TCP), defesa individual com desafio de código SEM IA.
- O trabalho conta também para as microcredenciações "Fundamentos de Blockchain" e "Algoritmos de Consenso" (esta exige implementar um protocolo de consenso; relatório em PDF assinado digitalmente pelos dois).

## Decisões fechadas
- Java, projeto NetBeans (Ant), compilado para Java 17. Interface Swing, como nas aulas.
- Hash SHA3-256 em Base64; assinaturas SHA3-256withRSA.
- Consenso Proof of Authority com DGAV, Laboratório e DOP como autoridades. Não tolera nós bizantinos (pressuposto a declarar no relatório); PBFT fica como evolução e exige pelo menos 4 validadores.
- Regras dos validadores (vindas do protótipo `ApisChain.html`): balanço de massa com tolerância de 2% (não se embala mais do que os kg colhidos do lote); segunda venda do mesmo frasco gera ALERTA_FRAUDE; lojas registadas pela DGAV; revogação de credenciais.
- RGPD: na cadeia só DIDs, chaves públicas, hashes das credenciais, embalagens, vendas, alertas e revogações; credenciais completas e dados pessoais ficam fora da cadeia.
- Tudo simulado: entidades e dados fictícios.

## Decisões fechadas a 15 nov 2026
- PoA com votação: proponente rotativo, as outras autoridades votam (assinam o hash). O bloco entra com 2 assinaturas em 3; a do proponente conta como uma delas.
- Quatro nós: N1 DGAV, N2 Laboratório, N3 DOP (validadores) e N4 não validador (submete transações e verifica, não vota).
- Chaves de cada interveniente em ficheiro (`.pub` em claro, `.priv` cifrada com palavra-passe) e ecrã de entrada como o `TemplarLogin` do professor.
- Os kg colhidos do lote ficam em claro na cadeia (necessário para o balanço de massa).
- Nomes `DigitalID` e `Node` (não `Identity` nem `PeerNode`).
- O ALERTA_FRAUDE de uma segunda venda é assinado pela autoridade proponente do bloco.
- Fraudes da demo: QR copiado (segunda venda), análise alterada fora da cadeia (hash não coincide), embalar mais do que se produziu (balanço de massa).

## Organização do trabalho
- Repositório GitHub público; cada tarefa é uma issue com dono (ver `CONTRIBUTING.md`). Implementar só o que a issue pede.
- Samuel: identidade, credenciais, consenso, painéis das autoridades. André: transações, estado e regras, serviço, restantes painéis.
- `apischain.core` e `apischain.utils` estão feitos e testados: não alterar sem a issue o pedir.
- Material do professor (fora do repositório, na pasta partilhada `computacao_distribuida`): `Templar2026_DigitalID.zip`, `Templar2026_ver2.zip`, `BlockChainGUI.zip`.

## Regras de estilo (obrigatórias)
- Código simples e parecido com o do professor (Templar2026, Templar2026_DigitalID, BlockChainGUI): a defesa é sem IA e os alunos têm de dominar cada linha.
- Nomes de classes e métodos em inglês, como nas aulas; comentários em português europeu, passo a passo.
- Campos privados alterados só por métodos `set`/`add` próprios.
- Português europeu em tudo; NUNCA usar travessões ("—") em código, comentários, documentos ou respostas.
- Quando se adapta código das aulas, indicar a origem no cabeçalho da classe e descrever as alterações.

## Estado (25 nov 2026)
O código da Fase 2 está em `main` e as issues #1 a #7 estão fechadas:
- `apischain.utils`: `SecurityUtils` (hash, RSA em String e em bytes, chaves em ficheiro com AES-GCM + PBKDF2 copiado do Templar ver2), `Conversion` (`objectToString` / `stringToObject`).
- `apischain.core`: `Block` (sem nonce; índice, timestamp, previousHash, transações em Base64, raiz de Merkle, validador, assinatura, votos), `Vote`, `BlockChain` (`add` com 3 verificações, `getInvalidIndex`, `save`/`load`), `MerkleTree` (do BlockChainGUI, com SHA3-256 e duas correções: `!` em falta no `isValid` e prova de nó sem irmão). `isProofValid` só compara com a raiz que vem na prova: quem verifica confirma também que é a raiz guardada no bloco.
- `apischain.identity`: `DigitalID` (DID derivado da chave pública, nunca vai para a cadeia), `Credential`, `OffChainStore`.
- `apischain.transactions`: `Transaction` abstrata e assinada (a chave pública não vai na transação) e seis subclasses; `PackTx` tem o peso por frasco.
- `apischain.ledger`: `LedgerState` (estado lido da cadeia) e `Rules.check`, que aceita, recusa com `RuleException` ou devolve um `FraudAlertTx` por assinar. Um lote só sai de um apiário registado do próprio apicultor; um DID só se regista uma vez.
- `apischain.consensus`: `ProofOfAuthority`, `Node` e `LocalNetwork`. O proponente da vez é saltado se estiver desligado ou com a cadeia atrasada ou corrompida; um nó que se liga sincroniza com a cadeia íntegra mais comprida.
- `apischain.service`: `ApisChainService` (única classe que os painéis chamam), `verifyJar`, `Actor` e `DemoData` (cenário da demonstração). As leituras da cadeia fazem-se com `synchronized (getNetwork())`.
- `apischain.gui`: interface Swing escrita à mão, sem `.form`. `BasePanel.runInBackground` é o padrão não bloqueante. `ApisChainApp` é o `main.class`.
- `apischain.test`: seis testes de consola. O `TesteDemo` percorre o cenário e as três fraudes e pode correr várias vezes seguidas.

Cuidados na demonstração: repor a análise depois da fraude 2, antes de emitir outra credencial; adulterar um nó só no fim e repor.

Falta na Fase 2 (entregas oficiais a 1 e 2 dez):
- #8: a especificação das credenciais, o mapa RGPD (`docs/credenciais_e_rgpd.md`) e os diagramas v2 (`docs/diagrama_*_v2`) estão feitos; falta o texto do André, juntar tudo no Word e rever o relatório.
- #9: diapositivos, ensaio cronometrado e PDF assinado pelos dois.

A seguir, Fase 3: issues #10 a #14.

## Referências
- Protótipo ilustrativo em JavaScript (modelo de dados, regras e fraudes): `projeto/1_fase/ApisChain.html` na pasta partilhada.
- Documento de decisões: `docs/decisoes_fase2.md`.
- Relatório da Fase 1: `Relatorio_Tecnico_Inicial_ApisChain_v4.docx`.
- Relatórios da Fase 2 em Word, com versões `relatorio_fase2_vX`.
