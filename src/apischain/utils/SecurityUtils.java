//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em utils.SecurityUtils do Templar2026 ver2/DigitalID        ::://
//::  (Prof. António Manso, 2026)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.utils;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Funções criptográficas usadas em todo o sistema: hash SHA3-256,
 * assinaturas digitais RSA (SHA3-256withRSA) e gravação das chaves em
 * ficheiro, com a chave privada cifrada por palavra-passe (AES-GCM + PBKDF2),
 * tal como no Templar2026 do professor.
 */
public class SecurityUtils {

    // algoritmo de hash usado nos blocos, nas transações e na árvore de Merkle
    public static final String HASH_ALGORITHM = "SHA3-256";
    // algoritmo de assinatura: calcula o SHA3-256 dos dados e cifra-o com a chave privada RSA
    public static final String SIGN_ALGORITHM = "SHA3-256withRSA";
    // tamanho das chaves RSA em bits
    public static final int RSA_KEY_SIZE = 2048;

    /**
     * Calcula o hash SHA3-256 de um texto, codificado em Base64 (como nas aulas).
     *
     * @param data texto a resumir
     * @return hash em Base64 (44 carateres)
     */
    public static String getHash(String data) {
        try {
            // 1. obter o objeto que implementa o SHA3-256
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            // 2. converter o texto em bytes e calcular o resumo (32 bytes)
            byte[] hash = md.digest(data.getBytes("UTF-8"));
            // 3. codificar os 32 bytes em Base64 para poder guardar como texto
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            // o SHA3-256 existe em qualquer JDK a partir do 9, por isso isto não deve acontecer
            throw new RuntimeException("Erro no hash: " + ex.getMessage());
        }
    }

    /**
     * Gera um novo par de chaves RSA (pública e privada).
     *
     * @return par de chaves
     * @throws Exception se o algoritmo RSA não existir
     */
    public static KeyPair generateKeyPair() throws Exception {
        // 1. obter o gerador de chaves RSA
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        // 2. definir o tamanho da chave
        gen.initialize(RSA_KEY_SIZE);
        // 3. gerar o par (a privada fica com o dono, a pública é partilhada)
        return gen.generateKeyPair();
    }

    /**
     * Assina um texto com a chave privada.
     *
     * @param data texto a assinar
     * @param key chave privada do assinante
     * @return assinatura em Base64
     * @throws Exception se a chave for inválida
     */
    public static String sign(String data, PrivateKey key) throws Exception {
        // 1. obter o objeto de assinatura SHA3-256withRSA
        Signature shaRsa = Signature.getInstance(SIGN_ALGORITHM);
        // 2. preparar a assinatura com a chave privada
        shaRsa.initSign(key);
        // 3. fornecer os dados a assinar
        shaRsa.update(data.getBytes("UTF-8"));
        // 4. assinar e codificar o resultado em Base64
        return Base64.getEncoder().encodeToString(shaRsa.sign());
    }

    /**
     * Verifica se uma assinatura corresponde ao texto e à chave pública.
     *
     * @param data texto original
     * @param signature assinatura em Base64
     * @param key chave pública do suposto assinante
     * @return true se a assinatura for válida
     */
    public static boolean verify(String data, String signature, PublicKey key) {
        try {
            // 1. obter o objeto de assinatura SHA3-256withRSA
            Signature shaRsa = Signature.getInstance(SIGN_ALGORITHM);
            // 2. preparar a verificação com a chave pública
            shaRsa.initVerify(key);
            // 3. fornecer os dados originais
            shaRsa.update(data.getBytes("UTF-8"));
            // 4. comparar com a assinatura recebida
            return shaRsa.verify(Base64.getDecoder().decode(signature));
        } catch (Exception ex) {
            // assinatura mal formada, chave errada, etc.: não é válida
            return false;
        }
    }

