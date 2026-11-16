//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.transactions;

/**
 * Embalagem de parte de um lote em frascos numerados de firstJar a lastJar.
 *
 * O PESO É POR FRASCO (como no protótipo ApisChain.html, que embala N frascos
 * de X kg). Exemplo: frascos 1 a 200 de 0.5 kg dão 200 frascos e 100 kg.
 * As regras (issue #3) somam getTotalKg() de todas as embalagens do lote e
 * rejeitam se passar os kg colhidos mais 2% (balanço de massa).
 *
 * O código de cada frasco é o lote seguido do número: LOTE-2026-0042-137.
 */
public class PackTx extends Transaction {

    private final String lot;        // id da credencial do lote
    private final int firstJar;      // número do primeiro frasco
    private final int lastJar;       // número do último frasco (inclusive)
    private final double weightKg;   // peso de CADA frasco em kg

    /**
     * @param author DID do apicultor dono do lote
     * @param lot id do lote
     * @param firstJar número do primeiro frasco
     * @param lastJar número do último frasco (inclusive)
     * @param weightKg peso de cada frasco em kg
     */
    public PackTx(String author, String lot, int firstJar, int lastJar, double weightKg) {
        super(author);
        // só se verifica se os números fazem sentido; o balanço de massa é das regras (#3)
        if (firstJar < 1 || lastJar < firstJar) {
            throw new IllegalArgumentException("Intervalo de frascos inválido: " + firstJar + " a " + lastJar);
        }
        if (weightKg <= 0) {
            throw new IllegalArgumentException("O peso de cada frasco tem de ser positivo");
        }
        this.lot = lot;
        this.firstJar = firstJar;
        this.lastJar = lastJar;
        this.weightKg = weightKg;
    }

    @Override
    public String getContent() {
        return "lote=" + lot + "|de=" + firstJar + "|ate=" + lastJar + "|kgPorFrasco=" + weightKg;
    }

    /**
     * @return quantos frascos esta embalagem cria
     */
    public int getJarCount() {
        return lastJar - firstJar + 1;
    }

    /**
     * @return kg totais embalados (frascos vezes peso de cada um)
     */
    public double getTotalKg() {
        return getJarCount() * weightKg;
    }

    /**
     * @param number número do frasco
     * @return true se o frasco pertence a esta embalagem
     */
    public boolean containsJar(int number) {
        return number >= firstJar && number <= lastJar;
    }

    /**
     * @param number número do frasco
     * @return código do frasco (o que vai no QR)
     */
    public String getJarId(int number) {
        return lot + "-" + number;
    }

    public String getLot() {
        return lot;
    }

    public int getFirstJar() {
        return firstJar;
    }

    public int getLastJar() {
        return lastJar;
    }

    public double getWeightKg() {
        return weightKg;
    }

    private static final long serialVersionUID = 202611160004L;
}
