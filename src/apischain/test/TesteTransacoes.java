//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.core.Block;
import apischain.core.MerkleTree;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Teste de consola das transações (issue #2): assinatura, verificação,
 * deteção de alterações, conversão para Base64 e entrada num bloco.
 */
public class TesteTransacoes {

    // número de verificações que falharam (no fim tem de ser 0)
    private static int falhas = 0;

    /**
     * Mostra o resultado de uma verificação e conta as falhas.
     */
    private static void check(String descricao, boolean ok) {
        System.out.println("   " + (ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }

    public static void main(String[] args) throws Exception {
        // ------------------------------------------------------------------
        System.out.println("1. CHAVES DOS INTERVENIENTES");
        KeyPair dgav = SecurityUtils.generateKeyPair();
        KeyPair lab = SecurityUtils.generateKeyPair();
        KeyPair dop = SecurityUtils.generateKeyPair();
        KeyPair apicultor = SecurityUtils.generateKeyPair();
        KeyPair loja = SecurityUtils.generateKeyPair();
        String hashFicticio = SecurityUtils.getHash("credencial completa guardada fora da cadeia");

        // ------------------------------------------------------------------
        System.out.println("\n2. UMA TRANSAÇÃO DE CADA TIPO, ASSINADA PELO AUTOR");
        GenesisTx genese = new GenesisTx("did:apis:dgav", Arrays.asList(
                new Authority("did:apis:dgav", "DGAV", SecurityUtils.keyToBase64(dgav.getPublic())),
                new Authority("did:apis:lab", "LAB", SecurityUtils.keyToBase64(lab.getPublic())),
                new Authority("did:apis:dop", "DOP", SecurityUtils.keyToBase64(dop.getPublic()))));
        genese.sign(dgav.getPrivate());

        IssueCredentialTx registo = new IssueCredentialTx("did:apis:dgav", "AP-0001", "REGISTO_APICULTOR",
                "did:apis:apicultor-0001", hashFicticio, SecurityUtils.keyToBase64(apicultor.getPublic()), 0);
        registo.sign(dgav.getPrivate());

        IssueCredentialTx lote = new IssueCredentialTx("did:apis:apicultor-0001", "LOTE-2026-0042", "LOTE",
                "did:apis:apiario-117", hashFicticio, "", 340);
        lote.sign(apicultor.getPrivate());

        IssueCredentialTx analise = new IssueCredentialTx("did:apis:lab", "AN-0042", "ANALISE_LOTE",
                "LOTE-2026-0042", hashFicticio);
        analise.sign(lab.getPrivate());

        PackTx embalagem = new PackTx("did:apis:apicultor-0001", "LOTE-2026-0042", 1, 200, 0.5);
        embalagem.sign(apicultor.getPrivate());

        SaleTx venda = new SaleTx("did:apis:loja-01", embalagem.getJarId(137));
        venda.sign(loja.getPrivate());

        RevokeTx revogacao = new RevokeTx("did:apis:dgav", "AP-0001", "apiário abandonado");
        revogacao.sign(dgav.getPrivate());

        FraudAlertTx alerta = new FraudAlertTx("did:apis:lab", embalagem.getJarId(137), "segunda venda: QR copiado");
        alerta.sign(lab.getPrivate());

        // cada transação com a chave pública de quem a assinou
        List<Transaction> txs = Arrays.asList(genese, registo, lote, analise, embalagem, venda, revogacao, alerta);
        KeyPair[] autores = {dgav, dgav, apicultor, lab, apicultor, loja, dgav, lab};
        for (Transaction tx : txs) {
            System.out.println("   " + tx);
        }

        // ------------------------------------------------------------------
        System.out.println("\n3. VERIFICAR AS ASSINATURAS");
        for (int i = 0; i < txs.size(); i++) {
            Transaction tx = txs.get(i);
            String nome = tx.getClass().getSimpleName();
            check(nome + " válida com a chave do autor",
                    tx.isSignatureValid(autores[i].getPublic()));
            check(nome + " inválida com a chave de outro interveniente",
                    !tx.isSignatureValid(dop.getPublic()));
        }
        // transação por assinar
        SaleTx porAssinar = new SaleTx("did:apis:loja-01", "LOTE-2026-0042-1");
        check("transação por assinar é inválida", !porAssinar.isSignatureValid(loja.getPublic()));

        // ------------------------------------------------------------------
        System.out.println("\n3b. CADA CAMPO ENTRA NO TEXTO ASSINADO");
        // Para cada campo: duas transações iguais exceto nesse campo têm de dar getContent()
        // diferente. Se uma subclasse se esquecer de um campo no getContent(), esse campo
        // podia ser alterado sem a assinatura falhar, e este teste apanha-o.
        // (Compara-se getContent() e não getDataToSign(), porque a data das duas transações
        // é sempre diferente e esconderia o esquecimento.)
        IssueCredentialTx base = new IssueCredentialTx("a", "C1", "LOTE", "S1", "H1", "K1", 10);
        campoAssinado("IssueCredentialTx.credentialId", base, new IssueCredentialTx("a", "C2", "LOTE", "S1", "H1", "K1", 10));
        campoAssinado("IssueCredentialTx.credentialType", base, new IssueCredentialTx("a", "C1", "ANALISE_LOTE", "S1", "H1", "K1", 10));
        campoAssinado("IssueCredentialTx.subject", base, new IssueCredentialTx("a", "C1", "LOTE", "S2", "H1", "K1", 10));
        campoAssinado("IssueCredentialTx.credentialHash", base, new IssueCredentialTx("a", "C1", "LOTE", "S1", "H2", "K1", 10));
        campoAssinado("IssueCredentialTx.subjectKey", base, new IssueCredentialTx("a", "C1", "LOTE", "S1", "H1", "K2", 10));
        campoAssinado("IssueCredentialTx.weightKg", base, new IssueCredentialTx("a", "C1", "LOTE", "S1", "H1", "K1", 11));
        RevokeTx rev = new RevokeTx("a", "C1", "M1");
        campoAssinado("RevokeTx.credentialId", rev, new RevokeTx("a", "C2", "M1"));
        campoAssinado("RevokeTx.reason", rev, new RevokeTx("a", "C1", "M2"));
        PackTx pack = new PackTx("a", "L1", 1, 10, 0.5);
        campoAssinado("PackTx.lot", pack, new PackTx("a", "L2", 1, 10, 0.5));
        campoAssinado("PackTx.firstJar", pack, new PackTx("a", "L1", 2, 10, 0.5));
        campoAssinado("PackTx.lastJar", pack, new PackTx("a", "L1", 1, 11, 0.5));
        campoAssinado("PackTx.weightKg", pack, new PackTx("a", "L1", 1, 10, 1.0));
        campoAssinado("SaleTx.jarId", new SaleTx("a", "J1"), new SaleTx("a", "J2"));
        FraudAlertTx fa = new FraudAlertTx("a", "J1", "M1");
        campoAssinado("FraudAlertTx.jarId", fa, new FraudAlertTx("a", "J2", "M1"));
        campoAssinado("FraudAlertTx.reason", fa, new FraudAlertTx("a", "J1", "M2"));
        Authority aut = new Authority("D1", "DGAV", "P1");
        GenesisTx gen1 = new GenesisTx("a", Arrays.asList(aut));
        campoAssinado("GenesisTx.authorities (DID)", gen1, new GenesisTx("a", Arrays.asList(new Authority("D2", "DGAV", "P1"))));
        campoAssinado("GenesisTx.authorities (papel)", gen1, new GenesisTx("a", Arrays.asList(new Authority("D1", "LAB", "P1"))));
        campoAssinado("GenesisTx.authorities (chave)", gen1, new GenesisTx("a", Arrays.asList(new Authority("D1", "DGAV", "P2"))));
        campoAssinado("GenesisTx.authorities (quantidade)", gen1, new GenesisTx("a", Arrays.asList(aut, aut)));
        // o autor e a data estão na classe base: alterar o autor muda o texto assinado
        check("Transaction.author entra no texto assinado",
                !new SaleTx("a", "J1").getDataToSign().equals(new SaleTx("b", "J1").getDataToSign()));
        // e a assinatura de uma transação não serve para outra com um campo diferente
        SaleTx vendaA = new SaleTx("did:apis:loja-01", "J1");
        vendaA.sign(loja.getPrivate());
        SaleTx vendaB = new SaleTx("did:apis:loja-01", "J2");
        check("assinatura da venda do frasco J1 não serve para o frasco J2",
                !SecurityUtils.verify(vendaB.getDataToSign(), vendaA.getSignature(), loja.getPublic()));

        // ------------------------------------------------------------------
        System.out.println("\n4. BASE64 E VOLTA (como fica guardada no bloco)");
        for (int i = 0; i < txs.size(); i++) {
            Transaction tx = txs.get(i);
            Transaction lida = Transaction.fromBase64(tx.toBase64());
            check(tx.getClass().getSimpleName() + " igual depois de ler e assinatura válida",
                    lida.getClass() == tx.getClass()
                    && lida.getDataToSign().equals(tx.getDataToSign())
                    && lida.isSignatureValid(autores[i].getPublic()));
        }
        // campos próprios da subclasse sobrevivem à conversão
        PackTx emb = (PackTx) Transaction.fromBase64(embalagem.toBase64());
        check("PackTx: 200 frascos de 0.5 kg = 100 kg", emb.getJarCount() == 200 && emb.getTotalKg() == 100.0);
        check("PackTx: frasco 137 pertence à embalagem", emb.containsJar(137) && !emb.containsJar(201));
        GenesisTx gen = (GenesisTx) Transaction.fromBase64(genese.toBase64());
        check("GenesisTx: 3 autoridades com chave pública",
                gen.getAuthorities().size() == 3
                && SecurityUtils.publicKeyFromBase64(gen.getAuthorities().get(1).getPublicKey()).equals(lab.getPublic()));

        // ------------------------------------------------------------------
        System.out.println("\n5. CAMPOS INVÁLIDOS SÃO RECUSADOS AO CRIAR");
        check("PackTx com último frasco antes do primeiro", lancaExcecao(() -> new PackTx("a", "L", 10, 5, 1)));
        check("PackTx com peso zero", lancaExcecao(() -> new PackTx("a", "L", 1, 5, 0)));
        check("GenesisTx sem autoridades", lancaExcecao(() -> new GenesisTx("a", new ArrayList<>())));
        // o separador | não pode aparecer nos campos: senão estas duas revogações diferentes
        // davam o mesmo texto assinado ("credencial=A|motivo=x|motivo=y")
        check("RevokeTx com | na credencial", lancaExcecao(() -> new RevokeTx("a", "A|motivo=x", "y")));
        check("RevokeTx com | no motivo", lancaExcecao(() -> new RevokeTx("a", "A", "x|motivo=y")));
        check("autor com |", lancaExcecao(() -> new SaleTx("a|b", "J1")));
        check("campo null", lancaExcecao(() -> new FraudAlertTx("a", null, "m")));
        check("autoridade com ; no DID", lancaExcecao(() -> new Authority("D1;D2", "DGAV", "P1")));

        // ------------------------------------------------------------------
        System.out.println("\n6. TRANSAÇÕES DENTRO DE UM BLOCO E PROVA DE MERKLE");
        List<String> emBase64 = new ArrayList<>();
        for (Transaction tx : txs) {
            emBase64.add(tx.toBase64());
        }
        Block bloco = new Block(1, "hash-anterior", emBase64, "did:apis:dgav");
        check("bloco íntegro", bloco.isValid());
        String vendaB64 = emBase64.get(5);
        List<String> prova = bloco.getMerkleTree().getProof(vendaB64);
        // a prova tem de levar à raiz GUARDADA NO BLOCO, e não só à raiz que vem dentro da prova
        check("prova de Merkle da venda é válida e acaba na raiz do bloco",
                MerkleTree.isProofValid(vendaB64, prova)
                && prova.get(prova.size() - 1).equals(bloco.getMerkleRoot()));
        check("a mesma prova não serve para outra transação",
                !MerkleTree.isProofValid(emBase64.get(6), prova));

        // ------------------------------------------------------------------
        System.out.println();
        if (falhas == 0) {
            System.out.println("TODAS AS VERIFICAÇÕES PASSARAM");
        } else {
            System.out.println(falhas + " VERIFICAÇÕES FALHARAM");
            System.exit(1);
        }
    }

    /**
     * Verifica que duas transações que só diferem num campo têm conteúdo assinado diferente.
     */
    private static void campoAssinado(String campo, Transaction a, Transaction b) {
        check(campo + " entra no texto assinado", !a.getContent().equals(b.getContent()));
    }

    /**
     * Interface para passar um bloco de código que pode lançar exceção.
     */
    private interface Codigo {
        void run() throws Exception;
    }

    /**
     * @return true se o código lançar uma exceção
     */
    private static boolean lancaExcecao(Codigo codigo) {
        try {
            codigo.run();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
