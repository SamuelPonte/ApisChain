//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.consensus.Node;
import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.service.ApisChainService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Explorador da cadeia: os blocos da cópia de um nó e as transações do bloco
 * escolhido. Tem o botão de demonstração que adultera uma transação.
 */
public class ChainPanel extends BasePanel {

    private static final String[] COLUMNS = {"#", "Data", "Anterior", "Hash", "Validador", "Votos", "Tx", "Estado"};
    // transação original de cada bloco adulterado, para a repor (por bloco, não por
    // conteúdo). É static porque trocar de interveniente cria painéis novos
    private static final Map<Block, String> ORIGINALS = new IdentityHashMap<>();

    private final JComboBox<String> cbNode = new JComboBox<>();
    private final JLabel lbState = new JLabel(" ");
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextArea txtBlock = new JTextArea(11, 60);
    private final JButton btUpdate = new JButton("Atualizar");
    private final JButton btTamper = new JButton("Adulterar a 1.ª transação do bloco");
    private final JButton btRestore = new JButton("Repor as transações adulteradas");

    public ChainPanel(ApisChainService service) {
        super(service, null);
        initComponents();
        updateChain();
    }

    private void initComponents() {
        // 1. de que nó é a cópia (cada um tem a sua); o N4 é a que o consumidor lê
        for (Node n : service.getNetwork().getNodes()) {
            cbNode.addItem(n.getId());
        }
        cbNode.setSelectedItem("N4");
        JPanel top = section("Cópia da cadeia");
        addRow(top, "Nó", cbNode);
        addRow(top, "Integridade", lbState);
        addRow(top, "", btUpdate);
        addSection(top);

        // 2. blocos
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // larguras preferidas de cada coluna (a data e o validador precisam de mais espaço)
        int[] widths = {30, 110, 80, 80, 180, 40, 30, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 180));
        JPanel blocks = new JPanel(new BorderLayout());
        blocks.setBorder(BorderFactory.createTitledBorder("Blocos"));
        blocks.add(scroll, BorderLayout.CENTER);
        addSection(blocks);

        // 3. transações do bloco escolhido
        txtBlock.setEditable(false);
        JPanel block = new JPanel(new BorderLayout());
        block.setBorder(BorderFactory.createTitledBorder("Bloco escolhido"));
        block.add(new JScrollPane(txtBlock), BorderLayout.CENTER);
        addSection(block);

        // 4. demonstração
        JPanel demo = section("Demonstração");
        addRow(demo, "Como um atacante com acesso ao nó", btTamper);
        addRow(demo, "Repor antes de sair", btRestore);
        addSection(demo);

        cbNode.addActionListener(evt -> updateChain());
        btUpdate.addActionListener(evt -> updateChain());
        table.getSelectionModel().addListSelectionListener(evt -> showBlock());
        btTamper.addActionListener(evt -> btTamperActionPerformed());
        btRestore.addActionListener(evt -> btRestoreActionPerformed());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                updateChain();
            }
        });
    }

    /**
     * Cadeia do nó escolhido.
     */
    private BlockChain chain() {
        return service.getNetwork().getNodes().get(cbNode.getSelectedIndex()).getChain();
    }

    /**
     * Preenche a tabela dos blocos e mostra o primeiro bloco inválido.
     */
    private void updateChain() {
        int selected = table.getSelectedRow();
        model.setRowCount(0);
        synchronized (service.getNetwork()) {
            BlockChain chain = chain();
            SimpleDateFormat date = new SimpleDateFormat("dd/MM HH:mm:ss");
            for (Block b : chain.getChain()) {
                model.addRow(new Object[]{b.getIndex(), date.format(new Date(b.getTimestamp())),
                    ChainView.shorten(b.getPreviousHash(), 8), ChainView.shorten(b.getCurrentHash(), 8),
                    b.getValidator(), b.getVotes().size(), b.getTransactions().size(),
                    b.isValid() ? "OK" : "CORROMPIDO"});
            }
            int invalid = chain.getInvalidIndex();
            lbState.setText(invalid < 0 ? "íntegra, " + chain.getSize() + " blocos"
                    : "CORROMPIDA: o primeiro bloco inválido é o #" + invalid);
        }
        if (selected >= 0 && selected < model.getRowCount()) {
            table.setRowSelectionInterval(selected, selected);
        }
        showBlock();
    }

    /**
     * Mostra o bloco escolhido com as transações legíveis.
     */
    private void showBlock() {
        int row = table.getSelectedRow();
        if (row < 0) {
            txtBlock.setText("Escolha um bloco na tabela.");
            return;
        }
        StringBuilder txt = new StringBuilder();
        synchronized (service.getNetwork()) {
            Block b = chain().get(row);
            txt.append("Bloco #").append(b.getIndex()).append(b.isValid() ? " (OK)" : " (CORROMPIDO)");
            txt.append("\nhash anterior: ").append(b.getPreviousHash());
            txt.append("\nhash: ").append(b.getCurrentHash());
            txt.append("\nraiz de Merkle: ").append(b.getMerkleRoot());
            txt.append("\nvalidador: ").append(b.getValidator());
            txt.append("\nvotos: ").append(b.getVotes());
            for (String t : b.getTransactions()) {
                txt.append("\n\n").append(ChainView.describe(t));
            }
        }
        txtBlock.setText(txt.toString());
        txtBlock.setCaretPosition(0);
    }

    /**
     * Adultera a primeira transação do bloco escolhido, só na cópia deste nó
     * e só em memória. O hash de Merkle deixa de coincidir.
     */
    private void btTamperActionPerformed() {
        int row = table.getSelectedRow();
        if (row < 0) {
            log("escolha um bloco na tabela");
            return;
        }
        synchronized (service.getNetwork()) {
            Block b = chain().get(row);
            // guardar o original só da primeira vez
            ORIGINALS.putIfAbsent(b, b.getTransactions().get(0));
            b.tamperTransaction(0, "ADULTERADA");
        }
        log("bloco #" + row + " de " + cbNode.getSelectedItem() + " adulterado: este nó deixa de aceitar blocos novos");
        updateChain();
    }

    /**
     * Repõe todas as transações adulteradas.
     */
    private void btRestoreActionPerformed() {
        synchronized (service.getNetwork()) {
            for (Map.Entry<Block, String> e : ORIGINALS.entrySet()) {
                e.getKey().tamperTransaction(0, e.getValue());
            }
            log(ORIGINALS.size() + " bloco(s) reposto(s). Se entraram blocos entretanto, desligue e ligue o nó "
                    + "no separador Rede para ele sincronizar");
            ORIGINALS.clear();
        }
        updateChain();
    }
}
