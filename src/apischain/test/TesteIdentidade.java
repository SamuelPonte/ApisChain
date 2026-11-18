//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.identity.Credential;
import apischain.identity.CredentialType;
import apischain.identity.DigitalID;
import apischain.identity.OffChainStore;
import apischain.identity.Role;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Teste de consola da identidade e das credenciais (issue #1): chaves em
 * ficheiro, DigitalID, credenciais assinadas e armazém fora da cadeia.
 *
 * Usa uma pasta temporária, para não mexer na pasta data/ do projeto.
 */
public class TesteIdentidade {

    // número de verificações que falharam (no fim tem de ser 0)
    private static int falhas = 0;

    /**
     * Mostra o resultado de uma verificação e conta as falhas.
     */
    private static void check(String descricao, boolean ok) {
        System.out.println("   " + (ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }

    public static void main(String[] args) throws Exception {
        // pasta temporária só para este teste
        File pasta = Files.createTempDirectory("apischain-keys").toFile();
        String keys = pasta.getPath();

        // ------------------------------------------------------------------
        System.out.println("1. CRIAR INTERVENIENTES (chaves e identidade em ficheiro)");
        DigitalID dgav = DigitalID.create("DGAV", Role.DGAV, "dgav123", keys);
        DigitalID lab = DigitalID.create("Laboratorio", Role.LAB, "lab123", keys);
        DigitalID apicultor = DigitalID.create("Apicultor-0001", Role.APICULTOR, "mel123", keys);
        System.out.println("   " + dgav);
        System.out.println("   " + lab);
        System.out.println("   " + apicultor);
        check("ficheiros DGAV.pub, DGAV.priv e DGAV.id criados",
                new File(pasta, "DGAV.pub").exists()
                && new File(pasta, "DGAV.priv").exists()
                && new File(pasta, "DGAV.id").exists());
        check("o DID começa por did:apis: e cada interveniente tem um DID diferente",
                dgav.getDid().startsWith(DigitalID.DID_PREFIX)
                && !dgav.getDid().equals(lab.getDid()));
        check("a identidade é válida (DID e assinatura do próprio)", dgav.isValid());

        // ------------------------------------------------------------------
        System.out.println("\n2. ENTRAR COM A PALAVRA-PASSE (como no ecrã de entrada)");
        DigitalID lida = DigitalID.load("DGAV", keys);
        check("identidade carregada do ficheiro com o mesmo DID", lida.getDid().equals(dgav.getDid()));
        PrivateKey priv = DigitalID.loadPrivateKey("DGAV", "dgav123", keys);
        String assinatura = SecurityUtils.sign("teste", priv);
        check("a chave privada lida assina e verifica com a chave pública da identidade",
                SecurityUtils.verify("teste", assinatura, lida.getPublicKey()));
        check("palavra-passe errada é recusada",
                lancaExcecao(() -> DigitalID.loadPrivateKey("DGAV", "errada", keys)));
        check("interveniente que não existe é recusado",
                lancaExcecao(() -> DigitalID.load("Ninguem", keys)));

        // ------------------------------------------------------------------
        System.out.println("\n3. ATAQUE: TROCAR A CHAVE PÚBLICA DE UM INTERVENIENTE");
        // o atacante põe a sua chave pública no ficheiro da DGAV, para depois assinar em nome dela
        Files.copy(new File(pasta, "Apicultor-0001.pub").toPath(), new File(pasta, "DGAV.pub").toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        check("a identidade da DGAV deixa de carregar", lancaExcecao(() -> DigitalID.load("DGAV", keys)));

        // ------------------------------------------------------------------
        System.out.println("\n4. CREDENCIAL ASSINADA PELO EMISSOR");
        PrivateKey privLab = DigitalID.loadPrivateKey("Laboratorio", "lab123", keys);
        Map<String, String> dados = new HashMap<>();
        dados.put("lote", "LOTE-2026-0042");
        dados.put("origemFloral", "urze 62%");
        dados.put("humidade", "17.2%");
        Credential analise = new Credential("AN-0042", CredentialType.ANALISE_LOTE,
                lab.getDid(), "LOTE-2026-0042", dados);
        check("credencial por assinar é inválida", !analise.isSignatureValid(lab.getPublicKey()));
        analise.sign(privLab);
        System.out.println("   " + analise);
        check("assinatura válida com a chave do Laboratório", analise.isSignatureValid(lab.getPublicKey()));
        check("assinatura inválida com a chave de outro interveniente",
                !analise.isSignatureValid(apicultor.getPublicKey()));
        check("os carateres | ; = são recusados nos dados",
                lancaExcecao(() -> new Credential("X", CredentialType.LOTE, "a", "b", Map.of("kg", "340|999"))));

        // a ordem em que os dados são inseridos não muda o texto assinado
        Map<String, String> invertidos = new LinkedHashMap<>();
        invertidos.put("humidade", "17.2%");
        invertidos.put("origemFloral", "urze 62%");
        invertidos.put("lote", "LOTE-2026-0042");
        Credential outra = new Credential("AN-0042", CredentialType.ANALISE_LOTE,
                lab.getDid(), "LOTE-2026-0042", invertidos);
        String semData = analise.getDataToSign().replace("|" + analise.getIssuedAt() + "|", "|");
        String semDataOutra = outra.getDataToSign().replace("|" + outra.getIssuedAt() + "|", "|");
        check("os mesmos dados por outra ordem dão o mesmo texto", semData.equals(semDataOutra));

        // ------------------------------------------------------------------
        System.out.println("\n5. ARMAZÉM FORA DA CADEIA E HASH ANCORADO");
        OffChainStore store = new OffChainStore();
        store.put(analise);
        // este é o hash que a transação IssueCredentialTx guarda na cadeia
        String hashNaCadeia = analise.getHash();
        check("credencial guardada e lida pelo id", store.contains("AN-0042") && store.get("AN-0042") == analise);

        File ficheiro = new File(pasta, "offchain.dat");
        store.save(ficheiro.getPath());
        OffChainStore copia = new OffChainStore();
        copia.load(ficheiro.getPath());
        Credential gravada = copia.get("AN-0042");
        check("depois de gravar e carregar: mesmo hash e assinatura válida",
                copia.getSize() == 1 && gravada.getHash().equals(hashNaCadeia)
                && gravada.isSignatureValid(lab.getPublicKey()));

        // ------------------------------------------------------------------
        System.out.println("\n6. FRAUDE 2: ALTERAR A ANÁLISE FORA DA CADEIA");
        store.tamper("AN-0042", "origemFloral", "urze 95%");
        System.out.println("   " + store.get("AN-0042"));
        check("o hash deixa de coincidir com o ancorado na cadeia",
                !store.get("AN-0042").getHash().equals(hashNaCadeia));
        check("a assinatura do Laboratório deixa de ser válida",
                !store.get("AN-0042").isSignatureValid(lab.getPublicKey()));

        // ------------------------------------------------------------------
        System.out.println();
        if (falhas == 0) {
            System.out.println("TODAS AS VERIFICAÇÕES PASSARAM");
        } else {
            System.out.println(falhas + " VERIFICAÇÕES FALHARAM");
            System.exit(1);
        }
    }

    /**
     * Interface para passar um bloco de código que pode lançar exceção.
     */
    private interface Codigo {
        void run() throws Exception;
    }

    /**
     * @return true se o código lançar uma exceção
     */
    private static boolean lancaExcecao(Codigo codigo) {
        try {
            codigo.run();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
