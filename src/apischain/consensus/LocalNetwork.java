//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.consensus;

import apischain.core.Block;
import apischain.core.Vote;
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.Transaction;
import apischain.utils.Conversion;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Rede local da PoC: os nós vivem todos no mesmo programa e a rede chama os
 * métodos de cada nó diretamente. Na Fase 3 esta classe é substituída pela
 * rede P2P com sockets TCP, que chama os mesmos métodos do Node.
 *
 * Uma ronda de consenso (submit):
 * 1. escolhe o proponente da vez (rotação), saltando as autoridades desligadas;
 * 2. o proponente verifica as transações e cria o bloco assinado;
 * 3. os outros validadores verificam o bloco e votam;
 * 4. com o quórum (2 em 3), o bloco é enviado a todos os nós ligados.
 *
 * Os blocos são sempre enviados como CÓPIA (serializados e lidos de novo),
 * como numa rede a sério: cada nó fica com o seu próprio objeto.
 */
public class LocalNetwork {

    private final List<Node> nodes = new ArrayList<>();   // todos os nós da rede
    private final List<String> log = new ArrayList<>();   // o que aconteceu, para mostrar na interface

    /**
     * Acrescenta um nó à rede.
     *
     * @param node nó
     */
    public void addNode(Node node) {
        nodes.add(node);
        addLog("nó " + node + " entrou na rede");
    }

    /**
     * Cria a rede: o bloco de génese com a lista das autoridades. É proposto
     * pelo autor da GenesisTx e votado pelas outras autoridades, como os outros.
     *
     * @param genesis transação de génese, assinada pelo autor
     * @return bloco de génese aceite
     * @throws Exception se a génese for recusada
     */
    public synchronized Block start(GenesisTx genesis) throws Exception {
        Node proposer = findNode(genesis.getAuthor());
        if (proposer == null || !proposer.isOnline()) {
            throw new Exception("O autor da génese tem de ser um nó validador ligado");
        }
        return runRound(proposer, Collections.singletonList(genesis), genesis.getAuthorities());
    }

    /**
     * Submete uma transação à rede (uma operação de um interveniente).
     *
     * @param tx transação assinada pelo autor
     * @return o bloco que entrou na cadeia
     * @throws Exception RuleException se as regras a rejeitarem, ou outra se não houver quórum
     */
    public Block submit(Transaction tx) throws Exception {
        return submit(Collections.singletonList(tx));
    }

    /**
     * Submete várias transações, que entram todas no mesmo bloco (ou nenhuma,
     * se alguma for rejeitada).
     *
     * synchronized: as interfaces chamam isto a partir de threads diferentes e
     * só pode haver uma ronda de consenso de cada vez.
     *
     * @param txs transações assinadas pelos autores
     * @return o bloco que entrou na cadeia
     * @throws Exception RuleException se as regras rejeitarem uma transação, ou
     *                   outra se não houver proponente ou quórum
     */
    public synchronized Block submit(List<Transaction> txs) throws Exception {
        // 1. as autoridades e a altura do próximo bloco, vistas por um nó ligado
        Node reference = firstOnline();
        if (reference == null || reference.getChain().getSize() == 0) {
            throw new Exception("A rede ainda não tem génese ou não há nós ligados");
        }
        List<Authority> authorities = ProofOfAuthority.getAuthorities(reference.getChain(), null);
        int height = reference.getChain().getSize();
        // 2. proponente da vez; se estiver desligado, passa ao seguinte
        for (int skip = 0; skip < authorities.size(); skip++) {
            Node proposer = findNode(ProofOfAuthority.selectProposer(authorities, height, skip));
            if (proposer != null && proposer.isOnline()) {
                return runRound(proposer, txs, authorities);
            }
            addLog("proponente " + ProofOfAuthority.selectProposer(authorities, height, skip)
                    + " desligado: passa ao seguinte");
        }
        throw new Exception("Nenhuma autoridade ligada: não é possível criar blocos");
    }

