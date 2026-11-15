# ApisChain · Fase 2: decisões e plano (atualizado a 15 nov 2026)

## Entregáveis
- Relatório (Word, versões `relatorio_fase2_vX`), entrega a 1 dez; sem limite de páginas.
- Pitch de cerca de 5 min com demonstração da PoC a correr num só computador, a 2 dez.
- Código neste repositório (projeto NetBeans, Java 17).

## Decisões fechadas
- Consenso: Proof of Authority com DGAV, Laboratório e DOP como autoridades (PBFT fica como evolução, exige pelo menos 4 validadores para tolerar 1 bizantino).
- Hash SHA3-256 e assinaturas RSA (SHA3-256withRSA), como nas aulas.
- Interface Swing no NetBeans, reaproveitando o BlockChainGUI (MerkleTree, painel de Merkle).
- Código simples e próximo do estilo do professor (Templar2026, BlockChainGUI), porque a defesa final tem desafio sem IA.
- Regras do protótipo ApisChain.html aceites: tolerância de 2% no balanço de massa, rotação dos validadores, lojas registadas pela DGAV, ALERTA_FRAUDE em segunda venda.
- Tudo simulado: entidades e dados fictícios.

## Decisões fechadas a 15 nov
- PoA com votação: proponente rotativo, as outras autoridades votam (assinam o hash do bloco). O bloco entra com 2 assinaturas em 3, e a assinatura do proponente conta como uma delas (proponente mais 1 voto chega).
- Quatro nós: N1 DGAV, N2 Laboratório, N3 DOP (validadores) e N4 não validador (apicultores, lojas e consumidores: submete transações e verifica, mas não vota).
- Chaves de cada interveniente em ficheiro (.pub em claro, .priv cifrada com palavra-passe) e ecrã de entrada, como no TemplarLogin.
- Os kg colhidos do lote ficam em claro na cadeia (não são dado pessoal). Sem isto os validadores não conseguem aplicar o balanço de massa, porque a credencial completa do lote fica fora da cadeia.
- Nomes: `DigitalID` (e não Identity) e `Node` (e não PeerNode), como no material mais recente do professor.
- O ALERTA_FRAUDE gerado por uma segunda venda é assinado pela autoridade que propõe o bloco (a loja não assina o seu próprio alerta).
- As 3 fraudes da demonstração (do protótipo): (1) QR copiado para outro frasco, apanhado como segunda venda; (2) análise do lote alterada fora da cadeia, o hash deixa de coincidir com o ancorado; (3) embalar mais do que se produziu, rejeitado pelo balanço de massa.
- Repositório GitHub privado, com 14 issues em 2 milestones (ver README e CONTRIBUTING).

## Por esclarecer
- Data exata da entrega e defesa da Fase 3 (janeiro).
- Material das aulas sobre redes P2P (ainda não disponível a 15 nov).
- Apresentação pública de 30 nov (Fundamentos): aplica-se aos alunos de CD?

## Material novo do professor (12 nov): Templar2026_ver2 e Templar2026_DigitalID
- SecurityUtils: chaves RSA em ficheiro (.pub e .priv cifrada com AES-GCM e PBKDF2). Já integrado na ApisChain.
- Conversion: objectToString e stringToObject. Nomes adotados na ApisChain.
- DigitalID (nome, chave pública, assinatura do próprio) registado num bloco; DigitalIDServices.getPublicKey procura a chave pela cadeia. Base para o registo de identidades da ApisChain, com a diferença de que o registo é autorizado pela DGAV (no do professor, qualquer pessoa pode registar qualquer nome).
- MinerPOW.getNonceP: mineração PoW paralela com AtomicInteger (ficha PoW). getSigner: escolha do assinante proporcional às moedas (ideia de PoS). Não usados na ApisChain (PoA).
- Interfaces não bloqueantes: new Thread para gravar na cadeia e SwingUtilities.invokeLater para atualizar o ecrã. A adotar nas interfaces.
- BlockChain grava e lê sozinha o ficheiro blockchain.bc. A adotar: cada nó grava a sua cópia.
- Professor usa JDK 26 (javac.source=26).

## Microcredenciações (critérios do Moodle)
- Algoritmos de Consenso: 25% quizzes individuais; 50% trabalho prático de grupo com apresentação que implemente um protocolo de consenso; 25% relatório técnico de grupo. Relatório em PDF assinado digitalmente por todos; todos submetem. Apresentação pública a 19 jun; alunos de CD dispensados.
- Fundamentos: 25% quizzes; 50% trabalho prático que use uma blockchain para guardar a informação; 25% relatório. PDF assinado por todos. Apresentação pública a 30 nov (por confirmar se se aplica aos alunos de CD).

## Plano (as issues do GitHub são a referência)
| Até | Issues |
|---|---|
| 19 nov | #1 Identidade e credenciais (Samuel), #2 Transações (André) |
| 24 nov | #3 Estado e regras (André), #4 Consenso PoA e rede local (Samuel) |
| 25 nov | #5 Serviço e cenário da demo (André) |
| 28 nov | #6 e #7 Interfaces (Samuel, André) |
| 30 nov | #8 Relatório (ambos) |
| 2 dez | #9 Pitch, ensaio e PDF assinado (ambos) |
