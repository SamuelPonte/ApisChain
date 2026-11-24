//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.service.Actor;
import apischain.service.ApisChainService;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Painel do Certificador DOP: emite o certificado de origem de um lote.
 */
public class DopPanel extends BasePanel {

    private final JComboBox<String> cbLot = new JComboBox<>();
    private final JTextField txtName = new JTextField("Mel da Serra de Tomar DOP (fictícia)");
    private final JTextField txtValidity = new JTextField("2027-12-31");
    private final JButton btIssue = new JButton("Emitir certificado");
    private final JButton btUpdate = new JButton("Atualizar lista de lotes");

    public DopPanel(ApisChainService service, Actor actor) {
        super(service, actor);
        initComponents();
        updateLots();
    }

    private void initComponents() {
        JPanel certificate = section("Emitir certificado de origem");
        addRow(certificate, "Lote", cbLot);
        addRow(certificate, "Denominação", txtName);
        addRow(certificate, "Validade", txtValidity);
        addRow(certificate, "", btIssue);
        addSection(certificate);
        JPanel lots = section("Lotes");
        addRow(lots, "", btUpdate);
        addSection(lots);

        btIssue.addActionListener(evt -> btIssueActionPerformed());
        btUpdate.addActionListener(evt -> updateLots());
    }

    /**
     * Lotes da cadeia que ainda não foram revogados.
     */
    private void updateLots() {
        cbLot.removeAllItems();
        for (String lot : listLots()) {
            cbLot.addItem(lot);
        }
    }

    private void btIssueActionPerformed() {
        // 1. ler os campos na thread do Swing
        String lot = (String) cbLot.getSelectedItem();
        Map<String, String> data = Map.of(
                "denominacao", txtName.getText().trim(),
                "validade", txtValidity.getText().trim());
        if (lot == null) {
            log("não há lotes na cadeia: o apicultor tem de criar um primeiro");
            return;
        }
        // 2. emitir na rede, sem bloquear a janela
        runInBackground(btIssue, () -> {
            String id = service.issueCertificate(actor, lot, data);
            return "certificado " + id + " do lote " + lot + " emitido (bloco #" + lastBlock() + ")";
        });
    }
}
