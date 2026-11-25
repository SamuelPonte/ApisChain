//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.consensus.Node;
import apischain.core.Block;
import apischain.core.MerkleTree;
import apischain.ledger.RuleException;
import apischain.service.ApisChainService;
import apischain.service.DemoData;
import apischain.service.VerificationResult;
import apischain.service.VerificationResult.Level;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.Transaction;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Teste de consola ponta a ponta (issue #5): o cenário base da demonstração e
 * as três fraudes, através do ApisChainService, como fará a interface.
 *
 * Usa a pasta data/ do projeto (está no .gitignore). Pode correr-se várias
 * vezes seguidas: da segunda em diante, carrega as chaves, as cadeias e o
 * armazém fora da cadeia dos ficheiros. Para recomeçar do zero, apagar data/.
 * Recebe outra pasta como argumento, se for preciso.
 */
public class TesteDemo {

    static int falhas = 0;

    public static void main(String[] args) throws Exception {
        String pasta = (args.length > 0) ? args[0] : "data";

        // ------------------------------------------------------------------
        System.out.println("1. PREPARAR A DEMONSTRAÇÃO (pasta " + pasta + ")");
        DemoData demo = DemoData.load(pasta);
        ApisChainService s = demo.getService();
        String lote = demo.getBaseLot();
        int blocos = s.getChain().getSize();
        System.out.println("   cadeia com " + blocos + " blocos, lote base " + lote);
        check("o cenário base existe (lote, 450 frascos)",
                lote != null && s.getState().jarExists(lote + "-450") && !s.getState().jarExists(lote + "-451"));

        // abrir a demonstração outra vez na mesma pasta: tem de carregar, não criar de novo
        DemoData outra = DemoData.load(pasta);
        check("abrir outra vez carrega dos ficheiros (mesmo número de blocos e mesmo lote)",
                outra.getService().getChain().getSize() == blocos && lote.equals(outra.getBaseLot()));

        // ------------------------------------------------------------------
        System.out.println("\n2. CENÁRIO BASE: O CONSUMIDOR LÊ O QR DE UM FRASCO BOM");
        VerificationResult r = s.verifyJar(lote + "-138");
        System.out.println(r);
        check("frasco 138 (bom, por vender): OK", r.getLevel() == Level.OK);
        VerificationResult vendido = s.verifyJar(lote + "-137");
        check("frasco 137 (vendido): não é FALHA por ter sido vendido",
                vendido.getLevel() == Level.OK || !s.getState().getAlerts(lote + "-137").isEmpty());
        VerificationResult falso = s.verifyJar(lote + "-9999");
        check("QR de um frasco que não existe: FALHA", falso.getLevel() == Level.FALHA);

        // ------------------------------------------------------------------
        System.out.println("\n3. FRAUDE 1: QR COPIADO (SEGUNDA VENDA DO FRASCO 137)");
        Transaction t = s.sellJar(demo.getShop(), lote + "-137");
        System.out.println("   entrou na cadeia: " + t);
        check("a segunda venda deu ALERTA_FRAUDE na cadeia", t instanceof FraudAlertTx);
        r = s.verifyJar(lote + "-137");
        System.out.println(r);
        check("verifyJar do 137 passa a FALHA", r.getLevel() == Level.FALHA);

        // ------------------------------------------------------------------
        System.out.println("\n4. FRAUDE 3: EMBALAR MAIS DO QUE SE PRODUZIU");
        int antes = s.getChain().getSize();
        try {
            s.packJars(demo.getBeekeeper(), lote, 451, 500, 1.0);
            check("embalar 50 kg a mais é recusado (FOI ACEITE)", false);
        } catch (RuleException e) {
            System.out.println("   recusado: " + e.getMessage());
            check("embalar 50 kg a mais é recusado e nada entra na cadeia", s.getChain().getSize() == antes);
        }

        // ------------------------------------------------------------------
        System.out.println("\n5. REVOGAÇÃO: A DGAV REVOGA O CERTIFICADO DE OUTRO LOTE");
        // um lote novo, completo, para não estragar o lote base
        String apiario = s.getState().getCredentials("REGISTO_APIARIO", demo.getBeekeeper().getDid()).get(0)
                .getCredentialId();
        String lote2 = s.createLot(demo.getBeekeeper(), apiario, 20, Map.of("origemFloral", "rosmaninho"));
        s.issueAnalysis(demo.getLab(), lote2, Map.of("origemFloral", "rosmaninho 55%", "humidade", "17.8%"));
        String cert2 = s.issueCertificate(demo.getDop(), lote2, Map.of("denominacao", "teste"));
        s.packJars(demo.getBeekeeper(), lote2, 1, 10, 1.0);
        check("frasco 1 do lote " + lote2 + " antes da revogação: OK", s.verifyJar(lote2 + "-1").getLevel() == Level.OK);
        s.revoke(demo.getDgav(), cert2, "certificado emitido por engano");
        r = s.verifyJar(lote2 + "-1");
        System.out.println(r);
        check("depois de revogar o certificado: FALHA", r.getLevel() == Level.FALHA);

        // ------------------------------------------------------------------
        System.out.println("\n6. FRAUDE 2: ANÁLISE ALTERADA FORA DA CADEIA");
        String analise = s.getState().getCredentials("ANALISE_LOTE", lote).get(0).getCredentialId();
        // o falsificador muda a origem floral na base de dados fora da cadeia (só em memória:
        // o ficheiro offchain.dat não é gravado, para a próxima execução voltar a estar limpa)
        s.getOffChain().tamper(analise, "origemFloral", "urze 95%");
        r = s.verifyJar(lote + "-138");
        System.out.println(r);
        check("o hash não coincide e verifyJar do 138 passa a FALHA",
                r.getLevel() == Level.FALHA && r.toString().contains("ALTERAÇÃO fora da cadeia"));

        // ------------------------------------------------------------------
        System.out.println("\n7. AS QUATRO CÓPIAS DA CADEIA");
        String ultimo = demo.getNodes().get(0).getChain().getLastBlockHash();
        boolean iguais = true;
        for (Node n : demo.getNodes()) {
            System.out.println("   " + n);
            iguais = iguais && n.getChain().isValid() && n.getChain().getLastBlockHash().equals(ultimo);
        }
        check("os 4 nós têm cadeias iguais e válidas", iguais);

        // ------------------------------------------------------------------
        // Os passos 8 a 15 usam pastas temporárias: estragam o cenário de propósito
        DemoData limpa = DemoData.load(Files.createTempDirectory("apischain-demo").toString());
        ApisChainService sl = limpa.getService();
        String lt = limpa.getBaseLot();

        System.out.println("\n8. VERIFICAR ENQUANTO OUTRA THREAD ESCREVE NA CADEIA");
        // uma thread vende frascos e desliga e volta a ligar o N4 (a sincronização também
        // escreve na cadeia); ao mesmo tempo o consumidor verifica um frasco bom
        Exception[] erro = new Exception[1];
        Thread vendedor = new Thread(() -> {
            try {
                for (int i = 1; i <= 15; i++) {
                    limpa.getNetwork().setOnline(limpa.getNodes().get(3), false);
                    sl.sellJar(limpa.getShop(), lt + "-" + i);
                    limpa.getNetwork().setOnline(limpa.getNodes().get(3), true);
                }
            } catch (Exception e) {
                erro[0] = e;
            }
        });
        vendedor.start();
        int leituras = 0;
        int leiturasMas = 0;
        while (vendedor.isAlive()) {
            leituras++;
            if (sl.verifyJar(lt + "-138").getLevel() != Level.OK) {
                leiturasMas++;
            }
            // pausa curta: sem ela este ciclo ocupava o bloqueio e quase não deixava vender
            Thread.sleep(1);
        }
        vendedor.join();
        check(leituras + " leituras, " + leiturasMas + " falharam (tem de ser 0)",
                leituras > 0 && leiturasMas == 0 && erro[0] == null);

        // ------------------------------------------------------------------
        System.out.println("\n9. ANÁLISE REVOGADA E SUBSTITUÍDA");
        String an1 = sl.getState().getCredentials("ANALISE_LOTE", lt).get(0).getCredentialId();
        sl.revoke(limpa.getDgav(), an1, "erro na amostra");
        check("única análise revogada: FALHA", sl.verifyJar(lt + "-138").getLevel() == Level.FALHA);
        sl.issueAnalysis(limpa.getLab(), lt, Map.of("origemFloral", "urze 60%", "humidade", "17.0%"));
        r = sl.verifyJar(lt + "-138");
        System.out.println(r);
        check("com uma análise nova e válida: OK, e a revogada aparece como informação",
                r.getLevel() == Level.OK && r.toString().contains(an1 + ": revogação registada, já há substituição"));

        // ------------------------------------------------------------------
        System.out.println("\n10. REGISTO DO APICULTOR REVOGADO");
        String registo = sl.getState().getCredentials("REGISTO_APICULTOR", limpa.getBeekeeper().getDid()).get(0)
                .getCredentialId();
        sl.revoke(limpa.getDgav(), registo, "licença suspensa");
        r = sl.verifyJar(lt + "-138");
        System.out.println(r);
        check("AVISO, e o texto não mostra null", r.getLevel() == Level.AVISO && !r.toString().contains("null"));

        // ------------------------------------------------------------------
        System.out.println("\n11. CÓPIA DA CADEIA ADULTERADA (N4)");
        limpa.getNodes().get(3).getChain().get(1).tamperTransaction(0, "adulterada");
        r = sl.verifyJar(lt + "-138");
        System.out.println(r);
        check("FALHA com uma mensagem que se percebe",
                r.getLevel() == Level.FALHA && r.toString().contains("corrompida no bloco #1")
                && r.getChecks().size() == 1);

        // ------------------------------------------------------------------
        System.out.println("\n12. ADULTERAÇÃO DETETADA PELO LABORATÓRIO");
        String pasta2 = Files.createTempDirectory("apischain-demo").toString();
        DemoData d2 = DemoData.load(pasta2);
        ApisChainService s2 = d2.getService();
        String l2 = d2.getBaseLot();
        String ma = s2.issueAnalysis(d2.getLab(), l2, Map.of("origemFloral", "urze 30%", "adulteracao", "detetada"));
        r = s2.verifyJar(l2 + "-138");
        System.out.println(r);
        check("uma análise íntegra que detetou adulteração: FALHA",
                r.getLevel() == Level.FALHA && r.toString().contains(ma + ": ADULTERAÇÃO"));
        s2.revoke(d2.getDgav(), ma, "amostra trocada");
        check("revogada essa análise (há outra em vigor, sem adulteração): OK",
                s2.verifyJar(l2 + "-138").getLevel() == Level.OK);

        // ------------------------------------------------------------------
        System.out.println("\n13. BLOCO COM VÁRIAS TRANSAÇÕES (ÁRVORE DE MERKLE)");
        Block cinco = null;
        for (Block b : s2.getChain().getChain()) {
            if (b.getTransactions().size() == 5) {
                cinco = b;
            }
        }
        check("o cenário base tem um bloco com 5 vendas", cinco != null);
        if (cinco != null) {
            String tx = cinco.getTransactions().get(3);
            List<String> prova = cinco.getMerkleTree().getProof(tx);
            check("prova da 4.ª venda com 3 irmãos e a raiz do bloco",
                    prova.size() == 4 && MerkleTree.isProofValid(tx, prova)
                    && prova.get(3).equals(cinco.getMerkleRoot()));
        }

        // ------------------------------------------------------------------
        System.out.println("\n14. ABRIR DEPOIS DE FECHAR COM UM NÓ DESLIGADO");
        s2.getNetwork().setOnline(d2.getNodes().get(3), false);
        s2.sellJar(d2.getShop(), l2 + "-140");
        DemoData d3 = DemoData.load(pasta2);
        ultimo = d3.getNodes().get(0).getChain().getLastBlockHash();
        iguais = true;
        for (Node n : d3.getNodes()) {
            iguais = iguais && n.getChain().getLastBlockHash().equals(ultimo);
        }
        check("ao abrir, o N4 recupera o bloco em falta", iguais);
        check("e o serviço vê a venda feita enquanto ele estava desligado", d3.getService().getState().isSold(l2 + "-140"));

        // ------------------------------------------------------------------
        System.out.println("\n15. LIGAR UM NÓ COM OUTRO CORROMPIDO");
        // o N1 tem a cadeia mais comprida, mas com o último bloco adulterado; o N2 está bom
        Node c1 = d3.getNodes().get(0);
        Node c3 = d3.getNodes().get(2);
        d3.getNetwork().setOnline(c3, false);
        d3.getService().sellJar(d3.getShop(), l2 + "-141");
        c1.getChain().getLastBlock().tamperTransaction(0, "adulterada");
        try {
            d3.getNetwork().setOnline(c3, true);
        } catch (Exception e) {
            System.out.println("   ao ligar o N3: " + e.getMessage());
        }
        Node c2 = d3.getNodes().get(1);
        check("o N3 sincroniza com um nó íntegro e fica igual ao N2",
                c3.getChain().isValid()
                && c3.getChain().getLastBlockHash().equals(c2.getChain().getLastBlockHash()));

        // ------------------------------------------------------------------
        System.out.println();
        if (falhas == 0) {
            System.out.println("TODAS AS VERIFICAÇÕES PASSARAM");
        } else {
            System.out.println(falhas + " VERIFICAÇÕES FALHARAM");
            System.exit(1);
        }
    }

    static void check(String descricao, boolean ok) {
        System.out.println("   " + (ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }
}
