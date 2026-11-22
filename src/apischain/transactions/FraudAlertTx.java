//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Alerta de fraude sobre um frasco (ALERTA_FRAUDE do protótipo).
 * É criado pelas regras (issue #3) quando aparece uma segunda venda do mesmo
 * frasco, e assinado pela autoridade que propõe o bloco (issue #4), não pela loja.
 */
public class FraudAlertTx extends Transaction {

    private final String jarId;   // código do frasco suspeito
    private final String reason;  // motivo, por exemplo "segunda venda: QR copiado"

    /**
     * @param author DID da autoridade que propõe o bloco
     * @param jarId código do frasco
     * @param reason motivo do alerta
     */
    public FraudAlertTx(String author, String jarId, String reason) {
        super(author);
        this.jarId = checkText(jarId, "frasco");
        this.reason = checkText(reason, "motivo");
    }

    @Override
    public String getContent() {
        return "frasco=" + jarId + "|motivo=" + reason;
    }

    public String getJarId() {
        return jarId;
    }

    public String getReason() {
        return reason;
    }

    private static final long serialVersionUID = 202611160007L;
}
