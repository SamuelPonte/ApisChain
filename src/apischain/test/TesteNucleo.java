//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.test;

import apischain.core.Block;
import apischain.core.BlockChain;
import apischain.core.MerkleTree;
import apischain.core.Vote;
import apischain.utils.SecurityUtils;
import java.io.File;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;

/**
 * Teste de consola do núcleo (semana 1): hash, cadeia, Merkle, assinaturas RSA,
 * deteção de adulteração e gravação em ficheiro.
 */
public class TesteNucleo {

    public static void main(String[] args) throws Exception {
        // ------------------------------------------------------------------
        System.out.println("1. HASH SHA3-256 (Base64)");
        // o mesmo texto dá sempre o mesmo hash; uma letra diferente muda tudo (efeito avalanche)
        System.out.println("   mel  -> " + SecurityUtils.getHash("mel"));
        System.out.println("   mel. -> " + SecurityUtils.getHash("mel."));

        // ------------------------------------------------------------------
        System.out.println("\n2. CHAVES RSA DE DUAS AUTORIDADES");
        KeyPair dgav = SecurityUtils.generateKeyPair();
        KeyPair lab = SecurityUtils.generateKeyPair();
        System.out.println("   chave pública da DGAV: "
                + SecurityUtils.keyToBase64(dgav.getPublic()).substring(0, 40) + "…");

        // ------------------------------------------------------------------
        System.out.println("\n3. CONSTRUIR A CADEIA");
        BlockChain bc = new BlockChain();
        // bloco de génese: na versão final terá a lista das autoridades
        Block genesis = new Block(0, bc.getLastBlockHash(), Arrays.asList("GENESE: DGAV, LAB, DOP"), "DGAV");
        genesis.setSignature(SecurityUtils.sign(genesis.getCurrentHash(), dgav.getPrivate()));
        System.out.println("   génese aceite: " + bc.add(genesis));
        // bloco 1 com quatro transações de exemplo
        List<String> txs = Arrays.asList("REGISTO apicultor A", "REGISTO apiario 117",
                "LOTE 0042 340kg", "ANALISE 0042");
        Block b1 = new Block(1, bc.getLastBlockHash(), txs, "LAB");
        // o proponente assina o hash do bloco e outra autoridade vota (assina também)
        b1.setSignature(SecurityUtils.sign(b1.getCurrentHash(), lab.getPrivate()));
        b1.addVote(new Vote("DGAV", SecurityUtils.sign(b1.getCurrentHash(), dgav.getPrivate())));
        System.out.println("   bloco 1 aceite: " + bc.add(b1));
        // bloco 2
        Block b2 = new Block(2, bc.getLastBlockHash(), Arrays.asList("VENDA frasco 137"), "DGAV");
        System.out.println("   bloco 2 aceite: " + bc.add(b2));
        // tentativa de meter um bloco que não aponta para o último
        Block falso = new Block(3, "hash-inventado", Arrays.asList("VENDA frasco 999"), "DGAV");
        System.out.println("   bloco com hash anterior errado aceite: " + bc.add(falso));
        System.out.print(bc);
        System.out.println("   cadeia íntegra: " + bc.isValid());

        // ------------------------------------------------------------------
        System.out.println("\n4. ASSINATURAS DO BLOCO 1");
        System.out.println("   assinatura do LAB válida: "
                + SecurityUtils.verify(b1.getCurrentHash(), b1.getSignature(), lab.getPublic()));
        System.out.println("   voto da DGAV válido: "
                + SecurityUtils.verify(b1.getCurrentHash(), b1.getVotes().get(0).getSignature(), dgav.getPublic()));
        System.out.println("   assinatura do LAB verificada com a chave da DGAV: "
                + SecurityUtils.verify(b1.getCurrentHash(), b1.getSignature(), dgav.getPublic()));

        // ------------------------------------------------------------------
        System.out.println("\n5. ÁRVORE DE MERKLE DO BLOCO 1");
        MerkleTree mt = b1.getMerkleTree();
        System.out.print(mt);
        System.out.println("   árvore íntegra: " + mt.isValid());
        List<String> prova = mt.getProof("LOTE 0042 340kg");
        System.out.println("   prova de 'LOTE 0042 340kg' tem " + prova.size() + " hashes, válida: "
                + MerkleTree.isProofValid("LOTE 0042 340kg", prova));
        System.out.println("   a mesma prova para 'LOTE 0042 999kg': "
                + MerkleTree.isProofValid("LOTE 0042 999kg", prova));
        // árvore com número ímpar de folhas (o último sobe sozinho)
        MerkleTree impar = new MerkleTree(Arrays.asList("a", "b", "c", "d", "e"));
        System.out.println("   prova de 'e' numa árvore de 5 folhas: "
                + MerkleTree.isProofValid("e", impar.getProof("e")));

        // ------------------------------------------------------------------
        System.out.println("\n6. GRAVAR E CARREGAR");
        File f = File.createTempFile("apischain", ".bc");
        bc.save(f.getPath());
        BlockChain copia = new BlockChain();
        copia.load(f.getPath());
        System.out.println("   carregados " + copia.getSize() + " blocos, íntegra: " + copia.isValid());

        // ------------------------------------------------------------------
        System.out.println("\n7. CHAVES EM FICHEIRO PROTEGIDAS POR PALAVRA-PASSE");
        String base = new File(System.getProperty("java.io.tmpdir"), "DGAV").getPath();
        // a pública fica em claro (DGAV.pub); a privada fica cifrada (DGAV.priv)
        SecurityUtils.savePublicKey(dgav.getPublic(), base);
        SecurityUtils.savePrivateKey(dgav.getPrivate(), base, "abelha123");
        java.security.PrivateKey lida = SecurityUtils.loadPrivateKey(base, "abelha123");
        String teste = SecurityUtils.sign("teste", lida);
        System.out.println("   chave lida com a palavra-passe certa assina bem: "
                + SecurityUtils.verify("teste", teste, SecurityUtils.loadPublicKey(base)));
        try {
            SecurityUtils.loadPrivateKey(base, "errada");
            System.out.println("   ERRO: a palavra-passe errada foi aceite");
        } catch (Exception e) {
            System.out.println("   palavra-passe errada recusada: " + e.getClass().getSimpleName());
        }

        // ------------------------------------------------------------------
        System.out.println("\n8. ATAQUE: ALTERAR UMA TRANSAÇÃO DO BLOCO 1");
        b1.tamperTransaction(2, "LOTE 0042 900kg");
        System.out.print(bc);
        System.out.println("   cadeia íntegra: " + bc.isValid()
                + " · primeiro bloco inválido: #" + bc.getInvalidIndex());
    }
}
