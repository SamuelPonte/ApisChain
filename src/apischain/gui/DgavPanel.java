//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.identity.DigitalID;
import apischain.identity.Role;
import apischain.service.Actor;
import apischain.service.ApisChainService;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Painel da DGAV: regista apicultores, apiários e lojas e revoga credenciais.
 *
 * Os dados pessoais (nome, NIF, morada, coordenadas) vão só para a credencial
 * fora da cadeia; na cadeia entram o DID, a chave pública e o hash.
 */
public class DgavPanel extends BasePanel {

    private final String keysFolder;  // pasta das identidades (data/keys)

    // registar apicultor
    private final JComboBox<String> cbBeekeeper = new JComboBox<>();
    private final JTextField txtBeekeeperName = new JTextField();
    private final JTextField txtBeekeeperNif = new JTextField();
    private final JTextField txtBeekeeperAddress = new JTextField();
    private final JButton btBeekeeper = new JButton("Registar apicultor");
    // registar apiário
    private final JComboBox<String> cbApiaryOwner = new JComboBox<>();
    private final JTextField txtApiaryPlace = new JTextField();
    private final JTextField txtApiaryCoords = new JTextField();
    private final JTextField txtApiaryHives = new JTextField();
    private final JButton btApiary = new JButton("Registar apiário");
    // registar loja
    private final JComboBox<String> cbShop = new JComboBox<>();
    private final JTextField txtShopName = new JTextField();
    private final JTextField txtShopAddress = new JTextField();
    private final JButton btShop = new JButton("Registar loja");
    // revogar
    private final JTextField txtRevokeId = new JTextField();
    private final JTextField txtRevokeReason = new JTextField();
    private final JButton btRevoke = new JButton("Revogar");

    public DgavPanel(ApisChainService service, Actor actor, String keysFolder) {
        super(service, actor);
        this.keysFolder = keysFolder;
        initComponents();
        updateIdentities();
    }

    private void initComponents() {
        // 1. registar apicultor: escolhe-se a identidade que ele criou no ecrã de entrada
        JPanel beekeeper = section("Registar apicultor");
        addRow(beekeeper, "Identidade (chave pública)", cbBeekeeper);
        addRow(beekeeper, "Nome completo", txtBeekeeperName);
        addRow(beekeeper, "NIF", txtBeekeeperNif);
        addRow(beekeeper, "Morada", txtBeekeeperAddress);
        addRow(beekeeper, "", btBeekeeper);
        addSection(beekeeper);

        // 2. registar apiário de um apicultor
        JPanel apiary = section("Registar apiário");
        addRow(apiary, "Apicultor", cbApiaryOwner);
        addRow(apiary, "Local", txtApiaryPlace);
        addRow(apiary, "Coordenadas", txtApiaryCoords);
        addRow(apiary, "N.º de colmeias", txtApiaryHives);
        addRow(apiary, "", btApiary);
        addSection(apiary);

        // 3. registar loja
        JPanel shop = section("Registar loja");
        addRow(shop, "Identidade (chave pública)", cbShop);
        addRow(shop, "Nome", txtShopName);
        addRow(shop, "Morada", txtShopAddress);
        addRow(shop, "", btShop);
        addSection(shop);

        // 4. revogar qualquer credencial (registo, lote, análise ou certificado)
        JPanel revoke = section("Revogar credencial");
        addRow(revoke, "Id da credencial (AP-0001, LOTE-2026-0001, AN-0001...)", txtRevokeId);
        addRow(revoke, "Motivo", txtRevokeReason);
        addRow(revoke, "", btRevoke);
        addSection(revoke);

        // 5. atualizar as listas de identidades (depois de criar uma no ecrã de entrada)
        JButton btUpdate = new JButton("Atualizar listas de identidades");
        JPanel update = section("Identidades");
        addRow(update, "", btUpdate);
        addSection(update);

        btBeekeeper.addActionListener(evt -> btBeekeeperActionPerformed());
        btApiary.addActionListener(evt -> btApiaryActionPerformed());
        btShop.addActionListener(evt -> btShopActionPerformed());
        btRevoke.addActionListener(evt -> btRevokeActionPerformed());
        btUpdate.addActionListener(evt -> updateIdentities());
    }

