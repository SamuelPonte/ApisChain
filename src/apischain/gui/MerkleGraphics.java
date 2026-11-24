//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::  Adaptado de blockchain.GUI.MerkleGraphics do BlockChainGUI            ::://
//::  (Prof. António Manso, 2022)                                           ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import apischain.core.MerkleTree;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;

/**
 * Desenho da árvore de Merkle de um bloco.
 *
 * Alterações face à versão das aulas:
 * 1. usa a MerkleTree da ApisChain, cujos hashes são SHA3-256: mostram-se só
 *    os primeiros carateres, senão não cabiam;
 * 2. por baixo das folhas aparece o tipo da transação, e não o texto Base64;
 * 3. sem editor de formulários.
 */
public class MerkleGraphics extends JPanel {

    private MerkleTree tree;       // árvore a desenhar (null: nada)
    private String element;        // transação escolhida
    private List<String> proof;    // prova da transação escolhida

    /**
     * @param tree árvore do bloco
     * @param element transação escolhida (Base64)
     * @param proof prova dessa transação
     */
    public void setTree(MerkleTree tree, String element, List<String> proof) {
        this.tree = tree;
        this.element = element;
        this.proof = proof;
        repaint();
    }

    @Override
    public void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        if (tree == null) {
            return;
        }
        drawLines(gr);
        List<List<String>> hashTree = tree.getHashTree();
        int sizeY = getHeight() / (hashTree.size() + 1);
        // 1. hashes: a raiz em cima e as folhas em baixo (verde: fazem parte da prova)
        for (int i = 0; i < hashTree.size(); i++) {
            int size = getWidth() / (int) Math.pow(2, i);
            for (int j = 0; j < hashTree.get(i).size(); j++) {
                String hash = hashTree.get(i).get(j);
                Color back = proof.contains(hash) ? new Color(200, 255, 200) : new Color(250, 200, 200);
                drawCenteredString(gr, ChainView.shorten(hash, 8), new Rectangle(j * size, sizeY * i, size, sizeY), back);
            }
        }
        // 2. transações por baixo das folhas (verde: a escolhida)
        int size = getWidth() / (int) Math.pow(2, hashTree.size() - 1);
        for (int j = 0; j < tree.getElements().size(); j++) {
            String tx = tree.getElements().get(j);
            Color back = tx.equals(element) ? new Color(200, 255, 200) : new Color(200, 200, 255);
            drawCenteredString(gr, ChainView.shortName(tx),
                    new Rectangle(j * size, sizeY * hashTree.size(), size, sizeY), back);
        }
    }

    /**
     * Linhas entre cada nó e o seu pai.
     */
    private void drawLines(Graphics gr) {
        gr.setColor(Color.BLACK);
        List<List<String>> hashTree = tree.getHashTree();
        for (int y = 1; y < hashTree.size(); y++) {
            for (int x = 0; x < hashTree.get(y).size(); x++) {
                Point p1 = getCenter(y - 1, x / 2);
                Point p2 = getCenter(y, x);
                gr.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
        // das folhas para as transações
        int sizeY = getHeight() / (hashTree.size() + 1);
        for (int x = 0; x < tree.getElements().size(); x++) {
            Point p = getCenter(hashTree.size() - 1, x);
            gr.drawLine(p.x, p.y, p.x, p.y + sizeY);
        }
    }

    private Point getCenter(int y, int x) {
        int sizeY = getHeight() / (tree.getHashTree().size() + 1);
        int size = getWidth() / (int) Math.pow(2, y);
        return new Point(x * size + size / 2, sizeY * y + sizeY / 2);
    }

    /**
     * Texto centrado num retângulo, com caixa à volta.
     */
    private void drawCenteredString(Graphics g, String text, Rectangle rect, Color back) {
        Font font = new Font(Font.MONOSPACED, Font.BOLD, 12);
        FontMetrics metrics = g.getFontMetrics(font);
        int w = metrics.stringWidth(text) + 8;
        int h = metrics.getHeight() + 6;
        int x = rect.x + (rect.width - w) / 2;
        int y = rect.y + (rect.height - h) / 2;
        g.setColor(back);
        g.fillRect(x, y, w, h);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, w, h);
        g.setFont(font);
        g.drawString(text, x + 4, y + 3 + metrics.getAscent());
    }
}
