//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Emissão de uma credencial: ancora na cadeia o HASH da credencial completa,
 * que fica fora da cadeia (OffChainStore). É isto que cumpre o RGPD: nomes,
 * moradas e resultados detalhados nunca entram na cadeia.
 *
 * Tipos (texto): REGISTO_APICULTOR, REGISTO_APIARIO, LOTE, ANALISE_LOTE,
 * CERTIFICADO_ORIGEM.
 *
 * subjectKey é a chave pública do titular em Base64 e só existe no registo do
 * apicultor: é assim que a cadeia fica a saber a chave dele. Nos outros tipos é "".
 */
public class IssueCredentialTx extends Transaction {

    private final String credentialId;    // id da credencial (por exemplo LOTE-0001)
    private final String credentialType;  // tipo, em texto
    private final String subject;         // DID ou id do titular (apicultor, apiário, lote...)
    private final String credentialHash;  // hash SHA3-256 da credencial guardada fora da cadeia
    private final String subjectKey;      // chave pública do titular em Base64, ou ""

    /**
     * @param author DID do emissor
     * @param credentialId id da credencial
     * @param credentialType tipo da credencial
     * @param subject titular
     * @param credentialHash hash da credencial fora da cadeia
     * @param subjectKey chave pública do titular em Base64 ("" ou null se não se aplica)
     */
    public IssueCredentialTx(String author, String credentialId, String credentialType, String subject,
            String credentialHash, String subjectKey) {
        super(author);
        this.credentialId = checkText(credentialId, "credencial");
        this.credentialType = checkText(credentialType, "tipo");
        this.subject = checkText(subject, "titular");
        this.credentialHash = checkText(credentialHash, "hash");
        // null passa a "" para o texto assinado ser sempre o mesmo
        this.subjectKey = checkText((subjectKey == null) ? "" : subjectKey, "chave");
    }

    @Override
    public String getContent() {
        return "credencial=" + credentialId + "|tipo=" + credentialType + "|titular=" + subject
                + "|hash=" + credentialHash + "|chave=" + subjectKey;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public String getSubject() {
        return subject;
    }

    public String getCredentialHash() {
        return credentialHash;
    }

    public String getSubjectKey() {
        return subjectKey;
    }

    private static final long serialVersionUID = 202611160003L;
}
