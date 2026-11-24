//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
//::  ApisChain · Certificação descentralizada da origem do mel             ::://
//::  Computação Distribuída 2026/2027 · ESTT · Instituto Politécnico Tomar ::://
//::  Samuel Ponte (27227) e André Vassalo (27454)                          ::://
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::://
package apischain.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Arranque da interface da ApisChain (main class do projeto).
 *
 * Abre o ecrã de entrada, que prepara a demonstração na pasta "data"
 * (ou na pasta passada como argumento).
 */
public class ApisChainApp {

    public static void main(String[] args) {
        String folder = (args.length > 0) ? args[0] : "data";
        // aspeto Nimbus, como nos formulários que o NetBeans gera
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            // sem Nimbus fica o aspeto por omissão do Java
        }
        // as janelas Swing criam-se na thread do Swing
        SwingUtilities.invokeLater(() -> new LoginFrame(folder).setVisible(true));
    }
}
