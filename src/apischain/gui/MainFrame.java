//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.identity.Role;
import apischain.service.Actor;
import apischain.service.ApisChainService;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * Janela principal: em cima, quem tem a sessão aberta e o botão para trocar
 * de interveniente; ao centro, um separador por cada painel do seu papel.
 */
public class MainFrame extends JFrame {

    private final ApisChainService service;  // serviço partilhado por todas as sessões
    private final Actor actor;               // quem entrou (null no consumidor)
    private final String keysFolder;         // pasta das chaves (para o painel da DGAV)
    private final JFrame login;              // ecrã de entrada, para onde se volta

    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton btSwitch = new JButton("Trocar de interveniente");

    /**
     * @param service serviço da ApisChain
     * @param actor interveniente com sessão aberta (null para o consumidor)
     * @param keysFolder pasta das chaves
     * @param login ecrã de entrada
     */
    public MainFrame(ApisChainService service, Actor actor, String keysFolder, JFrame login) {
        this.service = service;
        this.actor = actor;
        this.keysFolder = keysFolder;
        this.login = login;
        initComponents();
        addTabs();
        setSize(900, 680);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        String who = (actor == null) ? "Consumidor (sem chave)" : actor.toString();
        setTitle("ApisChain · " + who);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // barra de cima: sessão e botão de trocar
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        top.add(new JLabel("Sessão: " + who), BorderLayout.CENTER);
        top.add(btSwitch, BorderLayout.EAST);
        btSwitch.addActionListener(evt -> btSwitchActionPerformed());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
    }

    /**
     * Um separador por painel, conforme o papel de quem entrou.
     */
    private void addTabs() {
        Role role = (actor == null) ? null : actor.getRole();
        // 1. painéis das autoridades (issue #6)
        if (role == Role.DGAV) {
            tabs.addTab("DGAV", new DgavPanel(service, actor, keysFolder));
        } else if (role == Role.LAB) {
            tabs.addTab("Laboratório", new LabPanel(service, actor));
        } else if (role == Role.DOP) {
            tabs.addTab("Certificador DOP", new DopPanel(service, actor));
        }
        // 2. painéis da issue #7 (André): Apicultor, Loja e Consumidor, conforme o
        //    papel, e os separadores comuns Cadeia, Merkle e Rede. Acrescentam-se
        //    aqui com tabs.addTab(...), estendendo BasePanel.

        // 3. enquanto não houver painel para este papel, um aviso
        if (tabs.getTabCount() == 0) {
            tabs.addTab("Em preparação", new JLabel("Os painéis deste interveniente são da issue #7.",
                    SwingConstants.CENTER));
        }
    }

    /**
     * Fecha esta sessão e volta ao ecrã de entrada (o programa continua aberto).
     */
    private void btSwitchActionPerformed() {
        dispose();
        login.setVisible(true);
    }
}
