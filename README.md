# ApisChain

Certificação descentralizada da origem do mel (Blockchain ID). Trabalho prático de Computação Distribuída 2026/2027, ESTT, IPT.
Samuel Ponte (27227) e André Vassalo (27454).

Apicultores, apiários, lotes e frascos têm identificadores. A DGAV, o Laboratório e o Certificador DOP emitem credenciais assinadas e validam os blocos por Proof of Authority. O consumidor verifica o QR do frasco.

## Abrir no NetBeans
1. File > Open Project e escolher esta pasta.
2. Java 17 ou superior.
3. Run (F6) abre a interface. Na primeira vez cria a pasta `data/` (chaves, 4 nós e cenário da demonstração); para recomeçar do zero, fechar o programa e apagar essa pasta.
4. Os testes de consola estão em `apischain.test` e correm com Run File.

## Estrutura
| Pacote | Conteúdo | Estado |
|---|---|---|
| `apischain.utils` | `SecurityUtils` (SHA3-256, RSA, chaves em ficheiro), `Conversion` | feito |
| `apischain.core` | `Block`, `BlockChain`, `MerkleTree`, `Vote` | feito |
| `apischain.identity` | `DigitalID`, `Role`, `Credential`, `CredentialType`, `OffChainStore` | feito (#1) |
| `apischain.transactions` | `Transaction` e seis subclasses, `Authority` | feito (#2) |
| `apischain.ledger` | `LedgerState`, `Rules`, `LotBalance`, `RuleException` | feito (#3) |
| `apischain.consensus` | `Node`, `ProofOfAuthority`, `LocalNetwork` | feito (#4) |
| `apischain.service` | `ApisChainService`, `VerificationResult`, `Actor`, `DemoData` | feito (#5) |
| `apischain.gui` | `ApisChainApp` (arranque), `LoginFrame`, `MainFrame` e um painel por interveniente, mais Cadeia, Merkle e Rede | feito (#6 e #7) |
| `apischain.test` | seis testes de consola: `TesteNucleo`, `TesteIdentidade`, `TesteTransacoes`, `TesteRegras`, `TesteConsenso`, `TesteDemo` | feito |

A rede P2P da Fase 3 ainda não tem pacote definido.

## Planeamento
| Milestone | Data | Conteúdo |
|---|---|---|
| Fase 2 · PoC e entrega | 25 nov 2026 | PoC local num só computador, relatório e pitch prontos a 25 nov (entregas oficiais: relatório a 1 dez, pitch a 2 dez) |
| Fase 3 · Rede P2P e defesa | janeiro 2027 (data por confirmar) | Rede P2P com sockets TCP, relatório final, defesa com desafio sem IA |

## Documentação
- `docs/decisoes_fase2.md`: decisões fechadas e plano.
- `docs/`: diagramas de arquitetura e de classes (v1; a v2 sai na issue #8).
- `CONTRIBUTING.md`: como trabalhamos.
- `docs/CONTEXTO.md`: contexto e decisões do projeto.

## Origem do código
`Block`, `BlockChain`, `MerkleTree`, `SecurityUtils` e `Conversion` partem do código das aulas (Templar2026 e BlockChainGUI, Prof. António Manso). `Transaction`, `DigitalID`, `LoginFrame`, `MerkleTreePanel` e `MerkleGraphics` baseiam-se também em exemplos das aulas. As alterações estão descritas no início de cada classe.
