//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.identity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

/**
 * Armazém das credenciais completas, FORA da cadeia (RGPD).
 *
 * Na PoC é um mapa em memória gravado num ficheiro. Num sistema real seria a
 * base de dados de cada emissor; a cadeia só guarda o hash de cada credencial,
 * por isso uma alteração aqui é sempre detetada (getHash deixa de coincidir).
 */
public class OffChainStore {

    // credenciais indexadas pelo id
    private HashMap<String, Credential> credentials = new HashMap<>();

    /**
     * Guarda uma credencial (substitui se já existir uma com o mesmo id).
     *
     * @param credential credencial assinada
     */
    public void put(Credential credential) {
        credentials.put(credential.getId(), credential);
    }

    /**
     * @param id id da credencial
     * @return a credencial, ou null se não existir
     */
    public Credential get(String id) {
        return credentials.get(id);
    }

    /**
     * @param id id da credencial
     * @return true se existir
     */
    public boolean contains(String id) {
        return credentials.containsKey(id);
    }

    /**
     * APENAS PARA DEMONSTRAÇÃO (fraude 2): altera um dado de uma credencial
     * guardada, sem a voltar a assinar.
     *
     * @param id id da credencial
     * @param key nome do dado
     * @param value novo valor
     */
    public void tamper(String id, String key, String value) {
        Credential c = credentials.get(id);
        if (c == null) {
            throw new IllegalArgumentException("Não existe a credencial " + id);
        }
        c.tamperData(key, value);
    }

    /**
     * Grava todas as credenciais num ficheiro (serialização, como a BlockChain).
     *
     * @param fileName nome do ficheiro
     * @throws Exception erro de escrita
     */
    public void save(String fileName) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(credentials);
        }
    }

    /**
     * Carrega as credenciais de um ficheiro (substitui as que estão em memória).
     *
     * @param fileName nome do ficheiro
     * @throws Exception erro de leitura
     */
    @SuppressWarnings("unchecked")
    public void load(String fileName) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            this.credentials = (HashMap<String, Credential>) in.readObject();
        }
    }

    public int getSize() {
        return credentials.size();
    }
}
