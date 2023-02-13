/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui;

import com.eim.db.EIMI18N;
import com.eim.img.EIMImage;
import com.eim.util.EIMConstants;
import com.eim.util.EIMUtility;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMWizardUI
 *
 * @author Denis Meyer
 */
public abstract class EIMWizardUI extends JFrame {

    private static final Logger logger = LogManager.getLogger(EIMWizardUI.class.getName());
    private final ArrayList<JRadioButton> radiobuttons = new ArrayList<>();

    public static enum ACCOUNTTYPE {

        AOL,
        CUSTOM,
        GMAIL,
        NEOMAILBOX,
        OUTLOOK,
        UOS
    };

    public EIMWizardUI() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMWizardUI");
        }
        initComponents();

        radiobuttons.add(radiobutton_aol);
        radiobuttons.add(radiobutton_custom);
        radiobuttons.add(radiobutton_gmail);
        radiobuttons.add(radiobutton_neomailbox);
        radiobuttons.add(radiobutton_outlook);
        radiobuttons.add(radiobutton_uos);

        this.setTitle(EIMI18N.getInstance().getString("AccountWizard"));
        this.button_cancel.setText(EIMI18N.getInstance().getString("Cancel"));
        this.button_continue.setText(EIMI18N.getInstance().getString("Continue"));
        this.label_text.setText(EIMI18N.getInstance().getString("ChooseAccountToAdd"));
        this.label_custom.setText(EIMI18N.getInstance().getString("MiscAccount"));
        try {
            this.setIconImage(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_ICON)).getImage());
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        setLogos();

        clear();

        addListener();

        this.setLocationRelativeTo(null);
    }

    public abstract void accountSelected(ACCOUNTTYPE accType);

    public final void clear() {
        deselectAllRadiobuttons();
        check();
    }

    public void cancel() {
        clear();
        this.setVisible(false);
    }

    private void check() {
        boolean radioButtonSelected = false;
        this.button_continue.setEnabled(false);
        for (JRadioButton rb : radiobuttons) {
            if (rb.isSelected()) {
                radioButtonSelected = true;
                break;
            }
        }
        if (!radioButtonSelected) {
            this.radiobutton_gmail.requestFocus();
        } else {
            this.button_continue.setEnabled(true);
            this.button_continue.requestFocus();
        }
    }

    private void deselectAllRadiobuttons() {
        for (JRadioButton rb : radiobuttons) {
            rb.setSelected(false);
        }
    }

    private void setLogos() {
        try {
            this.label_gmail.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_WIZARD_GMAIL)));
            this.label_gmail.setText("");
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        try {
            this.label_aol.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_WIZARD_AOL)));
            this.label_aol.setText("");
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        try {
            this.label_uos.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_WIZARD_UOS)));
            this.label_uos.setText("");
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        try {
            this.label_neomailbox.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_WIZARD_NEOMAILBOX)));
            this.label_neomailbox.setText("");
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        try {
            this.label_outlook.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_WIZARD_OUTLOOK)));
            this.label_outlook.setText("");
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
    }

    private void accountSelected_internal() {
        if (radiobutton_aol.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.AOL);
        } else if (radiobutton_custom.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.CUSTOM);
        } else if (radiobutton_gmail.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.GMAIL);
        } else if (radiobutton_neomailbox.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.NEOMAILBOX);
        } else if (radiobutton_outlook.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.OUTLOOK);
        } else if (radiobutton_uos.isSelected()) {
            cancel();
            accountSelected(ACCOUNTTYPE.UOS);
        }
    }

    private void addListener() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                check();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        KeyAdapter escapePressed = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
            }
        };

        ArrayList<JComponent> al = new ArrayList<>();
        al.add(button_cancel);
        al.add(button_continue);
        for (JRadioButton rb : radiobuttons) {
            al.add(rb);
        }

        this.addKeyListener(escapePressed);
        for (JComponent c : al) {
            c.addKeyListener(escapePressed);
            c.getInputMap().put(
                    EIMUtility.getInstance().platformIsMac() ? EIMConstants.KEYSTROKE_CLOSE_WINDOW_MAC : EIMConstants.KEYSTROKE_CLOSE_WINDOW_WINDOWS,
                    "closeWindow");
            c.getActionMap().put(
                    "closeWindow",
                    new CloseWindowAction());
        }
    }

    private class CloseWindowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent tf) {
            cancel();
        }
    }

    private void accountTypeSelected(ACCOUNTTYPE accType) {
        deselectAllRadiobuttons();
        switch (accType) {
            case AOL:
                this.radiobutton_aol.setSelected(true);
                break;
            case CUSTOM:
                this.radiobutton_custom.setSelected(true);
                break;
            case GMAIL:
                this.radiobutton_gmail.setSelected(true);
                break;
            case NEOMAILBOX:
                this.radiobutton_neomailbox.setSelected(true);
                break;
            case OUTLOOK:
                this.radiobutton_outlook.setSelected(true);
                break;
            case UOS:
                this.radiobutton_uos.setSelected(true);
                break;
        }
        check();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panel_main = new javax.swing.JPanel();
        label_text = new javax.swing.JLabel();
        panel_radiobuttons = new javax.swing.JPanel();
        radiobutton_gmail = new javax.swing.JRadioButton();
        radiobutton_aol = new javax.swing.JRadioButton();
        radiobutton_uos = new javax.swing.JRadioButton();
        radiobutton_neomailbox = new javax.swing.JRadioButton();
        radiobutton_outlook = new javax.swing.JRadioButton();
        radiobutton_custom = new javax.swing.JRadioButton();
        panel_text = new javax.swing.JPanel();
        label_gmail = new javax.swing.JLabel();
        label_aol = new javax.swing.JLabel();
        label_uos = new javax.swing.JLabel();
        label_neomailbox = new javax.swing.JLabel();
        label_outlook = new javax.swing.JLabel();
        label_custom = new javax.swing.JLabel();
        panel_buttons = new javax.swing.JPanel();
        button_cancel = new javax.swing.JButton();
        button_continue = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Account wizard");
        setMaximumSize(new java.awt.Dimension(376, 418));
        setMinimumSize(new java.awt.Dimension(376, 418));
        setPreferredSize(new java.awt.Dimension(376, 418));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        panel_main.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel_main.setLayout(new java.awt.GridBagLayout());

        label_text.setText("Choose an account to add:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        panel_main.add(label_text, gridBagConstraints);

        panel_radiobuttons.setLayout(new java.awt.GridLayout(6, 1));

        radiobutton_gmail.setNextFocusableComponent(radiobutton_aol);
        radiobutton_gmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_gmailActionPerformed(evt);
            }
        });
        radiobutton_gmail.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_gmailKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_gmail);

        radiobutton_aol.setNextFocusableComponent(radiobutton_uos);
        radiobutton_aol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_aolActionPerformed(evt);
            }
        });
        radiobutton_aol.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_aolKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_aol);

        radiobutton_uos.setNextFocusableComponent(radiobutton_neomailbox);
        radiobutton_uos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_uosActionPerformed(evt);
            }
        });
        radiobutton_uos.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_uosKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_uos);

        radiobutton_neomailbox.setNextFocusableComponent(radiobutton_outlook);
        radiobutton_neomailbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_neomailboxActionPerformed(evt);
            }
        });
        radiobutton_neomailbox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_neomailboxKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_neomailbox);

        radiobutton_outlook.setNextFocusableComponent(radiobutton_custom);
        radiobutton_outlook.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_outlookActionPerformed(evt);
            }
        });
        radiobutton_outlook.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_outlookKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_outlook);

        radiobutton_custom.setNextFocusableComponent(button_continue);
        radiobutton_custom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radiobutton_customActionPerformed(evt);
            }
        });
        radiobutton_custom.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                radiobutton_customKeyReleased(evt);
            }
        });
        panel_radiobuttons.add(radiobutton_custom);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        panel_main.add(panel_radiobuttons, gridBagConstraints);

        panel_text.setLayout(new java.awt.GridLayout(6, 1));

        label_gmail.setText("Googlemail");
        label_gmail.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_gmailMouseReleased(evt);
            }
        });
        panel_text.add(label_gmail);

        label_aol.setText("AOL");
        label_aol.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_aolMouseReleased(evt);
            }
        });
        panel_text.add(label_aol);

        label_uos.setText("University of Osnabrück");
        label_uos.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_uosMouseReleased(evt);
            }
        });
        panel_text.add(label_uos);

        label_neomailbox.setText("Neomailbox");
        label_neomailbox.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_neomailboxMouseReleased(evt);
            }
        });
        panel_text.add(label_neomailbox);

        label_outlook.setText("Outlook/Hotmail");
        label_outlook.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_outlookMouseReleased(evt);
            }
        });
        panel_text.add(label_outlook);

        label_custom.setText("Other/Custom account");
        label_custom.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                label_customMouseReleased(evt);
            }
        });
        panel_text.add(label_custom);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panel_main.add(panel_text, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(panel_main, gridBagConstraints);

        panel_buttons.setLayout(new java.awt.GridLayout(1, 2));

        button_cancel.setText("Cancel");
        button_cancel.setNextFocusableComponent(radiobutton_gmail);
        button_cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_cancelActionPerformed(evt);
            }
        });
        button_cancel.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_cancelKeyReleased(evt);
            }
        });
        panel_buttons.add(button_cancel);

        button_continue.setText("Continue");
        button_continue.setNextFocusableComponent(button_cancel);
        button_continue.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button_continueMouseReleased(evt);
            }
        });
        button_continue.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_continueKeyReleased(evt);
            }
        });
        panel_buttons.add(button_continue);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(panel_buttons, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_cancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_cancelActionPerformed
        cancel();
    }//GEN-LAST:event_button_cancelActionPerformed

    private void button_cancelKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_cancelKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            cancel();
        }
    }//GEN-LAST:event_button_cancelKeyReleased

    private void radiobutton_gmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_gmailActionPerformed
        accountTypeSelected(ACCOUNTTYPE.GMAIL);
    }//GEN-LAST:event_radiobutton_gmailActionPerformed

    private void radiobutton_aolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_aolActionPerformed
        accountTypeSelected(ACCOUNTTYPE.AOL);
    }//GEN-LAST:event_radiobutton_aolActionPerformed

    private void radiobutton_uosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_uosActionPerformed
        accountTypeSelected(ACCOUNTTYPE.UOS);
    }//GEN-LAST:event_radiobutton_uosActionPerformed

    private void radiobutton_neomailboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_neomailboxActionPerformed
        accountTypeSelected(ACCOUNTTYPE.NEOMAILBOX);
    }//GEN-LAST:event_radiobutton_neomailboxActionPerformed

    private void radiobutton_outlookActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_outlookActionPerformed
        accountTypeSelected(ACCOUNTTYPE.OUTLOOK);
    }//GEN-LAST:event_radiobutton_outlookActionPerformed

    private void radiobutton_customActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radiobutton_customActionPerformed
        accountTypeSelected(ACCOUNTTYPE.CUSTOM);
    }//GEN-LAST:event_radiobutton_customActionPerformed

    private void button_continueMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_button_continueMouseReleased
        accountSelected_internal();
    }//GEN-LAST:event_button_continueMouseReleased

    private void button_continueKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_continueKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountSelected_internal();
        }
    }//GEN-LAST:event_button_continueKeyReleased

    private void label_gmailMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_gmailMouseReleased
        accountTypeSelected(ACCOUNTTYPE.GMAIL);
    }//GEN-LAST:event_label_gmailMouseReleased

    private void label_aolMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_aolMouseReleased
        accountTypeSelected(ACCOUNTTYPE.AOL);
    }//GEN-LAST:event_label_aolMouseReleased

    private void label_uosMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_uosMouseReleased
        accountTypeSelected(ACCOUNTTYPE.UOS);
    }//GEN-LAST:event_label_uosMouseReleased

    private void label_neomailboxMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_neomailboxMouseReleased
        accountTypeSelected(ACCOUNTTYPE.NEOMAILBOX);
    }//GEN-LAST:event_label_neomailboxMouseReleased

    private void label_outlookMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_outlookMouseReleased
        accountTypeSelected(ACCOUNTTYPE.OUTLOOK);
    }//GEN-LAST:event_label_outlookMouseReleased

    private void label_customMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_label_customMouseReleased
        accountTypeSelected(ACCOUNTTYPE.CUSTOM);
    }//GEN-LAST:event_label_customMouseReleased

    private void radiobutton_gmailKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_gmailKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.GMAIL);
        }
    }//GEN-LAST:event_radiobutton_gmailKeyReleased

    private void radiobutton_aolKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_aolKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.AOL);
        }
    }//GEN-LAST:event_radiobutton_aolKeyReleased

    private void radiobutton_uosKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_uosKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.UOS);
        }
    }//GEN-LAST:event_radiobutton_uosKeyReleased

    private void radiobutton_neomailboxKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_neomailboxKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.NEOMAILBOX);
        }
    }//GEN-LAST:event_radiobutton_neomailboxKeyReleased

    private void radiobutton_outlookKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_outlookKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.OUTLOOK);
        }
    }//GEN-LAST:event_radiobutton_outlookKeyReleased

    private void radiobutton_customKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_radiobutton_customKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            accountTypeSelected(ACCOUNTTYPE.CUSTOM);
        }
    }//GEN-LAST:event_radiobutton_customKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_continue;
    private javax.swing.JLabel label_aol;
    private javax.swing.JLabel label_custom;
    private javax.swing.JLabel label_gmail;
    private javax.swing.JLabel label_neomailbox;
    private javax.swing.JLabel label_outlook;
    private javax.swing.JLabel label_text;
    private javax.swing.JLabel label_uos;
    private javax.swing.JPanel panel_buttons;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_radiobuttons;
    private javax.swing.JPanel panel_text;
    private javax.swing.JRadioButton radiobutton_aol;
    private javax.swing.JRadioButton radiobutton_custom;
    private javax.swing.JRadioButton radiobutton_gmail;
    private javax.swing.JRadioButton radiobutton_neomailbox;
    private javax.swing.JRadioButton radiobutton_outlook;
    private javax.swing.JRadioButton radiobutton_uos;
    // End of variables declaration//GEN-END:variables
}
