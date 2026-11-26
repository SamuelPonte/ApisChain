//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em digitalID.core.DigitalIDServices do Templar2026_DigitalID  ::://
//::  (Prof. António Manso, 2026)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.identity.DigitalID;
import apischain.transactions.Authority;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.RevokeTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas à cadeia e regras dos validadores.
 *
 * Como no DigitalIDServices do professor, a informação não se guarda à parte:
 * procura-se percorrendo a cadeia do início ao fim.
 *
 * Diferenças face ao DigitalIDServices:
 * 1. a chave pública de um DID vem da génese (autoridades) ou de um registo
 *    feito pela DGAV (apicultores), e não de um registo que qualquer pessoa faz;
 * 2. tem as regras que as autoridades verificam antes de aceitar uma transação.
 */
public class ApisChainServices {

    /**
     * Todas as transações da cadeia, pela ordem dos blocos.
     *
     * @param chain cadeia
     * @return lista das transações
     * @throws Exception se um bloco tiver uma transação que não se consegue ler
     */
    public static List<Transaction> getTransactions(BlockChain chain) throws Exception {
        List<Transaction> list = new ArrayList<>();
        for (Block b : chain.getChain()) {
            list.add(Transaction.fromBase64(b.getData()));
        }
        return list;
    }

    /**
     * Autoridades da rede: estão na transação do bloco de génese.
     *
     * @param chain cadeia
     * @return lista das autoridades (vazia se a cadeia ainda não tiver génese)
     * @throws Exception se o bloco 0 não se conseguir ler
     */
    public static List<Authority> getAuthorities(BlockChain chain) throws Exception {
        if (chain.getSize() == 0) {
            return new ArrayList<>();
        }
        GenesisTx genesis = (GenesisTx) Transaction.fromBase64(chain.get(0).getData());
        return genesis.getAuthorities();
    }

    /**
     * Chave pública de um DID, tal como está na cadeia (como o getPublicKey do professor).
     *
     * @param did DID
     * @param chain cadeia
     * @return chave pública em Base64, ou null se o DID não estiver registado
     * @throws Exception se a cadeia não se conseguir ler
     */
    public static String getPublicKey(String did, BlockChain chain) throws Exception {
        // 1. autoridade da génese?
        for (Authority a : getAuthorities(chain)) {
            if (a.getDid().equals(did)) {
                return a.getPublicKey();
            }
        }
        // 2. apicultor registado pela DGAV?
        for (Transaction t : getTransactions(chain)) {
            if (t instanceof IssueCredentialTx c && c.getCredentialType().equals("REGISTO_APICULTOR")
                    && c.getSubject().equals(did)) {
                return c.getSubjectKey();
            }
        }
        return null;
    }

    /**
     * Papel atual de um DID.
     *
     * @param did DID
     * @param chain cadeia
     * @return DGAV, LAB, DOP, APICULTOR, ou null (desconhecido ou registo revogado)
     * @throws Exception se a cadeia não se conseguir ler
     */
    public static String getRole(String did, BlockChain chain) throws Exception {
        for (Authority a : getAuthorities(chain)) {
            if (a.getDid().equals(did)) {
                return a.getRole();
            }
        }
        for (Transaction t : getTransactions(chain)) {
            if (t instanceof IssueCredentialTx c && c.getCredentialType().equals("REGISTO_APICULTOR")
                    && c.getSubject().equals(did) && !isRevoked(c.getCredentialId(), chain)) {
                return "APICULTOR";
            }
        }
        return null;
    }

    /**
     * @param credentialId id da credencial
     * @param chain cadeia
     * @return a transação que emitiu a credencial (com o hash ancorado), ou null
     * @throws Exception se a cadeia não se conseguir ler
     */
    public static IssueCredentialTx getCredential(String credentialId, BlockChain chain) throws Exception {
        for (Transaction t : getTransactions(chain)) {
            if (t instanceof IssueCredentialTx c && c.getCredentialId().equals(credentialId)) {
                return c;
            }
        }
        return null;
    }

    /**
     * @param subject titular (por exemplo o id de um lote)
     * @param chain cadeia
     * @return credenciais emitidas sobre esse titular (análises e certificados de um lote)
     * @throws Exception se a cadeia não se conseguir ler
     */
    public static List<IssueCredentialTx> getCredentialsAbout(String subject, BlockChain chain) throws Exception {
        List<IssueCredentialTx> list = new ArrayList<>();
        for (Transaction t : getTransactions(chain)) {
            if (t instanceof IssueCredentialTx c && c.getSubject().equals(subject)) {
                list.add(c);
            }
        }
        return list;
    }

