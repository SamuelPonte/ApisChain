//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em core.Block do Templar2026 (Prof. António Manso)            ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.core;

import apischain.utils.SecurityUtils;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Bloco da ApisChain.
 *
 * Diferenças face ao bloco das aulas:
 * - não tem nonce nem dificuldade, porque o consenso é Proof of Authority
 *   e não Proof of Work (não há mineração);
 * - guarda várias transações e a raiz de Merkle delas;
 * - guarda quem o propôs (validator), a assinatura do proponente e os votos
 *   das outras autoridades.
 *
 * O hash do bloco cobre o cabeçalho (índice, data, hash anterior, raiz de
 * Merkle e validador). As transações ficam protegidas através da raiz de Merkle.
 */
public class Block implements Serializable {

    private final int index;              // posição na cadeia (0 = génese)
    private final long timestamp;         // data de criação em milissegundos
    private final String previousHash;    // ligação ao bloco anterior
    private final List<String> transactions; // transações serializadas em Base64
    private final String merkleRoot;      // raiz de Merkle das transações
    private final String validator;       // DID da autoridade que propôs o bloco
    private final String currentHash;     // hash do cabeçalho
    private String signature;             // assinatura do proponente sobre currentHash
    private final List<Vote> votes;       // votos das outras autoridades

    /**
     * Cria um bloco novo com as transações dadas.
     *
     * @param index posição na cadeia
     * @param previousHash hash do bloco anterior
     * @param transactions transações em Base64 (pelo menos uma)
     * @param validator DID da autoridade que propõe o bloco
     */
    public Block(int index, String previousHash, List<String> transactions, String validator) {
        // 1. um bloco sem transações não faz sentido e a árvore de Merkle não teria raiz
        if (transactions.isEmpty()) {
            throw new IllegalArgumentException("Um bloco tem de ter pelo menos uma transação");
        }
        // 2. guardar os dados do cabeçalho
        this.index = index;
        this.timestamp = System.currentTimeMillis();
        this.previousHash = previousHash;
        this.validator = validator;
        // 3. copiar as transações (para ninguém as alterar por fora)
        this.transactions = new ArrayList<>(transactions);
        // 4. calcular a raiz de Merkle das transações
        this.merkleRoot = new MerkleTree(this.transactions).getRoot();
        // 5. calcular o hash do cabeçalho
        this.currentHash = calculateHash();
        // 6. ainda sem assinatura nem votos: são acrescentados pelo consenso
        this.signature = "";
        this.votes = new ArrayList<>();
    }

    /**
     * Hash do cabeçalho do bloco.
     *
     * @return hash SHA3-256 em Base64
     */
    public String calculateHash() {
        // o "" no início obriga o Java a juntar como texto; sem ele, index + timestamp
        // seria uma SOMA de números e blocos diferentes podiam dar o mesmo resultado
        return SecurityUtils.getHash("" + index + "|" + timestamp + "|" + previousHash
                + "|" + merkleRoot + "|" + validator);
    }

    /**
     * Verifica a integridade interna do bloco: o hash do cabeçalho e a raiz de
     * Merkle têm de corresponder ao conteúdo. As assinaturas e os votos são
     * verificados pelo consenso, que conhece as chaves públicas das autoridades.
     *
     * @return true se o bloco não foi alterado
     */
    public boolean isValid() {
        // 1. o cabeçalho não foi alterado?
        if (!currentHash.equals(calculateHash())) {
            return false;
        }
        // 2. as transações não foram alteradas? (recalcula-se a raiz de Merkle)
        return merkleRoot.equals(new MerkleTree(transactions).getRoot());
    }

    /**
     * Árvore de Merkle das transações (para mostrar e gerar provas).
     *
     * @return árvore de Merkle
     */
    public MerkleTree getMerkleTree() {
        return new MerkleTree(transactions);
    }

    /**
     * Define a assinatura do proponente (chamado pelo consenso).
     *
     * @param signature assinatura em Base64
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * Acrescenta o voto de uma autoridade (chamado pelo consenso).
     *
     * @param vote voto assinado
     */
    public void addVote(Vote vote) {
        votes.add(vote);
    }

    /**
     * APENAS PARA DEMONSTRAÇÃO: altera uma transação sem recalcular nada,
     * como faria um atacante com acesso ao ficheiro da cadeia.
     *
     * @param i posição da transação
     * @param newData novo conteúdo
     */
    public void tamperTransaction(int i, String newData) {
        transactions.set(i, newData);
    }

    public int getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public List<String> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public String getValidator() {
        return validator;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public String getSignature() {
        return signature;
    }

    public List<Vote> getVotes() {
        return Collections.unmodifiableList(votes);
    }

    @Override
    public String toString() {
        String data = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(timestamp));
        return (isValid() ? "OK " : "CORROMPIDO ")
                + "Bloco #" + index + " [" + data + "] "
                + "anterior=" + previousHash.substring(0, Math.min(8, previousHash.length())) + "… "
                + "hash=" + currentHash.substring(0, 8) + "… "
                + "merkle=" + merkleRoot.substring(0, 8) + "… "
                + transactions.size() + " tx, validador=" + validator
                + ", votos=" + votes.size();
    }

    private static final long serialVersionUID = 202611110003L;
}
