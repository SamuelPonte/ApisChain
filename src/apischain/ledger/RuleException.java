//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.ledger;

/**
 * Transação rejeitada pelas regras dos validadores. A mensagem, em português,
 * explica porquê e é mostrada na interface.
 */
public class RuleException extends Exception {

    public RuleException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 202611220001L;
}
