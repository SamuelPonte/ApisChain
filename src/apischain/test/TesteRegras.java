//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.identity.DigitalID;
import apischain.ledger.LedgerState;
import apischain.ledger.RuleException;
import apischain.ledger.Rules;
import apischain.transactions.Authority;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import apischain.transactions.RevokeTx;
import apischain.transactions.SaleTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.security.KeyPair;
import java.util.Arrays;

/**
 * Teste de consola do estado e das regras (issue #3): um caso bom e um caso
 * mau por regra. A cadeia é construída à mão com BlockChain.add (sem consenso):
 * cada transação aceite entra num bloco seu.
 */
public class TesteRegras {

    // DIDs das autoridades (vêm da génese, podem ter qualquer nome)
    static final String DGAV = "did:apis:dgav";
    static final String LAB = "did:apis:lab";
    static final String DOP = "did:apis:dop";
    // DIDs dos registados pela DGAV: derivados da chave pública (DigitalID.createDid),
    // por isso só se calculam depois de gerar as chaves
    static String MANUEL;   // apicultor
    static String ROSA;     // outra apicultora
    static String LOJA;     // loja registada
    static String INTRUSA;  // loja que nunca foi registada
    static final String LOTE = "LOTE-2026-0042";

    static BlockChain chain = new BlockChain();
    static LedgerState state = new LedgerState();
    static KeyPair dgav, lab, dop, manuel, rosa, loja, intrusa;
    static int falhas = 0;

