//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em blockchain.GUI.MerkleTreePanel do BlockChainGUI            ::://
//::  (Prof. António Manso, 2022)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.core.Block;
import apischain.core.MerkleTree;
import apischain.service.ApisChainService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Árvore de Merkle do bloco escolhido e prova de Merkle de uma transação.
 *
 * A prova só vale se acabar na raiz GUARDADA NO BLOCO: isProofValid, por si só,
 * compara com a raiz que vem dentro da própria prova.
 */
public class MerkleTreePanel extends BasePanel {

    private final JComboBox<String> cbBlock = new JComboBox<>();
    private final JComboBox<String> cbTx = new JComboBox<>();
    private final MerkleGraphics graphics = new MerkleGraphics();
    private final JTextArea txtProof = new JTextArea(8, 60);

    private final List<Block> blocks = new ArrayList<>();  // blocos pela ordem da lista
    private boolean updating = false;                      // a preencher listas: ignorar eventos

    public MerkleTreePanel(ApisChainService service) {
        super(service, null);
        initComponents();
        updateBlocks();
    }

    private void initComponents() {
        JPanel choose = section("Bloco e transação");
        addRow(choose, "Bloco", cbBlock);
        addRow(choose, "Transação", cbTx);
        addSection(choose);

        graphics.setPreferredSize(new Dimension(700, 260));
        JPanel tree = new JPanel(new BorderLayout());
        tree.setBorder(BorderFactory.createTitledBorder("Árvore (verde: hashes da prova)"));
        tree.add(graphics, BorderLayout.CENTER);
        addSection(tree);

        txtProof.setEditable(false);
        JPanel proof = new JPanel(new BorderLayout());
        proof.setBorder(BorderFactory.createTitledBorder("Prova de Merkle"));
        proof.add(new JScrollPane(txtProof), BorderLayout.CENTER);
        addSection(proof);

        cbBlock.addActionListener(evt -> updateTransactions());
        cbTx.addActionListener(evt -> updateTree());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                updateBlocks();
            }
        });
    }

    /**
     * Blocos da cadeia, do mais recente para o mais antigo.
     */
    private void updateBlocks() {
        int selected = Math.max(0, cbBlock.getSelectedIndex());
        updating = true;
        cbBlock.removeAllItems();
        blocks.clear();
        try {
            synchronized (service.getNetwork()) {
                for (Block b : service.getChain().getChain()) {
                    blocks.add(0, b);
                }
            }
            for (Block b : blocks) {
                cbBlock.addItem("Bloco #" + b.getIndex() + " (" + b.getTransactions().size() + " tx)");
            }
        } catch (Exception ex) {
            log("não foi possível ler a cadeia: " + ex.getMessage());
        }
        if (selected < cbBlock.getItemCount()) {
            cbBlock.setSelectedIndex(selected);
        }
        updating = false;
        updateTransactions();
    }

    /**
     * Transações do bloco escolhido.
     */
    private void updateTransactions() {
        int i = cbBlock.getSelectedIndex();
        if (updating || i < 0) {
            return;
        }
        updating = true;
        cbTx.removeAllItems();
        int n = 1;
        for (String t : blocks.get(i).getTransactions()) {
            cbTx.addItem(n + ": " + ChainView.shortName(t));
            n++;
        }
        updating = false;
        updateTree();
    }

    /**
     * Desenha a árvore e mostra a prova da transação escolhida.
     */
    private void updateTree() {
        int i = cbBlock.getSelectedIndex();
        int j = cbTx.getSelectedIndex();
        if (updating || i < 0 || j < 0) {
            return;
        }
        Block block = blocks.get(i);
        String tx = block.getTransactions().get(j);
        MerkleTree tree = block.getMerkleTree();
        List<String> proof = tree.getProof(tx);
        graphics.setTree(tree, tx, proof);
        // a prova tem de ser válida E acabar na raiz que está no cabeçalho do bloco
        boolean valid = MerkleTree.isProofValid(tx, proof)
                && proof.get(proof.size() - 1).equals(block.getMerkleRoot());
        StringBuilder txt = new StringBuilder();
        txt.append("hash da transação: ").append(MerkleTree.getHashValue(tx));
        for (int k = 0; k < proof.size() - 1; k++) {
            txt.append("\nirmão ").append(k + 1).append(": ").append(proof.get(k));
        }
        txt.append("\nraiz da prova: ").append(proof.get(proof.size() - 1));
        txt.append("\nraiz no bloco: ").append(block.getMerkleRoot());
        txt.append(valid ? "\nVÁLIDA: a transação está neste bloco" : "\nINVÁLIDA: a raiz não é a do bloco");
        txtProof.setText(txt.toString());
        txtProof.setCaretPosition(0);
    }
}
