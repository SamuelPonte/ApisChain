# ApisChain · Especificação das credenciais e mapa RGPD

Secções do relatório da Fase 2 (issue #8). Descrevem o que o código em `main` faz a 25 nov 2026; os nomes das classes e dos campos são os do código.

Diagramas que acompanham estas secções (pasta `docs/`):
- `diagrama_fluxo_credencial_v2.png`: emissão, armazenamento e verificação de uma credencial;
- `diagrama_arquitetura_v2.png`: intervenientes, serviço, consenso, nós e onde ficam os dados;
- `diagrama_classes_v2.png`: classes Java por pacote.

## 1. Especificação das credenciais

### 1.1 Estrutura comum

Uma credencial tem duas partes, guardadas em sítios diferentes:

| Parte | Classe | Onde fica | Conteúdo |
|---|---|---|---|
| Credencial completa | `Credential` | fora da cadeia (`OffChainStore`, ficheiro `offchain.dat`) | id, tipo, emissor (DID), titular, dados (`chave = valor`), data de emissão e assinatura do emissor |
| Âncora | `IssueCredentialTx` | na cadeia, dentro de um bloco | id, tipo, titular, **hash SHA3-256 da credencial**, chave pública do titular (só nos registos de apicultor e de loja), kg colhidos (só no lote), autor (DID do emissor), data e assinatura do emissor |

Regras comuns:
- **Assinatura:** o emissor assina a credencial e a transação com a sua chave privada (SHA3-256withRSA). As chaves públicas verificam-se sempre com a chave **que está na cadeia**, nunca com uma chave que venha na própria credencial.
- **Texto assinado:** `tipo|id|emissor|titular|data|dados`, com os dados ordenados pelo nome (`TreeMap`), para os mesmos dados darem sempre o mesmo texto. Os carateres `|` e `;` são recusados nos valores e o `=` nos nomes dos dados, para dois conteúdos diferentes não darem o mesmo texto.
- **Hash ancorado:** `Credential.getHash()` cobre o conteúdo (não a assinatura). Se alguém alterar a credencial fora da cadeia, o hash deixa de coincidir com o da `IssueCredentialTx` (fraude 2).
- **Revogação:** só a DGAV revoga, com uma `RevokeTx` (id da credencial e motivo). A credencial continua na cadeia, mas passa a contar como revogada.
- **Identificadores:** o DID de cada interveniente é `did:apis:` seguido de 16 carateres do hash da sua chave pública (`DigitalID.createDid`); os ids das credenciais são gerados pelo serviço (`AP-0001`, `LOTE-2026-0001`, ...).

### 1.2 Tipos de credencial

Os campos "fora da cadeia" são os usados pelo cenário da demonstração (`DemoData`) e pelos painéis da interface. As regras são as de `Rules.check`, verificadas por cada validador antes de votar.

| Tipo (`CredentialType`) | Id | Emissor | Titular (`subject`) | Dados fora da cadeia | Na cadeia, além do hash | Regras dos validadores |
|---|---|---|---|---|---|---|
| `REGISTO_APICULTOR` | `AP-0001` | DGAV | DID do apicultor | nome, NIF, morada | chave pública do apicultor | o DID tem de ser o derivado dessa chave; um DID só é registado uma vez |
| `REGISTO_APIARIO` | `APIARIO-0001` | DGAV | DID do apicultor dono | local, coordenadas, n.º de colmeias | nada | o titular é um apicultor registado e não revogado |
| `REGISTO_LOJA` | `LJ-0001` | DGAV | DID da loja | nome, morada | chave pública da loja | como no registo de apicultor; só uma loja registada pode vender |
| `LOTE` | `LOTE-2026-0001` | apicultor | id do apiário de origem | origem floral, data da colheita, kg colhidos | **kg colhidos em claro** (`weightKg`) | kg positivos; o apiário existe, não está revogado e é do apicultor |
| `ANALISE_LOTE` | `AN-0001` | Laboratório | id do lote | origem floral, humidade, adulteração | nada | o lote existe e não está revogado |
| `CERTIFICADO_ORIGEM` | `DOP-0001` | Certificador DOP | id do lote | denominação, validade | nada | o lote existe e não está revogado |

Em todos os tipos: o emissor tem de ter o papel certo na cadeia (DGAV, LAB e DOP vêm da génese; APICULTOR e LOJA vêm dos registos não revogados), o id não pode repetir, e só os dois registos podem trazer chave pública.

### 1.3 Transações que não são credenciais

Também entram na cadeia, assinadas pelo autor, mas sem credencial fora da cadeia:

| Transação | Autor | Campos | Para que serve |
|---|---|---|---|
| `GenesisTx` | DGAV | lista das autoridades (DID, papel, chave pública) | bloco 0: raiz de confiança do Proof of Authority |
| `PackTx` | apicultor dono do lote | lote, primeiro e último frasco, kg por frasco | cria os frascos `LOTE-2026-0001-137`; balanço de massa com 2% de tolerância (fraude 3) |
| `SaleTx` | loja | código do frasco | primeira venda de cada frasco |
| `FraudAlertTx` | autoridade proponente do bloco | código do frasco, motivo | entra no lugar de uma segunda venda do mesmo frasco (fraude 1) |
| `RevokeTx` | DGAV | id da credencial, motivo | revogação |

### 1.4 Ciclo de vida (diagrama de fluxo)

**Emissão** (por exemplo, a análise do Laboratório):
1. O emissor entra com a palavra-passe, que decifra a sua chave privada (`DigitalID.loadPrivateKey`, AES-GCM com PBKDF2).
2. `ApisChainService` cria a `Credential` e o emissor assina-a.
3. Cria a `IssueCredentialTx` com o hash da credencial e o emissor assina-a.
4. `LocalNetwork.submit`: o proponente da vez verifica as regras e assina o bloco; as outras autoridades verificam e votam; com 2 assinaturas em 3, o bloco entra nos 4 nós.

**Armazenamento:**
5. Só depois de a rede aceitar, a credencial completa é guardada no `OffChainStore` (fora da cadeia).
6. Cada nó grava a sua cópia da cadeia no seu ficheiro (`data/N1/blockchain.bc`, ...).

**Verificação** (consumidor, `ApisChainService.verifyJar`, sem chave):
7. A cópia da cadeia do N4 está íntegra (hashes e raízes de Merkle).
8. O frasco existe: a `PackTx` está assinada pelo apicultor e a sua prova de Merkle leva à raiz guardada no bloco.
9. Para o lote, as análises e os certificados: não revogados; a credencial fora da cadeia tem o mesmo id, tipo, titular e emissor que a âncora, o mesmo hash, e a assinatura do emissor verifica com a chave que está na cadeia.
10. Nenhuma análise em vigor diz que o mel é adulterado, e não há alertas de fraude sobre o frasco.
11. Resultado: OK, AVISO (por exemplo, sem certificado DOP) ou FALHA.

## 2. Mapa RGPD

### 2.1 O que fica onde

| Dado | Na cadeia | Fora da cadeia | Notas |
|---|---|---|---|
| Nome, NIF e morada do apicultor | não | `Credential` REGISTO_APICULTOR | dado pessoal |
| Nome e morada da loja | não | `Credential` REGISTO_LOJA | normalmente de uma empresa |
| Local, coordenadas e n.º de colmeias do apiário | não | `Credential` REGISTO_APIARIO | as coordenadas podem identificar o terreno de uma pessoa |
| Nome do interveniente na identidade (`DigitalID`) | não | ficheiro `nome.id` no computador do dono | nunca é enviado para a rede |
| Chave privada | não | ficheiro `nome.priv`, cifrado com a palavra-passe | nunca sai do computador do dono |
| DID e chave pública dos apicultores e lojas | sim | também no `nome.id` e `nome.pub` | pseudónimo: necessário para verificar assinaturas |
| DID, papel e chave pública das autoridades | sim (génese) | | entidades públicas, não são dados pessoais |
| Hash de cada credencial | sim | | não permite reconstruir a credencial |
| Origem floral, humidade, adulteração (análise) | não | `Credential` ANALISE_LOTE | dados do produto |
| Denominação e validade (certificado) | não | `Credential` CERTIFICADO_ORIGEM | dados do produto |
| Kg colhidos do lote | sim | também na credencial do lote | dado do produto, necessário para o balanço de massa nos validadores |
| Embalagens (lote, frascos, kg por frasco) | sim | | dados do produto |
| Vendas (frasco e DID da loja) | sim | | o consumidor que compra nunca aparece |
| Alertas de fraude | sim | | frasco e motivo |
| Revogações | sim | | id da credencial e motivo |
| Consumidor | não | | verifica sem chave e sem se identificar |

### 2.2 Decisões e justificação

- **Minimização (art. 5.º, n.º 1, alínea c, do RGPD):** na cadeia só fica o que os validadores precisam para aplicar as regras (DIDs, chaves públicas, hashes, kg e frascos) e o que o consumidor precisa para verificar. Tudo o que identifica uma pessoa fica fora da cadeia.
- **Imutabilidade e direito ao apagamento:** a cadeia não se apaga. Por isso nunca lá entra um dado pessoal em claro. Para "apagar" uma credencial apaga-se a credencial completa fora da cadeia: o hash que fica na cadeia deixa de poder ser ligado a qualquer conteúdo.
- **Integridade sem exposição:** o hash prova que a credencial fora da cadeia não foi alterada (fraude 2) sem revelar o seu conteúdo.
- **Consumidor anónimo:** a verificação do frasco não pede chave nem dados ao consumidor.

### 2.3 Limitações a declarar

- **O DID é um pseudónimo, não é anónimo.** O DID de um apicultor que seja pessoa singular continua a ser dado pessoal pseudonimizado (considerando 26 do RGPD), porque a DGAV sabe a quem pertence. Fica na cadeia de forma permanente. Mitigação: o DID não contém o nome e deriva só da chave pública; se o registo for revogado, o interveniente pode criar chaves novas e um DID novo.
- **Hash de dados previsíveis.** Quem já conhecer os dados de uma credencial (por exemplo, nome e NIF de um apicultor) e o seu id e data de emissão pode calcular o hash e confirmar que estão certos. A credencial não tem um valor aleatório (sal) que o impeça. Evolução proposta: acrescentar um sal aleatório a cada credencial, guardado só fora da cadeia.
- **Texto livre na cadeia.** O motivo de uma revogação (`RevokeTx`) e de um alerta (`FraudAlertTx`) fica na cadeia em claro. Regra de utilização: nunca escrever dados pessoais nesses campos (o código não o consegue verificar).
- **Prova de conceito.** O `OffChainStore` é um ficheiro sem cifra no mesmo computador. Num sistema real seria a base de dados de cada emissor, com controlo de acessos.
