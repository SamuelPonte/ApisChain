//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Revogação de uma credencial (por exemplo, a DGAV revoga o registo de um
 * apicultor). A credencial continua na cadeia, porque a cadeia não se apaga,
 * mas passa a contar como revogada (LedgerState.isRevoked, issue #3).
 */
public class RevokeTx extends Transaction {

    private final String credentialId;  // credencial a revogar
    private final String reason;        // motivo da revogação

    /**
     * @param author DID de quem revoga
     * @param credentialId id da credencial
     * @param reason motivo
     */
    public RevokeTx(String author, String credentialId, String reason) {
        super(author);
        this.credentialId = checkText(credentialId, "credencial");
        this.reason = checkText(reason, "motivo");
    }

    @Override
    public String getContent() {
        return "credencial=" + credentialId + "|motivo=" + reason;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getReason() {
        return reason;
    }

    private static final long serialVersionUID = 202611160005L;
}
