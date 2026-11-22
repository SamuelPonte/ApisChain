//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Ideia baseada em DigitalIDServices.getPublicKey e                     ::://
//::  TemplarServices.getAmount do Templar2026 (Prof. António Manso)        ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.ledger;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.transactions.Authority;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import apischain.transactions.RevokeTx;
import apischain.transactions.SaleTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Estado da ApisChain neste momento: o que a cadeia diz, lida do início ao fim.
 *
 * Como no professor (DigitalIDServices.getPublicKey, TemplarServices.getAmount),
 * o estado não se guarda em ficheiro: calcula-se percorrendo a cadeia. A única
 * diferença é que se percorre a cadeia uma vez e se guardam os resultados em
 * mapas, para as consultas serem rápidas.
 *
 * O estado confia que as transações que estão na cadeia já foram aceites pelas
 * regras (Rules) quando entraram. Por isso apply() só regista, não valida.
 *
 * Papéis (texto, com os mesmos nomes do enum Role da issue #1):
 * - DGAV, LAB e DOP vêm da GenesisTx;
 * - APICULTOR e LOJA vêm das credenciais REGISTO_APICULTOR e REGISTO_LOJA,
 *   enquanto não forem revogadas.
 */
public class LedgerState {

    public static final String DGAV = "DGAV";
    public static final String LAB = "LAB";
    public static final String DOP = "DOP";
    public static final String APICULTOR = "APICULTOR";
    public static final String LOJA = "LOJA";

    // autoridades da génese, por DID
    private final HashMap<String, Authority> authorities = new HashMap<>();
    // chave pública (Base64) de cada DID conhecido: autoridades, apicultores e lojas
    private final HashMap<String, String> publicKeys = new HashMap<>();
    // credenciais emitidas, por id
    private final HashMap<String, IssueCredentialTx> credentials = new HashMap<>();
    // ids das credenciais revogadas
    private final HashSet<String> revoked = new HashSet<>();
    // embalagens, pela ordem em que entraram
    private final ArrayList<PackTx> packs = new ArrayList<>();
    // venda de cada frasco vendido, por código do frasco
    private final HashMap<String, SaleTx> sales = new HashMap<>();
    // alertas de fraude, pela ordem em que entraram
    private final ArrayList<FraudAlertTx> alerts = new ArrayList<>();

    /**
     * Estado vazio (antes da génese). Útil para validar o bloco de génese.
     */
    public LedgerState() {
    }

    /**
     * Calcula o estado a partir de uma cadeia.
     *
     * @param chain cadeia de blocos
     * @throws Exception se um bloco tiver uma transação que não se consegue ler
     */
    public LedgerState(BlockChain chain) throws Exception {
        // percorrer todos os blocos e, dentro de cada um, todas as transações
        for (Block b : chain.getChain()) {
            for (String txt : b.getTransactions()) {
                apply(Transaction.fromBase64(txt));
            }
        }
    }

    /**
     * Regista no estado uma transação já aceite. Também é usado pelo consenso
     * para ir atualizando o estado à medida que verifica as transações de um
     * bloco novo (por exemplo, para apanhar duas vendas do mesmo frasco no
     * mesmo bloco).
     *
     * @param tx transação aceite
     */
    public void apply(Transaction tx) {
        if (tx instanceof GenesisTx g) {
            // autoridades iniciais e as suas chaves
            for (Authority a : g.getAuthorities()) {
                authorities.put(a.getDid(), a);
                publicKeys.put(a.getDid(), a.getPublicKey());
            }
        } else if (tx instanceof IssueCredentialTx c) {
            credentials.put(c.getCredentialId(), c);
            // só nos registos de apicultor e de loja a cadeia fica a saber a chave do titular
            // (as regras só aceitam chave nestes dois tipos; assim o estado fica coerente com elas)
            if (Rules.carriesKey(c.getCredentialType())) {
                publicKeys.put(c.getSubject(), c.getSubjectKey());
            }
        } else if (tx instanceof RevokeTx r) {
            revoked.add(r.getCredentialId());
        } else if (tx instanceof PackTx p) {
            packs.add(p);
        } else if (tx instanceof SaleTx s) {
            sales.put(s.getJarId(), s);
        } else if (tx instanceof FraudAlertTx f) {
            alerts.add(f);
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  IDENTIDADES E PAPÉIS                                               ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * @return true se a génese já foi aplicada (há autoridades)
     */
    public boolean hasGenesis() {
        return !authorities.isEmpty();
    }

    /**
     * Chave pública de um DID, tal como está registada na cadeia.
     * Continua a devolver a chave depois de uma revogação, para se poderem
     * verificar as transações antigas; quem decide se o DID ainda pode agir é roleOf.
     *
     * @param did DID
     * @return chave pública, ou null se o DID não estiver registado
     */
    public PublicKey publicKeyOf(String did) {
        String key = publicKeys.get(did);
        if (key == null) {
            return null;
        }
        try {
            return SecurityUtils.publicKeyFromBase64(key);
        } catch (Exception e) {
            return null;  // chave mal formada: é como se não existisse
        }
    }

    /**
     * Papel atual de um DID.
     *
     * @param did DID
     * @return DGAV, LAB, DOP, APICULTOR ou LOJA; null se não tiver papel
     *         (desconhecido ou com o registo revogado)
     */
    public String roleOf(String did) {
        // 1. autoridade da génese?
        Authority a = authorities.get(did);
        if (a != null) {
            return a.getRole();
        }
        // 2. apicultor ou loja registado pela DGAV e não revogado?
        for (IssueCredentialTx c : credentials.values()) {
            if (c.getSubject().equals(did) && !revoked.contains(c.getCredentialId())) {
                if (c.getCredentialType().equals("REGISTO_APICULTOR")) {
                    return APICULTOR;
                }
                if (c.getCredentialType().equals("REGISTO_LOJA")) {
                    return LOJA;
                }
            }
        }
        return null;
    }

    /**
     * @param did DID
     * @return true se for uma das autoridades da génese (DGAV, LAB ou DOP)
     */
    public boolean isAuthority(String did) {
        return authorities.containsKey(did);
    }

    public List<Authority> getAuthorities() {
        return new ArrayList<>(authorities.values());
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  CREDENCIAIS                                                        ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public boolean credentialExists(String credentialId) {
        return credentials.containsKey(credentialId);
    }

    /**
     * @param credentialId id da credencial
     * @return a transação que a emitiu (com o hash ancorado), ou null
     */
    public IssueCredentialTx getCredential(String credentialId) {
        return credentials.get(credentialId);
    }

    public boolean isRevoked(String credentialId) {
        return revoked.contains(credentialId);
    }

    /**
     * Credenciais de um tipo sobre um titular (por exemplo, as análises de um lote).
     *
     * @param type tipo da credencial
     * @param subject titular
     * @return lista das credenciais (revogadas incluídas)
     */
    public List<IssueCredentialTx> getCredentials(String type, String subject) {
        List<IssueCredentialTx> list = new ArrayList<>();
        for (IssueCredentialTx c : credentials.values()) {
            if (c.getCredentialType().equals(type) && c.getSubject().equals(subject)) {
                list.add(c);
            }
        }
        return list;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  LOTES E FRASCOS                                                    ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Balanço de massa de um lote.
     *
     * @param lot id do lote
     * @return kg colhidos, embalados e livres (colhidos 0 se o lote não existir)
     */
    public LotBalance lotBalance(String lot) {
        IssueCredentialTx c = credentials.get(lot);
        double harvested = (c == null) ? 0 : c.getWeightKg();
        double packed = 0;
        for (PackTx p : packs) {
            if (p.getLot().equals(lot)) {
                packed += p.getTotalKg();
            }
        }
        return new LotBalance(harvested, packed);
    }

    /**
     * @param lot id do lote
     * @return embalagens do lote
     */
    public List<PackTx> getPacks(String lot) {
        List<PackTx> list = new ArrayList<>();
        for (PackTx p : packs) {
            if (p.getLot().equals(lot)) {
                list.add(p);
            }
        }
        return list;
    }

    /**
     * Embalagem que criou um frasco. O código do frasco é o lote seguido de
     * "-" e do número (LOTE-2026-0042-137).
     *
     * @param jarId código do frasco
     * @return a embalagem, ou null se o frasco não existir
     */
    public PackTx findPack(String jarId) {
        // 1. separar o lote do número pelo último "-"
        int i = jarId.lastIndexOf('-');
        if (i < 0) {
            return null;
        }
        String lot = jarId.substring(0, i);
        int number;
        try {
            number = Integer.parseInt(jarId.substring(i + 1));
        } catch (NumberFormatException e) {
            return null;
        }
        // 2. procurar a embalagem desse lote que contém esse número
        for (PackTx p : packs) {
            // o código tem de ser EXATAMENTE o que a embalagem gera: sem isto, "LOTE-1-0137"
            // e "LOTE-1-+137" eram o mesmo frasco 137 com outro texto, e a venda dupla passava
            if (p.getLot().equals(lot) && p.containsJar(number) && jarId.equals(p.getJarId(number))) {
                return p;
            }
        }
        return null;
    }

    public boolean jarExists(String jarId) {
        return findPack(jarId) != null;
    }

    public boolean isSold(String jarId) {
        return sales.containsKey(jarId);
    }

    /**
     * @param jarId código do frasco
     * @return a primeira venda do frasco, ou null
     */
    public SaleTx getSale(String jarId) {
        return sales.get(jarId);
    }

    /**
     * @param jarId código do frasco
     * @return alertas de fraude sobre o frasco
     */
    public List<FraudAlertTx> getAlerts(String jarId) {
        List<FraudAlertTx> list = new ArrayList<>();
        for (FraudAlertTx f : alerts) {
            if (f.getJarId().equals(jarId)) {
                list.add(f);
            }
        }
        return list;
    }
}
