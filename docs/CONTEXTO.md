# ApisChain · contexto do projeto (versão simples da Fase 2)

Trabalho prático de Computação Distribuída (3.º ano, Eng. Informática, IPT, 2026/2027, Prof. António Manso).
Grupo: Samuel Ponte (27227) e André Vassalo (27454).
Ideia: Blockchain ID para certificar a origem do mel. Apicultores, apiários e lotes têm identificadores; DGAV, Laboratório e Certificador DOP emitem credenciais assinadas; o consumidor verifica o lote (o código que está no QR do frasco).

## Porque há uma versão simples
A 26 nov o professor disse que a árvore de Merkle não é precisa nesta fase (vai dar a matéria mais à frente). A Fase 2 passou a ser só o que foi pedido, com uma complexidade parecida à do Templar2026_DigitalID das aulas:
1. uma blockchain;
2. um protocolo de consenso;
3. um serviço de identidade digital (Blockchain ID);
4. uma aplicação no NetBeans que o mostre a funcionar.

A versão completa (árvore de Merkle, 4 nós simulados, regras de negócio, frascos, lojas e fraudes, interface com vários painéis) está na branch `versao-completa` e o seu contexto em `docs/CONTEXTO_versao_completa.md`. Pode ser reaproveitada na Fase 3.

## Fases e prazos
- Fase 1 (outubro): relatório inicial de 1 página. Entregue.
- **Fase 2: relatório até 1 dez 2026; pitch de cerca de 5 min com demonstração a 2 dez.** PoC num só computador.
- Fase 3 (janeiro): relatório final, sistema em rede P2P (sockets TCP), defesa individual com desafio de código SEM IA.
- O trabalho conta também para as microcredenciações "Fundamentos de Blockchain" e "Algoritmos de Consenso" (relatório em PDF assinado digitalmente pelos dois).

## Decisões em vigor
- Java, projeto NetBeans (Ant), compilado para Java 17. Interface Swing com `.form` do NetBeans, como nas aulas.
- Hash SHA3-256 em Base64; assinaturas SHA3-256withRSA.
- Um bloco guarda UMA transação em Base64, como o bloco das aulas guarda um dado. Sem nonce nem dificuldade (não há mineração).
- Consenso Proof of Authority com votação. As autoridades (DGAV, Laboratório e DOP) estão declaradas no bloco de génese. O proponente roda (o bloco n é proposto pela autoridade n módulo 3), verifica as regras, cria o bloco e assina o hash; as outras confirmam que o bloco está íntegro e assinado pelo proponente e votam (assinam o hash). O bloco entra com 2 assinaturas em 3; a do proponente conta como uma.
- Pressuposto: as autoridades não mentem de propósito (sem nós bizantinos). PBFT fica como evolução e exige pelo menos 4 validadores (3f + 1).
- Uma só cadeia, num só computador: as chaves privadas das três autoridades estão no mesmo computador para poderem assinar. A rede é matéria da Fase 3.
- Regras dos validadores (só de segurança): a assinatura verifica com a chave do autor que está na cadeia; cada tipo de credencial só é emitido pelo papel certo; o id da credencial é único; o DID do apicultor tem de ser o derivado da sua chave pública e só se regista uma vez; só a DGAV revoga, uma vez e só credenciais que existem.
- 5 tipos de credencial: `REGISTO_APICULTOR` e `REGISTO_APIARIO` (DGAV), `LOTE` (apicultor), `ANALISE_LOTE` (Laboratório), `CERTIFICADO_ORIGEM` (DOP).
- RGPD: na cadeia só ficam DIDs, chaves públicas, ids, tipos e o HASH de cada credencial, e as revogações. A credencial completa (nomes, moradas, resultados) fica fora da cadeia, no `OffChainStore`.
- Identidade: o DID é derivado da chave pública; a chave privada fica em ficheiro cifrada com palavra-passe (AES-GCM e PBKDF2, do Templar ver2); ecrã de entrada baseado no TemplarLogin.
- Verificar um lote: a cadeia tem de estar íntegra e cada credencial do lote não pode estar revogada, tem de ser igual à ancorada (mesmo hash) e assinada pelo emissor. Resultado AUTÊNTICO ou NÃO CONFIAR.
- Tudo simulado: entidades e dados fictícios.

