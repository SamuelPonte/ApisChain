//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.consensus;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.core.Vote;
import apischain.service.ApisChainServices;
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Consenso Proof of Authority (PoA) com votação. Faz o papel do MinerPOW das
 * aulas: decide como um bloco novo entra na cadeia.
 *
 * Só as autoridades declaradas no bloco de génese (DGAV, Laboratório e DOP)
 * podem propor blocos e votar:
 * 1. o proponente roda: o bloco n é proposto pela autoridade n módulo 3;
 * 2. o proponente verifica a transação (regras), cria o bloco e assina o hash;
 * 3. as outras autoridades verificam o bloco e votam (assinam o hash);
 * 4. o bloco entra quando tem assinaturas de quorum() autoridades diferentes
 *    (2 em 3; a do proponente conta como uma).
 *
 * Pressuposto: as autoridades não mentem de propósito (não há nós bizantinos).
 * Para tolerar uma autoridade desonesta seria preciso PBFT, com pelo menos 4.
 *
 * Na PoC as três autoridades estão no mesmo computador: as suas chaves privadas
 * ficam em keys para poderem assinar. Na Fase 3 cada uma estará no seu nó.
 */
public class ProofOfAuthority {

    private final BlockChain chain = new BlockChain();             // a cadeia
    private final String fileName;                                 // ficheiro da cadeia
    private final HashMap<String, PrivateKey> keys = new HashMap<>(); // chave de cada autoridade (DID)

    /**
     * Carrega a cadeia do ficheiro, se já existir.
     *
     * @param fileName ficheiro da cadeia, por exemplo data/blockchain.bc
     * @throws Exception se o ficheiro existir mas não se conseguir ler
     */
    public ProofOfAuthority(String fileName) throws Exception {
        this.fileName = fileName;
        if (new File(fileName).exists()) {
            chain.load(fileName);
        }
    }

    /**
     * Junta a chave privada de uma autoridade, para ela poder propor e votar.
     *
     * @param did DID da autoridade
     * @param key chave privada
     */
    public void addAuthorityKey(String did, PrivateKey key) {
        keys.put(did, key);
    }

    /**
     * Número mínimo de assinaturas diferentes: maioria das autoridades (2 em 3).
     *
     * @param authorities número de autoridades
     * @return quórum
     */
    public static int quorum(int authorities) {
        return authorities / 2 + 1;
    }

    /**
     * Acrescenta uma transação à cadeia, num bloco novo, pelo consenso.
     *
     * @param tx transação assinada pelo autor
     * @return o bloco que entrou
     * @throws Exception se as regras recusarem a transação ou não houver quórum
     */
    public synchronized Block addTransaction(Transaction tx) throws Exception {
        // 1. as autoridades: da génese que está na cadeia, ou da própria transação de génese
        List<Authority> authorities = (tx instanceof GenesisTx g)
                ? g.getAuthorities() : ApisChainServices.getAuthorities(chain);
        // 2. regras dos validadores (lança exceção se a transação for recusada)
        ApisChainServices.validate(tx, chain);
        // 3. o proponente da vez cria o bloco e assina o hash
        String proposer = authorities.get(chain.getSize() % authorities.size()).getDid();
        if (!keys.containsKey(proposer)) {
            throw new Exception("A chave do proponente " + proposer + " não está neste computador");
        }
        Block block = new Block(chain.getSize(), chain.getLastBlockHash(), tx.toBase64(), proposer);
        block.setSignature(SecurityUtils.sign(block.getCurrentHash(), keys.get(proposer)));
        // 4. as outras autoridades verificam o bloco (íntegro e assinado pelo proponente) e votam
        boolean blockOk = block.isValid() && countSignatures(block, authorities) == 1;
        for (Authority a : authorities) {
            PrivateKey key = keys.get(a.getDid());
            if (blockOk && key != null && !a.getDid().equals(proposer)) {
                block.addVote(new Vote(a.getDid(), SecurityUtils.sign(block.getCurrentHash(), key)));
            }
        }
        // 5. sem quórum o bloco não entra
        int signatures = countSignatures(block, authorities);
        if (signatures < quorum(authorities.size())) {
            throw new Exception("Sem quórum: " + signatures + " assinaturas, são precisas "
                    + quorum(authorities.size()));
        }
        // 6. acrescentar à cadeia e gravar
        if (!chain.add(block)) {
            throw new Exception("A cadeia recusou o bloco #" + block.getIndex());
        }
        chain.save(fileName);
        return block;
    }

    /**
     * Conta as assinaturas de autoridades DIFERENTES num bloco: a do proponente
     * mais os votos. Assinaturas falsas, repetidas ou de quem não é autoridade
     * não contam.
     *
     * @param block bloco
     * @param authorities autoridades da rede
     * @return número de assinaturas válidas
     */
    public static int countSignatures(Block block, List<Authority> authorities) {
        HashSet<String> signers = new HashSet<>();
        for (Authority a : authorities) {
            try {
                // 1. a autoridade é o proponente e assinou o bloco?
                if (a.getDid().equals(block.getValidator())
                        && SecurityUtils.verify(block.getCurrentHash(), block.getSignature(),
                                SecurityUtils.publicKeyFromBase64(a.getPublicKey()))) {
                    signers.add(a.getDid());
                }
                // 2. a autoridade votou no bloco? (o HashSet ignora votos repetidos)
                for (Vote v : block.getVotes()) {
                    if (a.getDid().equals(v.getValidator())
                            && SecurityUtils.verify(block.getCurrentHash(), v.getSignature(),
                                    SecurityUtils.publicKeyFromBase64(a.getPublicKey()))) {
                        signers.add(a.getDid());
                    }
                }
            } catch (Exception e) {
                // chave pública mal formada: a assinatura não conta
            }
        }
        return signers.size();
    }

    public BlockChain getChain() {
        return chain;
    }
}
