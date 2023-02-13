/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui;

import com.eim.eim.EIMController;
import com.eim.mail.EIMAccount;
import com.eim.mail.EIMEmailAccount;
import com.eim.mail.EIMEmailMessage;
import com.eim.util.EIMConstants;
import com.eim.db.EIMI18N;
import com.eim.img.EIMImage;
import com.eim.ui.components.EIMCombobox;
import com.eim.ui.components.EIMListCellRenderer;
import com.eim.util.EIMUtility;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMContactListUI
 *
 * @author Denis Meyer
 */
public abstract class EIMContactListUI extends JFrame {

    private static final Logger logger = LogManager.getLogger(EIMContactListUI.class.getName());
    private final EIMController controller;
    private ArrayList<EIMMessagesUI> messagesuiList = null;
    private final EIMEmailAccount account;
    private JPopupMenu menu = null;
    private final JMenuItem menuItem_logout;
    private final JMenuItem menuItem_close;
    private boolean menuVisible = false;
    private Timer menuTimer = null;
    private boolean mouseOverMenuButton = false;

    public EIMContactListUI(EIMController _controller, EIMEmailAccount account) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMMessagesUI");
        }
        this.controller = _controller;
        messagesuiList = new ArrayList<>();
        this.account = account;

        try {
            this.setIconImage(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_ICON)).getImage());
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }

        initComponents();
        this.list_to.setCellRenderer(new EIMListCellRenderer());

        menu = new JPopupMenu();
        menu.setInvoker(menu);
        menuItem_logout = new JMenuItem(EIMI18N.getInstance().getString("LogoutSep"));
        menuItem_close = new JMenuItem(EIMI18N.getInstance().getString("Close"));
        try {
            menuItem_close.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_CLOSE)));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        addListeners();
        buildMenu();

        this.setTitle(EIMI18N.getInstance().getString("ContactList"));
        this.label_contactList.setText(account.getAccount().get_emailaddress());
        this.label_to.setText(EIMI18N.getInstance().getString("To") + ":");
        this.label_status.setText("");
        stateChanged(account.getAccount().getStatus());

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
        Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
        int x = (int) rect.getMaxX() - this.getWidth();
        int y = 0;
        this.setLocation(x, y);

        addListener();

        setAutoComplete();
    }

    public abstract void aMailSent(String from, String to);

    public abstract void aMailNotSent(String info, String msg, String content);

    public abstract void messageSeen(String from);

    public String getAccountName() {
        return account.getAccount().get_emailaddress();
    }

    public void deinit() {
        menuClosed();
        menu.setVisible(false);
        for (EIMMessagesUI ui : messagesuiList) {
            ui.deinit();
            ui.dispose();
        }
        messagesuiList.clear();
    }

    public boolean addMailToConversation(EIMEmailMessage msg, boolean addFrom) {
        boolean addedToUI = false;
        String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1]);
        for (EIMMessagesUI ui : messagesuiList) {
            if (ui.isVisible()) {
                addedToUI = ui.addMailToConversation(msg, addFrom);
                if (addedToUI) {
                    messageSeen(from);
                    break;
                }
            }
        }
        setAutoComplete();
        return addedToUI;
    }

    public final void stateChanged(EIMAccount.LOGIN_STATUS status) {
        switch (status) {
            case LOGGED_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_GREEN);
                setAllMessagesUIEnabled(true);
                this.list_to.setEnabled(true);
                this.combobox_to.setEnabled(true);
                menuItem_logout.setText(EIMI18N.getInstance().getString("LogoutSep"));
                try {
                    menuItem_logout.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_NETWORK_STATUS_YELLOW)));
                } catch (FileNotFoundException e) {
                    logger.error("FileNotFoundException: " + e.getMessage());
                }
                break;
            case LOGGED_OUT:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_YELLOW);
                setAllMessagesUIEnabled(false);
                this.list_to.setEnabled(false);
                this.combobox_to.setEnabled(false);
                menuItem_logout.setText(EIMI18N.getInstance().getString("LoginSep"));
                try {
                    menuItem_logout.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_NETWORK_STATUS_GREEN)));
                } catch (FileNotFoundException e) {
                    logger.error("FileNotFoundException: " + e.getMessage());
                }
                break;
            case LOGGING_OUT:
            case LOGGING_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                setAllMessagesUIEnabled(false);
                this.list_to.setEnabled(false);
                this.combobox_to.setEnabled(false);
                break;
            case LOGIN_FAILED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_RED);
                setAllMessagesUIEnabled(false);
                this.list_to.setEnabled(false);
                this.combobox_to.setEnabled(false);
                break;
            case DELETED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                setAllMessagesUIEnabled(false);
                this.list_to.setEnabled(false);
                this.combobox_to.setEnabled(false);
                this.setVisible(false);
                for (EIMMessagesUI ui : messagesuiList) {
                    ui.setVisible(false);
                    ui.dispose();
                }
                this.dispose();
                break;
            default:
                break;
        }
    }

    public void addRecieverToAutocomplete(String reciever) {
        ((EIMCombobox) combobox_to).removeAllItems();
        DefaultListModel listModel = (DefaultListModel) this.list_to.getModel();
        listModel.removeAllElements();
        if (account != null) {
            ArrayList<String> emailAddresses = new ArrayList<>();
            for (Map.Entry pairs : controller.getAccountManager().getEmails().entrySet()) {
                EIMEmailMessage msg = (EIMEmailMessage) pairs.getValue();
                String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1]);
                String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1]);
                if (account.getAccount().isAlias(to)) {
                    if (!emailAddresses.contains(from)) {
                        emailAddresses.add(from);
                    }
                }
                if (account.getAccount().isAlias(from)) {
                    if (!emailAddresses.contains(to)) {
                        emailAddresses.add(to);
                    }
                }
            }
            emailAddresses.add(reciever);

            if (logger.isDebugEnabled()) {
                logger.debug("From list size: " + emailAddresses.size());
            }
            if (!emailAddresses.isEmpty()) {
                HashSet hs = new HashSet();
                hs.addAll(emailAddresses);
                emailAddresses.clear();
                emailAddresses.addAll(hs);
                Collections.sort(emailAddresses);
            }
            ((EIMCombobox) combobox_to).setDataList(emailAddresses);
            for (String s : emailAddresses) {
                listModel.addElement(s);
            }
            this.list_to.setModel(listModel);
        }
    }

    public synchronized void showConversation(final String from) {
        if ((from != null) && (!from.isEmpty())) {
            boolean foundMUI = false;
            for (EIMMessagesUI ui : messagesuiList) {
                if (ui.getToName().equals(from)) {
                    ui.setVisible(true);
                    foundMUI = true;
                    break;
                }
            }
            if (!foundMUI) {
                final EIMMessagesUI mUI = new EIMMessagesUI(controller, account, from) {
                    @Override
                    public synchronized void mailSent(String from, String to) {
                        aMailSent(from, to);
                    }

                    @Override
                    public void couldNotSendMail(String info, String msg, String content) {
                        aMailNotSent(info, msg, content);
                    }
                };
                mUI.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        messagesuiList.remove(mUI);
                    }
                });
                if (!messagesuiList.contains(mUI)) {
                    mUI.setVisible(true);
                    messagesuiList.add(mUI);
                }
            }
            messageSeen(from);
        }
    }

    private void menuClosed() {
        menu.setVisible(false);
        if (!mouseOverMenuButton) {
            setImageForStatus(account.getAccount());
        }
        menuVisible = false;
        stopMenuTimer();
    }

    private void stopMenuTimer() {
        if (menuTimer != null) {
            menuTimer.cancel();
            menuTimer.purge();
            menuTimer = null;
        } else {
            menuTimer = null;
        }
    }

    private void refreshMenuTimer() {
        if (menuTimer != null) {
            menuTimer.cancel();
            menuTimer.purge();
            menuTimer = new Timer();
        } else {
            menuTimer = new Timer();
        }
        menuTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                menu.setVisible(false);
                menuClosed();
            }
        }, EIMConstants.AUTO_CLOSE_MENU_TIME);
    }

    private void addListener() {
        final JComboBox combobox = this.combobox_to;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                combobox.requestFocus();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                menuClosed();
                menu.setVisible(false);
                cancel();
            }
        });
        KeyAdapter escapePressed = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancel();
                }
                menuClosed();
                menu.setVisible(false);
            }
        };
        this.addKeyListener(escapePressed);
        this.list_to.addKeyListener(escapePressed);
        this.combobox_to.addKeyListener(escapePressed);

        this.list_to.getInputMap().put(
                EIMUtility.getInstance().platformIsMac() ? EIMConstants.KEYSTROKE_CLOSE_WINDOW_MAC : EIMConstants.KEYSTROKE_CLOSE_WINDOW_WINDOWS,
                "closeWindow");
        this.list_to.getActionMap().put(
                "closeWindow",
                new CloseWindowAction());
    }

    private void setIcon(EIMConstants.IMAGE imgStatus) {
        try {
            this.label_status.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(imgStatus)));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
    }

    private void setAllMessagesUIEnabled(boolean enabled) {
        for (EIMMessagesUI ui : messagesuiList) {
            ui.setAllEnabled(enabled);
        }
    }

    private void cancel() {
        menuClosed();
        setVisible(false);
    }

    private void setAutoComplete() {
        ((EIMCombobox) combobox_to).removeAllItems();
        DefaultListModel listModel = (DefaultListModel) this.list_to.getModel();
        listModel.removeAllElements();
        if (account != null) {
            ArrayList<String> emailAddresses = new ArrayList<>();
            for (Map.Entry pairs : controller.getAccountManager().getEmails().entrySet()) {
                EIMEmailMessage msg = (EIMEmailMessage) pairs.getValue();
                String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1]);
                String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1]);
                if (account.getAccount().isAlias(to)) {
                    if (!emailAddresses.contains(from)) {
                        emailAddresses.add(from);
                    }
                }
                if (account.getAccount().isAlias(from)) {
                    if (!emailAddresses.contains(to)) {
                        emailAddresses.add(to);
                    }
                }
            }

            if (!emailAddresses.isEmpty()) {
                HashSet hs = new HashSet();
                hs.addAll(emailAddresses);
                emailAddresses.clear();
                emailAddresses.addAll(hs);
                Collections.sort(emailAddresses);
            }
            ((EIMCombobox) combobox_to).setDataList(emailAddresses);
            for (String s : emailAddresses) {
                listModel.addElement(s);
            }
            this.list_to.setModel(listModel);
        }
    }

    private class CloseWindowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent tf) {
            cancel();
        }
    }

    private void setImageForStatus(EIMAccount acc) {
        switch (acc.getStatus()) {
            case LOGGED_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_GREEN);
                break;
            case LOGGED_OUT:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_YELLOW);
                break;
            case LOGGING_OUT:
            case LOGGING_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                break;
            case LOGIN_FAILED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_RED);
                break;
            case DELETED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                break;
        }
    }

    private void setHoverImageForStatus(EIMAccount acc) {
        switch (acc.getStatus()) {
            case LOGGED_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_GREEN_HOVER);
                break;
            case LOGGED_OUT:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_YELLOW_HOVER);
                break;
            case LOGGING_OUT:
            case LOGGING_IN:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                break;
            case LOGIN_FAILED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_RED_HOVER);
                break;
            case DELETED:
                setIcon(EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                break;
        }
        buildMenu();
    }

    private void buildMenu() {
        menuClosed();
        menu.removeAll();
        menu.add(menuItem_logout);
        if ((account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_OUT)
                || (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGIN_FAILED)) {
            menu.addSeparator();
            menu.add(menuItem_close);
        }
    }

    private void addListeners() {
        menuItem_logout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (EIMContactListUI.this.account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN) {
                    controller.logoutOrDeleteAccount(EIMContactListUI.this.account.getAccount().get_emailaddress());
                } else if (EIMContactListUI.this.account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_OUT) {
                    controller.login(EIMContactListUI.this.account.getAccount(), true);
                }
                menuClosed();
            }
        });
        menuItem_logout.addMouseListener(refreshMouseListener);
        final JFrame frame = this;
        menuItem_close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                menuClosed();
                frame.setVisible(false);
            }
        });
        menuItem_close.addMouseListener(refreshMouseListener);
        this.label_status.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ((EIMContactListUI.this.account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)
                        || (EIMContactListUI.this.account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_OUT)) {
                    menuVisible = !menuVisible;
                    menu.setLocation(e.getXOnScreen(), e.getYOnScreen());
                    menu.setVisible(menuVisible);
                    if (!menuVisible) {
                        menuClosed();
                    } else {
                        refreshMenuTimer();
                    }
                } else {
                    menuClosed();
                    menu.setVisible(false);
                }
            }
        });
        this.label_status.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOverMenuButton = true;
                setHoverImageForStatus(account.getAccount());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseOverMenuButton = false;
                if (!menuVisible) {
                    setImageForStatus(account.getAccount());
                }
            }
        });
    }

    private final MouseAdapter refreshMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            stopMenuTimer();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            refreshMenuTimer();
        }
    };

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
        panel_header = new javax.swing.JPanel();
        label_status = new javax.swing.JLabel();
        label_contactList = new javax.swing.JLabel();
        panel_combobox = new javax.swing.JPanel();
        label_to = new javax.swing.JLabel();
        combobox_to = new EIMCombobox();
        scrollpane_to = new javax.swing.JScrollPane();
        list_to = new javax.swing.JList(new DefaultListModel());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Contact List");
        setMinimumSize(new java.awt.Dimension(323, 132));
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        panel_main.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel_main.setMinimumSize(new java.awt.Dimension(241, 188));
        panel_main.setPreferredSize(new java.awt.Dimension(307, 504));
        panel_main.setLayout(new java.awt.GridBagLayout());

        panel_header.setLayout(new java.awt.GridBagLayout());

        label_status.setText("STATUS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        panel_header.add(label_status, gridBagConstraints);

        label_contactList.setText("ACCOUNT_NAME");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panel_header.add(label_contactList, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panel_main.add(panel_header, gridBagConstraints);

        panel_combobox.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 2, 1));
        panel_combobox.setLayout(new java.awt.GridBagLayout());

        label_to.setText("To:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        panel_combobox.add(label_to, gridBagConstraints);

        combobox_to.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_toActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        panel_combobox.add(combobox_to, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        panel_main.add(panel_combobox, gridBagConstraints);

        list_to.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list_to.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                list_toMouseClicked(evt);
            }
        });
        list_to.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                list_toKeyReleased(evt);
            }
        });
        scrollpane_to.setViewportView(list_to);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panel_main.add(scrollpane_to, gridBagConstraints);

        getContentPane().add(panel_main);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void list_toKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_list_toKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (this.list_to.isEnabled()) {
                showConversation((String) this.list_to.getSelectedValue());
                menuClosed();
            }
        }
    }//GEN-LAST:event_list_toKeyReleased

    private void list_toMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_list_toMouseClicked
        if (this.list_to.isEnabled()) {
            if (evt.getClickCount() == 2) {
                showConversation((String) this.list_to.getSelectedValue());
                menuClosed();
            }
        }
    }//GEN-LAST:event_list_toMouseClicked

    private void combobox_toActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_toActionPerformed
        if (this.combobox_to.isEnabled()) {
            showConversation((String) this.combobox_to.getSelectedItem());
        }
    }//GEN-LAST:event_combobox_toActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox combobox_to;
    private javax.swing.JLabel label_contactList;
    private javax.swing.JLabel label_status;
    private javax.swing.JLabel label_to;
    private javax.swing.JList list_to;
    private javax.swing.JPanel panel_combobox;
    private javax.swing.JPanel panel_header;
    private javax.swing.JPanel panel_main;
    private javax.swing.JScrollPane scrollpane_to;
    // End of variables declaration//GEN-END:variables
}
