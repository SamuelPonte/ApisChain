//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

import java.io.Serializable;

/**
 * Autoridade inicial da rede (DGAV, Laboratório ou DOP), registada no bloco de
 * génese. As chaves públicas que estão aqui são a raiz de confiança: é com elas
 * que se verificam as assinaturas dos blocos e dos votos (consenso, issue #4).
 */
public class Authority implements Serializable {

    private final String did;        // identificador da autoridade
    private final String role;       // papel: DGAV, LAB ou DOP
    private final String publicKey;  // chave pública em Base64

    public Authority(String did, String role, String publicKey) {
        this.did = did;
        this.role = role;
        this.publicKey = publicKey;
    }

    /**
     * @return texto determinista com os três campos (entra na assinatura da génese)
     */
    public String getContent() {
        return did + "," + role + "," + publicKey;
    }

    public String getDid() {
        return did;
    }

    public String getRole() {
        return role;
    }

    public String getPublicKey() {
        return publicKey;
    }

    @Override
    public String toString() {
        return role + " (" + did + ")";
    }

    private static final long serialVersionUID = 202611160008L;
}
