//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Emissão de uma credencial: ancora na cadeia o HASH da credencial completa,
 * que fica fora da cadeia (OffChainStore, issue #1). É isto que cumpre o RGPD:
 * nomes, moradas e resultados detalhados nunca entram na cadeia.
 *
 * Tipos (texto, para não depender do enum da issue #1): REGISTO_APICULTOR,
 * REGISTO_APIARIO, REGISTO_LOJA, LOTE, ANALISE_LOTE, CERTIFICADO_ORIGEM.
 *
 * Campos opcionais:
 * - subjectKey: chave pública do titular em Base64, só nos registos de
 *   apicultor e de loja (é assim que a cadeia fica a saber a chave deles);
 * - weightKg: kg colhidos, só no tipo LOTE (ficam em claro na cadeia porque
 *   as regras precisam deles para o balanço de massa).
 * Quando não se aplicam ficam "" e 0.
 */
public class IssueCredentialTx extends Transaction {

    private final String credentialId;    // id da credencial (por exemplo LOTE-2026-0042)
    private final String credentialType;  // tipo, em texto
    private final String subject;         // DID ou id do titular (apicultor, apiário, lote...)
    private final String credentialHash;  // hash SHA3-256 da credencial guardada fora da cadeia
    private final String subjectKey;      // chave pública do titular em Base64, ou ""
    private final double weightKg;        // kg colhidos (só LOTE), ou 0

    /**
     * Construtor completo.
     *
     * @param author DID do emissor (DGAV, Laboratório, DOP ou apicultor no LOTE)
     * @param credentialId id da credencial
     * @param credentialType tipo da credencial
     * @param subject titular
     * @param credentialHash hash da credencial fora da cadeia
     * @param subjectKey chave pública do titular em Base64 ("" ou null se não se aplica)
     * @param weightKg kg colhidos no tipo LOTE (0 nos outros)
     */
    public IssueCredentialTx(String author, String credentialId, String credentialType, String subject,
            String credentialHash, String subjectKey, double weightKg) {
        super(author);
        this.credentialId = checkText(credentialId, "credencial");
        this.credentialType = checkText(credentialType, "tipo");
        this.subject = checkText(subject, "titular");
        this.credentialHash = checkText(credentialHash, "hash");
        // null passa a "" para o texto assinado ser sempre o mesmo
        this.subjectKey = checkText((subjectKey == null) ? "" : subjectKey, "chave");
        this.weightKg = weightKg;
    }

    /**
     * Construtor para credenciais sem chave nem peso (apiário, análise, certificado).
     */
    public IssueCredentialTx(String author, String credentialId, String credentialType, String subject,
            String credentialHash) {
        this(author, credentialId, credentialType, subject, credentialHash, "", 0);
    }

    @Override
    public String getContent() {
        return "credencial=" + credentialId + "|tipo=" + credentialType + "|titular=" + subject
                + "|hash=" + credentialHash + "|chave=" + subjectKey + "|kg=" + weightKg;
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

    public double getWeightKg() {
        return weightKg;
    }

    private static final long serialVersionUID = 202611160003L;
}
