//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em templarCoin.core.Transaction do Templar2026_DigitalID      ::://
//::  (Prof. António Manso)                                                 ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

import apischain.utils.Conversion;
import apischain.utils.SecurityUtils;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Transação da ApisChain: um facto assinado pelo seu autor.
 *
 * Tudo o que entra na cadeia é uma transação. Cada tipo de facto (registo de
 * credencial, revogação, embalagem, venda, alerta) é uma subclasse que diz
 * quais são os seus campos através de getContent().
 *
 * Diferenças face à Transaction do professor:
 * 1. é abstrata: os campos comuns (autor, data, assinatura) ficam aqui e cada
 *    subclasse acrescenta os seus;
 * 2. a chave pública do autor NÃO vai dentro da transação. Quem verifica tem de
 *    a ir buscar à cadeia (LedgerState, issue #3). No código do professor a
 *    chave vinha na própria transação, e um atacante podia assinar em nome de
 *    outro com as suas próprias chaves;
 * 3. os campos assinados são separados por "|", para "ab"+"c" e "a"+"bc" não
 *    darem o mesmo texto. Para o separador funcionar, nenhum campo de texto
 *    pode conter "|" (ver checkText): senão "A|motivo=x" + "y" e "A" + "x|motivo=y"
 *    voltavam a dar o mesmo texto assinado;
 * 4. tem data (timestamp), para duas transações iguais não serem indistinguíveis;
 * 5. o texto assinado começa pelo nome da classe, para a assinatura de um tipo
 *    não servir para outro tipo com os mesmos valores.
 *
 * Uso: criar a transação (new SaleTx(...)) e só DEPOIS chamar sign(chave).
 * Não se assina no construtor porque, quando o construtor desta classe corre,
 * os campos da subclasse ainda não estão preenchidos.
 */
public abstract class Transaction implements Serializable {

    private final String author;    // DID de quem declara o facto (e assina)
    private final long timestamp;   // data de criação em milissegundos
    private String signature;       // assinatura do autor em Base64 ("" se ainda não assinada)

    /**
     * Construtor usado pelas subclasses.
     *
     * @param author DID do autor
     */
    protected Transaction(String author) {
        this.author = checkText(author, "autor");
        this.timestamp = System.currentTimeMillis();
        this.signature = "";
    }

    /**
     * Valida um campo de texto antes de o guardar: não pode ser null nem conter
     * o separador "|". As subclasses chamam-no no construtor para cada campo de texto.
     *
     * @param value valor do campo
     * @param name nome do campo (para a mensagem de erro)
     * @return o próprio valor, para se poder escrever this.x = checkText(x, "x")
     */
    protected static String checkText(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException("O campo " + name + " não pode ser null");
        }
        if (value.contains("|")) {
            throw new IllegalArgumentException("O campo " + name + " não pode conter o separador |");
        }
        return value;
    }

    /**
     * Campos próprios da subclasse, num texto determinista separado por "|".
     * Determinista quer dizer: os mesmos valores dão sempre o mesmo texto.
     *
     * @return campos da subclasse
     */
    public abstract String getContent();

    /**
     * Texto que é assinado: tipo, autor, data e campos da subclasse.
     * Um campo que não esteja aqui pode ser alterado sem que a assinatura falhe.
     *
     * @return texto a assinar
     */
    public String getDataToSign() {
        return getClass().getSimpleName() + "|" + author + "|" + timestamp + "|" + getContent();
    }

    /**
     * Assina a transação com a chave privada do autor (SHA3-256withRSA).
     *
     * @param key chave privada do autor
     * @throws Exception se a chave for inválida
     */
    public void sign(PrivateKey key) throws Exception {
        this.signature = SecurityUtils.sign(getDataToSign(), key);
    }

    /**
     * Verifica a assinatura com a chave pública do autor.
     *
     * @param key chave pública do autor, obtida da cadeia (não da transação)
     * @return true se a transação foi assinada por essa chave e não foi alterada
     */
    public boolean isSignatureValid(PublicKey key) {
        // uma transação por assinar nunca é válida
        if (signature.isEmpty()) {
            return false;
        }
        return SecurityUtils.verify(getDataToSign(), signature, key);
    }

    /**
     * Converte a transação em texto Base64, que é o que se guarda no bloco.
     *
     * @return transação em Base64
     * @throws Exception se a serialização falhar
     */
    public String toBase64() throws Exception {
        return Conversion.objectToString(this);
    }

    /**
     * Reconstrói uma transação a partir do texto Base64 guardado no bloco.
     *
     * @param txt transação em Base64
     * @return transação (usar instanceof para saber o tipo)
     * @throws Exception se o texto não for uma transação
     */
    public static Transaction fromBase64(String txt) throws Exception {
        return Conversion.stringToObject(txt);
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{autor=" + author + ", " + getContent()
                + (signature.isEmpty() ? ", por assinar}" : ", assinada}");
    }

    private static final long serialVersionUID = 202611160001L;
}