    /**
     * @param credentialId id da credencial
     * @param chain cadeia
     * @return true se a DGAV a revogou
     * @throws Exception se a cadeia não se conseguir ler
     */
    public static boolean isRevoked(String credentialId, BlockChain chain) throws Exception {
        for (Transaction t : getTransactions(chain)) {
            if (t instanceof RevokeTx r && r.getCredentialId().equals(credentialId)) {
                return true;
            }
        }
        return false;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  REGRAS DOS VALIDADORES                                             ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Verifica se uma transação pode entrar na cadeia. Só tem regras de
     * segurança: quem assina, com que chave e com que papel.
     *
     * @param tx transação
     * @param chain cadeia atual
     * @throws Exception com a razão, se a transação for recusada
     */
    public static void validate(Transaction tx, BlockChain chain) throws Exception {
        // 1. a génese só entra numa cadeia vazia, assinada por uma das autoridades que declara
        if (tx instanceof GenesisTx g) {
            if (chain.getSize() > 0) {
                throw new Exception("A cadeia já tem génese");
            }
            for (Authority a : g.getAuthorities()) {
                if (a.getDid().equals(g.getAuthor())
                        && g.isSignatureValid(SecurityUtils.publicKeyFromBase64(a.getPublicKey()))) {
                    return;
                }
            }
            throw new Exception("A génese tem de ser assinada por uma das autoridades que declara");
        }
        if (chain.getSize() == 0) {
            throw new Exception("A cadeia ainda não tem génese");
        }
        // 2. a assinatura verifica com a chave do autor QUE ESTÁ NA CADEIA
        String key = getPublicKey(tx.getAuthor(), chain);
        if (key == null) {
            throw new Exception("Autor desconhecido na cadeia: " + tx.getAuthor());
        }
        PublicKey pub = SecurityUtils.publicKeyFromBase64(key);
        if (!tx.isSignatureValid(pub)) {
            throw new Exception("Assinatura inválida de " + tx.getAuthor());
        }
        // 3. regras de cada tipo
        String role = getRole(tx.getAuthor(), chain);
        if (tx instanceof IssueCredentialTx c) {
            validateIssue(c, role, chain);
        } else if (tx instanceof RevokeTx r) {
            if (!"DGAV".equals(role)) {
                throw new Exception("Só a DGAV pode revogar credenciais");
            }
            if (getCredential(r.getCredentialId(), chain) == null) {
                throw new Exception("A credencial " + r.getCredentialId() + " não existe");
            }
            if (isRevoked(r.getCredentialId(), chain)) {
                throw new Exception("A credencial " + r.getCredentialId() + " já está revogada");
            }
        } else {
            throw new Exception("Tipo de transação desconhecido");
        }
    }

    /**
     * Regras da emissão de uma credencial.
     */
    private static void validateIssue(IssueCredentialTx c, String role, BlockChain chain) throws Exception {
        String type = c.getCredentialType();
        // 1. quem pode emitir cada tipo
        String needed;
        switch (type) {
            case "REGISTO_APICULTOR", "REGISTO_APIARIO" -> needed = "DGAV";
            case "LOTE" -> needed = "APICULTOR";
            case "ANALISE_LOTE" -> needed = "LAB";
            case "CERTIFICADO_ORIGEM" -> needed = "DOP";
            default -> throw new Exception("Tipo de credencial desconhecido: " + type);
        }
        if (!needed.equals(role)) {
            throw new Exception("Só " + needed + " pode emitir " + type + " (o autor tem o papel " + role + ")");
        }
        // 2. o id da credencial é único
        if (getCredential(c.getCredentialId(), chain) != null) {
            throw new Exception("Já existe uma credencial com o id " + c.getCredentialId());
        }
        // 3. a chave do titular é o que dá a um DID o poder de assinar: só o registo
        //    do apicultor a traz, o DID tem de ser o derivado dela e só se regista uma vez
        if (type.equals("REGISTO_APICULTOR")) {
            if (!c.getSubject().equals(DigitalID.createDid(c.getSubjectKey()))) {
                throw new Exception("O DID " + c.getSubject() + " não corresponde à chave pública do registo");
            }
            if (getPublicKey(c.getSubject(), chain) != null) {
                throw new Exception("O DID " + c.getSubject() + " já está registado");
            }
        } else if (!c.getSubjectKey().isEmpty()) {
            throw new Exception("Uma credencial " + type + " não pode trazer chave pública");
        }
    }
}
