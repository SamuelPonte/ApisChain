//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.ledger.LedgerState;
import apischain.ledger.LotBalance;
import apischain.ledger.Rules;
import apischain.service.Actor;
import apischain.service.ApisChainService;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Painel do apicultor: cria lotes, embala frascos e vê o balanço de massa.
 * Tem também o botão da fraude 3 (embalar mais do que se produziu).
 */
public class BeekeeperPanel extends BasePanel {

    // criar lote
    private final JComboBox<String> cbApiary = new JComboBox<>();
    private final JTextField txtHarvest = new JTextField("100");
    private final JTextField txtFloral = new JTextField("urze");
    private final JTextField txtDate = new JTextField("setembro de 2026");
    private final JButton btLot = new JButton("Criar lote");
    // embalar
    private final JComboBox<String> cbLot = new JComboBox<>();
    private final JLabel lbBalance = new JLabel(" ");
    private final JTextField txtFirst = new JTextField("1");
    private final JTextField txtLast = new JTextField("10");
    private final JTextField txtJarKg = new JTextField("0.5");
    private final JButton btPack = new JButton("Embalar");
    // fraude 3
    private final JButton btFraud = new JButton("Fraude 3: embalar mais do que se produziu");

    public BeekeeperPanel(ApisChainService service, Actor actor) {
        super(service, actor);
        initComponents();
        updateLists();
    }

    private void initComponents() {
        JPanel lot = section("Criar lote");
        addRow(lot, "Apiário", cbApiary);
        addRow(lot, "Kg colhidos", txtHarvest);
        addRow(lot, "Origem floral", txtFloral);
        addRow(lot, "Colheita", txtDate);
        addRow(lot, "", btLot);
        addSection(lot);

        JPanel pack = section("Embalar frascos");
        addRow(pack, "Lote", cbLot);
        addRow(pack, "Balanço de massa", lbBalance);
        addRow(pack, "Primeiro frasco", txtFirst);
        addRow(pack, "Último frasco", txtLast);
        addRow(pack, "Kg por frasco", txtJarKg);
        addRow(pack, "", btPack);
        addSection(pack);

        JPanel fraud = section("Demonstração");
        addRow(fraud, "Usa o lote escolhido em cima", btFraud);
        addSection(fraud);

        btLot.addActionListener(evt -> btLotActionPerformed());
        btPack.addActionListener(evt -> btPackActionPerformed());
        btFraud.addActionListener(evt -> btFraudActionPerformed());
        cbLot.addActionListener(evt -> updateBalance());
        // ao voltar a este separador, as listas podem ter mudado
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                updateLists();
            }
        });
    }

    /**
     * Apiários e lotes deste apicultor que não estão revogados.
     */
    private void updateLists() {
        String selected = (String) cbLot.getSelectedItem();
        cbApiary.removeAllItems();
        cbLot.removeAllItems();
        try {
            LedgerState state = service.getState();
            for (IssueCredentialTx a : state.getCredentials("REGISTO_APIARIO", actor.getDid())) {
                if (!state.isRevoked(a.getCredentialId())) {
                    cbApiary.addItem(a.getCredentialId());
                }
                // os lotes têm o apiário como titular
                for (IssueCredentialTx l : state.getCredentials("LOTE", a.getCredentialId())) {
                    if (l.getAuthor().equals(actor.getDid()) && !state.isRevoked(l.getCredentialId())) {
                        cbLot.addItem(l.getCredentialId());
                    }
                }
            }
        } catch (Exception ex) {
            log("não foi possível ler a cadeia: " + ex.getMessage());
        }
        // manter o lote que estava escolhido, se ainda existir
        if (selected != null) {
            cbLot.setSelectedItem(selected);
        }
        updateBalance();
    }

    /**
     * Balanço do lote escolhido e próximo número de frasco livre.
     */
    private void updateBalance() {
        String lot = (String) cbLot.getSelectedItem();
        if (lot == null) {
            lbBalance.setText("sem lotes");
            return;
        }
        try {
            LedgerState state = service.getState();
            LotBalance b = state.lotBalance(lot);
            lbBalance.setText(b.toString());
            int next = nextJar(state, lot);
            txtFirst.setText(String.valueOf(next));
            txtLast.setText(String.valueOf(next + 9));
        } catch (Exception ex) {
            lbBalance.setText("não foi possível ler: " + ex.getMessage());
        }
    }

    /**
     * Primeiro número de frasco ainda não usado num lote.
     */
    private static int nextJar(LedgerState state, String lot) {
        int last = 0;
        for (PackTx p : state.getPacks(lot)) {
            last = Math.max(last, p.getLastJar());
        }
        return last + 1;
    }

    /**
     * Converte um número escrito com vírgula ou com ponto.
     */
    private static double number(JTextField field) {
        return Double.parseDouble(field.getText().trim().replace(',', '.'));
    }

    private void btLotActionPerformed() {
        // 1. ler os campos na thread do Swing
        String apiary = (String) cbApiary.getSelectedItem();
        if (apiary == null) {
            log("não há apiários: a DGAV tem de registar um primeiro");
            return;
        }
        double kg;
        try {
            kg = number(txtHarvest);
        } catch (NumberFormatException ex) {
            log("os kg colhidos têm de ser um número");
            return;
        }
        Map<String, String> data = Map.of(
                "origemFloral", txtFloral.getText().trim(),
                "colheita", txtDate.getText().trim());
        // 2. criar na rede, sem bloquear a janela
        runInBackground(btLot, () -> {
            String id = service.createLot(actor, apiary, kg, data);
            updateLater();
            return "lote " + id + " criado com " + kg + " kg";
        });
    }

    private void btPackActionPerformed() {
        String lot = (String) cbLot.getSelectedItem();
        if (lot == null) {
            log("não há lotes: crie um primeiro");
            return;
        }
        int first;
        int last;
        double kg;
        try {
            first = Integer.parseInt(txtFirst.getText().trim());
            last = Integer.parseInt(txtLast.getText().trim());
            kg = number(txtJarKg);
        } catch (NumberFormatException ex) {
            log("os frascos e os kg têm de ser números");
            return;
        }
        runInBackground(btPack, () -> {
            PackTx p = service.packJars(actor, lot, first, last, kg);
            updateLater();
            return p.getJarCount() + " frascos embalados, de " + p.getJarId(first) + " a " + p.getJarId(last);
        });
    }

    /**
     * Fraude 3: tenta embalar mais 10 kg do que o balanço de massa permite,
     * em frascos de 1 kg. Os validadores têm de recusar.
     */
    private void btFraudActionPerformed() {
        String lot = (String) cbLot.getSelectedItem();
        if (lot == null) {
            log("não há lotes: crie um primeiro");
            return;
        }
        runInBackground(btFraud, () -> {
            LedgerState state = service.getState();
            int first = nextJar(state, lot);
            // 10 kg acima do que resta, já a contar com a tolerância
            LotBalance b = state.lotBalance(lot);
            double limit = b.getHarvestedKg() * (1 + Rules.TOLERANCE) - b.getPackedKg();
            int jars = (int) Math.ceil(Math.max(0, limit)) + 10;
            service.packJars(actor, lot, first, first + jars - 1, 1.0);
            return "A FRAUDE PASSOU: foram embalados " + jars + " kg a mais";
        });
    }

    /**
     * Atualiza as listas depois de uma operação (chamado noutra thread).
     */
    private void updateLater() {
        SwingUtilities.invokeLater(this::updateLists);
    }
}