    public static void main(String[] args) throws Exception {
        // ------------------------------------------------------------------
        System.out.println("1. CHAVES E GÉNESE");
        dgav = SecurityUtils.generateKeyPair();
        lab = SecurityUtils.generateKeyPair();
        dop = SecurityUtils.generateKeyPair();
        manuel = SecurityUtils.generateKeyPair();
        rosa = SecurityUtils.generateKeyPair();
        loja = SecurityUtils.generateKeyPair();
        intrusa = SecurityUtils.generateKeyPair();
        MANUEL = did(manuel);
        ROSA = did(rosa);
        LOJA = did(loja);
        INTRUSA = did(intrusa);

        GenesisTx genese = new GenesisTx(DGAV, Arrays.asList(
                new Authority(DGAV, "DGAV", SecurityUtils.keyToBase64(dgav.getPublic())),
                new Authority(LAB, "LAB", SecurityUtils.keyToBase64(lab.getPublic())),
                new Authority(DOP, "DOP", SecurityUtils.keyToBase64(dop.getPublic()))));
        // génese assinada por quem não é autoridade é recusada
        GenesisTx geneseFalsa = new GenesisTx(INTRUSA, genese.getAuthorities());
        rejeita("génese assinada por quem não é autoridade", geneseFalsa, intrusa);
        aceita("génese assinada pela DGAV", genese, dgav);
        rejeita("segunda génese", new GenesisTx(DGAV, genese.getAuthorities()), dgav);
        check("papéis das autoridades vêm da génese",
                "DGAV".equals(state.roleOf(DGAV)) && "LAB".equals(state.roleOf(LAB)) && "DOP".equals(state.roleOf(DOP)));

        // ------------------------------------------------------------------
        System.out.println("\n2. ASSINATURA COM A CHAVE QUE ESTÁ NA CADEIA");
        rejeita("autor desconhecido (loja nunca registada)", new SaleTx(INTRUSA, LOTE + "-1"), intrusa);
        // a DGAV "assina" com uma chave que não é a sua
        rejeita("assinatura com a chave errada", registo("AP-0001", "REGISTO_APICULTOR", MANUEL, manuel), intrusa);

        // ------------------------------------------------------------------
        System.out.println("\n3. REGISTOS (SÓ A DGAV)");
        rejeita("Laboratório a registar um apicultor",
                new IssueCredentialTx(LAB, "AP-0001", "REGISTO_APICULTOR", MANUEL, hash("AP-0001"),
                        SecurityUtils.keyToBase64(manuel.getPublic()), 0), lab);
        rejeita("registo de apicultor sem chave pública",
                new IssueCredentialTx(DGAV, "AP-0001", "REGISTO_APICULTOR", MANUEL, hash("x")), dgav);
        aceita("DGAV regista o apicultor Manuel", registo("AP-0001", "REGISTO_APICULTOR", MANUEL, manuel), dgav);
        rejeita("credencial com id repetido", registo("AP-0001", "REGISTO_APICULTOR", ROSA, rosa), dgav);
        aceita("DGAV regista a apicultora Rosa", registo("AP-0002", "REGISTO_APICULTOR", ROSA, rosa), dgav);
        aceita("DGAV regista o apiário 117", new IssueCredentialTx(DGAV, "APIARIO-117", "REGISTO_APIARIO", MANUEL, hash("apiário")), dgav);
        aceita("DGAV regista a loja", registo("LJ-01", "REGISTO_LOJA", LOJA, loja), dgav);
        rejeita("tipo de credencial desconhecido",
                new IssueCredentialTx(DGAV, "X-1", "DIPLOMA", MANUEL, hash("x")), dgav);
        rejeita("registo com um DID que não é o da chave",
                new IssueCredentialTx(DGAV, "AP-0003", "REGISTO_APICULTOR", "did:apis:manuel",
                        hash("AP-0003"), SecurityUtils.keyToBase64(intrusa.getPublic()), 0), dgav);
        rejeita("segundo registo do mesmo DID (como loja)", registo("LJ-02", "REGISTO_LOJA", MANUEL, manuel), dgav);
        check("papel do Manuel é APICULTOR e a chave dele está na cadeia",
                "APICULTOR".equals(state.roleOf(MANUEL)) && manuel.getPublic().equals(state.publicKeyOf(MANUEL)));
        check("papel da loja é LOJA", "LOJA".equals(state.roleOf(LOJA)));

        // ------------------------------------------------------------------
        System.out.println("\n4. LOTE, ANÁLISE E CERTIFICADO");
        rejeita("loja a criar um lote", lote(LOTE, 340, LOJA), loja);
        rejeita("lote com 0 kg", lote(LOTE, 0, MANUEL), manuel);
        // ataque da revisão do PR #17: um "lote" cujo titular é o Laboratório, com a chave do atacante
        rejeita("lote com chave pública (tentar trocar a chave do Laboratório)",
                new IssueCredentialTx(MANUEL, "LOTE-X", "LOTE", LAB, hash("x"),
                        SecurityUtils.keyToBase64(intrusa.getPublic()), 100), manuel);
        check("a chave do Laboratório continua a ser a da génese", lab.getPublic().equals(state.publicKeyOf(LAB)));
        aceita("Manuel cria o lote com 340 kg", lote(LOTE, 340, MANUEL), manuel);
        rejeita("análise de um lote que não existe",
                new IssueCredentialTx(LAB, "AN-9999", "ANALISE_LOTE", "LOTE-9999", hash("an")), lab);
        rejeita("DOP a emitir uma análise",
                new IssueCredentialTx(DOP, "AN-0042", "ANALISE_LOTE", LOTE, hash("an")), dop);
        aceita("Laboratório emite a análise",
                new IssueCredentialTx(LAB, "AN-0042", "ANALISE_LOTE", LOTE, hash("an")), lab);
        aceita("DOP emite o certificado de origem",
                new IssueCredentialTx(DOP, "DOP-0042", "CERTIFICADO_ORIGEM", LOTE, hash("dop")), dop);

        // ------------------------------------------------------------------
        System.out.println("\n5. EMBALAGEM E BALANÇO DE MASSA (340 kg, tolerância 2% = 346,8 kg)");
        rejeita("loja a embalar", new PackTx(LOJA, LOTE, 1, 10, 1), loja);
        rejeita("Rosa a embalar o lote do Manuel", new PackTx(ROSA, LOTE, 1, 10, 1), rosa);
        rejeita("embalar um lote que não existe", new PackTx(MANUEL, "LOTE-9999", 1, 10, 1), manuel);
        aceita("frascos 1 a 200 de 1 kg (200 kg)", new PackTx(MANUEL, LOTE, 1, 200, 1), manuel);
        aceita("frascos 201 a 450 de 0,5 kg (325 kg no total)", new PackTx(MANUEL, LOTE, 201, 450, 0.5), manuel);
        rejeita("frascos 100 a 110 de novo (números repetidos)", new PackTx(MANUEL, LOTE, 100, 110, 0.1), manuel);
        String msg = rejeita("frascos 451 a 480 de 1 kg (355 kg: passa o limite)", new PackTx(MANUEL, LOTE, 451, 480, 1), manuel);
        // 346,8 - 325 = 21,8 kg (a vírgula ou o ponto dependem da língua do computador)
        check("a mensagem diz quantos kg restam (21,80)", msg.contains("restam 21,80") || msg.contains("restam 21.80"));
        aceita("frascos 451 a 471 de 1 kg (346 kg: acima de 340, dentro dos 2%)", new PackTx(MANUEL, LOTE, 451, 471, 1), manuel);
        check("balanço do lote: 346 kg embalados", Math.abs(state.lotBalance(LOTE).getPackedKg() - 346) < 1e-9);
        System.out.println("   " + state.lotBalance(LOTE));

        // ------------------------------------------------------------------
        System.out.println("\n6. VENDA E VENDA DUPLA");
        rejeita("venda de um frasco que não existe", new SaleTx(LOJA, LOTE + "-999"), loja);
        rejeita("apicultor a vender", new SaleTx(MANUEL, LOTE + "-137"), manuel);
        aceita("loja vende o frasco 137", new SaleTx(LOJA, LOTE + "-137"), loja);
        // ataque da revisão do PR #17: o mesmo frasco 137 escrito de outra forma
        rejeita("venda do frasco 137 escrito com zero à esquerda (-0137)", new SaleTx(LOJA, LOTE + "-0137"), loja);
        rejeita("venda do frasco 137 escrito com sinal (-+137)", new SaleTx(LOJA, LOTE + "-+137"), loja);
        check("frasco 137 vendido", state.isSold(LOTE + "-137") && !state.isSold(LOTE + "-138"));
        Transaction r = submeter(new SaleTx(LOJA, LOTE + "-137"), loja);
        check("segunda venda do 137 não entra e gera um alerta de fraude",
                r instanceof FraudAlertTx && state.getAlerts(LOTE + "-137").size() == 1);
        System.out.println("   " + r);
        check("o alerta é assinado pelo proponente (DGAV)",
                r.getAuthor().equals(DGAV) && r.isSignatureValid(dgav.getPublic()));

        // ------------------------------------------------------------------
        System.out.println("\n7. REVOGAÇÃO");
        rejeita("Laboratório a revogar", new RevokeTx(LAB, "LJ-01", "teste"), lab);
        rejeita("revogar uma credencial que não existe", new RevokeTx(DGAV, "LJ-99", "teste"), dgav);
        aceita("DGAV revoga o registo da loja", new RevokeTx(DGAV, "LJ-01", "licença suspensa"), dgav);
        check("isRevoked da loja e a loja deixa de ter papel",
                state.isRevoked("LJ-01") && state.roleOf(LOJA) == null);
        rejeita("revogar outra vez", new RevokeTx(DGAV, "LJ-01", "teste"), dgav);
        rejeita("loja revogada a vender", new SaleTx(LOJA, LOTE + "-138"), loja);
        aceita("DGAV revoga o lote", new RevokeTx(DGAV, LOTE, "adulteração detetada"), dgav);
        rejeita("embalar um lote revogado", new PackTx(MANUEL, LOTE, 472, 472, 0.1), manuel);

        // ------------------------------------------------------------------
        System.out.println("\n8. O ESTADO CALCULADO DE NOVO A PARTIR DA CADEIA É O MESMO");
        LedgerState relido = new LedgerState(chain);
        check(chain.getSize() + " blocos, cadeia íntegra", chain.isValid());
        check("mesmo balanço, vendas, alertas e revogações",
                Math.abs(relido.lotBalance(LOTE).getPackedKg() - 346) < 1e-9
                && relido.isSold(LOTE + "-137") && relido.getAlerts(LOTE + "-137").size() == 1
                && relido.isRevoked("LJ-01") && relido.isRevoked(LOTE) && relido.roleOf(LOJA) == null
                && "APICULTOR".equals(relido.roleOf(MANUEL)));

        // ------------------------------------------------------------------
        System.out.println();
        if (falhas == 0) {
            System.out.println("TODAS AS VERIFICAÇÕES PASSARAM");
        } else {
            System.out.println(falhas + " VERIFICAÇÕES FALHARAM");
            System.exit(1);
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  AUXILIARES                                                         ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Assina a transação, passa-a pelas regras (a DGAV é a proponente) e, se for
     * aceite, põe-na num bloco novo. Se as regras devolverem um alerta, é o
     * alerta que entra, assinado pela DGAV.
     */
    static Transaction submeter(Transaction tx, KeyPair assinante) throws Exception {
        tx.sign(assinante.getPrivate());
        Transaction result = Rules.check(tx, state, DGAV);
        if (result != tx) {
            result.sign(dgav.getPrivate());  // o proponente assina o alerta
        }
        Block b = new Block(chain.getSize(), chain.getLastBlockHash(),
                Arrays.asList(result.toBase64()), DGAV);
        chain.add(b);
        state.apply(result);
        return result;
    }

    static void aceita(String descricao, Transaction tx, KeyPair assinante) {
        try {
            check("aceite: " + descricao, submeter(tx, assinante) == tx);
        } catch (Exception e) {
            check("aceite: " + descricao + " (recusada: " + e.getMessage() + ")", false);
        }
    }

    /**
     * @return a mensagem de erro (para se poder verificar o texto)
     */
    static String rejeita(String descricao, Transaction tx, KeyPair assinante) {
        int antes = chain.getSize();
        try {
            submeter(tx, assinante);
            check("rejeitada: " + descricao + " (FOI ACEITE)", false);
            return "";
        } catch (RuleException e) {
            check("rejeitada: " + descricao + "\n             -> " + e.getMessage(), chain.getSize() == antes);
            return e.getMessage();
        } catch (Exception e) {
            check("rejeitada: " + descricao + " (erro inesperado: " + e + ")", false);
            return "";
        }
    }

    static String did(KeyPair chaves) {
        return DigitalID.createDid(SecurityUtils.keyToBase64(chaves.getPublic()));
    }

    static IssueCredentialTx registo(String id, String tipo, String did, KeyPair chaves) {
        return new IssueCredentialTx(DGAV, id, tipo, did, hash(id), SecurityUtils.keyToBase64(chaves.getPublic()), 0);
    }

    static IssueCredentialTx lote(String id, double kg, String autor) {
        return new IssueCredentialTx(autor, id, "LOTE", "APIARIO-117", hash(id), "", kg);
    }

    static String hash(String txt) {
        return SecurityUtils.getHash("credencial fora da cadeia: " + txt);
    }

    static void check(String descricao, boolean ok) {
        System.out.println("   " + (ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }
}
