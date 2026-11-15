//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Baseado em utils.Conversion do Templar2026 (Prof. António Manso)      ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

/**
 * Converte objetos em texto Base64 e vice-versa. É assim que as transações
 * são guardadas dentro dos blocos (tal como no Templar2026).
 */
public class Conversion {

    /**
     * Serializa um objeto e codifica-o em Base64.
     *
     * @param object objeto serializável
     * @return objeto em texto Base64
     * @throws Exception se o objeto não for serializável
     */
    public static String objectToString(Serializable object) throws Exception {
        // 1. criar um fluxo de bytes em memória
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // 2. escrever o objeto nesse fluxo (serialização)
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        // 3. converter os bytes em texto Base64
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /**
     * Descodifica um texto Base64 e reconstrói o objeto.
     *
     * @param <T> tipo esperado
     * @param base64 texto produzido por objectToString (mesmos nomes do Templar2026)
     * @return objeto reconstruído
     * @throws Exception se o texto estiver corrompido
     */
    @SuppressWarnings("unchecked")
    public static <T> T stringToObject(String base64) throws Exception {
        // 1. descodificar o Base64 para obter os bytes
        byte[] data = Base64.getDecoder().decode(base64);
        // 2. ler o objeto a partir dos bytes (desserialização)
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (T) in.readObject();
        }
    }
}
