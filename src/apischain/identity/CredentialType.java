//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.identity;

/**
 * Tipos de credencial da ApisChain e quem os emite.
 *
 * Esta classe só enumera os tipos. Quem verifica se o emissor tem o papel
 * certo para cada tipo são as regras dos validadores (Rules, issue #3).
 */
public enum CredentialType {
    REGISTO_APICULTOR,   // emitida pela DGAV ao apicultor
    REGISTO_APIARIO,     // emitida pela DGAV a um apiário de um apicultor
    REGISTO_LOJA,        // emitida pela DGAV à loja
    LOTE,                // criada pelo apicultor (kg colhidos, apiário de origem)
    ANALISE_LOTE,        // emitida pelo Laboratório sobre um lote
    CERTIFICADO_ORIGEM   // emitida pelo Certificador DOP sobre um lote
}
