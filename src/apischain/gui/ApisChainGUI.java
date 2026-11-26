//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em digitalID.ui.DigitalIDui e templarCoin.ui.TemplarCoin      ::://
//::  do Templar2026 (Prof. António Manso, 2026)                            ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.identity.CredentialType;
import apischain.identity.DigitalID;
import apischain.identity.Role;
import apischain.service.ApisChain;
import apischain.transactions.Transaction;
import java.io.File;
import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Painel da ApisChain, aberto pelo LoginFrame com a sessão de quem entrou
 * (como o TemplarCoin recebe o utilizador e as chaves do TemplarLogin).
 *
 * Separadores:
 * 1. Credenciais: emitir os tipos que o papel de quem entrou pode emitir e,
 *    só para a DGAV, revogar (o consumidor não tem este separador);
 * 2. Verificar: verificar um lote ou uma credencial (sem chave);
 * 3. Blockchain: os blocos da cadeia, como o explorador do DigitalIDui.
 *
 * Como no professor, as operações que escrevem na cadeia correm numa thread
 * nova e o ecrã é atualizado com SwingUtilities.invokeLater, para a janela
 * não ficar parada enquanto as autoridades assinam o bloco.
 *
 * O desenho da janela está em ApisChainGUI.form (editor do NetBeans).
 */
public class ApisChainGUI extends javax.swing.JFrame {

    private final ApisChain apisChain;  // serviço
    private final DigitalID myId;       // quem entrou (null no consumidor)
    private final PrivateKey myKey;     // chave privada de quem entrou (null no consumidor)
    private final JFrame login;         // ecrã de entrada, para onde se volta

    /**
     * @param apisChain serviço
     * @param myId identidade de quem entrou (null para o consumidor)
     * @param myKey chave privada de quem entrou (null para o consumidor)
     * @param login ecrã de entrada
     */
    public ApisChainGUI(ApisChain apisChain, DigitalID myId, PrivateKey myKey, JFrame login) {
        this.apisChain = apisChain;
        this.myId = myId;
        this.myKey = myKey;
        this.login = login;
        initComponents();
        // 1. quem tem a sessão aberta
        lblSession.setText(myId == null ? "Sessão: consumidor (sem chave)" : "Sessão: " + myId);
        // 2. o consumidor só verifica: não tem o separador Credenciais
        if (myId == null) {
            jTabbedPane1.remove(pnlCredentials);
        } else {
            fillTypes(myId.getRole());
            // só a DGAV revoga
            pnlRevoke.setVisible(myId.getRole() == Role.DGAV);
        }
        updateChain();
        setLocationRelativeTo(null);
    }

    /**
     * Mostra na lista só os tipos de credencial que este papel pode emitir
     * (as regras dos validadores recusariam os outros).
     */
    private void fillTypes(Role role) {
        switch (role) {
            case DGAV -> {
                cbType.addItem(CredentialType.REGISTO_APICULTOR.name());
                cbType.addItem(CredentialType.REGISTO_APIARIO.name());
            }
            case APICULTOR -> cbType.addItem(CredentialType.LOTE.name());
            case LAB -> cbType.addItem(CredentialType.ANALISE_LOTE.name());
            case DOP -> cbType.addItem(CredentialType.CERTIFICADO_ORIGEM.name());
        }
    }

    /**
     * Mostra os blocos da cadeia na lista do separador Blockchain.
     */
    private void updateChain() {
        DefaultListModel<String> model = new DefaultListModel<>();
        synchronized (apisChain) {
            BlockChain chain = apisChain.getChain();
            for (Block b : chain.getChain()) {
                model.addElement(b.toString());
            }
            lblChainState.setText(chain.isValid() ? "Cadeia íntegra, " + chain.getSize() + " blocos"
                    : "Cadeia CORROMPIDA no bloco #" + chain.getInvalidIndex());
        }
        lstBlockChain.setModel(model);
    }

