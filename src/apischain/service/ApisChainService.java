//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Ideia baseada em TemplarServices do Templar2026 (Prof. António Manso) ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import apischain.consensus.LocalNetwork;
import apischain.consensus.Node;
import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.core.MerkleTree;
import apischain.identity.Credential;
import apischain.identity.CredentialType;
import apischain.identity.DigitalID;
import apischain.identity.OffChainStore;
import apischain.ledger.LedgerState;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import apischain.transactions.RevokeTx;
import apischain.transactions.SaleTx;
import apischain.transactions.Transaction;
import java.security.PublicKey;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fachada do serviço ApisChain: cada operação do serviço, do princípio ao fim,
 * num só método. As interfaces só falam com esta classe.
 *
 * Uma operação que emite uma credencial faz sempre os mesmos passos:
 * 1. cria a credencial completa e o emissor assina-a;
 * 2. cria a transação com o HASH da credencial e o emissor assina-a;
 * 3. entrega a transação à rede (consenso PoA com as regras dos validadores);
 * 4. só se a rede aceitar, guarda a credencial completa fora da cadeia.
 *
 * Esta classe não tem regras de negócio: quem decide é Rules, dentro do
 * consenso. Se uma operação for recusada, a exceção (RuleException ou outra)
 * traz a mensagem em português para a interface mostrar.
 *
 * Os métodos que alteram a cadeia são synchronized porque as interfaces os
 * chamam a partir de threads (para não bloquear o ecrã).
 */
public class ApisChainService {

    private final LocalNetwork network;  // rede de nós (PoC: todos no mesmo programa)
    private final OffChainStore offChain; // credenciais completas, fora da cadeia
    private final String offChainFile;   // ficheiro onde se grava o OffChainStore (null: não grava)

