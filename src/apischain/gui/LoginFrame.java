//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em templarCoin.ui.TemplarLogin do Templar2026_DigitalID       ::://
//::  (Prof. António Manso, 2026)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.identity.DigitalID;
import apischain.identity.Role;
import apischain.service.Actor;
import apischain.service.DemoData;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Ecrã de entrada: cada interveniente escolhe quem é e dá a palavra-passe da
 * sua chave privada; também se podem criar apicultores e lojas novos.
 *
 * Diferenças face ao TemplarLogin do professor:
 * 1. a lista de intervenientes vem das identidades em data/keys (DigitalID);
 * 2. ao abrir, prepara a demonstração (chaves, 4 nós, cadeia) numa thread;
 * 3. a palavra-passe é lida numa thread, porque decifrar a chave demora;
 * 4. depois de sair de uma sessão volta-se a este ecrã (trocar de interveniente
 *    sem fechar o programa, para a demonstração num só computador).
 *
 * Interface escrita à mão (sem editor de formulários), com layouts simples.
 */
public class LoginFrame extends JFrame {

    private final String folder;      // pasta da demonstração (data)
    private final String keysFolder;  // pasta das chaves (data/keys)
    private DemoData demo;            // rede, serviço e cenário base (null até estar pronto)
    private final List<String> actorNames = new ArrayList<>(); // nomes pela ordem da lista

    // componentes do separador "Entrar"
    private final JComboBox<String> cbActors = new JComboBox<>();
    private final JPasswordField txtPassword = new JPasswordField(16);
    private final JButton btLogin = new JButton("Entrar");
    private final JButton btConsumer = new JButton("Entrar como consumidor (sem chave)");
    // componentes do separador "Criar interveniente"
    private final JTextField txtNewName = new JTextField(16);
    private final JComboBox<Role> cbNewRole = new JComboBox<>(new Role[]{Role.APICULTOR, Role.LOJA});
    private final JPasswordField txtNewPass1 = new JPasswordField(16);
    private final JPasswordField txtNewPass2 = new JPasswordField(16);
    private final JButton btCreate = new JButton("Criar");
    // estado da preparação, em baixo
    private final JLabel lbStatus = new JLabel(" ");

    /**
     * @param folder pasta da demonstração, por exemplo "data"
     */
    public LoginFrame(String folder) {
        this.folder = folder;
        this.keysFolder = new File(folder, "keys").getPath();
        initComponents();
        setLocationRelativeTo(null);
        loadDemo();
    }

