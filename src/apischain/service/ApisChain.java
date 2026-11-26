//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import apischain.consensus.ProofOfAuthority;
import apischain.core.BlockChain;
import apischain.identity.Credential;
import apischain.identity.CredentialType;
import apischain.identity.DigitalID;
import apischain.identity.OffChainStore;
import apischain.identity.Role;
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.RevokeTx;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * O serviço ApisChain: emitir, revogar e verificar credenciais.
 *
 * Emitir uma credencial faz sempre os mesmos passos:
 * 1. cria a credencial completa e o emissor assina-a;
 * 2. cria a transação com o HASH da credencial e o emissor assina-a;
 * 3. entrega a transação ao consenso (regras e votação das autoridades);
 * 4. só se for aceite, guarda a credencial completa fora da cadeia.
 *
 * Ao abrir pela primeira vez, cria as chaves das três autoridades (palavra-passe
 * PASSWORD) e o bloco de génese. Entidades e dados fictícios.
 */
public class ApisChain {

    // palavra-passe das chaves das autoridades na demonstração
    public static final String PASSWORD = "abelha2026";
    // nomes das autoridades (nomes dos ficheiros das chaves)
    public static final String DGAV = "DGAV";
    public static final String LAB = "Laboratorio";
    public static final String DOP = "Certificador-DOP";

    private final String keysFolder;         // pasta das chaves (data/keys)
    private final String offChainFile;       // ficheiro das credenciais fora da cadeia
    private final ProofOfAuthority poa;      // consenso e cadeia
    private final OffChainStore offChain = new OffChainStore(); // credenciais completas

    /**
     * Abre o serviço na pasta dada (cria tudo na primeira vez).
     *
     * @param folder pasta dos dados, por exemplo "data"
     * @throws Exception se os ficheiros existirem mas estiverem corrompidos
     */
    public ApisChain(String folder) throws Exception {
        // 1. pastas e ficheiros
        new File(folder).mkdirs();
        this.keysFolder = new File(folder, "keys").getPath();
        this.offChainFile = new File(folder, "offchain.dat").getPath();
        this.poa = new ProofOfAuthority(new File(folder, "blockchain.bc").getPath());
        if (new File(offChainFile).exists()) {
            offChain.load(offChainFile);
        }
        // 2. as três autoridades: criadas na primeira vez e carregadas nas seguintes
        DigitalID dgav = authority(DGAV, Role.DGAV);
        DigitalID lab = authority(LAB, Role.LAB);
        DigitalID dop = authority(DOP, Role.DOP);
        // 3. cadeia vazia: bloco de génese com as autoridades, assinado pela DGAV
        if (poa.getChain().getSize() == 0) {
            GenesisTx genesis = new GenesisTx(dgav.getDid(), List.of(
                    new Authority(dgav.getDid(), "DGAV", dgav.getPublicKeyBase64()),
                    new Authority(lab.getDid(), "LAB", lab.getPublicKeyBase64()),
                    new Authority(dop.getDid(), "DOP", dop.getPublicKeyBase64())));
            genesis.sign(DigitalID.loadPrivateKey(DGAV, PASSWORD, keysFolder));
            poa.addTransaction(genesis);
        }
    }

    /**
     * Carrega (ou cria) uma autoridade e entrega a sua chave ao consenso.
     */
    private DigitalID authority(String name, Role role) throws Exception {
        if (!new File(keysFolder, name + ".id").exists()) {
            DigitalID.create(name, role, PASSWORD, keysFolder);
        }
        DigitalID id = DigitalID.load(name, keysFolder);
        poa.addAuthorityKey(id.getDid(), DigitalID.loadPrivateKey(name, PASSWORD, keysFolder));
        return id;
    }

    /**
     * Emite uma credencial (passos 1 a 4 descritos no início da classe).
     *
     * @param issuer identidade do emissor
     * @param key chave privada do emissor
     * @param type tipo da credencial
     * @param subject titular: DID do apicultor, id do apiário ou id do lote
     * @param subjectKey chave pública do titular (só no REGISTO_APICULTOR; senão "")
     * @param data dados da credencial (ficam fora da cadeia)
     * @return id da credencial
     * @throws Exception se o consenso recusar
     */
    public synchronized String issue(DigitalID issuer, PrivateKey key, CredentialType type, String subject,
            String subjectKey, Map<String, String> data) throws Exception {
        // 1. credencial completa, assinada pelo emissor
        String id = nextId(type);
        Credential c = new Credential(id, type, issuer.getDid(), subject, data);
        c.sign(key);
        // 2. transação com o hash da credencial, assinada pelo emissor
        IssueCredentialTx tx = new IssueCredentialTx(issuer.getDid(), id, type.name(), subject, c.getHash(), subjectKey);
        tx.sign(key);
        // 3. consenso (lança exceção se for recusada)
        poa.addTransaction(tx);
        // 4. aceite: guardar a credencial completa fora da cadeia
        offChain.put(c);
        offChain.save(offChainFile);
        return id;
    }

