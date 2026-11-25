//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.consensus.LocalNetwork;
import apischain.consensus.Node;
import apischain.consensus.ProofOfAuthority;
import apischain.core.Block;
import apischain.core.Vote;
import apischain.identity.DigitalID;
import apischain.ledger.RuleException;
import apischain.transactions.Authority;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.GenesisTx;
import apischain.transactions.IssueCredentialTx;
import apischain.transactions.PackTx;
import apischain.transactions.SaleTx;
import apischain.transactions.Transaction;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Teste de consola do consenso (issue #4): Proof of Authority com votação
 * entre DGAV, Laboratório e DOP, com quatro nós na rede local.
 *
 * Usa uma pasta temporária, para não mexer na pasta data/ do projeto.
 */
public class TesteConsenso {

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

    /**
     * DID derivado da chave pública (como nas identidades da issue #1).
     */
    private static String did(KeyPair k) {
        return DigitalID.createDid(SecurityUtils.keyToBase64(k.getPublic()));
    }

    public static void main(String[] args) throws Exception {
        String pasta = Files.createTempDirectory("apischain-nos").toString();

        // ------------------------------------------------------------------
        System.out.println("1. QUATRO NÓS E O BLOCO DE GÉNESE");
        KeyPair dgav = SecurityUtils.generateKeyPair();
        KeyPair lab = SecurityUtils.generateKeyPair();
        KeyPair dop = SecurityUtils.generateKeyPair();
        KeyPair apicultor = SecurityUtils.generateKeyPair();
        KeyPair loja = SecurityUtils.generateKeyPair();
        Node n1 = new Node("N1", did(dgav), dgav.getPrivate(), pasta + "/N1");
        Node n2 = new Node("N2", did(lab), lab.getPrivate(), pasta + "/N2");
        Node n3 = new Node("N3", did(dop), dop.getPrivate(), pasta + "/N3");
        Node n4 = new Node("N4", null, null, pasta + "/N4");
        LocalNetwork rede = new LocalNetwork();
        for (Node n : Arrays.asList(n1, n2, n3, n4)) {
            rede.addNode(n);
        }
        List<Authority> autoridades = Arrays.asList(
                new Authority(did(dgav), "DGAV", SecurityUtils.keyToBase64(dgav.getPublic())),
                new Authority(did(lab), "LAB", SecurityUtils.keyToBase64(lab.getPublic())),
                new Authority(did(dop), "DOP", SecurityUtils.keyToBase64(dop.getPublic())));
        GenesisTx genese = new GenesisTx(did(dgav), autoridades);
        genese.sign(dgav.getPrivate());
        Block b0 = rede.start(genese);
        check("a génese entra nos 4 nós", tamanhos(rede, 1));
        check("a génese tem a assinatura do proponente e 2 votos (3 assinaturas)",
                ProofOfAuthority.countSignatures(b0, autoridades) == 3);
        check("quórum com 3 autoridades é 2", ProofOfAuthority.quorum(3) == 2);

        // ------------------------------------------------------------------
        System.out.println("\n2. ROTAÇÃO DO PROPONENTE");
        IssueCredentialTx regApicultor = new IssueCredentialTx(did(dgav), "AP-0001", "REGISTO_APICULTOR",
                did(apicultor), "hash-ap", SecurityUtils.keyToBase64(apicultor.getPublic()), 0);
        regApicultor.sign(dgav.getPrivate());
        Block b1 = rede.submit(regApicultor);
        IssueCredentialTx regLoja = new IssueCredentialTx(did(dgav), "LJ-0001", "REGISTO_LOJA",
                did(loja), "hash-lj", SecurityUtils.keyToBase64(loja.getPublic()), 0);
        regLoja.sign(dgav.getPrivate());
        // o apiário vai no mesmo bloco (as regras exigem que o lote saia de um apiário registado)
        IssueCredentialTx apiario = new IssueCredentialTx(did(dgav), "APIARIO-117", "REGISTO_APIARIO",
                did(apicultor), "hash-apiario");
        apiario.sign(dgav.getPrivate());
        Block b2 = rede.submit(Arrays.asList(regLoja, apiario));
        IssueCredentialTx lote = new IssueCredentialTx(did(apicultor), "LOTE-2026-0042", "LOTE",
                "APIARIO-117", "hash-lote", "", 100);
        lote.sign(apicultor.getPrivate());
        Block b3 = rede.submit(lote);
        System.out.println("   proponentes: #0 " + b0.getValidator() + ", #1 " + b1.getValidator()
                + ", #2 " + b2.getValidator() + ", #3 " + b3.getValidator());
        check("os blocos 0, 1 e 2 são propostos por DGAV, LAB e DOP; o 3 volta à DGAV",
                b0.getValidator().equals(did(dgav)) && b1.getValidator().equals(did(lab))
                && b2.getValidator().equals(did(dop)) && b3.getValidator().equals(did(dgav)));
        check("os 4 nós têm os 4 blocos e a mesma cadeia", tamanhos(rede, 4) && iguais(rede));

        // ------------------------------------------------------------------
        System.out.println("\n3. O NÓ N4 NÃO VALIDA");
        boolean n4Votou = false;
        for (Block b : n1.getChain().getChain()) {
            for (Vote v : b.getVotes()) {
                n4Votou |= (v.getValidator() == null);
            }
        }
        check("nenhum voto vem do N4", !n4Votou);
        check("N4 recusa votar", n4.validate(LocalNetwork.copy(b3)) == null);
        check("N4 não pode propor blocos", lancaExcecao(() -> n4.propose(Collections.singletonList(lote))));

        // ------------------------------------------------------------------
        System.out.println("\n4. AUTORIDADES DESLIGADAS");
        rede.setOnline(n3, false);
        PackTx emb = new PackTx(did(apicultor), "LOTE-2026-0042", 1, 100, 0.5);
        emb.sign(apicultor.getPrivate());
        Block b4 = rede.submit(emb);
        check("com o DOP desligado o bloco entra (2 assinaturas em 3)",
                ProofOfAuthority.countSignatures(b4, autoridades) == 2 && n1.getChain().getSize() == 5);
        check("o DOP desligado ficou para trás", n3.getChain().getSize() == 4);
        rede.setOnline(n2, false);
        SaleTx venda = new SaleTx(did(loja), "LOTE-2026-0042-7");
        venda.sign(loja.getPrivate());
        check("com LAB e DOP desligados não há quórum", lancaExcecao(() -> rede.submit(venda)));
        check("e o bloco não entrou em nenhum nó", n1.getChain().getSize() == 5 && n4.getChain().getSize() == 5);

        // ------------------------------------------------------------------
        System.out.println("\n5. SINCRONIZAR AO VOLTAR A LIGAR");
        rede.setOnline(n2, true);
        rede.setOnline(n3, true);
        check("o LAB e o DOP ficam com a mesma cadeia dos outros", tamanhos(rede, 5) && iguais(rede));

        // ------------------------------------------------------------------
        System.out.println("\n6. REGRAS DOS VALIDADORES DENTRO DO CONSENSO");
        Block b5 = rede.submit(venda);
        check("primeira venda do frasco 7 entra", tamanhos(rede, 6));
        SaleTx copiaQR = new SaleTx(did(loja), "LOTE-2026-0042-7");
        copiaQR.sign(loja.getPrivate());
        Block b6 = rede.submit(copiaQR);
        Transaction t6 = Transaction.fromBase64(b6.getTransactions().get(0));
        check("segunda venda: entra um FraudAlertTx assinado pelo proponente do bloco",
                t6 instanceof FraudAlertTx && t6.getAuthor().equals(b6.getValidator())
                && t6.isSignatureValid(SecurityUtils.publicKeyFromBase64(autoridade(autoridades, b6.getValidator()))));
        PackTx demais = new PackTx(did(apicultor), "LOTE-2026-0042", 101, 300, 0.5);
        demais.sign(apicultor.getPrivate());
        try {
            rede.submit(demais);
            check("embalar acima do balanço de massa é rejeitado", false);
        } catch (RuleException e) {
            System.out.println("   mensagem: " + e.getMessage());
            check("embalar acima do balanço de massa é rejeitado e nada entra", tamanhos(rede, 7));
        }
        KeyPair estranho = SecurityUtils.generateKeyPair();
        SaleTx vendaEstranho = new SaleTx(did(estranho), "LOTE-2026-0042-8");
        vendaEstranho.sign(estranho.getPrivate());
        check("transação de um autor desconhecido é rejeitada",
                lancaExcecao(() -> rede.submit(vendaEstranho)) && tamanhos(rede, 7));

        // ------------------------------------------------------------------
        System.out.println("\n7. BLOCOS FORJADOS");
        SaleTx venda9 = new SaleTx(did(loja), "LOTE-2026-0042-9");
        venda9.sign(loja.getPrivate());
        List<String> txs9 = Collections.singletonList(venda9.toBase64());
        // a) proposto por quem não é autoridade, com votos verdadeiros de DGAV e LAB
        Block falso = new Block(7, n1.getChain().getLastBlockHash(), txs9, did(estranho));
        falso.setSignature(SecurityUtils.sign(falso.getCurrentHash(), estranho.getPrivate()));
        falso.addVote(new Vote(did(dgav), SecurityUtils.sign(falso.getCurrentHash(), dgav.getPrivate())));
        falso.addVote(new Vote(did(lab), SecurityUtils.sign(falso.getCurrentHash(), lab.getPrivate())));
        check("bloco proposto por quem não é autoridade é recusado", lancaExcecao(() -> n1.commit(falso)));
        // b) proposto pela DGAV com um voto do LAB forjado (assinado por outra chave)
        Block votoFalso = n1.propose(Collections.singletonList(venda9));
        votoFalso.addVote(new Vote(did(lab), SecurityUtils.sign(votoFalso.getCurrentHash(), estranho.getPrivate())));
        check("bloco com um voto forjado não tem quórum e é recusado", lancaExcecao(() -> n1.commit(votoFalso)));
        // c) a DGAV vota duas vezes no próprio bloco: conta só uma assinatura
        Block repetido = n1.propose(Collections.singletonList(venda9));
        Vote proprio = new Vote(did(dgav), SecurityUtils.sign(repetido.getCurrentHash(), dgav.getPrivate()));
        repetido.addVote(proprio);
        repetido.addVote(proprio);
        check("votos repetidos da mesma autoridade contam uma vez e o bloco é recusado",
                ProofOfAuthority.countSignatures(repetido, autoridades) == 1 && lancaExcecao(() -> n1.commit(repetido)));
        check("nenhum bloco forjado entrou", tamanhos(rede, 7));

        // ------------------------------------------------------------------
        System.out.println("\n8. CÓPIA EM FICHEIRO DE CADA NÓ");
        Node n1Outra = new Node("N1", did(dgav), dgav.getPrivate(), pasta + "/N1");
        check("um N1 criado de novo carrega os 7 blocos do ficheiro e a cadeia está íntegra",
                n1Outra.getChain().getSize() == 7 && n1Outra.getChain().isValid());
        check("cada nó tem o seu ficheiro", new File(pasta + "/N4/blockchain.bc").exists());

        // ------------------------------------------------------------------
        System.out.println("\n9. CADA NÓ TEM A SUA CÓPIA");
        n4.getChain().get(5).tamperTransaction(0, "venda falsificada");
        check("adulterar a cadeia do N4 não muda a do N1",
                !n4.getChain().isValid() && n1.getChain().isValid());

        // ------------------------------------------------------------------
        System.out.println("\n10. PROPONENTE COM A CÓPIA DA CADEIA CORROMPIDA");
        // duas vendas para a cadeia ficar com 9 blocos: o bloco #9 é da vez do N1 (9 % 3 = 0)
        for (int frasco = 20; frasco <= 21; frasco++) {
            SaleTx v = new SaleTx(did(loja), "LOTE-2026-0042-" + frasco);
            v.sign(loja.getPrivate());
            rede.submit(v);
        }
        // um atacante adultera a cópia do N1: o N1 deixa de conseguir propor e votar
        n1.getChain().get(3).tamperTransaction(0, "lote falsificado");
        int aceites = 0;
        for (int frasco = 22; frasco <= 24; frasco++) {
            SaleTx v = new SaleTx(did(loja), "LOTE-2026-0042-" + frasco);
            v.sign(loja.getPrivate());
            try {
                rede.submit(v);
                aceites++;
            } catch (Exception e) {
                System.out.println("   recusada: " + e.getMessage());
            }
        }
        check("com o N1 corrompido, as 3 vendas seguintes entram (LAB e DOP chegam para o quórum)",
                aceites == 3 && n2.getChain().getSize() == 12 && n3.getChain().getSize() == 12);
        check("o bloco #9, que era da vez do N1, foi proposto pelo LAB",
                n2.getChain().getSize() > 9 && n2.getChain().get(9).getValidator().equals(did(lab))
                && String.join("\n", rede.getLog()).contains("N1 com a cópia da cadeia atrasada ou corrompida"));
        check("o N1 corrompido ficou parado nos 9 blocos", n1.getChain().getSize() == 9);

        System.out.println("\nREGISTO DA REDE (últimas linhas)");
        List<String> log = rede.getLog();
        for (String linha : log.subList(Math.max(0, log.size() - 6), log.size())) {
            System.out.println("   " + linha);
        }

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
     * @return true se todos os nós tiverem este número de blocos
     */
    private static boolean tamanhos(LocalNetwork rede, int n) {
        for (Node node : rede.getNodes()) {
            if (node.getChain().getSize() != n) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true se todos os nós tiverem o mesmo último hash e a cadeia íntegra
     */
    private static boolean iguais(LocalNetwork rede) {
        String ultimo = rede.getNodes().get(0).getChain().getLastBlockHash();
        for (Node node : rede.getNodes()) {
            if (!node.getChain().getLastBlockHash().equals(ultimo) || !node.getChain().isValid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return chave pública (Base64) da autoridade com este DID
     */
    private static String autoridade(List<Authority> autoridades, String did) {
        for (Authority a : autoridades) {
            if (a.getDid().equals(did)) {
                return a.getPublicKey();
            }
        }
        return null;
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
