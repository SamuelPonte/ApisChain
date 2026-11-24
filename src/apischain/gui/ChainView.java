//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.ledger.LedgerState;
import apischain.service.ApisChainService;
import apischain.transactions.PackTx;
import apischain.transactions.SaleTx;
import apischain.transactions.Transaction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Leituras da cadeia partilhadas pelos painéis (só para mostrar no ecrã; não
 * há aqui regras). Quem percorre a cadeia bloqueia a rede, como pede o
 * comentário de ApisChainService.getChain.
 */
public class ChainView {

    /**
     * Todas as transações da cadeia, por ordem.
     */
    private static List<Transaction> transactions(ApisChainService service) throws Exception {
        List<Transaction> list = new ArrayList<>();
        synchronized (service.getNetwork()) {
            BlockChain chain = service.getChain();
            for (Block b : chain.getChain()) {
                for (String txt : b.getTransactions()) {
                    list.add(Transaction.fromBase64(txt));
                }
            }
        }
        return list;
    }

    /**
     * @return código do primeiro frasco vendido, ou null se não houver vendas
     */
    public static String firstSoldJar(ApisChainService service) throws Exception {
        for (Transaction t : transactions(service)) {
            if (t instanceof SaleTx s) {
                return s.getJarId();
            }
        }
        return null;
    }

    /**
     * Frascos de exemplo para escolher no painel do consumidor: os já vendidos
     * e o primeiro por vender de cada embalagem.
     */
    public static List<String> exampleJars(ApisChainService service) throws Exception {
        List<String> jars = new ArrayList<>();
        LedgerState state = service.getState();
        for (Transaction t : transactions(service)) {
            if (t instanceof SaleTx s) {
                jars.add(s.getJarId());
            } else if (t instanceof PackTx p) {
                for (int n = p.getFirstJar(); n <= p.getLastJar(); n++) {
                    if (!state.isSold(p.getJarId(n))) {
                        jars.add(p.getJarId(n));
                        break;
                    }
                }
            }
        }
        return jars;
    }

    /**
     * Transação em texto legível: tipo, autor, data e um campo por linha.
     *
     * @param base64 transação como está no bloco
     * @return texto, ou um aviso se não se conseguir ler (adulterada)
     */
    public static String describe(String base64) {
        try {
            Transaction t = Transaction.fromBase64(base64);
            StringBuilder txt = new StringBuilder(t.getClass().getSimpleName());
            txt.append("\n   autor: ").append(t.getAuthor());
            txt.append("\n   data: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(t.getTimestamp())));
            for (String field : t.getContent().split("\\|")) {
                txt.append("\n   ").append(shorten(field, 70));
            }
            return txt.toString();
        } catch (Exception ex) {
            return "ILEGÍVEL (transação adulterada): " + shorten(base64, 40);
        }
    }

    /**
     * Nome curto de uma transação, para listas e para a árvore.
     */
    public static String shortName(String base64) {
        try {
            return Transaction.fromBase64(base64).getClass().getSimpleName();
        } catch (Exception ex) {
            return "ilegível";
        }
    }

    /**
     * Corta um texto comprido (chaves e hashes) para caber no ecrã.
     */
    public static String shorten(String txt, int max) {
        return (txt.length() <= max) ? txt : txt.substring(0, max) + "…";
    }
}
