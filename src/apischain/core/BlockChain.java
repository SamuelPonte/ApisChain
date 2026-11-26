//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em BlockChain do Templar2026 e do BlockChainGUI               ::://
//::  (Prof. António Manso)                                                 ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cadeia de blocos (o "livro-razão").
 *
 * A cadeia só aceita blocos que encaixam no fim (índice e hash anterior certos)
 * e que estão íntegros. Quem decide QUE bloco entra é o consenso (PoA); a
 * cadeia limita-se a guardar e a verificar a ligação entre blocos.
 */
public class BlockChain implements Serializable {

    // hash "anterior" do bloco de génese, que não tem bloco anterior
    public static final String GENESIS_PREVIOUS_HASH = String.format("%08d", 0);

    // lista de blocos, do génese (índice 0) ao último
    private ArrayList<Block> chain = new ArrayList<>();

    /**
     * Hash do último bloco (ou o valor do génese se a cadeia estiver vazia).
     *
     * @return hash a usar como previousHash do próximo bloco
     */
    public String getLastBlockHash() {
        if (chain.isEmpty()) {
            return GENESIS_PREVIOUS_HASH;
        }
        return chain.get(chain.size() - 1).getCurrentHash();
    }

    /**
     * Acrescenta um bloco ao fim da cadeia, se encaixar e estiver íntegro.
     *
     * @param block bloco aprovado pelo consenso
     * @return true se foi aceite
     */
    public boolean add(Block block) {
        // 1. o índice tem de ser o seguinte (evita blocos repetidos ou fora de ordem)
        if (block.getIndex() != chain.size()) {
            return false;
        }
        // 2. o bloco tem de apontar para o hash do último bloco
        if (!block.getPreviousHash().equals(getLastBlockHash())) {
            return false;
        }
        // 3. o bloco não pode ter sido alterado
        if (!block.isValid()) {
            return false;
        }
        // 4. tudo certo: entra na cadeia
        chain.add(block);
        return true;
    }

    /**
     * Procura o primeiro bloco inválido da cadeia.
     *
     * @return índice do primeiro bloco inválido, ou -1 se a cadeia estiver íntegra
     */
    public int getInvalidIndex() {
        String previous = GENESIS_PREVIOUS_HASH;
        for (int i = 0; i < chain.size(); i++) {
            Block b = chain.get(i);
            // o bloco foi alterado OU não aponta para o anterior: a cadeia parte-se aqui
            if (!b.isValid() || !b.getPreviousHash().equals(previous)) {
                return i;
            }
            previous = b.getCurrentHash();
        }
        return -1;
    }

    /**
     * A cadeia está íntegra?
     *
     * @return true se todos os blocos forem válidos e estiverem ligados
     */
    public boolean isValid() {
        return getInvalidIndex() < 0;
    }

    public int getSize() {
        return chain.size();
    }

    public Block get(int index) {
        return chain.get(index);
    }

    public Block getLastBlock() {
        return chain.isEmpty() ? null : chain.get(chain.size() - 1);
    }

    public List<Block> getChain() {
        return Collections.unmodifiableList(chain);
    }

    /**
     * Grava a cadeia num ficheiro (serialização de objetos, como nas aulas).
     *
     * @param fileName nome do ficheiro
     * @throws Exception erro de escrita
     */
    public void save(String fileName) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(chain);
        }
    }

    /**
     * Carrega a cadeia de um ficheiro.
     *
     * @param fileName nome do ficheiro
     * @throws Exception erro de leitura
     */
    @SuppressWarnings("unchecked")
    public void load(String fileName) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            this.chain = (ArrayList<Block>) in.readObject();
        }
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder("BlockChain (" + chain.size() + " blocos)\n");
        for (Block block : chain) {
            txt.append(block).append("\n");
        }
        return txt.toString();
    }

    private static final long serialVersionUID = 202611110004L;
}
