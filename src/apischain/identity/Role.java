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
 * o apicultor e a loja só criam transações.
 */
public enum Role {
    DGAV,       // regista apicultores, apiários e lojas; revoga credenciais
    LAB,        // Laboratório: emite a análise de cada lote
    DOP,        // Certificador DOP: emite o certificado de origem
    APICULTOR,  // cria lotes e embala frascos
    LOJA        // regista a venda de cada frasco
}
