//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.consensus.LocalNetwork;
import apischain.consensus.Node;
import apischain.service.ApisChainService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Rede: os 4 nós, o tamanho da cadeia de cada um, o botão para os desligar e
 * ligar, e o registo do que a rede fez (propostas, votos, recusas).
 */
public class NetworkPanel extends BasePanel {

    private static final String[] COLUMNS = {"Nó", "Tipo", "Estado", "Blocos", "Último hash", "Cadeia"};

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JButton btToggle = new JButton("Ligar ou desligar o nó escolhido");
    private final JTextArea txtNetwork = new JTextArea(13, 60);
    // atualiza o ecrã de segundo a segundo, só enquanto o painel está visível
    private final Timer timer = new Timer(1000, evt -> {
        if (isShowing()) {
            updateNetwork();
        }
    });
    private int shown = 0;  // linhas do registo da rede já mostradas

    public NetworkPanel(ApisChainService service) {
        super(service, null);
        initComponents();
        updateNetwork();
    }

    private void initComponents() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(700, 110));
        JPanel nodes = new JPanel(new BorderLayout(4, 4));
        nodes.setBorder(BorderFactory.createTitledBorder("Nós"));
        nodes.add(scroll, BorderLayout.CENTER);
        nodes.add(btToggle, BorderLayout.SOUTH);
        addSection(nodes);

        txtNetwork.setEditable(false);
        JPanel net = new JPanel(new BorderLayout());
        net.setBorder(BorderFactory.createTitledBorder("O que a rede fez"));
        net.add(new JScrollPane(txtNetwork), BorderLayout.CENTER);
        addSection(net);

        btToggle.addActionListener(evt -> btToggleActionPerformed());
    }

    /**
     * O painel entrou numa janela: começar a atualizar.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    /**
     * A janela foi fechada (trocar de interveniente): parar o temporizador.
     */
    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /**
     * Atualiza a tabela dos nós e acrescenta as linhas novas do registo da rede.
     */
    private void updateNetwork() {
        LocalNetwork network = service.getNetwork();
        int selected = table.getSelectedRow();
        List<String> lines;
        // copiar tudo com a rede bloqueada; mostrar depois
        synchronized (network) {
            model.setRowCount(0);
            for (Node n : network.getNodes()) {
                int invalid = n.getChain().getInvalidIndex();
                model.addRow(new Object[]{n.getId(), n.isValidator() ? "validador" : "não validador",
                    n.isOnline() ? "ligado" : "DESLIGADO", n.getChain().getSize(),
                    ChainView.shorten(n.getChain().getLastBlockHash(), 12),
                    invalid < 0 ? "íntegra" : "CORROMPIDA no #" + invalid});
            }
            lines = new ArrayList<>(network.getLog());
        }
        if (selected >= 0) {
            table.setRowSelectionInterval(selected, selected);
        }
        while (shown < lines.size()) {
            txtNetwork.append(lines.get(shown) + "\n");
            shown++;
            txtNetwork.setCaretPosition(txtNetwork.getDocument().getLength());
        }
    }

    private void btToggleActionPerformed() {
        int row = table.getSelectedRow();
        if (row < 0) {
            log("escolha um nó na tabela");
            return;
        }
        Node node = service.getNetwork().getNodes().get(row);
        // ligar sincroniza a cadeia, por isso corre fora da thread do Swing
        runInBackground(btToggle, () -> {
            boolean on = !node.isOnline();
            service.getNetwork().setOnline(node, on);
            return node.getId() + (on ? " ligado" : " desligado");
        });
    }
}
