//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.identity;

import apischain.utils.SecurityUtils;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Credencial digital completa, assinada pelo emissor.
 *
 * A credencial completa fica FORA da cadeia (OffChainStore), porque pode ter
 * dados pessoais (nome, NIF, coordenadas do apiário). Na cadeia só fica o seu
 * hash (IssueCredentialTx, issue #2). Para verificar uma credencial:
 * 1. calcular getHash() e comparar com o hash ancorado na cadeia;
 * 2. verificar a assinatura com a chave pública do emissor, obtida da cadeia.
 * Se alguém alterar um dado fora da cadeia, o hash deixa de coincidir.
 */
public class Credential implements Serializable {

    private final String id;                  // identificador, por exemplo LOTE-2026-0042
    private final CredentialType type;        // tipo de credencial
    private final String issuer;              // DID de quem emite
    private final String subject;             // DID ou id do titular
    private final TreeMap<String, String> data; // dados da credencial (ordenados pela chave)
    private final long issuedAt;              // data de emissão em milissegundos
    private String signature;                 // assinatura do emissor ("" se ainda não assinada)

    /**
     * Cria uma credencial por assinar.
     *
     * @param id identificador da credencial
     * @param type tipo
     * @param issuer DID do emissor
     * @param subject titular
     * @param data dados (copiados; a ordem em que foram inseridos não importa)
     */
    public Credential(String id, CredentialType type, String issuer, String subject, Map<String, String> data) {
        // 1. os separadores do texto assinado não podem aparecer nos campos
        checkText(id, "id");
        checkText(issuer, "emissor");
        checkText(subject, "titular");
        for (Map.Entry<String, String> e : data.entrySet()) {
            checkText(e.getKey(), "nome do dado");
            // o "=" só é proibido no nome do dado: como os nomes não têm "=", o
            // primeiro "=" de cada par separa sempre o nome do valor. Assim os
            // valores podem ser hashes e chaves em Base64, que acabam em "="
            if (e.getKey().contains("=")) {
                throw new IllegalArgumentException("O nome do dado não pode conter =: " + e.getKey());
            }
            checkText(e.getValue(), "valor de " + e.getKey());
        }
        // 2. guardar os campos
        this.id = id;
        this.type = type;
        this.issuer = issuer;
        this.subject = subject;
        // 3. o TreeMap ordena os dados pela chave: os mesmos dados dão sempre o
        //    mesmo texto, seja qual for a ordem em que foram inseridos
        this.data = new TreeMap<>(data);
        this.issuedAt = System.currentTimeMillis();
        this.signature = "";
    }

    /**
     * Recusa textos vazios (null) ou com os separadores usados em getDataToSign().
     * Sem isto, ("a|b", "c") e ("a", "b|c") davam o mesmo texto assinado.
     *
     * @param txt texto a verificar
     * @param name nome do campo, para a mensagem de erro
     */
    private static void checkText(String txt, String name) {
        if (txt == null) {
            throw new IllegalArgumentException("O campo " + name + " não pode ser null");
        }
        if (txt.contains("|") || txt.contains(";")) {
            throw new IllegalArgumentException("O campo " + name + " não pode conter | nem ;: " + txt);
        }
    }

    /**
     * Texto que o emissor assina: tipo, id, emissor, titular, data e os dados
     * no formato chave=valor separados por ";".
     *
     * @return texto a assinar
     */
    public String getDataToSign() {
        StringBuilder txt = new StringBuilder();
        txt.append(type).append("|").append(id).append("|").append(issuer)
                .append("|").append(subject).append("|").append(issuedAt).append("|");
        // os dados já estão ordenados pela chave (TreeMap)
        boolean first = true;
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (!first) {
                txt.append(";");
            }
            txt.append(e.getKey()).append("=").append(e.getValue());
            first = false;
        }
        return txt.toString();
    }

    /**
     * Hash da credencial: é este valor que fica ancorado na cadeia.
     * Cobre o conteúdo (não a assinatura): a assinatura verifica-se à parte.
     *
     * @return hash SHA3-256 em Base64
     */
    public String getHash() {
        return SecurityUtils.getHash(getDataToSign());
    }

    /**
     * Assina a credencial com a chave privada do emissor.
     *
     * @param key chave privada do emissor
     * @throws Exception se a chave for inválida
     */
    public void sign(PrivateKey key) throws Exception {
        this.signature = SecurityUtils.sign(getDataToSign(), key);
    }

    /**
     * Verifica a assinatura com a chave pública do emissor.
     *
     * @param key chave pública do emissor (obtida da cadeia, não da credencial)
     * @return true se foi assinada por essa chave e não foi alterada
     */
    public boolean isSignatureValid(PublicKey key) {
        // uma credencial por assinar nunca é válida
        if (signature.isEmpty()) {
            return false;
        }
        return SecurityUtils.verify(getDataToSign(), signature, key);
    }

    /**
     * APENAS PARA DEMONSTRAÇÃO (fraude 2): altera um dado sem voltar a assinar,
     * como faria um falsificador com acesso à base de dados fora da cadeia.
     *
     * @param key nome do dado
     * @param value novo valor
     */
    public void tamperData(String key, String value) {
        data.put(key, value);
    }

    public String getId() {
        return id;
    }

    public CredentialType getType() {
        return type;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    public Map<String, String> getData() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * @param key nome do dado
     * @return valor do dado, ou null se não existir
     */
    public String getData(String key) {
        return data.get(key);
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return type + " " + id + " (emissor=" + issuer + ", titular=" + subject + ", " + data
                + (signature.isEmpty() ? ", por assinar)" : ", assinada)");
    }

    private static final long serialVersionUID = 202611180002L;
}
