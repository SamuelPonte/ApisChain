//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.consensus;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.core.Vote;
import apischain.ledger.LedgerState;
import apischain.ledger.Rules;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Nó da rede ApisChain. Cada nó tem a SUA cópia da cadeia, gravada no seu
 * ficheiro, e verifica sozinho cada bloco antes de o aceitar.
 *
 * Há dois tipos de nó:
 * - validador (N1 DGAV, N2 Laboratório, N3 DOP): tem a chave privada de uma
 *   autoridade, por isso pode propor blocos e votar;
 * - não validador (N4): não tem chave; recebe e verifica os blocos, mas não
 *   propõe nem vota.
 *
 * O nó não cria threads nem sockets: quem o chama é a LocalNetwork (Fase 2) e,
 * na Fase 3, a rede TCP chamará os mesmos métodos.
 */
public class Node {

    private final String id;              // nome do nó (N1, N2, ...)
    private final String did;             // DID da autoridade, ou null no não validador
    private final PrivateKey privateKey;  // chave da autoridade, ou null no não validador
    private final String fileName;        // ficheiro da cópia da cadeia
    private BlockChain chain = new BlockChain();  // cópia própria da cadeia
    private boolean online = true;        // ligado à rede?

    /**
     * Cria o nó e carrega a sua cópia da cadeia, se já existir em ficheiro.
     *
     * @param id nome do nó
     * @param did DID da autoridade (null no não validador)
     * @param privateKey chave privada da autoridade (null no não validador)
     * @param folder pasta do nó, por exemplo data/N1
     * @throws Exception se o ficheiro existir mas não se conseguir ler
     */
    public Node(String id, String did, PrivateKey privateKey, String folder) throws Exception {
        this.id = id;
        this.did = did;
        this.privateKey = privateKey;
        // 1. cada nó grava a sua cópia na sua pasta
        new File(folder).mkdirs();
        this.fileName = new File(folder, "blockchain.bc").getPath();
        // 2. se já existir uma cópia gravada, continua de onde ficou
        if (new File(fileName).exists()) {
            chain.load(fileName);
        }
    }

    /**
     * @return true se for um nó validador (tem a chave de uma autoridade)
     */
    public boolean isValidator() {
        return privateKey != null;
    }

    /**
     * O nó, como proponente, cria e assina um bloco com as transações dadas.
     *
     * Cada transação passa nas regras pela ordem da lista. Se as regras
     * devolverem um alerta de fraude (segunda venda), é o alerta que entra no
     * bloco, assinado por este nó. Se alguma transação for rejeitada, não se
     * cria bloco nenhum e a razão segue na RuleException.
     *
     * @param txs transações a incluir (assinadas pelos autores)
     * @return bloco assinado pelo proponente, ainda sem votos
     * @throws Exception RuleException se uma transação for rejeitada
     */
    public Block propose(List<Transaction> txs) throws Exception {
        if (!isValidator()) {
            throw new Exception("O nó " + id + " não é validador: não pode propor blocos");
        }
        // 1. verificar cada transação com o estado atual da cadeia
        LedgerState state = new LedgerState(chain);
        List<String> accepted = new ArrayList<>();
        for (Transaction tx : txs) {
            Transaction result = Rules.check(tx, state, did);
            // 2. alerta de fraude: as regras devolvem-no por assinar; assina o proponente
            if (result instanceof FraudAlertTx && result != tx) {
                result.sign(privateKey);
            }
            // 3. atualizar o estado para a transação seguinte e guardar em Base64
            state.apply(result);
            accepted.add(result.toBase64());
        }
        // 4. criar o bloco no fim da cadeia e assinar o seu hash
        Block block = new Block(chain.getSize(), chain.getLastBlockHash(), accepted, did);
        block.setSignature(SecurityUtils.sign(block.getCurrentHash(), privateKey));
        return block;
    }

    /**
     * O nó, como validador, verifica um bloco proposto por outro e, se estiver
     * tudo certo, vota nele (assina o hash do bloco).
     *
     * @param block bloco proposto
     * @return o voto, ou null se recusar (desligado, não validador ou bloco inválido)
     */
    public Vote validate(Block block) {
        if (!online || !isValidator()) {
            return null;
        }
        try {
            ProofOfAuthority.verifyBlock(block, chain);
            return new Vote(did, SecurityUtils.sign(block.getCurrentHash(), privateKey));
        } catch (Exception e) {
            return null;  // bloco recusado: não vota
        }
    }

    /**
     * Acrescenta um bloco à cópia da cadeia deste nó, depois de verificar o
     * bloco e o quórum de assinaturas, e grava a cadeia no ficheiro.
     *
     * @param block bloco com assinatura do proponente e votos
     * @throws Exception com a razão, se o bloco for recusado ou o nó estiver desligado
     */
    public void commit(Block block) throws Exception {
        if (!online) {
            throw new Exception("O nó " + id + " está desligado");
        }
        // 1. verificação completa (regras, proponente e quórum)
        ProofOfAuthority.verifyCommit(block, chain);
        // 2. acrescentar à cadeia (a BlockChain volta a verificar a ligação)
        if (!chain.add(block)) {
            throw new Exception("A cadeia de " + id + " recusou o bloco #" + block.getIndex());
        }
        // 3. gravar a cópia deste nó
        chain.save(fileName);
    }

    /**
     * Copia de outro nó os blocos que faltam a este (por exemplo, depois de
     * ter estado desligado), verificando cada um como se fosse novo.
     *
     * @param other nó de onde copiar
     * @return número de blocos copiados
     * @throws Exception se um bloco do outro nó for inválido (a cópia para aí)
     */
    public int syncFrom(Node other) throws Exception {
        int copied = 0;
        // os blocos em falta são os de índice igual ou superior ao tamanho desta cadeia
        for (int i = chain.getSize(); i < other.chain.getSize(); i++) {
            commit(LocalNetwork.copy(other.chain.get(i)));
            copied++;
        }
        return copied;
    }

    public String getId() {
        return id;
    }

    public String getDid() {
        return did;
    }

    public BlockChain getChain() {
        return chain;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return id + (isValidator() ? " (validador " + did + ")" : " (não validador)")
                + (online ? "" : " DESLIGADO") + ", " + chain.getSize() + " blocos";
    }
}
