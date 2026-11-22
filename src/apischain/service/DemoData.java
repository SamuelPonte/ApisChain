//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import apischain.consensus.LocalNetwork;
import apischain.consensus.Node;
import apischain.identity.DigitalID;
import apischain.identity.OffChainStore;
import apischain.identity.Role;
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.Transaction;
import apischain.core.Block;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dados de demonstração da PoC. ENTIDADES E DADOS FICTÍCIOS.
 *
 * load(pasta) prepara tudo o que a demonstração precisa:
 * 1. as identidades (chaves em pasta/keys), criadas na primeira vez e
 *    carregadas nas seguintes;
 * 2. os quatro nós (N1 DGAV, N2 Laboratório, N3 DOP, N4 só de leitura), cada
 *    um com a sua cópia da cadeia em pasta/N1, pasta/N2, ...;
 * 3. o armazém fora da cadeia (pasta/offchain.dat);
 * 4. se a cadeia estiver vazia: o bloco de génese e o cenário base (apicultor,
 *    apiário, loja, lote de 340 kg, análise, certificado, 450 frascos e uma venda).
 *
 * Correr duas vezes seguidas dá o mesmo resultado: a segunda vez carrega tudo
 * dos ficheiros em vez de criar de novo.
 */
public class DemoData {

    // palavra-passe de todas as chaves da demonstração
    public static final String PASSWORD = "abelha2026";

    // nomes dos intervenientes (nomes dos ficheiros das chaves)
    public static final String DGAV = "DGAV";
    public static final String LAB = "Laboratorio";
    public static final String DOP = "Certificador-DOP";
    public static final String BEEKEEPER = "Apicultor-Manuel";
    public static final String SHOP = "Loja-Mel-de-Tomar";

    private final String folder;          // pasta da demonstração (normalmente "data")
    private final List<Node> nodes = new ArrayList<>();
    private LocalNetwork network;
    private ApisChainService service;
    private Actor dgav, lab, dop, beekeeper, shop;

    private DemoData(String folder) {
        this.folder = folder;
    }

    /**
     * Prepara a demonstração na pasta dada (ver o comentário da classe).
     *
     * @param folder pasta da demonstração, por exemplo "data"
     * @return a demonstração pronta a usar
     * @throws Exception se os ficheiros existirem mas estiverem corrompidos
     */
    public static DemoData load(String folder) throws Exception {
        DemoData d = new DemoData(folder);
        d.loadActors();
        d.loadNetwork();
        // cadeia vazia: primeira vez, cria-se a génese e o cenário base
        if (d.service.getChain().getSize() == 0) {
            d.createGenesis();
            d.createBaseScenario();
        }
        return d;
    }

    /**
     * Identidades: carregadas se já existirem, criadas na primeira vez.
     */
    private void loadActors() throws Exception {
        String keys = new File(folder, "keys").getPath();
        dgav = actor(DGAV, Role.DGAV, keys);
        lab = actor(LAB, Role.LAB, keys);
        dop = actor(DOP, Role.DOP, keys);
        beekeeper = actor(BEEKEEPER, Role.APICULTOR, keys);
        shop = actor(SHOP, Role.LOJA, keys);
    }

    private static Actor actor(String name, Role role, String keys) throws Exception {
        if (!new File(keys, name + ".id").exists()) {
            DigitalID.create(name, role, PASSWORD, keys);
        }
        return Actor.login(name, PASSWORD, keys);
    }

    /**
     * Os quatro nós (cada um carrega a sua cópia, se existir) e o armazém fora da cadeia.
     */
    private void loadNetwork() throws Exception {
        nodes.add(new Node("N1", dgav.getDid(), dgav.getKey(), new File(folder, "N1").getPath()));
        nodes.add(new Node("N2", lab.getDid(), lab.getKey(), new File(folder, "N2").getPath()));
        nodes.add(new Node("N3", dop.getDid(), dop.getKey(), new File(folder, "N3").getPath()));
        nodes.add(new Node("N4", null, null, new File(folder, "N4").getPath()));
        network = new LocalNetwork();
        for (Node n : nodes) {
            network.addNode(n);
        }
        OffChainStore offChain = new OffChainStore();
        String offChainFile = new File(folder, "offchain.dat").getPath();
        if (new File(offChainFile).exists()) {
            offChain.load(offChainFile);
        }
        service = new ApisChainService(network, offChain, offChainFile);
    }

    /**
     * Bloco de génese com as três autoridades, proposto pela DGAV.
     */
    private void createGenesis() throws Exception {
        GenesisTx genesis = new GenesisTx(dgav.getDid(), List.of(
                new Authority(dgav.getDid(), "DGAV", dgav.getId().getPublicKeyBase64()),
                new Authority(lab.getDid(), "LAB", lab.getId().getPublicKeyBase64()),
                new Authority(dop.getDid(), "DOP", dop.getId().getPublicKeyBase64())));
        genesis.sign(dgav.getKey());
        network.start(genesis);
    }

    /**
     * Cenário base da demonstração (como no protótipo ApisChain.html).
     */
    private void createBaseScenario() throws Exception {
        service.registerBeekeeper(dgav, beekeeper.getId(), Map.of(
                "nome", "Manuel Ferreira (fictício)", "nif", "999999990", "morada", "Serra de Tomar"));
        String apiary = service.registerApiary(dgav, beekeeper.getDid(), Map.of(
                "local", "Serra de Tomar", "coordenadas", "39.60, -8.41 (fictícias)", "colmeias", "40"));
        service.registerShop(dgav, shop.getId(), Map.of(
                "nome", "Mel de Tomar, Lda. (fictícia)", "morada", "Praça da República, Tomar"));
        String lot = service.createLot(beekeeper, apiary, 340, Map.of(
                "origemFloral", "urze", "colheita", "setembro de 2026"));
        service.issueAnalysis(lab, lot, Map.of(
                "origemFloral", "urze 62%", "humidade", "17.2%", "adulteracao", "não detetada"));
        service.issueCertificate(dop, lot, Map.of(
                "denominacao", "Mel da Serra de Tomar DOP (fictícia)", "validade", "2027-12-31"));
        service.packJars(beekeeper, lot, 1, 200, 1.0);
        service.packJars(beekeeper, lot, 201, 450, 0.5);
        service.sellJar(shop, lot + "-137");
    }

    /**
     * Lote do cenário base: o primeiro LOTE que entrou na cadeia.
     *
     * @return id do lote, ou null se ainda não existir
     */
    public String getBaseLot() throws Exception {
        for (Block b : service.getChain().getChain()) {
            for (String txt : b.getTransactions()) {
                Transaction t = Transaction.fromBase64(txt);
                if (t instanceof IssueCredentialTx c && c.getCredentialType().equals("LOTE")) {
                    return c.getCredentialId();
                }
            }
        }
        return null;
    }

    public ApisChainService getService() {
        return service;
    }

    public LocalNetwork getNetwork() {
        return network;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public Actor getDgav() {
        return dgav;
    }

    public Actor getLab() {
        return lab;
    }

    public Actor getDop() {
        return dop;
    }

    public Actor getBeekeeper() {
        return beekeeper;
    }

    public Actor getShop() {
        return shop;
    }
}
