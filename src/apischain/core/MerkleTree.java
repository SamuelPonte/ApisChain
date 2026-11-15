//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Adaptado de blockchain.utils.MerkleTree do BlockChainGUI             ::://
//::  (Prof. António Manso, 2022)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.core;

import apischain.utils.SecurityUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Árvore de Merkle das transações de um bloco.
 *
 * Alterações face à versão das aulas:
 * 1. o hash passa a ser SHA3-256 (antes era String.hashCode);
 * 2. corrigido o isValid, que devolvia false quando o hash estava CERTO
 *    (faltava o ! antes do equals);
 * 3. os elementos passam a ser só texto (as transações em Base64).
 */
public final class MerkleTree implements Serializable {

    // níveis da árvore: o índice 0 é a raiz, o último nível são as folhas
    private List<List<String>> hashTree;
    // elementos originais (as transações)
    private List<String> elements;

    /**
     * Constrói a árvore de Merkle de uma lista de elementos.
     *
     * @param listOfData elementos (não pode estar vazia)
     */
    public MerkleTree(List<String> listOfData) {
        // 1. criar as listas vazias
        hashTree = new ArrayList<>();
        elements = new ArrayList<>(listOfData);
        // 2. calcular o hash de cada elemento: são as folhas da árvore
        List<String> leaves = new ArrayList<>();
        for (String elem : listOfData) {
            leaves.add(getHashValue(elem));
        }
        // 3. construir os níveis de baixo para cima até chegar à raiz
        makeTree(leaves);
    }

    /**
     * Raiz da árvore: um único hash que resume todos os elementos.
     *
     * @return hash da raiz
     */
    public String getRoot() {
        // a raiz está no nível 0, posição 0
        return hashTree.get(0).get(0);
    }

    /**
     * Constrói a árvore de forma recursiva, um nível de cada vez.
     *
     * @param hashList hashes do nível atual
     */
    private void makeTree(List<String> hashList) {
        // 1. o nível atual passa a ser o primeiro da lista (fica por cima dos anteriores)
        hashTree.add(0, hashList);
        // 2. se só sobra um hash, chegámos à raiz
        if (hashList.size() <= 1) {
            return;
        }
        // 3. construir o nível de cima juntando os hashes dois a dois
        List<String> newLevel = new ArrayList<>();
        for (int i = 0; i < hashList.size(); i += 2) {
            // elemento da esquerda
            String data = hashList.get(i);
            // se existir elemento à direita, junta-se (concatenação de texto)
            if (i + 1 < hashList.size()) {
                data = data + hashList.get(i + 1);
            }
            // o pai é o hash da junção (num nível ímpar, o último sobe sozinho)
            newLevel.add(getHashValue(data));
        }
        // 4. repetir para o novo nível
        makeTree(newLevel);
    }

    /**
     * Prova de Merkle de um elemento: os irmãos ao longo do caminho até à raiz,
     * terminando na própria raiz.
     *
     * @param data elemento
     * @return lista de hashes da prova (vazia se o elemento não existir)
     */
    public List<String> getProof(String data) {
        List<String> proof = new ArrayList<>();
        // 1. procurar a posição do elemento nas folhas
        int index = elements.indexOf(data);
        if (index < 0) {
            return proof; // não pertence à árvore
        }
        // 2. subir desde as folhas (último nível) até à raiz
        return getProof(index, hashTree.size() - 1, proof);
    }

    private List<String> getProof(int index, int level, List<String> proof) {
        if (level > 0) {
            if (index % 2 == 0) {
                // índice par: o nó está à esquerda, o irmão está à direita
                if (index + 1 < hashTree.get(level).size()) {
                    proof.add(hashTree.get(level).get(index + 1));
                } else {
                    // não tem irmão (nível ímpar): usa-se o próprio hash
                    proof.add(hashTree.get(level).get(index));
                }
            } else {
                // índice ímpar: o nó está à direita, o irmão está à esquerda
                proof.add(hashTree.get(level).get(index - 1));
            }
            // o pai está no nível de cima, na posição index / 2
            return getProof(index / 2, level - 1, proof);
        } else {
            // chegámos ao topo: a prova termina com a raiz
            proof.add(getRoot());
            return proof;
        }
    }

    /**
     * Verifica a prova de Merkle de um elemento.
     *
     * @param data elemento
     * @param proof prova obtida com getProof
     * @return true se a prova levar até à raiz
     */
    public static boolean isProofValid(String data, List<String> proof) {
        if (proof.isEmpty()) {
            return false;
        }
        // começa no hash do elemento e sobe nível a nível
        return isProofValid(getHashValue(data), proof, 0);
    }

    private static boolean isProofValid(String currentHash, List<String> proof, int i) {
        // 1. no último elemento da prova está a raiz: tem de coincidir
        if (i == proof.size() - 1) {
            return currentHash.equals(proof.get(proof.size() - 1));
        }
        // 2. caso especial: nó sem irmão sobe sozinho (hash de si próprio)
        if (proof.get(i).equals(currentHash)
                && isProofValid(getHashValue(currentHash), proof, i + 1)) {
            return true;
        }
        // 3. tentar com o irmão à direita
        if (isProofValid(getHashValue(currentHash + proof.get(i)), proof, i + 1)) {
            return true;
        }
        // 4. tentar com o irmão à esquerda
        return isProofValid(getHashValue(proof.get(i) + currentHash), proof, i + 1);
    }

    /**
     * Verifica se a árvore está íntegra (folhas e todos os níveis).
     *
     * @return true se todos os hashes estiverem certos
     */
    public boolean isValid() {
        // 1. cada folha tem de ser o hash do elemento correspondente
        List<String> leaves = hashTree.get(hashTree.size() - 1);
        for (int i = 0; i < elements.size(); i++) {
            if (!getHashValue(elements.get(i)).equals(leaves.get(i))) {
                return false;
            }
        }
        // 2. cada nó tem de ser o hash da junção dos seus filhos
        for (int level = 0; level < hashTree.size() - 1; level++) {
            for (int index = 0; index < hashTree.get(level).size(); index++) {
                // filho da esquerda
                String dataLeafs = hashTree.get(level + 1).get(index * 2);
                // filho da direita, se existir
                if (index * 2 + 1 < hashTree.get(level + 1).size()) {
                    dataLeafs = dataLeafs + hashTree.get(level + 1).get(index * 2 + 1);
                }
                // CORREÇÃO: na versão das aulas faltava o "!" e a árvore certa dava inválida
                if (!hashTree.get(level).get(index).equals(getHashValue(dataLeafs))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Função de hash da árvore (SHA3-256 em Base64).
     *
     * @param data texto
     * @return hash
     */
    public static String getHashValue(String data) {
        return SecurityUtils.getHash(data);
    }

    public List<List<String>> getHashTree() {
        return hashTree;
    }

    public List<String> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        // mostra a árvore nível a nível, da raiz para as folhas (hashes abreviados)
        StringBuilder txt = new StringBuilder();
        for (int i = 0; i < hashTree.size(); i++) {
            txt.append("nível ").append(i).append(": ");
            for (String h : hashTree.get(i)) {
                txt.append(h, 0, 8).append("…  ");
            }
            txt.append("\n");
        }
        return txt.toString();
    }

    private static final long serialVersionUID = 202611110001L;
}
