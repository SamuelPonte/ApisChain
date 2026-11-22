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
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.util.HashSet;
import java.util.List;

/**
 * Regras do consenso Proof of Authority (PoA) com votação.
 *
 * Só as autoridades declaradas no bloco de génese (DGAV, Laboratório e DOP)
 * podem propor blocos e votar. Um bloco entra na cadeia quando tem as
 * assinaturas de pelo menos quorum() autoridades diferentes: a do proponente
 * conta como uma, as outras são os votos (Vote).
 *
 * Pressuposto (declarado no relatório): as autoridades não mentem de propósito
 * (não há nós bizantinos). Para tolerar 1 autoridade desonesta seria preciso
 * PBFT com pelo menos 4 validadores.
 *
 * Esta classe só tem métodos estáticos: são as regras do protocolo, iguais em
 * todos os nós. Cada nó (Node) usa-as para verificar os blocos sozinho.
 */
public class ProofOfAuthority {

    /**
     * Autoridades da rede, pela ordem em que estão na génese (a ordem conta
     * para a rotação do proponente).
     *
     * @param chain cadeia do nó
     * @param block bloco a verificar (é dele que se leem as autoridades quando
     *              a cadeia ainda está vazia, ou seja, quando é o bloco de génese)
     * @return lista das autoridades
     * @throws Exception se não houver génese
     */
    public static List<Authority> getAuthorities(BlockChain chain, Block block) throws Exception {
        // 1. a génese é o bloco 0 da cadeia; se a cadeia estiver vazia, é o próprio bloco
        Block genesisBlock = (chain.getSize() > 0) ? chain.get(0) : block;
        // 2. a primeira transação do bloco de génese é a GenesisTx
        Transaction tx = Transaction.fromBase64(genesisBlock.getTransactions().get(0));
        if (!(tx instanceof GenesisTx g)) {
            throw new Exception("O bloco 0 não tem a transação de génese");
        }
        return g.getAuthorities();
    }

    /**
     * Número mínimo de assinaturas diferentes para um bloco entrar: maioria
     * das autoridades. Com 3 autoridades dá 2 (2 em 3).
     *
     * @param authorities número de autoridades
     * @return quórum
     */
    public static int quorum(int authorities) {
        return authorities / 2 + 1;
    }

    /**
     * Proponente da vez: rotação pelas autoridades. O bloco de altura h é
     * proposto pela autoridade h módulo n; se essa estiver desligada, a rede
     * tenta a seguinte (skip = 1), e assim por diante.
     *
     * @param authorities autoridades, pela ordem da génese
     * @param height altura do bloco a propor (índice que vai ter na cadeia)
     * @param skip quantas autoridades à frente (0 = a da vez)
     * @return DID do proponente
     */
    public static String selectProposer(List<Authority> authorities, int height, int skip) {
        return authorities.get((height + skip) % authorities.size()).getDid();
    }

    /**
     * Procura uma autoridade pelo DID.
     *
     * @return a autoridade, ou null se o DID não for uma autoridade
     */
    private static Authority findAuthority(List<Authority> authorities, String did) {
        for (Authority a : authorities) {
            if (a.getDid().equals(did)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Verifica se uma assinatura sobre o hash do bloco é de uma autoridade.
     */
    private static boolean isSignedBy(Block block, String signature, Authority a) {
        try {
            return SecurityUtils.verify(block.getCurrentHash(), signature,
                    SecurityUtils.publicKeyFromBase64(a.getPublicKey()));
        } catch (Exception e) {
            return false;  // chave pública mal formada
        }
    }

    /**
     * Verifica um bloco proposto ANTES de votar: o bloco encaixa na cadeia,
     * não foi alterado, foi proposto e assinado por uma autoridade, e todas as
     * transações passam nas regras dos validadores.
     *
     * Não verifica os votos (quando um validador vota, o bloco ainda não os tem).
     *
     * @param block bloco proposto
     * @param chain cadeia do nó que verifica
     * @throws Exception com a razão, se o bloco for recusado
     */
    public static void verifyBlock(Block block, BlockChain chain) throws Exception {
        // 1. o bloco encaixa no fim da cadeia e não foi alterado
        if (block.getIndex() != chain.getSize()) {
            throw new Exception("Bloco #" + block.getIndex() + " fora de ordem (a cadeia tem "
                    + chain.getSize() + " blocos)");
        }
        if (!block.getPreviousHash().equals(chain.getLastBlockHash())) {
            throw new Exception("O bloco #" + block.getIndex() + " não aponta para o último bloco");
        }
        if (!block.isValid()) {
            throw new Exception("O bloco #" + block.getIndex() + " foi alterado (hash ou Merkle)");
        }
        // 2. o proponente é uma autoridade e assinou o hash do bloco
        List<Authority> authorities = getAuthorities(chain, block);
        Authority proposer = findAuthority(authorities, block.getValidator());
        if (proposer == null) {
            throw new Exception(block.getValidator() + " não é uma autoridade: não pode propor blocos");
        }
        if (!isSignedBy(block, block.getSignature(), proposer)) {
            throw new Exception("A assinatura do proponente " + block.getValidator() + " é inválida");
        }
        // 3. cada transação passa nas regras, pela ordem do bloco; o estado vai
        //    sendo atualizado para apanhar, por exemplo, duas vendas do mesmo
        //    frasco dentro do mesmo bloco
        LedgerState state = new LedgerState(chain);
        for (String txt : block.getTransactions()) {
            Transaction tx = Transaction.fromBase64(txt);
            Transaction result = Rules.check(tx, state, block.getValidator());
            if (result != tx) {
                // as regras transformaram a transação num alerta: o proponente
                // devia ter posto o alerta no bloco em vez da transação
                throw new Exception("O bloco tem uma transação que devia ser um alerta de fraude: " + tx);
            }
            state.apply(tx);
        }
    }

    /**
     * Conta as assinaturas de autoridades DIFERENTES num bloco: a do
     * proponente mais os votos válidos. Votos repetidos, de quem não é
     * autoridade ou com assinatura falsa não contam.
     *
     * @param block bloco
     * @param authorities autoridades da rede
     * @return número de assinaturas válidas
     */
    public static int countSignatures(Block block, List<Authority> authorities) {
        HashSet<String> signers = new HashSet<>();
        // 1. a assinatura do proponente
        Authority proposer = findAuthority(authorities, block.getValidator());
        if (proposer != null && isSignedBy(block, block.getSignature(), proposer)) {
            signers.add(proposer.getDid());
        }
        // 2. os votos (o HashSet ignora uma autoridade que vote duas vezes)
        for (Vote v : block.getVotes()) {
            Authority a = findAuthority(authorities, v.getValidator());
            if (a != null && isSignedBy(block, v.getSignature(), a)) {
                signers.add(a.getDid());
            }
        }
        return signers.size();
    }

    /**
     * Verificação completa de um bloco que vai entrar na cadeia: verifyBlock
     * mais o quórum de assinaturas.
     *
     * @param block bloco
     * @param chain cadeia do nó que verifica
     * @throws Exception com a razão, se o bloco for recusado
     */
    public static void verifyCommit(Block block, BlockChain chain) throws Exception {
        verifyBlock(block, chain);
        List<Authority> authorities = getAuthorities(chain, block);
        int signatures = countSignatures(block, authorities);
        int needed = quorum(authorities.size());
        if (signatures < needed) {
            throw new Exception("O bloco #" + block.getIndex() + " só tem " + signatures
                    + " assinaturas de autoridades; são precisas " + needed);
        }
    }
}
