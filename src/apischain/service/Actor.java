//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import apischain.identity.DigitalID;
import apischain.identity.Role;
import java.security.PrivateKey;

/**
 * Interveniente com sessão aberta: a sua identidade e a chave privada, lida
 * do ficheiro com a palavra-passe (como no TemplarLogin do professor).
 * É o que o ecrã de entrada (issue #6) entrega às operações do serviço.
 */
public class Actor {

    private final DigitalID id;        // identidade pública
    private final PrivateKey key;      // chave privada (só em memória, nunca gravada em claro)

    public Actor(DigitalID id, PrivateKey key) {
        this.id = id;
        this.key = key;
    }

    /**
     * Abre a sessão de um interveniente que já existe na pasta das chaves.
     *
     * @param name nome do interveniente (nome dos ficheiros)
     * @param password palavra-passe da chave privada
     * @param folder pasta das chaves
     * @return o interveniente com sessão aberta
     * @throws Exception se não existir ou a palavra-passe estiver errada
     */
    public static Actor login(String name, String password, String folder) throws Exception {
        DigitalID id = DigitalID.load(name, folder);
        PrivateKey key = DigitalID.loadPrivateKey(name, password, folder);
        return new Actor(id, key);
    }

    public DigitalID getId() {
        return id;
    }

    public PrivateKey getKey() {
        return key;
    }

    public String getDid() {
        return id.getDid();
    }

    public String getName() {
        return id.getName();
    }

    public Role getRole() {
        return id.getRole();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
