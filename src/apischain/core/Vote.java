//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.core;

import java.io.Serializable;

/**
 * Voto de uma autoridade num bloco proposto (consenso PoA com votação).
 * A autoridade assina o hash do bloco com a sua chave privada: assim
 * ninguém consegue forjar o voto de outra autoridade.
 */
public class Vote implements Serializable {

    private final String validator;  // DID da autoridade que votou
    private final String signature;  // assinatura RSA do hash do bloco, em Base64

    public Vote(String validator, String signature) {
        this.validator = validator;
        this.signature = signature;
    }

    public String getValidator() {
        return validator;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "Vote{" + validator + "}";
    }

    private static final long serialVersionUID = 202611110002L;
}