    /**
     * @param network rede já com os nós e com a génese
     * @param offChain armazém fora da cadeia
     * @param offChainFile ficheiro do armazém (null para não gravar)
     */
    public ApisChainService(LocalNetwork network, OffChainStore offChain, String offChainFile) {
        this.network = network;
        this.offChain = offChain;
        this.offChainFile = offChainFile;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  LEITURA                                                            ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Cadeia vista por um nó ligado (de preferência o N4, que não é validador
     * e representa quem só consulta, como o consumidor).
     *
     * Devolve a cadeia do PRÓPRIO nó, não uma cópia: outra thread pode estar a
     * acrescentar-lhe blocos. Quem a percorrer tem de o fazer dentro de
     * synchronized (getNetwork()), que é o bloqueio usado por quem escreve.
     *
     * @return cadeia de um nó ligado
     * @throws Exception se não houver nós ligados
     */
    public BlockChain getChain() throws Exception {
        Node reference = null;
        for (Node n : network.getNodes()) {
            if (n.isOnline() && (reference == null || !n.isValidator())) {
                reference = n;
            }
        }
        if (reference == null) {
            throw new Exception("Não há nós ligados");
        }
        return reference.getChain();
    }

    /**
     * Estado atual, calculado a partir da cadeia. O bloqueio da rede impede que
     * entrem blocos (submit) ou que um nó sincronize (setOnline) a meio da leitura.
     *
     * @return estado atual
     * @throws Exception se a cadeia estiver corrompida ou não houver nós ligados
     */
    public LedgerState getState() throws Exception {
        synchronized (network) {
            return readState(getChain());
        }
    }

    /**
     * Verifica a integridade da cadeia ANTES de a ler: numa cadeia adulterada as
     * transações podem nem se conseguir ler, e o erro não se percebia.
     */
    private static LedgerState readState(BlockChain chain) throws Exception {
        int invalid = chain.getInvalidIndex();
        if (invalid >= 0) {
            throw new Exception("a cópia da cadeia está corrompida no bloco #" + invalid);
        }
        return new LedgerState(chain);
    }

    public LocalNetwork getNetwork() {
        return network;
    }

    public OffChainStore getOffChain() {
        return offChain;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  OPERAÇÕES DA DGAV                                                  ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * A DGAV regista um apicultor. Os dados pessoais (nome, NIF, morada) ficam
     * só na credencial fora da cadeia; na cadeia entram o DID e a chave pública.
     *
     * @param dgav DGAV com sessão aberta
     * @param beekeeper identidade do apicultor (DID e chave pública)
     * @param data dados pessoais do apicultor
     * @return id da credencial (AP-0001, ...)
     * @throws Exception se a rede recusar
     */
    public synchronized String registerBeekeeper(Actor dgav, DigitalID beekeeper, Map<String, String> data)
            throws Exception {
        return issue(dgav, "AP-", CredentialType.REGISTO_APICULTOR, beekeeper.getDid(),
                beekeeper.getPublicKeyBase64(), 0, data);
    }

    /**
     * A DGAV regista uma loja (só uma loja registada pode vender).
     *
     * @return id da credencial (LJ-0001, ...)
     */
    public synchronized String registerShop(Actor dgav, DigitalID shop, Map<String, String> data) throws Exception {
        return issue(dgav, "LJ-", CredentialType.REGISTO_LOJA, shop.getDid(), shop.getPublicKeyBase64(), 0, data);
    }

    /**
     * A DGAV regista um apiário de um apicultor (localização fica fora da cadeia).
     *
     * @param beekeeperDid DID do apicultor dono do apiário
     * @return id do apiário (APIARIO-0001, ...)
     */
    public synchronized String registerApiary(Actor dgav, String beekeeperDid, Map<String, String> data)
            throws Exception {
        return issue(dgav, "APIARIO-", CredentialType.REGISTO_APIARIO, beekeeperDid, "", 0, data);
    }

    /**
     * A DGAV revoga uma credencial (registo, lote, análise ou certificado).
     *
     * @param credentialId credencial a revogar
     * @param reason motivo
     */
    public synchronized void revoke(Actor dgav, String credentialId, String reason) throws Exception {
        RevokeTx tx = new RevokeTx(dgav.getDid(), credentialId, reason);
        tx.sign(dgav.getKey());
        network.submit(tx);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  OPERAÇÕES DO APICULTOR, DO LABORATÓRIO E DO DOP                    ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * O apicultor cria um lote com os kg colhidos. Os kg ficam em claro na
     * cadeia (são precisos para o balanço de massa).
     *
     * @param apiaryId apiário de origem
     * @param kg kg colhidos
     * @param data outros dados do lote (origem floral, data da colheita...)
     * @return id do lote (LOTE-2026-0001, ...)
     */
    public synchronized String createLot(Actor beekeeper, String apiaryId, double kg, Map<String, String> data)
            throws Exception {
        // os kg também ficam na credencial, para quem a consultar
        Map<String, String> all = new HashMap<>(data);
        all.put("kgColhidos", String.valueOf(kg));
        return issue(beekeeper, "LOTE-" + Year.now().getValue() + "-", CredentialType.LOTE, apiaryId, "", kg, all);
    }

    /**
     * O Laboratório emite a análise de um lote.
     *
     * @return id da análise (AN-0001, ...)
     */
    public synchronized String issueAnalysis(Actor lab, String lotId, Map<String, String> data) throws Exception {
        return issue(lab, "AN-", CredentialType.ANALISE_LOTE, lotId, "", 0, data);
    }

    /**
     * O Certificador DOP emite o certificado de origem de um lote.
     *
     * @return id do certificado (DOP-0001, ...)
     */
    public synchronized String issueCertificate(Actor dop, String lotId, Map<String, String> data) throws Exception {
        return issue(dop, "DOP-", CredentialType.CERTIFICADO_ORIGEM, lotId, "", 0, data);
    }

    /**
     * O apicultor embala parte de um lote em frascos numerados.
     *
     * @param first primeiro frasco
     * @param last último frasco (inclusive)
     * @param kgPerJar peso de cada frasco
     * @return a embalagem que entrou na cadeia (getJarId(n) dá o código de cada frasco)
     * @throws Exception RuleException se passar o balanço de massa, entre outras
     */
    public synchronized PackTx packJars(Actor beekeeper, String lotId, int first, int last, double kgPerJar)
            throws Exception {
        PackTx tx = new PackTx(beekeeper.getDid(), lotId, first, last, kgPerJar);
        tx.sign(beekeeper.getKey());
        network.submit(tx);
        return tx;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  OPERAÇÃO DA LOJA                                                   ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * A loja regista a venda de um frasco.
     *
     * @param jarId código do frasco (lido do QR)
     * @return a transação que entrou: SaleTx se foi a primeira venda, ou
     *         FraudAlertTx se o frasco já tinha sido vendido (QR copiado)
     */
    public synchronized Transaction sellJar(Actor shop, String jarId) throws Exception {
        SaleTx tx = new SaleTx(shop.getDid(), jarId);
        tx.sign(shop.getKey());
        Block b = network.submit(tx);
        // o proponente pode ter trocado a venda por um alerta: devolve-se o que entrou mesmo
        return Transaction.fromBase64(b.getTransactions().get(0));
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  VERIFICAÇÃO DO FRASCO (CONSUMIDOR)                                 ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Verifica um frasco do princípio ao fim, sem confiar em ninguém: só na
     * cadeia (cópia do nó N4) e nas assinaturas.
     *
     * Só lê, mas é synchronized para não apanhar uma operação a meio (a transação
     * já na cadeia e a credencial ainda por guardar fora dela), e bloqueia também
     * a rede, porque ligar um nó escreve na cadeia dele sem passar por esta classe.
     *
     * @param jarId código do frasco (lido do QR)
     * @return resultado com o nível (OK, AVISO, FALHA) e as verificações feitas
     */
    public synchronized VerificationResult verifyJar(String jarId) {
        VerificationResult r = new VerificationResult(jarId);
        try {
            synchronized (network) {
                checkJar(r, jarId);
            }
        } catch (Exception e) {
            r.fail("não foi possível verificar: " + e.getMessage());
        }
        return r;
    }

    /**
     * As verificações de um frasco, por ordem.
     */
    private void checkJar(VerificationResult r, String jarId) throws Exception {
        // 1. a cópia da cadeia está íntegra? (readState lança exceção se não estiver)
        BlockChain chain = getChain();
        LedgerState state = readState(chain);
        // 2. o frasco existe? (foi embalado por um apicultor)
        PackTx pack = state.findPack(jarId);
        if (pack == null) {
            r.fail("frasco desconhecido na cadeia: QR falso ou mal lido");
            return;
        }
        checkPack(r, chain, state, pack);
        // 3. o lote
        String lotId = pack.getLot();
        IssueCredentialTx lot = state.getCredential(lotId);
        checkCredential(r, state, lot, "lote");
        if (lot != null && state.roleOf(lot.getAuthor()) == null) {
            r.warn("o registo do apicultor " + lot.getAuthor() + " foi revogado pela DGAV");
        }
        // 4. análises e certificados do lote (pode haver mais do que um)
        checkAll(r, state, state.getCredentials("ANALISE_LOTE", lotId), "análise",
                "o lote não tem análise do Laboratório");
        checkAll(r, state, state.getCredentials("CERTIFICADO_ORIGEM", lotId), "certificado de origem",
                "o lote não tem certificado de origem DOP");
        // 5. alertas de fraude sobre o frasco
        List<FraudAlertTx> alerts = state.getAlerts(jarId);
        for (FraudAlertTx a : alerts) {
            r.fail("ALERTA_FRAUDE: " + a.getReason());
        }
        // 6. venda (informação)
        SaleTx sale = state.getSale(jarId);
        if (sale == null) {
            r.ok("ainda não foi vendido");
        } else if (alerts.isEmpty()) {
            r.ok("vendido uma vez, pela loja " + sale.getShop());
        }
    }

    /**
     * Embalagem: assinatura do apicultor e prova de Merkle de que está num bloco.
     */
    private void checkPack(VerificationResult r, BlockChain chain, LedgerState state, PackTx pack) throws Exception {
        // 1. assinatura do apicultor, com a chave que está na cadeia
        PublicKey key = state.publicKeyOf(pack.getAuthor());
        if (key == null || !pack.isSignatureValid(key)) {
            r.fail("a assinatura da embalagem não é do apicultor " + pack.getAuthor());
        }
        // 2. procurar o bloco que tem a embalagem e provar com Merkle que ela lá está
        for (Block b : chain.getChain()) {
            for (String txt : b.getTransactions()) {
                Transaction t = Transaction.fromBase64(txt);
                if (t instanceof PackTx p && p.getSignature().equals(pack.getSignature())) {
                    List<String> proof = b.getMerkleTree().getProof(txt);
                    // a prova tem de levar à raiz GUARDADA NO BLOCO (não basta a raiz que vem na prova)
                    if (MerkleTree.isProofValid(txt, proof)
                            && proof.get(proof.size() - 1).equals(b.getMerkleRoot())) {
                        r.ok(String.format("embalado pelo apicultor (frascos %d a %d de %.2f kg), no bloco #%d; "
                                + "prova de Merkle válida até à raiz do bloco", pack.getFirstJar(), pack.getLastJar(),
                                pack.getWeightKg(), b.getIndex()));
                    } else {
                        r.fail("a prova de Merkle da embalagem não leva à raiz do bloco #" + b.getIndex());
                    }
                    return;
                }
            }
        }
        r.fail("a embalagem não foi encontrada em nenhum bloco");
    }

    /**
     * Verifica as credenciais do mesmo tipo de um lote. Se não houver nenhuma, é
     * um aviso. Uma credencial revogada só é FALHA se não sobrar nenhuma por
     * revogar: se o emissor emitiu outra, o erro foi corrigido e a revogada
     * aparece só como informação. As que sobram são verificadas por completo.
     */
    private void checkAll(VerificationResult r, LedgerState state, List<IssueCredentialTx> list,
            String label, String missing) {
        if (list.isEmpty()) {
            r.warn(missing);
        }
        // 1. sobra alguma por revogar?
        boolean replaced = false;
        for (IssueCredentialTx c : list) {
            if (!state.isRevoked(c.getCredentialId())) {
                replaced = true;
            }
        }
        // 2. verificar cada uma
        for (IssueCredentialTx c : list) {
            if (replaced && state.isRevoked(c.getCredentialId())) {
                r.ok(label + " " + c.getCredentialId() + ": revogação registada, já há substituição em vigor");
            } else {
                checkCredential(r, state, c, label);
            }
        }
    }

    /**
     * Verifica uma credencial ancorada na cadeia contra a sua cópia fora da cadeia:
     * 1. existe e não está revogada;
     * 2. a credencial fora da cadeia existe e tem o mesmo id, tipo, titular e
     *    emissor que a transação (senão podia ancorar-se o hash de outra credencial);
     * 3. o hash coincide com o ancorado (ninguém a alterou fora da cadeia);
     * 4. a assinatura é do emissor, com a chave que está na cadeia.
     */
    private void checkCredential(VerificationResult r, LedgerState state, IssueCredentialTx tx, String label) {
        if (tx == null) {
            r.fail(label + " não existe na cadeia");
            return;
        }
        String id = tx.getCredentialId();
        if (state.isRevoked(id)) {
            r.fail(label + " " + id + ": REVOGAÇÃO registada pela DGAV");
            return;
        }
        Credential c = offChain.get(id);
        if (c == null) {
            r.fail(label + " " + id + ": a credencial completa não existe fora da cadeia");
            return;
        }
        if (!c.getId().equals(id) || !c.getType().name().equals(tx.getCredentialType())
                || !c.getSubject().equals(tx.getSubject()) || !c.getIssuer().equals(tx.getAuthor())) {
            r.fail(label + " " + id + ": a credencial fora da cadeia não corresponde à ancorada");
            return;
        }
        if (!c.getHash().equals(tx.getCredentialHash())) {
            r.fail(label + " " + id + ": ALTERAÇÃO fora da cadeia, o hash não coincide com o ancorado");
            return;
        }
        PublicKey key = state.publicKeyOf(c.getIssuer());
        if (key == null || !c.isSignatureValid(key)) {
            r.fail(label + " " + id + ": a assinatura não é do emissor " + c.getIssuer());
            return;
        }
        // um emissor com o registo revogado já não tem papel (roleOf devolve null)
        String role = state.roleOf(c.getIssuer());
        r.ok(label + " " + id + ": assinatura de " + (role == null ? c.getIssuer() : role)
                + " válida e hash igual ao ancorado " + c.getData());
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  AUXILIARES                                                         ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Emite uma credencial: passos 1 a 4 descritos no início da classe.
     *
     * @return id da credencial
     */
    private String issue(Actor issuer, String prefix, CredentialType type, String subject,
            String subjectKey, double weightKg, Map<String, String> data) throws Exception {
        // 1. credencial completa, assinada pelo emissor
        String id = nextId(prefix);
        Credential c = new Credential(id, type, issuer.getDid(), subject, data);
        c.sign(issuer.getKey());
        // 2. transação com o hash da credencial, assinada pelo emissor
        IssueCredentialTx tx = new IssueCredentialTx(issuer.getDid(), id, type.name(), subject,
                c.getHash(), subjectKey, weightKg);
        tx.sign(issuer.getKey());
        // 3. entregar à rede (lança exceção se as regras ou o consenso recusarem)
        network.submit(tx);
        // 4. aceite: guardar a credencial completa fora da cadeia
        offChain.put(c);
        if (offChainFile != null) {
            offChain.save(offChainFile);
        }
        return id;
    }

    /**
     * Próximo id livre com um prefixo: AP-0001, AP-0002, ...
     */
    private String nextId(String prefix) throws Exception {
        LedgerState state = getState();
        int n = 1;
        while (state.credentialExists(prefix + String.format("%04d", n))) {
            n++;
        }
        return prefix + String.format("%04d", n);
    }
}
