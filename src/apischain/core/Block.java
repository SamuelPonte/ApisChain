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
 * Bloco da ApisChain: guarda UMA transação (em Base64), como o bloco das aulas
 * guarda um dado.
 *
 * Diferenças face ao bloco das aulas:
 * - não tem nonce nem dificuldade, porque o consenso é Proof of Authority
 *   e não Proof of Work (não há mineração);
 * - guarda o índice e a data;
 * - guarda quem o propôs (validator), a assinatura do proponente e os votos
 *   (assinaturas) das outras autoridades.
 */
public class Block implements Serializable {

    private final int index;              // posição na cadeia (0 = génese)
    private final long timestamp;         // data de criação em milissegundos
    private final String previousHash;    // ligação ao bloco anterior
    private final String data;            // transação serializada em Base64
    private final String validator;       // DID da autoridade que propôs o bloco
    private final String currentHash;     // hash do bloco
    private String signature;             // assinatura do proponente sobre currentHash
    private final List<Vote> votes;       // votos das outras autoridades

    /**
     * Cria um bloco novo.
     *
     * @param index posição na cadeia
     * @param previousHash hash do bloco anterior
     * @param data transação em Base64
     * @param validator DID da autoridade que propõe o bloco
     */
    public Block(int index, String previousHash, String data, String validator) {
        // 1. guardar os dados do bloco
        this.index = index;
        this.timestamp = System.currentTimeMillis();
        this.previousHash = previousHash;
        this.data = data;
        this.validator = validator;
        // 2. calcular o hash
        this.currentHash = calculateHash();
        // 3. ainda sem assinatura nem votos: são acrescentados pelo consenso
        this.signature = "";
        this.votes = new ArrayList<>();
    }

    /**
     * Hash do bloco: índice, data, hash anterior, transação e validador.
     *
     * @return hash SHA3-256 em Base64
     */
    public String calculateHash() {
        // o "" no início obriga o Java a juntar como texto; sem ele, index + timestamp
        // seria uma SOMA de números e blocos diferentes podiam dar o mesmo resultado
        return SecurityUtils.getHash("" + index + "|" + timestamp + "|" + previousHash
                + "|" + data + "|" + validator);
    }

    /**
     * O bloco não foi alterado? (o hash guardado tem de ser o hash do conteúdo)
     *
     * @return true se o bloco estiver íntegro
     */
    public boolean isValid() {
        return currentHash.equals(calculateHash());
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

    public int getIndex() {
        return index;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getData() {
        return data;
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
        String date = new SimpleDateFormat("dd/MM HH:mm:ss").format(new Date(timestamp));
        return "#" + index + " [" + date + "] " + currentHash.substring(0, 8) + (isValid() ? " OK" : " CORROMPIDO");
    }

    private static final long serialVersionUID = 202611260001L;
}
