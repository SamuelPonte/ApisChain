//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em digitalID.core.DigitalID do Templar2026_DigitalID          ::://
//::  (Prof. António Manso, 2026)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.identity;

import apischain.utils.SecurityUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Identidade digital de um interveniente: nome, papel, chave pública, DID e a
 * assinatura do próprio sobre esses dados.
 *
 * A chave privada NUNCA fica neste objeto: fica no ficheiro nome.priv, cifrada
 * com a palavra-passe do dono. Assim o DigitalID pode ser copiado, gravado e
 * publicado sem risco.
 *
 * Diferenças face ao DigitalID do professor:
 * 1. tem papel (Role), porque na ApisChain cada interveniente faz coisas diferentes;
 * 2. tem um DID derivado da chave pública: quem não tem a chave privada
 *    correspondente não consegue usar esse DID;
 * 3. a assinatura é feita no construtor e verifica-se com isValid();
 * 4. a chave pública é guardada com SecurityUtils.keyToBase64 (o mesmo formato
 *    usado nas transações), em vez de Conversion.objectToString;
 * 5. métodos para criar e carregar a identidade a partir de ficheiros
 *    (nome.pub, nome.priv e nome.id), como no TemplarLogin.
 */
public class DigitalID implements Serializable {

    // pasta onde ficam as chaves e as identidades (está no .gitignore)
    public static final String KEYS_FOLDER = "data/keys";
    // prefixo de todos os DID da ApisChain
    public static final String DID_PREFIX = "did:apis:";

    private final String name;       // nome do interveniente (também é o nome dos ficheiros)
    private final Role role;         // papel na rede
    private final String publicKey;  // chave pública em Base64
    private final String did;        // identificador derivado da chave pública
    private final String signature;  // assinatura do próprio sobre getDataToSign()

    /**
     * Cria a identidade e assina-a com a chave privada do próprio.
     *
     * @param name nome do interveniente
     * @param role papel
     * @param publicKey chave pública
     * @param privateKey chave privada (só usada para assinar; não é guardada)
     * @throws Exception se a chave privada for inválida
     */
    public DigitalID(String name, Role role, PublicKey publicKey, PrivateKey privateKey) throws Exception {
        // 1. guardar os dados públicos
        this.name = name;
        this.role = role;
        this.publicKey = SecurityUtils.keyToBase64(publicKey);
        // 2. o DID é calculado a partir da chave pública
        this.did = createDid(this.publicKey);
        // 3. o dono assina os seus próprios dados: prova que tem a chave privada
        this.signature = SecurityUtils.sign(getDataToSign(), privateKey);
    }

    /**
     * Calcula o DID a partir da chave pública: prefixo mais os primeiros 16
     * carateres do hash SHA3-256 da chave.
     *
     * @param publicKeyBase64 chave pública em Base64
     * @return DID, por exemplo did:apis:knWqrQC4nTxZNhuN
     */
    public static String createDid(String publicKeyBase64) {
        String hash = SecurityUtils.getHash(publicKeyBase64);
        // o Base64 pode ter "+" e "/": trocam-se por "-" e "_" para o DID ser
        // seguro em nomes de ficheiros e endereços (é o "Base64 URL")
        return DID_PREFIX + hash.substring(0, 16).replace('+', '-').replace('/', '_');
    }

    /**
     * Texto assinado pelo dono: nome, papel, chave pública e DID separados por "|".
     *
     * @return texto a assinar
     */
    public String getDataToSign() {
        return name + "|" + role + "|" + publicKey + "|" + did;
    }

    /**
     * Verifica a identidade: o DID corresponde à chave pública e a assinatura
     * foi feita pela chave privada que corresponde a essa chave pública.
     *
     * @return true se a identidade não foi alterada
     */
    public boolean isValid() {
        try {
            // 1. o DID tem de ser o da chave pública (ninguém pode trocar um sem o outro)
            if (!did.equals(createDid(publicKey))) {
                return false;
            }
            // 2. a assinatura tem de verificar com a própria chave pública
            return SecurityUtils.verify(getDataToSign(), signature, getPublicKey());
        } catch (Exception ex) {
            // chave pública mal formada: identidade inválida
            return false;
        }
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  IDENTIDADE EM FICHEIRO (como o TemplarLogin do professor)          ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Cria um interveniente novo: gera as chaves e grava três ficheiros na pasta:
     * nome.pub (chave pública em claro), nome.priv (chave privada cifrada com a
     * palavra-passe) e nome.id (o DigitalID).
     *
     * @param name nome do interveniente
     * @param role papel
     * @param password palavra-passe que protege a chave privada
     * @param folder pasta (normalmente KEYS_FOLDER)
     * @return a identidade criada
     * @throws Exception erro ao gerar as chaves ou ao gravar
     */
    public static DigitalID create(String name, Role role, String password, String folder) throws Exception {
        // 1. criar a pasta se ainda não existir
        new File(folder).mkdirs();
        String base = new File(folder, name).getPath();
        // 2. gerar o par de chaves RSA
        KeyPair keys = SecurityUtils.generateKeyPair();
        // 3. gravar as chaves (a privada fica cifrada com a palavra-passe)
        SecurityUtils.savePublicKey(keys.getPublic(), base);
        SecurityUtils.savePrivateKey(keys.getPrivate(), base, password);
        // 4. criar a identidade assinada e gravá-la
        DigitalID id = new DigitalID(name, role, keys.getPublic(), keys.getPrivate());
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(base + ".id"))) {
            out.writeObject(id);
        }
        return id;
    }

    /**
     * Carrega a identidade de um interveniente (ficheiro nome.id) e confirma que
     * é válida e que a chave pública coincide com a do ficheiro nome.pub.
     * Não precisa da palavra-passe: a identidade é pública.
     *
     * @param name nome do interveniente
     * @param folder pasta
     * @return a identidade
     * @throws Exception se os ficheiros não existirem ou não forem coerentes
     */
    public static DigitalID load(String name, String folder) throws Exception {
        String base = new File(folder, name).getPath();
        // 1. ler o DigitalID gravado
        DigitalID id;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(base + ".id"))) {
            id = (DigitalID) in.readObject();
        }
        // 2. a assinatura e o DID têm de estar certos
        if (!id.isValid()) {
            throw new Exception("A identidade de " + name + " foi alterada");
        }
        // 3. a chave pública do .id tem de ser a mesma do .pub
        PublicKey pub = SecurityUtils.loadPublicKey(base);
        if (!id.getPublicKeyBase64().equals(SecurityUtils.keyToBase64(pub))) {
            throw new Exception("A chave pública de " + name + " não coincide com a identidade");
        }
        return id;
    }

    /**
     * Lê a chave privada de um interveniente (ficheiro nome.priv).
     *
     * @param name nome do interveniente
     * @param password palavra-passe
     * @param folder pasta
     * @return chave privada
     * @throws Exception se a palavra-passe estiver errada (AEADBadTagException)
     */
    public static PrivateKey loadPrivateKey(String name, String password, String folder) throws Exception {
        return SecurityUtils.loadPrivateKey(new File(folder, name).getPath(), password);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    public String getDid() {
        return did;
    }

    public String getPublicKeyBase64() {
        return publicKey;
    }

    /**
     * @return a chave pública já como objeto, pronta a usar em verificações
     * @throws Exception se o texto Base64 estiver corrompido
     */
    public PublicKey getPublicKey() throws Exception {
        return SecurityUtils.publicKeyFromBase64(publicKey);
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return name + " (" + role + ", " + did + ")";
    }

    private static final long serialVersionUID = 202611180001L;
}