## Organização do trabalho
- Repositório GitHub público; cada tarefa é uma issue com dono (ver `CONTRIBUTING.md`). Merge com merge commit, para as branches ficarem visíveis.
- A versão simples foi feita pelo Samuel a 26 nov (branch `versao-simples`). Os dois têm de conhecer todas as classes: a defesa é individual.
- Material do professor (fora do repositório, na pasta partilhada `computacao_distribuida`): `Templar2026_DigitalID.zip`, `Templar2026_ver2.zip`, `BlockChainGUI.zip`.

## Regras de estilo (obrigatórias)
- Código simples e parecido com o do professor (Templar2026, Templar2026_DigitalID): a defesa é sem IA e os alunos têm de dominar cada linha.
- Nomes de classes e métodos em inglês, como nas aulas; comentários em português europeu, passo a passo.
- Campos privados alterados só por métodos `set`/`add` próprios.
- Português europeu em tudo; NUNCA usar travessões ("—") em código, comentários, documentos ou respostas.
- Quando se adapta código das aulas, indicar a origem no cabeçalho da classe e descrever as alterações.

## Estado (26 nov 2026)
Código na branch `versao-simples` (cerca de 3 400 linhas); compila e os dois testes passam.
- `apischain.utils`: `SecurityUtils` (hash, RSA, chaves em ficheiro com AES-GCM e PBKDF2), `Conversion`.
- `apischain.core`: `Block` (índice, data, hash anterior, transação, proponente, assinatura e votos), `Vote`, `BlockChain` (`add` com 3 verificações, `getInvalidIndex`, `save`/`load`).
- `apischain.consensus`: `ProofOfAuthority` (`addTransaction`, `quorum`, `countSignatures`). Faz o papel do MinerPOW das aulas.
- `apischain.identity`: `DigitalID`, `Role`, `Credential`, `CredentialType`, `OffChainStore`.
- `apischain.transactions`: `Transaction` (abstrata e assinada), `GenesisTx`, `Authority`, `IssueCredentialTx`, `RevokeTx`.
- `apischain.service`: `ApisChain` (emitir, revogar, verificar; cria as autoridades e a génese na primeira vez) e `ApisChainServices` (consultas à cadeia e regras dos validadores, como o DigitalIDServices do professor).
- `apischain.gui`: `LoginFrame` (Entrar, Criar chaves, Consumidor; é o `main.class`) e `ApisChainGUI` (Credenciais, Verificar, Blockchain). O consenso corre noutra thread e o ecrã atualiza com `SwingUtilities.invokeLater`.
- `apischain.test`: `TesteApisChain` (génese, registos, lote, análise e certificado, rotação e assinaturas, verificação, análise alterada fora da cadeia, revogação, gravar e reabrir) e `TesteIdentidade`.

Demonstração: palavra-passe das autoridades `abelha2026`; apagar a pasta `data/` antes de cada ensaio. A interface não tem botão para adulterar: para mostrar a deteção de uma análise alterada, correr o `TesteApisChain` (secção 6).

Desatualizados (descrevem a versão completa): `docs/decisoes_fase2.md`, os diagramas v1 em `docs/` e o PR #29 (6 credenciais com a loja e diagramas v2 da versão completa).

## Plano até 1 dez (proposta)
| Até | O quê | Quem |
|---|---|---|
| 27 nov | Juntar a `versao-simples` ao `main` (PR com merge commit) | Samuel abre, André revê |
| 28 nov | Adaptar o #29: especificação das 5 credenciais, mapa RGPD e diagramas da versão simples | Samuel |
| 28 nov | Adaptar os textos do relatório, os diapositivos e o guião da demonstração | André |
| 29 nov | Juntar e rever o relatório | Os dois |
| 30 nov | Ensaio cronometrado com F6 e PDF assinado pelos dois | Os dois |
| 1 dez | Entrega do relatório | |
| 2 dez | Pitch com demonstração | |

## Referências
- Contexto da versão completa: `docs/CONTEXTO_versao_completa.md` (branch `versao-completa`).
- Documento de decisões (versão completa): `docs/decisoes_fase2.md`.
- Relatório da Fase 1: `Relatorio_Tecnico_Inicial_ApisChain_v4.docx`.
- Relatórios da Fase 2 em Word, com versões `relatorio_fase2_vX`.
