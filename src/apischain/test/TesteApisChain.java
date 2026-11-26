//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.consensus.ProofOfAuthority;
import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.identity.CredentialType;
import apischain.identity.DigitalID;
import apischain.identity.Role;
import apischain.service.ApisChain;
import apischain.service.ApisChainServices;
import apischain.utils.SecurityUtils;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;

/**
 * Teste de consola da ApisChain: blockchain, consenso PoA e serviço
 * (emitir, revogar e verificar credenciais).
 *
 * Usa uma pasta temporária, para não mexer na pasta data/ do projeto.
 */
public class TesteApisChain {

    // número de verificações que falharam (no fim tem de ser 0)
    private static int falhas = 0;

    private static void check(String descricao, boolean ok) {
        System.out.println("   " + (ok ? "OK     " : "FALHOU ") + descricao);
        if (!ok) {
            falhas++;
        }
    }

    public static void main(String[] args) throws Exception {
        String pasta = Files.createTempDirectory("apischain").toString();

        // ------------------------------------------------------------------
        System.out.println("1. ABRIR: AUTORIDADES E BLOCO DE GÉNESE");
        ApisChain apis = new ApisChain(pasta);
        String keys = apis.getKeysFolder();
        DigitalID dgav = DigitalID.load(ApisChain.DGAV, keys);
        DigitalID lab = DigitalID.load(ApisChain.LAB, keys);
        DigitalID dop = DigitalID.load(ApisChain.DOP, keys);
        PrivateKey kDgav = DigitalID.loadPrivateKey(ApisChain.DGAV, ApisChain.PASSWORD, keys);
        PrivateKey kLab = DigitalID.loadPrivateKey(ApisChain.LAB, ApisChain.PASSWORD, keys);
        PrivateKey kDop = DigitalID.loadPrivateKey(ApisChain.DOP, ApisChain.PASSWORD, keys);
        BlockChain chain = apis.getChain();
        check("a cadeia tem o bloco de génese", chain.getSize() == 1);
        check("3 autoridades na génese, quórum 2",
                ApisChainServices.getAuthorities(chain).size() == 3 && ProofOfAuthority.quorum(3) == 2);
        check("a génese tem as 3 assinaturas",
                ProofOfAuthority.countSignatures(chain.get(0), ApisChainServices.getAuthorities(chain)) == 3);

        // ------------------------------------------------------------------
        System.out.println("\n2. REGISTAR UM APICULTOR");
        DigitalID manuel = DigitalID.create("Manuel", Role.APICULTOR, "mel123", keys);
        PrivateKey kManuel = DigitalID.loadPrivateKey("Manuel", "mel123", keys);
        String ap = apis.issue(dgav, kDgav, CredentialType.REGISTO_APICULTOR, manuel.getDid(),
                manuel.getPublicKeyBase64(), Map.of("nome", "Manuel Ferreira (fictício)", "nif", "999999990"));
        check("a DGAV regista o Manuel (" + ap + ")", ap.equals("AP-0001"));
        check("o Manuel passa a ter o papel APICULTOR",
                "APICULTOR".equals(ApisChainServices.getRole(manuel.getDid(), chain)));
        check("registar o mesmo DID outra vez é recusado", recusa(() -> apis.issue(dgav, kDgav,
                CredentialType.REGISTO_APICULTOR, manuel.getDid(), manuel.getPublicKeyBase64(), Map.of())));
        check("o Laboratório não pode registar apicultores", recusa(() -> apis.issue(lab, kLab,
                CredentialType.REGISTO_APICULTOR, "did:apis:x", manuel.getPublicKeyBase64(), Map.of())));
        KeyPair outra = SecurityUtils.generateKeyPair();
        check("registo com um DID que não é o da chave é recusado", recusa(() -> apis.issue(dgav, kDgav,
                CredentialType.REGISTO_APICULTOR, manuel.getDid() + "x", SecurityUtils.keyToBase64(outra.getPublic()), Map.of())));

        // ------------------------------------------------------------------
        System.out.println("\n3. APIÁRIO, LOTE, ANÁLISE E CERTIFICADO");
        String apiario = apis.issue(dgav, kDgav, CredentialType.REGISTO_APIARIO, manuel.getDid(), "",
                Map.of("local", "Serra de Tomar", "coordenadas", "39.60, -8.41 (fictícias)"));
        String lote = apis.issue(manuel, kManuel, CredentialType.LOTE, apiario, "",
                Map.of("origemFloral", "urze", "kg", "340"));
        String analise = apis.issue(lab, kLab, CredentialType.ANALISE_LOTE, lote, "",
                Map.of("origemFloral", "urze 62%", "humidade", "17.2%"));
        String cert = apis.issue(dop, kDop, CredentialType.CERTIFICADO_ORIGEM, lote, "",
                Map.of("denominacao", "Mel da Serra de Tomar DOP (fictícia)"));
        check("ids gerados: " + apiario + ", " + lote + ", " + analise + ", " + cert,
                lote.equals("LOTE-0001") && analise.equals("AN-0001") && cert.equals("DOP-0001"));
        check("o apicultor não pode emitir análises", recusa(() -> apis.issue(manuel, kManuel,
                CredentialType.ANALISE_LOTE, lote, "", Map.of())));
        check("credencial assinada com a chave de outro é recusada", recusa(() -> apis.issue(lab, kDop,
                CredentialType.ANALISE_LOTE, lote, "", Map.of())));

        // ------------------------------------------------------------------
        System.out.println("\n4. CONSENSO: ROTAÇÃO DO PROPONENTE E ASSINATURAS");
        List<String> proponentes = List.of(chain.get(1).getValidator(), chain.get(2).getValidator(),
                chain.get(3).getValidator());
        check("os blocos 1, 2 e 3 foram propostos por LAB, DOP e DGAV",
                proponentes.equals(List.of(lab.getDid(), dop.getDid(), dgav.getDid())));
        boolean todos = true;
        for (Block b : chain.getChain()) {
            todos &= ProofOfAuthority.countSignatures(b, ApisChainServices.getAuthorities(chain)) == 3;
        }
        check("todos os blocos têm as assinaturas das 3 autoridades", todos);
        Block falso = new Block(chain.getSize(), "hash-inventado", "x", dgav.getDid());
        check("um bloco que não aponta para o último é recusado pela cadeia", !chain.add(falso));

        // ------------------------------------------------------------------
        System.out.println("\n5. VERIFICAR O LOTE (CONSUMIDOR)");
        List<String> r = apis.verify(lote);
        r.forEach(l -> System.out.println("      " + l));
        check("lote autêntico, com lote, análise e certificado verificados",
                r.get(0).startsWith("AUTÊNTICO") && r.size() == 4);
        check("um lote que não existe não é de confiar", apis.verify("LOTE-9999").get(0).startsWith("NÃO CONFIAR"));

        // ------------------------------------------------------------------
        System.out.println("\n6. ANÁLISE ALTERADA FORA DA CADEIA");
        apis.getOffChain().tamper(analise, "origemFloral", "urze 95%");
        r = apis.verify(lote);
        check("o hash deixa de coincidir: " + r.get(2), r.get(0).startsWith("NÃO CONFIAR") && r.get(2).contains("ALTERADA"));
        apis.getOffChain().tamper(analise, "origemFloral", "urze 62%");
        check("reposta a análise, volta a ser autêntico", apis.verify(lote).get(0).startsWith("AUTÊNTICO"));

        // ------------------------------------------------------------------
        System.out.println("\n7. REVOGAÇÃO");
        check("o Laboratório não pode revogar", recusa(() -> apis.revoke(lab, kLab, cert, "teste")));
        apis.revoke(dgav, kDgav, cert, "certificado emitido por engano");
        r = apis.verify(lote);
        check("certificado revogado: " + r.get(3), r.get(0).startsWith("NÃO CONFIAR") && r.get(3).contains("revogada"));
        check("revogar duas vezes é recusado", recusa(() -> apis.revoke(dgav, kDgav, cert, "outra vez")));

        // ------------------------------------------------------------------
        System.out.println("\n8. GRAVAR E VOLTAR A ABRIR");
        int blocos = chain.getSize();
        ApisChain outraVez = new ApisChain(pasta);
        check("a cadeia é carregada do ficheiro (" + blocos + " blocos) e está íntegra",
                outraVez.getChain().getSize() == blocos && outraVez.getChain().isValid());
        check("a análise continua verificável depois de reabrir",
                outraVez.verify(analise).get(0).startsWith("AUTÊNTICO"));

        // ------------------------------------------------------------------
        System.out.println();
        if (falhas == 0) {
            System.out.println("TODAS AS VERIFICAÇÕES PASSARAM");
        } else {
            System.out.println(falhas + " VERIFICAÇÕES FALHARAM");
            System.exit(1);
        }
    }

    private interface Codigo {
        void run() throws Exception;
    }

    /**
     * @return true se o código lançar uma exceção (a operação foi recusada)
     */
    private static boolean recusa(Codigo codigo) {
        try {
            codigo.run();
            return false;
        } catch (Exception e) {
            System.out.println("      recusado: " + e.getMessage());
            return true;
        }
    }
}
