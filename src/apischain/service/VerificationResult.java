//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado da verificação de um frasco (o que o consumidor vê ao ler o QR).
 *
 * Tem um nível geral e a lista de verificações feitas, cada uma com o seu
 * nível. O nível geral é o pior de todos: basta uma FALHA para o frasco falhar.
 */
public class VerificationResult {

    /**
     * Níveis, do melhor para o pior (a ordem conta: ver add).
     */
    public enum Level {
        OK,     // tudo certo
        AVISO,  // não é fraude, mas falta alguma coisa (por exemplo, sem certificado DOP)
        FALHA   // fraude ou dados que não batem certo
    }

    private final String jarId;                          // frasco verificado
    private Level level = Level.OK;                      // nível geral
    private final List<String> checks = new ArrayList<>();  // verificações, já com o nível à frente

    public VerificationResult(String jarId) {
        this.jarId = jarId;
    }

    /**
     * Regista uma verificação. O nível geral só piora, nunca melhora.
     *
     * @param l nível desta verificação
     * @param text descrição
     */
    public void add(Level l, String text) {
        checks.add(l + ": " + text);
        // compareTo usa a ordem do enum: OK < AVISO < FALHA
        if (l.compareTo(level) > 0) {
            level = l;
        }
    }

    public void ok(String text) {
        add(Level.OK, text);
    }

    public void warn(String text) {
        add(Level.AVISO, text);
    }

    public void fail(String text) {
        add(Level.FALHA, text);
    }

    public Level getLevel() {
        return level;
    }

    /**
     * @return frase curta para o topo do ecrã do consumidor
     */
    public String getTitle() {
        switch (level) {
            case OK:
                return "Frasco " + jarId + " autêntico";
            case AVISO:
                return "Frasco " + jarId + " verificado com avisos";
            default:
                return "Frasco " + jarId + ": NÃO CONFIAR";
        }
    }

    public String getJarId() {
        return jarId;
    }

    public List<String> getChecks() {
        return Collections.unmodifiableList(checks);
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder(getTitle());
        for (String c : checks) {
            txt.append("\n   ").append(c);
        }
        return txt.toString();
    }
}