    /**
     * Monta o ecrã: dois separadores, como no TemplarLogin.
     */
    private void initComponents() {
        setTitle("ApisChain · Entrar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();

        // 1. separador Entrar
        JPanel login = new JPanel(new GridLayout(0, 2, 8, 8));
        login.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        login.add(new JLabel("Interveniente"));
        login.add(cbActors);
        login.add(new JLabel("Palavra-passe"));
        login.add(txtPassword);
        login.add(btConsumer);
        login.add(btLogin);
        tabs.addTab("Entrar", login);

        // 2. separador Criar interveniente (as autoridades são fixas: vêm da génese)
        JPanel create = new JPanel(new GridLayout(0, 2, 8, 8));
        create.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        create.add(new JLabel("Nome (letras, algarismos, - e _)"));
        create.add(txtNewName);
        create.add(new JLabel("Papel"));
        create.add(cbNewRole);
        create.add(new JLabel("Palavra-passe"));
        create.add(txtNewPass1);
        create.add(new JLabel("Repetir a palavra-passe"));
        create.add(txtNewPass2);
        create.add(new JLabel("(depois a DGAV tem de o registar)"));
        create.add(btCreate);
        tabs.addTab("Criar interveniente", create);

        // 3. eventos dos botões (o Enter na palavra-passe também entra)
        btLogin.addActionListener(evt -> btLoginActionPerformed());
        txtPassword.addActionListener(evt -> btLoginActionPerformed());
        btConsumer.addActionListener(evt -> btConsumerActionPerformed());
        btCreate.addActionListener(evt -> btCreateActionPerformed());

        lbStatus.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(lbStatus, BorderLayout.SOUTH);
        setButtonsEnabled(false);
        pack();
    }

    /**
     * Prepara a demonstração numa thread (a primeira vez cria as chaves, a
     * génese e o cenário base, o que demora alguns segundos).
     */
    private void loadDemo() {
        lbStatus.setText("A preparar a rede (chaves, 4 nós e cadeia)...");
        new Thread(() -> {
            try {
                DemoData d = DemoData.load(folder);
                int blocks = d.getService().getChain().getSize();
                SwingUtilities.invokeLater(() -> {
                    demo = d;
                    updateActors();
                    setButtonsEnabled(true);
                    lbStatus.setText("Rede pronta: " + blocks + " blocos. Palavra-passe da demonstração: "
                            + DemoData.PASSWORD);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> lbStatus.setText("Erro ao preparar a rede: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Preenche a lista com as identidades que existem na pasta das chaves.
     */
    private void updateActors() {
        cbActors.removeAllItems();
        actorNames.clear();
        File[] files = new File(keysFolder).listFiles((dir, name) -> name.endsWith(".id"));
        if (files == null) {
            return;
        }
        java.util.Arrays.sort(files);
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length() - 3);
            try {
                DigitalID id = DigitalID.load(name, keysFolder);
                actorNames.add(name);
                cbActors.addItem(name + "  (" + id.getRole() + ")");
            } catch (Exception ex) {
                // identidade alterada ou incompleta: não aparece na lista
            }
        }
    }

    private void setButtonsEnabled(boolean on) {
        btLogin.setEnabled(on);
        btConsumer.setEnabled(on);
        btCreate.setEnabled(on);
    }

    /**
     * Entrar: lê a chave privada com a palavra-passe e abre a janela principal.
     */
    private void btLoginActionPerformed() {
        int i = cbActors.getSelectedIndex();
        if (i < 0 || !btLogin.isEnabled()) {
            return;
        }
        // 1. ler os campos na thread do Swing
        String name = actorNames.get(i);
        String password = new String(txtPassword.getPassword());
        setButtonsEnabled(false);
        lbStatus.setText("A abrir a chave de " + name + "...");
        // 2. decifrar a chave privada numa thread (o PBKDF2 é lento de propósito)
        new Thread(() -> {
            try {
                Actor actor = Actor.login(name, password, keysFolder);
                SwingUtilities.invokeLater(() -> openMain(actor));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setButtonsEnabled(true);
                    lbStatus.setText(" ");
                    JOptionPane.showMessageDialog(this, "Palavra-passe errada ou identidade inválida",
                            "Entrar", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    /**
     * Entrar como consumidor: não precisa de chave (só verifica frascos).
     */
    private void btConsumerActionPerformed() {
        openMain(null);
    }

    /**
     * Abre a janela principal e esconde este ecrã (volta a aparecer ao trocar de interveniente).
     */
    private void openMain(Actor actor) {
        txtPassword.setText("");
        setButtonsEnabled(true);
        lbStatus.setText(" ");
        new MainFrame(demo.getService(), actor, keysFolder, this).setVisible(true);
        setVisible(false);
    }

    /**
     * Criar um apicultor ou uma loja: gera as chaves e a identidade em data/keys.
     */
    private void btCreateActionPerformed() {
        // 1. ler e verificar os campos
        String name = txtNewName.getText().trim();
        Role role = (Role) cbNewRole.getSelectedItem();
        String pass1 = new String(txtNewPass1.getPassword());
        String pass2 = new String(txtNewPass2.getPassword());
        if (!pass1.equals(pass2) || pass1.isEmpty()) {
            JOptionPane.showMessageDialog(this, "As palavras-passe são diferentes ou estão vazias",
                    "Criar interveniente", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setButtonsEnabled(false);
        // 2. gerar as chaves numa thread
        new Thread(() -> {
            try {
                DigitalID id = DigitalID.create(name, role, pass1, keysFolder);
                SwingUtilities.invokeLater(() -> {
                    updateActors();
                    setButtonsEnabled(true);
                    txtNewName.setText("");
                    txtNewPass1.setText("");
                    txtNewPass2.setText("");
                    JOptionPane.showMessageDialog(this, "Criado: " + id + "\nA DGAV tem agora de o registar.",
                            "Criar interveniente", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setButtonsEnabled(true);
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Criar interveniente",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
