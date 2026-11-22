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
 *
 * A loja que vende é o autor da transação. Não há um campo "loja" à parte
 * porque, se houvesse, uma loja podia assinar uma venda em nome de outra
 * (autor = loja A, loja = loja B).
 */
public class SaleTx extends Transaction {

    private final String jarId;  // código do frasco (o que está no QR)

    /**
     * @param shop DID da loja que vende (é quem assina)
     * @param jarId código do frasco
     */
    public SaleTx(String shop, String jarId) {
        super(shop);
        this.jarId = checkText(jarId, "frasco");
    }

    @Override
    public String getContent() {
        return "frasco=" + jarId;
    }

    public String getJarId() {
        return jarId;
    }

    /**
     * @return DID da loja que vendeu (o autor da transação)
     */
    public String getShop() {
        return getAuthor();
    }

    private static final long serialVersionUID = 202611160006L;
}
