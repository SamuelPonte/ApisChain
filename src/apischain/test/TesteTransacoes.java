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

        SaleTx venda = new SaleTx("did:apis:loja-01", embalagem.getJarId(137), "did:apis:loja-01");
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
            // alterar o último campo da subclasse depois de assinar (por exemplo "kg=340.0" passa
            // a "kg=340.09"): o texto assinado muda e a assinatura deixa de servir
            String alterado = tx.getDataToSign() + "9";
            check(nome + " alterada depois de assinar é recusada",
                    !SecurityUtils.verify(alterado, tx.getSignature(), autores[i].getPublic()));
        }
        // transação por assinar
        SaleTx porAssinar = new SaleTx("did:apis:loja-01", "LOTE-2026-0042-1", "did:apis:loja-01");
        check("transação por assinar é inválida", !porAssinar.isSignatureValid(loja.getPublic()));

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