    /**
     * Converte uma chave em texto Base64 (para guardar na blockchain).
     *
     * @param key chave pública ou privada
     * @return chave em Base64
     */
    public static String keyToBase64(Key key) {
        // getEncoded devolve a chave no formato normalizado (X.509 para a pública)
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Reconstrói uma chave pública RSA a partir do texto Base64.
     *
     * @param txt chave pública em Base64
     * @return chave pública
     * @throws Exception se o texto não for uma chave válida
     */
    public static PublicKey publicKeyFromBase64(String txt) throws Exception {
        // 1. descodificar o Base64 para obter os bytes da chave
        byte[] bytes = Base64.getDecoder().decode(txt);
        // 2. a chave pública está no formato X.509
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        // 3. pedir à fábrica de chaves RSA que reconstrua a chave
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  ASSINATURAS COM BYTES (mesma assinatura de métodos do professor)   ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Assina bytes com a chave privada (versão do Templar2026).
     *
     * @param data dados
     * @param key chave privada
     * @return assinatura
     * @throws Exception se a chave for inválida
     */
    public static byte[] sign(byte[] data, PrivateKey key) throws Exception {
        Signature sign = Signature.getInstance(SIGN_ALGORITHM);
        // inicializa a assinatura com a chave privada
        sign.initSign(key);
        // fornece os dados a assinar
        sign.update(data);
        // devolve a assinatura
        return sign.sign();
    }

    /**
     * Verifica uma assinatura em bytes (versão do Templar2026).
     *
     * @param data dados originais
     * @param signature assinatura
     * @param key chave pública
     * @return true se for válida
     * @throws Exception se a chave for inválida
     */
    public static boolean verifySign(byte[] data, byte[] signature, PublicKey key) throws Exception {
        Signature sign = Signature.getInstance(SIGN_ALGORITHM);
        // inicializa a verificação com a chave pública
        sign.initVerify(key);
        // fornece os dados e compara com a assinatura
        sign.update(data);
        return sign.verify(signature);
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  CHAVES EM FICHEIRO (copiado do Templar2026 ver2 do professor)       ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * Grava a chave pública em claro (formato X.509), no ficheiro nome.pub.
     *
     * @param publicKey chave pública
     * @param filePath caminho do ficheiro (acrescenta .pub se faltar)
     * @throws Exception erro de escrita
     */
    public static void savePublicKey(PublicKey publicKey, String filePath) throws Exception {
        if (!filePath.endsWith(".pub")) {
            filePath += ".pub";
        }
        Files.write(Paths.get(filePath), publicKey.getEncoded());
    }

    /**
     * Grava a chave privada (PKCS#8) CIFRADA com a palavra-passe, no ficheiro nome.priv.
     * Assim, quem copiar o ficheiro não consegue usar a chave sem a palavra-passe.
     *
     * @param privateKey chave privada
     * @param filePath caminho do ficheiro (acrescenta .priv se faltar)
     * @param password palavra-passe do dono
     * @throws Exception erro de cifra ou de escrita
     */
    public static void savePrivateKey(PrivateKey privateKey, String filePath, String password) throws Exception {
        if (!filePath.endsWith(".priv")) {
            filePath += ".priv";
        }
        byte[] encrypted = encrypt(privateKey.getEncoded(), password);
        Files.write(Paths.get(filePath), encrypted);
    }

    /**
     * Lê a chave pública de um ficheiro .pub.
     *
     * @param filePath caminho do ficheiro
     * @return chave pública
     * @throws Exception se o ficheiro não existir ou estiver corrompido
     */
    public static PublicKey loadPublicKey(String filePath) throws Exception {
        if (!filePath.endsWith(".pub")) {
            filePath += ".pub";
        }
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    /**
     * Lê a chave privada de um ficheiro .priv, decifrando-a com a palavra-passe.
     *
     * @param filePath caminho do ficheiro
     * @param password palavra-passe
     * @return chave privada
     * @throws Exception se a palavra-passe estiver errada (AEADBadTagException)
     */
    public static PrivateKey loadPrivateKey(String filePath, String password) throws Exception {
        if (!filePath.endsWith(".priv")) {
            filePath += ".priv";
        }
        byte[] decrypted = decrypt(Files.readAllBytes(Paths.get(filePath)), password);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decrypted));
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    //::  CIFRA POR PALAVRA-PASSE (copiado do Templar2026 ver2 do professor)  ::
    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private static final int SALT_LENGTH = 16;          // sal aleatório de 128 bits
    private static final int IV_LENGTH = 12;            // vetor inicial de 96 bits (recomendado no GCM)
    private static final int TAG_LENGTH_BITS = 128;     // etiqueta de autenticação do GCM
    private static final int KEY_LENGTH_BITS = 256;     // chave AES de 256 bits
    private static final int PBKDF2_ITERATIONS = 600_000; // iterações recomendadas pela OWASP
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Cifra bytes com uma chave AES derivada da palavra-passe.
     * Resultado: sal || iv || texto cifrado com etiqueta.
     *
     * @param data dados em claro
     * @param password palavra-passe
     * @return dados cifrados
     * @throws Exception erro de cifra
     */
    public static byte[] encrypt(byte[] data, String password) throws Exception {
        // 1. sal e iv aleatórios novos em cada cifra
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(iv);
        // 2. derivar a chave AES a partir da palavra-passe e do sal
        SecretKey key = deriveKey(password, salt);
        // 3. cifrar em AES-GCM (o GCM também garante que ninguém alterou os dados)
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(data);
        // 4. juntar sal, iv e texto cifrado num só array
        return ByteBuffer.allocate(salt.length + iv.length + ciphertext.length)
                .put(salt).put(iv).put(ciphertext).array();
    }

    /**
     * Decifra dados produzidos por encrypt.
     *
     * @param encrypted sal || iv || texto cifrado
     * @param password palavra-passe
     * @return dados em claro
     * @throws Exception se a palavra-passe estiver errada ou os dados alterados
     */
    public static byte[] decrypt(byte[] encrypted, String password) throws Exception {
        // 1. separar sal, iv e texto cifrado
        ByteBuffer buffer = ByteBuffer.wrap(encrypted);
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(salt);
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        // 2. derivar a mesma chave com o mesmo sal
        SecretKey key = deriveKey(password, salt);
        // 3. decifrar (falha se a palavra-passe estiver errada)
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    /**
     * Deriva uma chave AES de 256 bits a partir da palavra-passe (PBKDF2-HMAC-SHA256).
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(keyBytes, "AES");
            } finally {
                // apagar a chave da memória assim que deixa de ser precisa
                Arrays.fill(keyBytes, (byte) 0);
            }
        } finally {
            spec.clearPassword();
        }
    }
}
