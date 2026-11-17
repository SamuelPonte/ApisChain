//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Venda de um frasco por uma loja registada pela DGAV.
 * Uma segunda venda do mesmo frasco não entra na cadeia: as regras (issue #3)
 * transformam-na num FraudAlertTx (QR copiado).
 */
public class SaleTx extends Transaction {

    private final String jarId;  // código do frasco (o que está no QR)
    private final String shop;   // DID da loja que vende

    /**
     * @param author DID de quem assina (a própria loja)
     * @param jarId código do frasco
     * @param shop DID da loja
     */
    public SaleTx(String author, String jarId, String shop) {
        super(author);
        this.jarId = jarId;
        this.shop = shop;
    }

    @Override
    public String getContent() {
        return "frasco=" + jarId + "|loja=" + shop;
    }

    public String getJarId() {
        return jarId;
    }

    public String getShop() {
        return shop;
    }

    private static final long serialVersionUID = 202611160006L;
}