    /**
     * Preenche as listas com as identidades de apicultores e lojas que existem
     * na pasta das chaves.
     */
    private void updateIdentities() {
        cbBeekeeper.removeAllItems();
        cbApiaryOwner.removeAllItems();
        cbShop.removeAllItems();
        File[] files = new File(keysFolder).listFiles((dir, name) -> name.endsWith(".id"));
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File f : files) {
            String name = f.getName().substring(0, f.getName().length() - 3);
            try {
                Role role = DigitalID.load(name, keysFolder).getRole();
                if (role == Role.APICULTOR) {
                    cbBeekeeper.addItem(name);
                    cbApiaryOwner.addItem(name);
                } else if (role == Role.LOJA) {
                    cbShop.addItem(name);
                }
            } catch (Exception ex) {
                // identidade alterada ou incompleta: não aparece
            }
        }
    }

    /**
     * EXEMPLO DO PADRÃO: 1) ler os campos aqui, na thread do Swing;
     * 2) passar ao runInBackground o que se faz no serviço; 3) devolver a mensagem.
     */
    private void btBeekeeperActionPerformed() {
        // 1. ler os campos na thread do Swing
        String name = (String) cbBeekeeper.getSelectedItem();
        Map<String, String> data = Map.of(
                "nome", txtBeekeeperName.getText().trim(),
                "nif", txtBeekeeperNif.getText().trim(),
                "morada", txtBeekeeperAddress.getText().trim());
        if (name == null) {
            log("não há identidades de apicultores: crie uma no ecrã de entrada");
            return;
        }
        // 2. o trabalho no serviço corre noutra thread
        runInBackground(btBeekeeper, () -> {
            DigitalID beekeeper = DigitalID.load(name, keysFolder);
            String id = service.registerBeekeeper(actor, beekeeper, data);
            // 3. mensagem para o registo do painel
            return "apicultor " + name + " registado com a credencial " + id + " (bloco #" + lastBlock() + ")";
        });
    }

    private void btApiaryActionPerformed() {
        String owner = (String) cbApiaryOwner.getSelectedItem();
        Map<String, String> data = Map.of(
                "local", txtApiaryPlace.getText().trim(),
                "coordenadas", txtApiaryCoords.getText().trim(),
                "colmeias", txtApiaryHives.getText().trim());
        if (owner == null) {
            log("não há identidades de apicultores: crie uma no ecrã de entrada");
            return;
        }
        runInBackground(btApiary, () -> {
            String did = DigitalID.load(owner, keysFolder).getDid();
            String id = service.registerApiary(actor, did, data);
            return "apiário " + id + " de " + owner + " registado (bloco #" + lastBlock() + ")";
        });
    }

    private void btShopActionPerformed() {
        String name = (String) cbShop.getSelectedItem();
        Map<String, String> data = Map.of(
                "nome", txtShopName.getText().trim(),
                "morada", txtShopAddress.getText().trim());
        if (name == null) {
            log("não há identidades de lojas: crie uma no ecrã de entrada");
            return;
        }
        runInBackground(btShop, () -> {
            DigitalID shop = DigitalID.load(name, keysFolder);
            String id = service.registerShop(actor, shop, data);
            return "loja " + name + " registada com a credencial " + id + " (bloco #" + lastBlock() + ")";
        });
    }

    private void btRevokeActionPerformed() {
        String id = txtRevokeId.getText().trim();
        String reason = txtRevokeReason.getText().trim();
        runInBackground(btRevoke, () -> {
            service.revoke(actor, id, reason);
            return "credencial " + id + " revogada: " + reason + " (bloco #" + lastBlock() + ")";
        });
    }
}