    /**
     * Mostra um erro numa caixa de diálogo.
     */
    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "ApisChain", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Converte o texto dos dados (uma linha por dado, nome=valor) num mapa.
     */
    private static Map<String, String> parseData(String text) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            int i = line.indexOf('=');
            if (i > 0) {
                data.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
            }
        }
        return data;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlTop = new javax.swing.JPanel();
        lblSession = new javax.swing.JLabel();
        btSwitch = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pnlCredentials = new javax.swing.JPanel();
        pnlIssue = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cbType = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        txtSubject = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        btIssue = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtData = new javax.swing.JTextArea();
        pnlRevoke = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        txtRevokeId = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtReason = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        btRevoke = new javax.swing.JButton();
        pnlVerify = new javax.swing.JPanel();
        pnlVerifyForm = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        txtVerifyId = new javax.swing.JTextField();
        btVerify = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtResult = new javax.swing.JTextArea();
        pnlChain = new javax.swing.JPanel();
        lblChainState = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        lstBlockChain = new javax.swing.JList<>();
        jScrollPane5 = new javax.swing.JScrollPane();
        txtBlock = new javax.swing.JTextArea();
        lblStatus = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ApisChain");

        pnlTop.setLayout(new java.awt.BorderLayout());

        lblSession.setText("Sessão");
        pnlTop.add(lblSession, java.awt.BorderLayout.CENTER);

        btSwitch.setText("Trocar de interveniente");
        btSwitch.addActionListener(this::btSwitchActionPerformed);
        pnlTop.add(btSwitch, java.awt.BorderLayout.LINE_END);

        getContentPane().add(pnlTop, java.awt.BorderLayout.PAGE_START);

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(800, 520));

        pnlCredentials.setLayout(new java.awt.BorderLayout());

        pnlIssue.setBorder(javax.swing.BorderFactory.createTitledBorder("Emitir credencial"));
        pnlIssue.setLayout(new java.awt.GridLayout(3, 2, 8, 8));

        jLabel3.setText("Tipo");
        pnlIssue.add(jLabel3);

        pnlIssue.add(cbType);

        jLabel4.setText("Titular (nome do apicultor, id do apiário ou do lote)");
        pnlIssue.add(jLabel4);
        pnlIssue.add(txtSubject);

        jLabel5.setText("Dados em baixo, um por linha: nome=valor");
        pnlIssue.add(jLabel5);

        btIssue.setText("Emitir");
        btIssue.addActionListener(this::btIssueActionPerformed);
        pnlIssue.add(btIssue);

        pnlCredentials.add(pnlIssue, java.awt.BorderLayout.PAGE_START);

        txtData.setColumns(20);
        txtData.setRows(5);
        jScrollPane2.setViewportView(txtData);

        pnlCredentials.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        pnlRevoke.setBorder(javax.swing.BorderFactory.createTitledBorder("Revogar credencial (DGAV)"));
        pnlRevoke.setLayout(new java.awt.GridLayout(3, 2, 8, 8));

        jLabel6.setText("Id da credencial");
        pnlRevoke.add(jLabel6);
        pnlRevoke.add(txtRevokeId);

        jLabel7.setText("Motivo (sem dados pessoais: fica na cadeia)");
        pnlRevoke.add(jLabel7);
        pnlRevoke.add(txtReason);
        pnlRevoke.add(jLabel8);

        btRevoke.setText("Revogar");
        btRevoke.addActionListener(this::btRevokeActionPerformed);
        pnlRevoke.add(btRevoke);

        pnlCredentials.add(pnlRevoke, java.awt.BorderLayout.PAGE_END);

        jTabbedPane1.addTab("Credenciais", pnlCredentials);

        pnlVerify.setLayout(new java.awt.BorderLayout());

        pnlVerifyForm.setBorder(javax.swing.BorderFactory.createTitledBorder("Verificar (sem chave)"));
        pnlVerifyForm.setLayout(new java.awt.GridLayout(1, 3, 8, 8));

        jLabel9.setText("Id do lote ou da credencial");
        pnlVerifyForm.add(jLabel9);

        txtVerifyId.setText("LOTE-0001");
        pnlVerifyForm.add(txtVerifyId);

        btVerify.setText("Verificar");
        btVerify.addActionListener(this::btVerifyActionPerformed);
        pnlVerifyForm.add(btVerify);

        pnlVerify.add(pnlVerifyForm, java.awt.BorderLayout.PAGE_START);

        txtResult.setEditable(false);
        txtResult.setColumns(20);
        txtResult.setLineWrap(true);
        txtResult.setRows(5);
        jScrollPane3.setViewportView(txtResult);

        pnlVerify.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Verificar", pnlVerify);

        pnlChain.setLayout(new java.awt.BorderLayout());

        lblChainState.setText("Cadeia");
        pnlChain.add(lblChainState, java.awt.BorderLayout.PAGE_START);

        lstBlockChain.setFont(new java.awt.Font("Courier New", 0, 14)); // NOI18N
        lstBlockChain.addListSelectionListener(this::lstBlockChainValueChanged);
        jScrollPane4.setViewportView(lstBlockChain);

        pnlChain.add(jScrollPane4, java.awt.BorderLayout.LINE_START);

        txtBlock.setEditable(false);
        txtBlock.setColumns(20);
        txtBlock.setLineWrap(true);
        txtBlock.setRows(5);
        jScrollPane5.setViewportView(txtBlock);

        pnlChain.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Blockchain", pnlChain);

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        lblStatus.setText(" ");
        getContentPane().add(lblStatus, java.awt.BorderLayout.PAGE_END);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Trocar de interveniente: fecha este painel e volta ao ecrã de entrada.
     */
    private void btSwitchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btSwitchActionPerformed
        dispose();
        login.setVisible(true);
    }//GEN-LAST:event_btSwitchActionPerformed

    /**
     * Emitir uma credencial. No registo do apicultor e do apiário, o titular é o
     * nome do apicultor: a sua identidade dá o DID (e, no registo, a chave pública).
     */
    private void btIssueActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btIssueActionPerformed
        // 1. ler os campos na thread do Swing
        CredentialType type = CredentialType.valueOf((String) cbType.getSelectedItem());
        String subject = txtSubject.getText().trim();
        Map<String, String> data = parseData(txtData.getText());
        btIssue.setEnabled(false);
        // 2. o consenso corre noutra thread
        new Thread(() -> {
            try {
                // se o titular for o nome de um apicultor, usa-se o seu DID;
                // senão é o id de um apiário ou de um lote
                String did = subject;
                String key = "";
                if (new File(apisChain.getKeysFolder(), subject + ".id").exists()) {
                    DigitalID beekeeper = DigitalID.load(subject, apisChain.getKeysFolder());
                    did = beekeeper.getDid();
                    if (type == CredentialType.REGISTO_APICULTOR) {
                        key = beekeeper.getPublicKeyBase64();
                    }
                }
                String id = apisChain.issue(myId, myKey, type, did, key, data);
                // 3. de volta à thread do Swing para mostrar o resultado
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Credencial " + id + " emitida");
                    updateChain();
                    btIssue.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showError(ex);
                    btIssue.setEnabled(true);
                });
            }
        }).start();
    }//GEN-LAST:event_btIssueActionPerformed

    /**
     * Revogar uma credencial (só aparece à DGAV).
     */
    private void btRevokeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btRevokeActionPerformed
        String id = txtRevokeId.getText().trim();
        String reason = txtReason.getText().trim();
        new Thread(() -> {
            try {
                apisChain.revoke(myId, myKey, id, reason);
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Credencial " + id + " revogada");
                    updateChain();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> showError(ex));
            }
        }).start();
    }//GEN-LAST:event_btRevokeActionPerformed

    /**
     * Verificar um lote ou uma credencial.
     */
    private void btVerifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btVerifyActionPerformed
        String id = txtVerifyId.getText().trim();
        new Thread(() -> {
            List<String> lines = apisChain.verify(id);
            SwingUtilities.invokeLater(() -> txtResult.setText(String.join("\n", lines)));
        }).start();
    }//GEN-LAST:event_btVerifyActionPerformed

    /**
     * Mostra o bloco escolhido na lista.
     */
    private void lstBlockChainValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstBlockChainValueChanged
        int i = lstBlockChain.getSelectedIndex();
        if (i < 0) {
            return;
        }
        synchronized (apisChain) {
            Block b = apisChain.getChain().get(i);
            String tx;
            try {
                tx = Transaction.fromBase64(b.getData()).toString();
            } catch (Exception ex) {
                tx = "ilegível";
            }
            txtBlock.setText("Bloco #" + b.getIndex() + (b.isValid() ? " (íntegro)" : " (CORROMPIDO)")
                    + "\ndata: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(b.getTimestamp()))
                    + "\nhash anterior: " + b.getPreviousHash()
                    + "\nhash: " + b.getCurrentHash()
                    + "\nproponente: " + b.getValidator()
                    + "\nvotos: " + b.getVotes()
                    + "\n\ntransação: " + tx);
        }
    }//GEN-LAST:event_lstBlockChainValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btIssue;
    private javax.swing.JButton btRevoke;
    private javax.swing.JButton btSwitch;
    private javax.swing.JButton btVerify;
    private javax.swing.JComboBox<String> cbType;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel lblChainState;
    private javax.swing.JLabel lblSession;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JList<String> lstBlockChain;
    private javax.swing.JPanel pnlChain;
    private javax.swing.JPanel pnlCredentials;
    private javax.swing.JPanel pnlIssue;
    private javax.swing.JPanel pnlRevoke;
    private javax.swing.JPanel pnlTop;
    private javax.swing.JPanel pnlVerify;
    private javax.swing.JPanel pnlVerifyForm;
    private javax.swing.JTextArea txtBlock;
    private javax.swing.JTextArea txtData;
    private javax.swing.JTextField txtReason;
    private javax.swing.JTextArea txtResult;
    private javax.swing.JTextField txtRevokeId;
    private javax.swing.JTextField txtSubject;
    private javax.swing.JTextField txtVerifyId;
    // End of variables declaration//GEN-END:variables
}