    /**
     * Uma ronda de consenso com um proponente já escolhido.
     */
    private Block runRound(Node proposer, List<Transaction> txs, List<Authority> authorities) throws Exception {
        // 1. o proponente cria e assina o bloco (lança RuleException se uma transação for rejeitada)
        Block block = proposer.propose(txs);
        addLog(proposer.getId() + " propôs o bloco #" + block.getIndex() + " com " + txs.size() + " transação(ões)");
        // 2. os outros validadores verificam e votam
        for (Node n : nodes) {
            if (n != proposer && n.isValidator()) {
                Vote v = n.validate(copy(block));
                if (v != null) {
                    block.addVote(v);
                    addLog(n.getId() + " votou no bloco #" + block.getIndex());
                } else {
                    addLog(n.getId() + " não votou no bloco #" + block.getIndex()
                            + (n.isOnline() ? " (recusou)" : " (desligado)"));
                }
            }
        }
        // 3. sem quórum o bloco não entra em lado nenhum
        int signatures = ProofOfAuthority.countSignatures(block, authorities);
        int needed = ProofOfAuthority.quorum(authorities.size());
        if (signatures < needed) {
            addLog("bloco #" + block.getIndex() + " recusado: " + signatures + " assinaturas em " + needed);
            throw new Exception("Sem quórum: o bloco teve " + signatures + " assinaturas de autoridades e são precisas "
                    + needed + ". Há autoridades desligadas?");
        }
        // 4. enviar o bloco a todos os nós ligados
        broadcast(block);
        return block;
    }

    /**
     * Envia um bloco aprovado a todos os nós ligados; cada um verifica-o e
     * acrescenta-o à sua cópia da cadeia.
     *
     * @param block bloco com o quórum de assinaturas
     */
    public void broadcast(Block block) {
        for (Node n : nodes) {
            if (!n.isOnline()) {
                continue;  // fica para trás e sincroniza quando voltar a ligar
            }
            try {
                n.commit(copy(block));
                addLog(n.getId() + " acrescentou o bloco #" + block.getIndex());
            } catch (Exception e) {
                addLog(n.getId() + " recusou o bloco #" + block.getIndex() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Liga ou desliga um nó. Ao voltar a ligar, o nó copia os blocos em falta
     * do nó ligado com a cadeia mais comprida.
     *
     * @param node nó
     * @param on true para ligar
     * @throws Exception se a sincronização encontrar um bloco inválido
     */
    public synchronized void setOnline(Node node, boolean on) throws Exception {
        node.setOnline(on);
        addLog("nó " + node.getId() + (on ? " ligado" : " desligado"));
        if (on) {
            // procurar o nó ligado com mais blocos
            Node best = null;
            for (Node n : nodes) {
                if (n != node && n.isOnline() && (best == null || n.getChain().getSize() > best.getChain().getSize())) {
                    best = n;
                }
            }
            if (best != null) {
                int copied = node.syncFrom(best);
                addLog(node.getId() + " sincronizou " + copied + " bloco(s) de " + best.getId());
            }
        }
    }

    /**
     * Cópia de um bloco, como se tivesse viajado pela rede (serializado e lido).
     *
     * @param block bloco
     * @return cópia independente
     * @throws Exception se a serialização falhar
     */
    public static Block copy(Block block) throws Exception {
        return Conversion.stringToObject(Conversion.objectToString(block));
    }

    /**
     * @return o nó com este DID de autoridade, ou null
     */
    private Node findNode(String did) {
        for (Node n : nodes) {
            if (did.equals(n.getDid())) {
                return n;
            }
        }
        return null;
    }

    /**
     * @return o primeiro nó ligado, ou null
     */
    private Node firstOnline() {
        for (Node n : nodes) {
            if (n.isOnline()) {
                return n;
            }
        }
        return null;
    }

    private void addLog(String msg) {
        log.add(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + msg);
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<String> getLog() {
        return Collections.unmodifiableList(log);
    }
}
