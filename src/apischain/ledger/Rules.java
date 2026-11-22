//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Regras de referência: protótipo ApisChain.html (Fase 1)               ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.ledger;

import apischain.identity.DigitalID;
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

/**
 * Regras dos validadores: decidem se uma transação pode entrar na cadeia.
 *
 * check() tem três resultados possíveis:
 * 1. devolve a própria transação: é aceite;
 * 2. lança RuleException: é rejeitada e não entra (a mensagem diz porquê);
 * 3. devolve um FraudAlertTx POR ASSINAR: a transação não entra, entra o alerta
 *    no seu lugar (segunda venda do mesmo frasco). Quem assina o alerta é a
 *    autoridade que propõe o bloco (consenso, issue #4).
 *
 * As regras só leem o estado (LedgerState); não o alteram. Depois de aceitar
 * uma transação, quem a aceitou chama state.apply(tx).
 */
public class Rules {

    // tolerância do balanço de massa: perdas e arredondamentos de pesagem (2%)
    public static final double TOLERANCE = 0.02;

    /**
     * Verifica uma transação.
     *
     * @param tx transação a verificar
     * @param state estado atual da cadeia
     * @param proposer DID da autoridade que propõe o bloco (autor de um eventual alerta)
     * @return a transação aceite, ou um FraudAlertTx por assinar
     * @throws RuleException se a transação for rejeitada
     */
    public static Transaction check(Transaction tx, LedgerState state, String proposer) throws RuleException {
        // 1. a génese tem regras próprias: só pode haver uma, no início
        if (tx instanceof GenesisTx g) {
            checkGenesis(g, state);
            return tx;
        }
        // 2. a assinatura tem de verificar com a chave do autor QUE ESTÁ NA CADEIA
        checkSignature(tx, state);
        // 3. regras de cada tipo
        String role = state.roleOf(tx.getAuthor());
        if (tx instanceof IssueCredentialTx c) {
            checkIssue(c, role, state);
        } else if (tx instanceof RevokeTx r) {
            checkRevoke(r, role, state);
        } else if (tx instanceof PackTx p) {
            checkPack(p, role, state);
        } else if (tx instanceof SaleTx s) {
            return checkSale(s, role, state, proposer);
        } else if (tx instanceof FraudAlertTx f) {
            requireRole(role, "emitir um alerta de fraude", LedgerState.DGAV, LedgerState.LAB, LedgerState.DOP);
            if (!state.jarExists(f.getJarId())) {
                throw new RuleException("O frasco " + f.getJarId() + " não existe");
            }
        } else {
            throw new RuleException("Tipo de transação desconhecido: " + tx.getClass().getSimpleName());
        }
        return tx;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  REGRAS COMUNS                                                      ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * A génese só entra numa cadeia vazia e tem de ser assinada por uma das
     * autoridades que ela própria declara.
     */
    private static void checkGenesis(GenesisTx g, LedgerState state) throws RuleException {
        if (state.hasGenesis()) {
            throw new RuleException("A cadeia já tem génese: não pode haver uma segunda");
        }
        // procurar o autor na lista de autoridades da própria génese
        for (Authority a : g.getAuthorities()) {
            if (a.getDid().equals(g.getAuthor())) {
                try {
                    PublicKey key = SecurityUtils.publicKeyFromBase64(a.getPublicKey());
                    if (g.isSignatureValid(key)) {
                        return;
                    }
                } catch (Exception e) {
                    throw new RuleException("Chave pública inválida na génese: " + a.getDid());
                }
                throw new RuleException("Assinatura da génese inválida");
            }
        }
        throw new RuleException("A génese tem de ser assinada por uma das autoridades que declara");
    }

    /**
     * A assinatura verifica-se com a chave registada na cadeia, nunca com uma
     * chave que viesse na própria transação.
     */
    private static void checkSignature(Transaction tx, LedgerState state) throws RuleException {
        PublicKey key = state.publicKeyOf(tx.getAuthor());
        if (key == null) {
            throw new RuleException("Autor desconhecido na cadeia: " + tx.getAuthor());
        }
        if (!tx.isSignatureValid(key)) {
            throw new RuleException("Assinatura inválida de " + tx.getAuthor());
        }
    }

    /**
     * O papel do autor tem de ser um dos permitidos para a ação.
     *
     * @param role papel atual do autor (null se não tiver)
     * @param action descrição da ação, para a mensagem de erro
     * @param allowed papéis permitidos
     */
    private static void requireRole(String role, String action, String... allowed) throws RuleException {
        for (String r : allowed) {
            if (r.equals(role)) {
                return;
            }
        }
        throw new RuleException("Sem permissão para " + action + ": papel "
                + (role == null ? "nenhum (desconhecido ou revogado)" : role)
                + ", é preciso " + String.join(" ou ", allowed));
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  REGRAS DE CADA TIPO                                                ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Emissão de credencial: o papel do emissor depende do tipo; o id não pode
     * repetir; cada tipo tem os seus campos obrigatórios.
     */
    private static void checkIssue(IssueCredentialTx c, String role, LedgerState state) throws RuleException {
        String type = c.getCredentialType();
        // 1. quem pode emitir cada tipo
        switch (type) {
            case "REGISTO_APICULTOR", "REGISTO_APIARIO", "REGISTO_LOJA" ->
                requireRole(role, "emitir " + type, LedgerState.DGAV);
            case "LOTE" ->
                requireRole(role, "criar um lote", LedgerState.APICULTOR);
            case "ANALISE_LOTE" ->
                requireRole(role, "emitir uma análise", LedgerState.LAB);
            case "CERTIFICADO_ORIGEM" ->
                requireRole(role, "emitir um certificado de origem", LedgerState.DOP);
            default ->
                throw new RuleException("Tipo de credencial desconhecido: " + type);
        }
        // 2. o id da credencial é único
        if (state.credentialExists(c.getCredentialId())) {
            throw new RuleException("Já existe uma credencial com o id " + c.getCredentialId());
        }
        // 3. chave do titular: é o que dá a um DID o poder de assinar, por isso é a regra mais sensível
        if (carriesKey(type)) {
            checkSubjectKey(c, state);
        } else if (!c.getSubjectKey().isEmpty()) {
            // sem isto, um apicultor criava um "lote" com titular did:lab e a sua própria chave,
            // e passava a assinar em nome do Laboratório
            throw new RuleException("Uma credencial " + type + " não pode trazer chave pública");
        }
        if (type.equals("LOTE") && c.getWeightKg() <= 0) {
            throw new RuleException("Um lote tem de ter kg colhidos positivos");
        }
        // 4. a análise e o certificado referem-se a um lote que existe e não está revogado
        if (type.equals("ANALISE_LOTE") || type.equals("CERTIFICADO_ORIGEM")) {
            checkLot(c.getSubject(), state);
        }
    }

    /**
     * Tipos de credencial que registam a chave pública do titular na cadeia.
     *
     * @param type tipo da credencial
     * @return true para REGISTO_APICULTOR e REGISTO_LOJA
     */
    public static boolean carriesKey(String type) {
        return type.equals("REGISTO_APICULTOR") || type.equals("REGISTO_LOJA");
    }

    /**
     * Regras da chave num registo de apicultor ou de loja:
     * 1. tem de trazer a chave;
     * 2. o DID do titular tem de ser o derivado dessa chave (DigitalID.createDid):
     *    assim um DID nunca pode ficar com uma chave que não é a sua;
     * 3. o titular ainda não pode ter chave na cadeia: um DID só é registado uma
     *    vez (também evita ter REGISTO_APICULTOR e REGISTO_LOJA no mesmo DID).
     *    Consequência: um DID revogado não volta a ser registado; o interveniente
     *    cria chaves novas e recebe um DID novo.
     */
    private static void checkSubjectKey(IssueCredentialTx c, LedgerState state) throws RuleException {
        if (c.getSubjectKey().isEmpty()) {
            throw new RuleException("O registo de " + c.getSubject() + " tem de incluir a sua chave pública");
        }
        if (!c.getSubject().equals(DigitalID.createDid(c.getSubjectKey()))) {
            throw new RuleException("O DID " + c.getSubject() + " não corresponde à chave pública do registo");
        }
        if (state.publicKeyOf(c.getSubject()) != null) {
            throw new RuleException("O DID " + c.getSubject() + " já está registado na cadeia");
        }
    }

    /**
     * Revogação: só a DGAV; a credencial existe e ainda não foi revogada.
     */
    private static void checkRevoke(RevokeTx r, String role, LedgerState state) throws RuleException {
        requireRole(role, "revogar credenciais", LedgerState.DGAV);
        if (!state.credentialExists(r.getCredentialId())) {
            throw new RuleException("A credencial " + r.getCredentialId() + " não existe");
        }
        if (state.isRevoked(r.getCredentialId())) {
            throw new RuleException("A credencial " + r.getCredentialId() + " já está revogada");
        }
    }

    /**
     * Embalagem: só o apicultor dono do lote; o lote existe e não está revogado;
     * os números dos frascos não se sobrepõem aos já embalados; e o balanço de
     * massa não passa os kg colhidos mais 2%.
     */
    private static void checkPack(PackTx p, String role, LedgerState state) throws RuleException {
        requireRole(role, "embalar", LedgerState.APICULTOR);
        // 1. o lote existe, não está revogado e é deste apicultor
        IssueCredentialTx lot = checkLot(p.getLot(), state);
        if (!lot.getAuthor().equals(p.getAuthor())) {
            throw new RuleException("Só o apicultor dono de " + p.getLot() + " o pode embalar");
        }
        // 2. números de frascos repetidos dariam dois frascos com o mesmo QR
        for (PackTx other : state.getPacks(p.getLot())) {
            if (p.getFirstJar() <= other.getLastJar() && other.getFirstJar() <= p.getLastJar()) {
                throw new RuleException("Os frascos " + p.getFirstJar() + " a " + p.getLastJar()
                        + " sobrepõem-se aos já embalados (" + other.getFirstJar() + " a " + other.getLastJar() + ")");
            }
        }
        // 3. balanço de massa: embalado + novo <= colhido * (1 + 2%)
        LotBalance b = state.lotBalance(p.getLot());
        double limit = b.getHarvestedKg() * (1 + TOLERANCE);
        // o 1e-9 evita recusar por erros de arredondamento dos double (0.1 + 0.2 != 0.3)
        if (b.getPackedKg() + p.getTotalKg() > limit + 1e-9) {
            throw new RuleException(String.format(
                    "Balanço de massa: %s tem %.2f kg colhidos e %.2f kg já embalados. "
                    + "Embalar mais %.2f kg excede o lote; restam %.2f kg (com a tolerância de %.0f%%)",
                    p.getLot(), b.getHarvestedKg(), b.getPackedKg(), p.getTotalKg(),
                    Math.max(0, limit - b.getPackedKg()), TOLERANCE * 100));
        }
    }

    /**
     * Venda: só uma loja registada; o frasco existe. Se o frasco já foi vendido,
     * a venda não entra e devolve-se um alerta de fraude (QR copiado).
     */
    private static Transaction checkSale(SaleTx s, String role, LedgerState state, String proposer)
            throws RuleException {
        requireRole(role, "vender", LedgerState.LOJA);
        if (!state.jarExists(s.getJarId())) {
            throw new RuleException("O frasco " + s.getJarId() + " não existe");
        }
        if (state.isSold(s.getJarId())) {
            SaleTx first = state.getSale(s.getJarId());
            // por assinar: o consenso assina com a chave do proponente
            return new FraudAlertTx(proposer, s.getJarId(), "segunda venda (QR copiado): já vendido por "
                    + first.getShop() + ", nova tentativa de " + s.getShop());
        }
        return s;
    }

    /**
     * O lote existe, é do tipo LOTE e não está revogado.
     *
     * @return a credencial do lote
     */
    private static IssueCredentialTx checkLot(String lotId, LedgerState state) throws RuleException {
        IssueCredentialTx lot = state.getCredential(lotId);
        if (lot == null || !lot.getCredentialType().equals("LOTE")) {
            throw new RuleException("O lote " + lotId + " não existe");
        }
        if (state.isRevoked(lotId)) {
            throw new RuleException("O lote " + lotId + " foi revogado");
        }
        return lot;
    }
}
