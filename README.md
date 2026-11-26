# ApisChain

Certificação descentralizada da origem do mel (Blockchain ID). Trabalho prático de Computação Distribuída 2026/2027, ESTT, IPT.
Samuel Ponte (27227) e André Vassalo (27454).

**Versão simples da Fase 2** (branch local `versao-simples`): uma blockchain, um protocolo de consenso (Proof of Authority com votação) e um serviço de identidade digital, com uma complexidade parecida à do Templar2026_DigitalID das aulas. A versão completa (árvore de Merkle, 4 nós simulados, regras de negócio, frascos e fraudes, interface com vários painéis) está na branch `versao-completa`.

Apicultores, apiários e lotes têm identificadores. A DGAV, o Laboratório e o Certificador DOP emitem credenciais assinadas e validam os blocos por Proof of Authority. O consumidor verifica o lote (o código que está no QR do frasco).

## Abrir no NetBeans
1. File > Open Project e escolher esta pasta.
2. Java 17 ou superior.
3. Run (F6) abre a janela `ApisChainGUI`. Na primeira vez cria a pasta `data/` com as chaves das três autoridades (palavra-passe `abelha2026`) e o bloco de génese; para recomeçar do zero, fechar o programa e apagar essa pasta.
4. Os testes de consola (`TesteApisChain`, `TesteIdentidade`) estão em `apischain.test` e correm com Run File.

## Estrutura
| Pacote | Conteúdo |
|---|---|
| `apischain.utils` | `SecurityUtils` (SHA3-256, RSA, chaves em ficheiro), `Conversion` |
| `apischain.core` | `Block` (uma transação por bloco), `BlockChain`, `Vote` |
| `apischain.consensus` | `ProofOfAuthority`: proponente rotativo, votos das outras autoridades, 2 assinaturas em 3 |
| `apischain.identity` | `DigitalID`, `Role`, `Credential`, `CredentialType`, `OffChainStore` |
| `apischain.transactions` | `Transaction`, `GenesisTx`, `Authority`, `IssueCredentialTx`, `RevokeTx` |
| `apischain.service` | `ApisChain` (emitir, revogar, verificar) e `ApisChainServices` (consultas à cadeia e regras dos validadores) |
| `apischain.gui` | `ApisChainGUI`: uma janela com separadores Identidade, Credenciais, Verificar e Blockchain (`.form` do NetBeans) |

## Como usar (demonstração)
1. Identidade: criar as chaves de um apicultor (nome e palavra-passe).
2. Entrar como `DGAV` e, em Credenciais, emitir `REGISTO_APICULTOR` e `REGISTO_APIARIO` com o nome do apicultor como titular.
3. Entrar como o apicultor e emitir `LOTE` (titular: o id do apiário).
4. Entrar como `Laboratorio` e `Certificador-DOP` e emitir a análise e o certificado (titular: o id do lote).
5. Verificar: escrever o id do lote. Blockchain: ver os blocos, o proponente e os votos.

## Origem do código
`Block`, `BlockChain`, `SecurityUtils` e `Conversion` partem do código das aulas (Templar2026, Prof. António Manso). `Transaction`, `DigitalID`, `ApisChainServices` e `ApisChainGUI` baseiam-se no Templar2026_DigitalID. As alterações estão descritas no início de cada classe.
