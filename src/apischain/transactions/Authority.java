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
 * que se verificam as assinaturas dos blocos e dos votos (ProofOfAuthority).
 */
public class Authority implements Serializable {

    private final String did;        // identificador da autoridade
    private final String role;       // papel: DGAV, LAB ou DOP
    private final String publicKey;  // chave pública em Base64

    public Authority(String did, String role, String publicKey) {
        this.did = checkText(did, "DID");
        this.role = checkText(role, "papel");
        this.publicKey = checkText(publicKey, "chave pública");
    }

    /**
     * Na GenesisTx as autoridades são separadas por ";" e os campos de cada uma
     * por ",", dentro de um texto que usa "|". Nenhum campo pode conter estes
     * separadores, senão dois conjuntos diferentes de autoridades podiam dar o
     * mesmo texto assinado.
     */
    private static String checkText(String value, String name) {
        if (value == null || value.contains(",") || value.contains(";") || value.contains("|")) {
            throw new IllegalArgumentException("O campo " + name + " da autoridade não pode ser null nem conter , ; ou |");
        }
        return value;
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
