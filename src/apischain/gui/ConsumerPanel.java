//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.identity.Credential;
import apischain.ledger.LedgerState;
import apischain.service.ApisChainService;
import apischain.service.VerificationResult;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Painel do consumidor: lê o código de um frasco (o "QR") e mostra o resultado
 * da verificação. Não precisa de chave. Tem também o botão da fraude 2
 * (análise alterada fora da cadeia), que não precisa de assinar nada.
 */
public class ConsumerPanel extends BasePanel {

    // dado da análise que o falsificador altera
    private static final String FORGED_KEY = "origemFloral";
    // valores originais das análises falsificadas, para os repor. É static porque
    // trocar de interveniente cria painéis novos e o valor original perdia-se
    private static final Map<String, String> ORIGINALS = new HashMap<>();

    private final JComboBox<String> cbJar = new JComboBox<>();
    private final JButton btVerify = new JButton("Verificar frasco");
    private final JLabel lbTitle = new JLabel(" ");
    private final JTextArea txtChecks = new JTextArea(9, 60);
    private final JButton btFraud = new JButton("Fraude 2: falsificar a análise fora da cadeia");
    private final JButton btRestore = new JButton("Repor a análise original");

    public ConsumerPanel(ApisChainService service) {
        super(service, null);
        initComponents();
        updateJars();
    }

    private void initComponents() {
        // 1. frasco: escolhe-se da lista ou escreve-se
        JPanel jar = section("Ler o QR de um frasco");
        cbJar.setEditable(true);
        addRow(jar, "Código do frasco", cbJar);
        addRow(jar, "", btVerify);
        addSection(jar);

        // 2. resultado: título com a cor do nível e a lista de verificações
        JPanel result = new JPanel(new BorderLayout(4, 4));
        result.setBorder(BorderFactory.createTitledBorder("Resultado"));
        lbTitle.setFont(lbTitle.getFont().deriveFont(Font.BOLD, 18f));
        lbTitle.setOpaque(true);
        lbTitle.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        txtChecks.setEditable(false);
        txtChecks.setLineWrap(true);
        txtChecks.setWrapStyleWord(true);
        result.add(lbTitle, BorderLayout.NORTH);
        result.add(new JScrollPane(txtChecks), BorderLayout.CENTER);
        addSection(result);

        // 3. fraude 2
        JPanel fraud = section("Demonstração (usa o frasco escolhido em cima)");
        addRow(fraud, "", btFraud);
        addRow(fraud, "Repor antes de sair", btRestore);
        addSection(fraud);

        btVerify.addActionListener(evt -> btVerifyActionPerformed());
        btFraud.addActionListener(evt -> btFraudActionPerformed());
        btRestore.addActionListener(evt -> btRestoreActionPerformed());
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent evt) {
                updateJars();
            }
        });
    }

    /**
     * Frascos de exemplo da cadeia; mantém o que estava escrito.
     */
    private void updateJars() {
        String selected = selectedJar();
        cbJar.removeAllItems();
        try {
            for (String j : ChainView.exampleJars(service)) {
                cbJar.addItem(j);
            }
        } catch (Exception ex) {
            log("não foi possível ler a cadeia: " + ex.getMessage());
        }
        if (!selected.isEmpty()) {
            cbJar.setSelectedItem(selected);
        }
    }

    private String selectedJar() {
        // numa lista editável, o texto escrito pode ainda não estar "selecionado"
        Object item = cbJar.getEditor().getItem();
        return (item == null) ? "" : item.toString().trim();
    }

    private void btVerifyActionPerformed() {
        String jar = selectedJar();
        if (jar.isEmpty()) {
            log("escreva ou escolha o código do frasco");
            return;
        }
        runInBackground(btVerify, () -> {
            VerificationResult r = service.verifyJar(jar);
            SwingUtilities.invokeLater(() -> showResult(r));
            return "verificação do frasco " + jar + " terminada, resultado " + r.getLevel();
        });
    }

    /**
     * Mostra o resultado (na thread do Swing).
     */
    private void showResult(VerificationResult r) {
        lbTitle.setText(r.getLevel() + ": " + r.getTitle());
        switch (r.getLevel()) {
            case OK:
                lbTitle.setBackground(new Color(200, 255, 200));
                break;
            case AVISO:
                lbTitle.setBackground(new Color(255, 240, 180));
                break;
            default:
                lbTitle.setBackground(new Color(255, 190, 190));
        }
        txtChecks.setText(String.join("\n", r.getChecks()));
        txtChecks.setCaretPosition(0);
    }

    /**
     * Fraude 2: um falsificador muda a origem floral na análise guardada fora
     * da cadeia. O hash deixa de coincidir com o que está ancorado.
     */
    private void btFraudActionPerformed() {
        String jar = selectedJar();
        runInBackground(btFraud, () -> {
            synchronized (service) {
                String id = analysisOf(jar);
                Credential c = service.getOffChain().get(id);
                // guardar o original só da primeira vez
                ORIGINALS.putIfAbsent(id, c.getData(FORGED_KEY));
                service.getOffChain().tamper(id, FORGED_KEY, "urze 95%");
                return "análise " + id + " falsificada fora da cadeia: verifique agora o frasco";
            }
        });
    }

    /**
     * Repõe as análises falsificadas. Importante: se a DGAV, o Laboratório, o
     * DOP ou o apicultor emitirem uma credencial antes disto, o armazém é
     * gravado em ficheiro com a análise falsificada lá dentro.
     */
    private void btRestoreActionPerformed() {
        runInBackground(btRestore, () -> {
            synchronized (service) {
                int n = ORIGINALS.size();
                for (Map.Entry<String, String> e : ORIGINALS.entrySet()) {
                    service.getOffChain().tamper(e.getKey(), FORGED_KEY, e.getValue());
                }
                ORIGINALS.clear();
                return n + " análise(s) reposta(s)";
            }
        });
    }

    /**
     * Análise em vigor do lote de um frasco.
     */
    private String analysisOf(String jar) throws Exception {
        LedgerState state = service.getState();
        PackTx pack = state.findPack(jar);
        if (pack == null) {
            throw new Exception("O frasco " + jar + " não existe na cadeia");
        }
        for (IssueCredentialTx a : state.getCredentials("ANALISE_LOTE", pack.getLot())) {
            // só uma análise com origem floral é que se pode falsificar e depois repor
            Credential c = service.getOffChain().get(a.getCredentialId());
            if (!state.isRevoked(a.getCredentialId()) && c != null && c.getData(FORGED_KEY) != null) {
                return a.getCredentialId();
            }
        }
        throw new Exception("O lote " + pack.getLot() + " não tem análise em vigor");
    }
}
