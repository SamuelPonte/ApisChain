//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Transação do bloco de génese: a lista das autoridades iniciais (DID, papel
 * e chave pública). É a partir dela que todos os nós sabem quem pode propor
 * blocos e votar (Proof of Authority).
 */
public class GenesisTx extends Transaction {

    private final List<Authority> authorities;  // autoridades iniciais

    /**
     * @param author DID de quem cria a rede (por exemplo a DGAV)
     * @param authorities autoridades iniciais (pelo menos uma)
     */
    public GenesisTx(String author, List<Authority> authorities) {
        super(author);
        if (authorities.isEmpty()) {
            throw new IllegalArgumentException("A génese tem de ter pelo menos uma autoridade");
        }
        // copiar a lista para ninguém a alterar por fora depois de assinada
        this.authorities = new ArrayList<>(authorities);
    }

    @Override
    public String getContent() {
        // as autoridades separadas por ";" e os campos de cada uma por ","
        StringBuilder txt = new StringBuilder("autoridades=");
        for (int i = 0; i < authorities.size(); i++) {
            if (i > 0) {
                txt.append(";");
            }
            txt.append(authorities.get(i).getContent());
        }
        return txt.toString();
    }

    public List<Authority> getAuthorities() {
        return Collections.unmodifiableList(authorities);
    }

    @Override
    public String toString() {
        return "GenesisTx{autor=" + getAuthor() + ", autoridades=" + authorities + "}";
    }

    private static final long serialVersionUID = 202611160002L;
}
