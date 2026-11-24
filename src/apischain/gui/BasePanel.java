//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.ledger.LedgerState;
import apischain.service.Actor;
import apischain.service.ApisChainService;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.Transaction;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Base dos painéis da ApisChain: guarda o serviço e quem tem a sessão aberta,
 * tem o registo das operações em baixo e o padrão NÃO BLOQUEANTE que todos os
 * painéis usam (ver runInBackground).
 *
 * Um painel novo (issue #7) faz assim:
 * 1. extends BasePanel;
 * 2. cria as secções com section("Título") e acrescenta-as com addSection;
 * 3. em cada botão, lê os campos (na thread do Swing) e chama runInBackground
 *    com o que tem de fazer no serviço. Ver o exemplo no DgavPanel.
 */
public abstract class BasePanel extends JPanel {

    /**
     * Uma operação que corre fora da thread do Swing e devolve a mensagem a
     * mostrar quando termina bem.
     */
    public interface Operation {
        String run() throws Exception;
    }

    protected final ApisChainService service;  // a única coisa com que os painéis falam
    protected final Actor actor;               // quem tem a sessão aberta (null no consumidor)
    private final JPanel sections = new JPanel();             // secções do formulário, umas por baixo das outras
    private final JTextArea txtLog = new JTextArea(5, 60);    // registo das operações deste painel

    protected BasePanel(ApisChainService service, Actor actor) {
        this.service = service;
        this.actor = actor;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        // ao centro, as secções do formulário (com barra de deslocamento se não couberem)
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        add(new JScrollPane(sections), BorderLayout.CENTER);
        // em baixo, o registo das operações
        txtLog.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registo"));
        add(scrollLog, BorderLayout.SOUTH);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  PADRÃO NÃO BLOQUEANTE (igual ao do professor)                      ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Corre uma operação sem bloquear a janela.
     *
     * Assinar, fazer o consenso entre os 4 nós e gravar os ficheiros demora.
     * Se isso corresse na thread do Swing (a que desenha o ecrã e trata os
     * cliques), a janela ficava parada até acabar. Por isso:
     * 1. na thread do Swing: desativa o botão (para não se carregar duas vezes);
     * 2. numa thread nova: corre a operação no serviço;
     * 3. de volta à thread do Swing (SwingUtilities.invokeLater): mostra o
     *    resultado ou o erro e volta a ativar o botão. O Swing só pode ser
     *    alterado a partir da sua thread.
     *
     * Os valores dos campos têm de ser lidos ANTES, na thread do Swing, e
     * passados à operação já como texto.
     *
     * @param button botão que lançou a operação
     * @param operation o que fazer no serviço
     */
    protected void runInBackground(JButton button, Operation operation) {
        // 1. thread do Swing
        button.setEnabled(false);
        log("a validar na rede...");
        // 2. thread nova
        new Thread(() -> {
            try {
                String msg = operation.run();
                // 3. de volta à thread do Swing: tudo correu bem
                SwingUtilities.invokeLater(() -> {
                    log("OK: " + msg);
                    button.setEnabled(true);
                });
            } catch (Exception ex) {
                // 3. de volta à thread do Swing: a rede ou as regras recusaram
                SwingUtilities.invokeLater(() -> {
                    log("RECUSADO: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Operação recusada",
                            JOptionPane.ERROR_MESSAGE);
                    button.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * Acrescenta uma linha ao registo do painel (chamar só na thread do Swing).
     *
     * @param msg mensagem
     */
    protected void log(String msg) {
        txtLog.append(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + msg + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  CONSTRUÇÃO DO FORMULÁRIO                                           ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Secção do formulário com título e duas colunas (rótulo e campo).
     *
     * @param title título da secção
     * @return painel onde se acrescentam as linhas com addRow
     */
    protected static JPanel section(String title) {
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 4));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title), BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        return p;
    }

    /**
     * Acrescenta uma linha (rótulo e componente) a uma secção.
     */
    protected static void addRow(JPanel section, String label, Component field) {
        section.add(new JLabel(label));
        section.add(field);
    }

    /**
     * Acrescenta uma secção ao formulário do painel.
     */
    protected void addSection(JPanel section) {
        // sem isto o BoxLayout estica a secção na vertical
        section.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, section.getPreferredSize().height));
        sections.add(section);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  LEITURAS DA CADEIA (só para preencher listas no ecrã)              ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Ids dos lotes que existem na cadeia e não estão revogados.
     *
     * @return lista dos lotes (vazia se não for possível ler a cadeia)
     */
    protected List<String> listLots() {
        List<String> lots = new ArrayList<>();
        try {
            // o bloqueio da rede impede que entre um bloco a meio da leitura
            synchronized (service.getNetwork()) {
                LedgerState state = service.getState();
                BlockChain chain = service.getChain();
                for (Block b : chain.getChain()) {
                    for (String txt : b.getTransactions()) {
                        Transaction t = Transaction.fromBase64(txt);
                        if (t instanceof IssueCredentialTx c && c.getCredentialType().equals("LOTE")
                                && !state.isRevoked(c.getCredentialId())) {
                            lots.add(c.getCredentialId());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log("não foi possível ler os lotes: " + ex.getMessage());
        }
        return lots;
    }

    /**
     * Índice do último bloco da cadeia, para dizer ao utilizador onde entrou a operação.
     *
     * @return índice do último bloco
     * @throws Exception se não houver nós ligados
     */
    protected int lastBlock() throws Exception {
        synchronized (service.getNetwork()) {
            return service.getChain().getSize() - 1;
        }
    }
}
