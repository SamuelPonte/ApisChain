//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.identity;

/**
 * Papel de um interveniente na ApisChain.
 *
 * As três primeiras são as autoridades (validam blocos no Proof of Authority);
 * o apicultor só cria lotes.
 */
public enum Role {
    DGAV,       // regista apicultores e apiários; revoga credenciais
    LAB,        // Laboratório: emite a análise de cada lote
    DOP,        // Certificador DOP: emite o certificado de origem
    APICULTOR   // cria os lotes
}
