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
 * Painel do Laboratório: emite a análise de um lote. A análise completa fica
 * fora da cadeia; na cadeia fica o seu hash, assinado pelo Laboratório.
 */
public class LabPanel extends BasePanel {

    private final JComboBox<String> cbLot = new JComboBox<>();
    private final JTextField txtFloral = new JTextField("urze 62%");
    private final JTextField txtHumidity = new JTextField("17.2%");
    private final JComboBox<String> cbAdulteration = new JComboBox<>(new String[]{"não detetada", "detetada"});
    private final JButton btIssue = new JButton("Emitir análise");
    private final JButton btUpdate = new JButton("Atualizar lista de lotes");

    public LabPanel(ApisChainService service, Actor actor) {
        super(service, actor);
        initComponents();
        updateLots();
    }

    private void initComponents() {
        JPanel analysis = section("Emitir análise de um lote");
        addRow(analysis, "Lote", cbLot);
        addRow(analysis, "Origem floral", txtFloral);
        addRow(analysis, "Humidade", txtHumidity);
        addRow(analysis, "Adulteração", cbAdulteration);
        addRow(analysis, "", btIssue);
        addSection(analysis);
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
                "origemFloral", txtFloral.getText().trim(),
                "humidade", txtHumidity.getText().trim(),
                "adulteracao", (String) cbAdulteration.getSelectedItem());
        if (lot == null) {
            log("não há lotes na cadeia: o apicultor tem de criar um primeiro");
            return;
        }
        // 2. emitir na rede, sem bloquear a janela
        runInBackground(btIssue, () -> {
            String id = service.issueAnalysis(actor, lot, data);
            return "análise " + id + " do lote " + lot + " emitida (bloco #" + lastBlock() + ")";
        });
    }
}
