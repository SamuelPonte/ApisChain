# ApisChain

Certificação descentralizada da origem do mel (Blockchain ID). Trabalho prático de Computação Distribuída 2026/2027, ESTT, IPT.
Samuel Ponte (27227) e André Vassalo (27454).

Apicultores, apiários, lotes e frascos têm identificadores. A DGAV, o Laboratório e o Certificador DOP emitem credenciais assinadas e validam os blocos por Proof of Authority. O consumidor verifica o QR do frasco.

## Abrir no NetBeans
1. File > Open Project e escolher esta pasta.
2. Java 17 ou superior.
3. Run (F6) corre, por agora, o teste de consola `apischain.test.TesteNucleo`.

## Estrutura
| Pacote | Conteúdo | Estado |
|---|---|---|
| `apischain.utils` | `SecurityUtils` (SHA3-256, RSA, chaves em ficheiro), `Conversion` | feito |
| `apischain.core` | `Block`, `BlockChain`, `MerkleTree`, `Vote` | feito |
| `apischain.identity` | `DigitalID`, `Credential`, `CredentialType`, `OffChainStore` | issue #1 |
| `apischain.transactions` | `Transaction` e subclasses | issue #2 |
| `apischain.ledger` | `LedgerState`, `Rules` | issue #3 |
| `apischain.consensus` | `Node`, `ProofOfAuthority`, `LocalNetwork` | issue #4 |
| `apischain.service` | `ApisChainService` | issue #5 |
| `apischain.gui` | interfaces Swing | issues #6 e #7 |

Os nomes dos pacotes por fazer são os previstos no diagrama de classes; a rede P2P da Fase 3 ainda não tem pacote definido.

## Planeamento
| Milestone | Data | Conteúdo |
|---|---|---|
| Fase 2 · PoC e entrega | 2 dez 2026 | PoC local num só computador, relatório (1 dez) e pitch (2 dez) |
| Fase 3 · Rede P2P e defesa | janeiro 2027 (data por confirmar) | Rede P2P com sockets TCP, relatório final, defesa com desafio sem IA |

## Documentação
- `docs/decisoes_fase2.md`: decisões fechadas e plano.
- `docs/`: diagramas de arquitetura e de classes (v1; a v2 sai na issue #8).
- `CONTRIBUTING.md`: como trabalhamos.
- `docs/CONTEXTO.md`: contexto e decisões do projeto.

## Origem do código
`Block`, `BlockChain`, `MerkleTree`, `SecurityUtils` e `Conversion` partem do código das aulas (Templar2026 e BlockChainGUI, Prof. António Manso). As alterações estão descritas no início de cada classe.
