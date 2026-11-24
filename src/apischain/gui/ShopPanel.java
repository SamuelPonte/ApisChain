//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.service.Actor;
import apischain.service.ApisChainService;
import apischain.transactions.FraudAlertTx;
import apischain.transactions.Transaction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Painel da loja: regista a venda de cada frasco. Tem também o botão da
 * fraude 1 (vender a cópia de um frasco já vendido).
 */
public class ShopPanel extends BasePanel {

    private final JTextField txtJar = new JTextField();
    private final JButton btSell = new JButton("Registar venda");
    private final JButton btFraud = new JButton("Fraude 1: vender a cópia de um frasco já vendido");

    public ShopPanel(ApisChainService service, Actor actor) {
        super(service, actor);
        initComponents();
    }

    private void initComponents() {
        JPanel sell = section("Registar venda");
        addRow(sell, "Código do frasco (QR)", txtJar);
        addRow(sell, "Exemplo: LOTE-2026-0001-138", btSell);
        addSection(sell);

        JPanel fraud = section("Demonstração");
        addRow(fraud, "QR copiado de um frasco já vendido", btFraud);
        addSection(fraud);

        btSell.addActionListener(evt -> btSellActionPerformed());
        btFraud.addActionListener(evt -> btFraudActionPerformed());
    }

    private void btSellActionPerformed() {
        String jar = txtJar.getText().trim();
        if (jar.isEmpty()) {
            log("escreva o código do frasco");
            return;
        }
        runInBackground(btSell, () -> sell(jar));
    }

    /**
     * Fraude 1: procura na cadeia um frasco já vendido e tenta vendê-lo outra vez.
     */
    private void btFraudActionPerformed() {
        runInBackground(btFraud, () -> {
            String sold = ChainView.firstSoldJar(service);
            if (sold == null) {
                throw new Exception("Ainda não há frascos vendidos: registe uma venda primeiro");
            }
            return sell(sold);
        });
    }

    /**
     * Vende um frasco (corre fora da thread do Swing).
     *
     * @return mensagem da venda
     * @throws Exception se a venda for recusada, incluindo quando o frasco já
     *                   tinha sido vendido e entrou um alerta no lugar da venda
     */
    private String sell(String jar) throws Exception {
        Transaction tx = service.sellJar(actor, jar);
        if (tx instanceof FraudAlertTx alert) {
            throw new Exception("ALERTA_FRAUDE registado na cadeia para o frasco " + jar + ": " + alert.getReason());
        }
        return "frasco " + jar + " vendido";
    }
}
