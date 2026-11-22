//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.ledger;

/**
 * Balanço de massa de um lote: quantos kg foram colhidos, quantos já foram
 * embalados em frascos e quantos ainda estão livres.
 */
public class LotBalance {

    private final double harvestedKg;  // kg colhidos (da credencial LOTE)
    private final double packedKg;     // kg já embalados (soma das PackTx do lote)

    public LotBalance(double harvestedKg, double packedKg) {
        this.harvestedKg = harvestedKg;
        this.packedKg = packedKg;
    }

    public double getHarvestedKg() {
        return harvestedKg;
    }

    public double getPackedKg() {
        return packedKg;
    }

    /**
     * @return kg ainda por embalar (sem contar com a tolerância)
     */
    public double getFreeKg() {
        return harvestedKg - packedKg;
    }

    @Override
    public String toString() {
        return String.format("colhidos %.2f kg, embalados %.2f kg, livres %.2f kg",
                harvestedKg, packedKg, getFreeKg());
    }
}