    /**
     * A DGAV revoga uma credencial.
     *
     * @param dgav identidade da DGAV
     * @param key chave privada da DGAV
     * @param credentialId credencial a revogar
     * @param reason motivo (sem dados pessoais: fica na cadeia)
     * @throws Exception se o consenso recusar
     */
    public synchronized void revoke(DigitalID dgav, PrivateKey key, String credentialId, String reason)
            throws Exception {
        RevokeTx tx = new RevokeTx(dgav.getDid(), credentialId, reason);
        tx.sign(key);
        poa.addTransaction(tx);
    }

    /**
     * Verifica uma credencial e as credenciais emitidas sobre ela (por exemplo, um
     * lote com as suas análises e certificados). Não precisa de chave.
     *
     * @param id id da credencial ou do lote (o que está no QR do frasco)
     * @return linhas do resultado; a primeira diz se é de confiar
     */
    public synchronized List<String> verify(String id) {
        List<String> lines = new ArrayList<>();
        boolean ok = true;
        try {
            BlockChain chain = poa.getChain();
            // 1. a cadeia está íntegra?
            if (!chain.isValid()) {
                lines.add("NÃO CONFIAR: a cadeia está corrompida no bloco #" + chain.getInvalidIndex());
                return lines;
            }
            // 2. a credencial e as que foram emitidas sobre ela
            List<IssueCredentialTx> list = new ArrayList<>();
            IssueCredentialTx main = ApisChainServices.getCredential(id, chain);
            if (main != null) {
                list.add(main);
            }
            list.addAll(ApisChainServices.getCredentialsAbout(id, chain));
            if (list.isEmpty()) {
                lines.add("NÃO CONFIAR: " + id + " não existe na cadeia");
                return lines;
            }
            // 3. verificar cada uma
            for (IssueCredentialTx tx : list) {
                String problem = check(tx, chain);
                if (problem == null) {
                    lines.add("OK: " + tx.getCredentialType() + " " + tx.getCredentialId()
                            + " assinada por " + ApisChainServices.getRole(tx.getAuthor(), chain)
                            + " e igual à ancorada " + offChain.get(tx.getCredentialId()).getData());
                } else {
                    lines.add("FALHA: " + tx.getCredentialType() + " " + tx.getCredentialId() + ": " + problem);
                    ok = false;
                }
            }
        } catch (Exception ex) {
            lines.add("FALHA: não foi possível verificar: " + ex.getMessage());
            ok = false;
        }
        lines.add(0, ok ? "AUTÊNTICO: " + id : "NÃO CONFIAR: " + id);
        return lines;
    }

    /**
     * Verifica uma credencial ancorada contra a sua cópia fora da cadeia.
     *
     * @return null se estiver tudo certo, ou a descrição do problema
     */
    private String check(IssueCredentialTx tx, BlockChain chain) throws Exception {
        // 1. não foi revogada
        if (ApisChainServices.isRevoked(tx.getCredentialId(), chain)) {
            return "revogada pela DGAV";
        }
        // 2. a credencial completa existe e é a mesma que foi ancorada
        Credential c = offChain.get(tx.getCredentialId());
        if (c == null) {
            return "a credencial completa não existe fora da cadeia";
        }
        if (!c.getType().name().equals(tx.getCredentialType()) || !c.getSubject().equals(tx.getSubject())
                || !c.getIssuer().equals(tx.getAuthor())) {
            return "a credencial fora da cadeia não corresponde à ancorada";
        }
        // 3. ninguém a alterou: o hash tem de ser o que está na cadeia
        if (!c.getHash().equals(tx.getCredentialHash())) {
            return "ALTERADA fora da cadeia (o hash não coincide com o ancorado)";
        }
        // 4. foi assinada pelo emissor, com a chave que está na cadeia
        String key = ApisChainServices.getPublicKey(c.getIssuer(), chain);
        if (key == null || !c.isSignatureValid(SecurityUtils.publicKeyFromBase64(key))) {
            return "a assinatura não é do emissor";
        }
        return null;
    }

    /**
     * Próximo id livre de um tipo: AP-0001, APIARIO-0001, LOTE-0001, AN-0001, DOP-0001.
     */
    private String nextId(CredentialType type) throws Exception {
        String prefix;
        switch (type) {
            case REGISTO_APICULTOR -> prefix = "AP-";
            case REGISTO_APIARIO -> prefix = "APIARIO-";
            case LOTE -> prefix = "LOTE-";
            case ANALISE_LOTE -> prefix = "AN-";
            default -> prefix = "DOP-";
        }
        int n = 1;
        while (ApisChainServices.getCredential(prefix + String.format("%04d", n), poa.getChain()) != null) {
            n++;
        }
        return prefix + String.format("%04d", n);
    }

    public BlockChain getChain() {
        return poa.getChain();
    }

    public OffChainStore getOffChain() {
        return offChain;
    }

    public String getKeysFolder() {
        return keysFolder;
    }
}
