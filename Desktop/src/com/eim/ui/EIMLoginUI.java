/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui;

import com.eim.mail.EIMAccount;
import java.awt.event.KeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eim.util.EIMConstants;
import com.eim.db.EIMI18N;
import com.eim.img.EIMImage;
import com.eim.db.EIMServerDatabase;
import com.eim.ui.components.EIMBubbleBorder;
import com.eim.util.EIMUtility;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * EIMLoginUI
 *
 * @author Denis Meyer
 */
public abstract class EIMLoginUI extends JFrame {

    private static final Logger logger = LogManager.getLogger(EIMLoginUI.class.getName());
    private final Border default_border;
    private boolean textfield_imap_changed = false;
    private boolean combobox_imapPort_changed = false;
    private boolean combobox_imapSsl_changed = false;
    private boolean combobox_imapAuthentication_changed = false;
    private boolean textfield_smtp_changed = false;
    private boolean combobox_smtpPort_changed = false;
    private boolean combobox_smtpSsl_changed = false;
    private boolean combobox_smtpAuthentication_changed = false;
    private boolean textfield_emailaddress_changed = false;
    private boolean textfield_username_changed = false;
    private boolean textfield_password_changed = false;
    private boolean serverSectionVisible = true;
    private int serverSectionSize = 0;

    public EIMLoginUI() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMLoginUI");
        }
        if (EIMUtility.getInstance().platformIsMac()) {
            serverSectionSize = 120;
        } else {
            serverSectionSize = 110;
        }

        initComponents();

        this.setTitle(EIMI18N.getInstance().getString("Login"));
        this.label_logo_text.setText(EIMI18N.getInstance().getString("Login"));
        this.label_user.setText(EIMI18N.getInstance().getString("User"));
        this.label_emailaddress.setText(EIMI18N.getInstance().getString("EmailAddress") + ":");
        this.label_username.setText(EIMI18N.getInstance().getString("Username") + ":");
        this.label_password.setText(EIMI18N.getInstance().getString("Password") + ":");
        this.label_server.setText(EIMI18N.getInstance().getString("Server"));
        this.label_serverAddress.setText(EIMI18N.getInstance().getString("ServerAddress"));
        this.label_port.setText(EIMI18N.getInstance().getString("Port"));
        this.label_ssl.setText(EIMI18N.getInstance().getString("SSL"));
        this.label_authentication.setText(EIMI18N.getInstance().getString("Authentication"));
        this.label_imap.setText(EIMI18N.getInstance().getString("IMAP") + ":");
        this.label_smtp.setText(EIMI18N.getInstance().getString("SMTP") + ":");
        this.button_cancel.setText(EIMI18N.getInstance().getString("Cancel"));
        this.button_delete.setText(EIMI18N.getInstance().getString("Delete"));
        this.button_login.setText(EIMI18N.getInstance().getString("LoginSep"));
        this.button_showData.setText(EIMI18N.getInstance().getString("HideData"));
        this.label_warning.setText("");
        this.label_logo.setText("");
        try {
            this.setIconImage(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_ICON)).getImage());
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
        try {
            this.label_logo.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_NEW_ACCOUNT)));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }

        this.textfield_emailaddress.requestFocus();
        default_border = this.textfield_emailaddress.getBorder();
        this.button_delete.setEnabled(false);
        this.button_login.setEnabled(false);
        checkBadAndNotSupportedServerDBs();
        setServerSectionEnabled(false);

        this.setLocationRelativeTo(null);

        if (EIMUtility.getInstance().platformIsMac()) {
            Rectangle r = this.getBounds();
            this.setMinimumSize(new Dimension(r.width + 100, r.height + 40));
            this.setMaximumSize(new Dimension(r.width + 100, r.height + 40));
            this.setSize(new Dimension(r.width + 100, r.height + 40));
        }

        addListener();
    }

    public abstract void cancel();

    public abstract void delete(String emailAddress);

    public abstract void login(
            String imap,
            String imapPort,
            String imapSsl,
            String imapAuth,
            String smtp,
            String smtpPort,
            String smtpSsl,
            String smtpAuth,
            String email,
            String username,
            String password);

    public abstract void onError(String emailAddress, String info, String content);

    public void setServerSectionVisible(boolean visible) {

        if (visible && !serverSectionVisible) {
            serverSectionVisible = true;
            this.button_showData.setText(EIMI18N.getInstance().getString("HideData"));
            this.button_delete.setVisible(serverSectionVisible);
            this.panel_server.setVisible(serverSectionVisible);
            Rectangle r = this.getBounds();
            this.setMinimumSize(new Dimension(r.width, r.height + serverSectionSize));
            this.setMaximumSize(new Dimension(r.width, r.height + serverSectionSize));
            this.setSize(new Dimension(r.width, r.height + serverSectionSize));
        } else if (!visible && serverSectionVisible) {
            serverSectionVisible = false;
            this.button_showData.setText(EIMI18N.getInstance().getString("ShowData"));
            this.button_delete.setVisible(serverSectionVisible);
            this.panel_server.setVisible(serverSectionVisible);
            Rectangle r = this.getBounds();
            this.setMinimumSize(new Dimension(r.width, r.height - serverSectionSize));
            this.setMaximumSize(new Dimension(r.width, r.height - serverSectionSize));
            this.setSize(new Dimension(r.width, r.height - serverSectionSize));
        }
    }

    public void setData(EIMAccount acc) {
        if (acc != null) {
            this.textfield_imap.setText(acc.get_imap());
            this.combobox_imapPort.setSelectedItem(acc.get_imapPort());
            this.combobox_imapSsl.setSelectedItem(acc.get_imapSsl());
            this.combobox_imapAuthentication.setSelectedItem(acc.get_imapAuthentication());
            this.textfield_smtp.setText(acc.get_smtp());
            this.combobox_smtpPort.setSelectedItem(acc.get_smtpPort());
            this.combobox_smtpSsl.setSelectedItem(acc.get_smtpSsl());
            this.combobox_smtpAuthentication.setSelectedItem(acc.get_smtpAuthentication());
            this.textfield_emailaddress.setText(acc.get_emailaddress());
            this.textfield_username.setText(acc.get_username());
            this.textfield_password.setText(acc.get_password());
        } else {
            clear();
        }
        setServerSectionEnabled(EIMUtility.getInstance().containsHost(this.textfield_emailaddress.getText()));
        checkBadAndNotSupportedServerDBs();

        textfield_imap_changed = false;
        combobox_imapPort_changed = false;
        combobox_imapSsl_changed = false;
        combobox_imapAuthentication_changed = false;
        textfield_smtp_changed = false;
        combobox_smtpPort_changed = false;
        combobox_smtpSsl_changed = false;
        combobox_smtpAuthentication_changed = false;
        textfield_emailaddress_changed = false;
        textfield_username_changed = false;
        textfield_password_changed = false;

        checkValues();
    }

    public void checkServerDB() {
        setServerSectionEnabled(false);
        String str_name = this.textfield_emailaddress.getText();
        if (str_name.contains("@")) {
            int ioa = str_name.lastIndexOf("@") + 1;
            if (ioa < str_name.length()) {
                setServerSectionEnabled(true);
                String host = str_name.substring(ioa);
                String serverData = EIMServerDatabase.getInstance().getData(host);
                if (!serverData.equals(host)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Found server data for host " + host + ": " + serverData);
                    }
                    String[] arr = serverData.split(EIMConstants.SERVER_DATABASE_SEPARATOR);
                    if (arr.length == 8) {
                        EIMAccount acc = new EIMAccount(-1,
                                arr[0], arr[1], arr[2], arr[3],
                                arr[4], arr[5], arr[6], arr[7],
                                "", "", "", -1l);
                        fillServerData(acc);
                    }
                }
            }
        }
    }

    public void clearEmailAddressField() {
        this.textfield_emailaddress.setText("");
    }

    public void clear() {
        EIMAccount acc = new EIMAccount();
        this.textfield_imap.setText(acc.get_imap());
        this.combobox_imapPort.setSelectedItem(acc.get_imapPort());
        this.combobox_imapSsl.setSelectedItem(acc.get_imapSsl());
        this.combobox_imapAuthentication.setSelectedItem(acc.get_imapAuthentication());
        this.textfield_smtp.setText(acc.get_smtp());
        this.combobox_smtpPort.setSelectedItem(acc.get_smtpPort());
        this.combobox_smtpSsl.setSelectedItem(acc.get_smtpSsl());
        this.combobox_smtpAuthentication.setSelectedItem(acc.get_smtpAuthentication());
        this.textfield_emailaddress.setText(acc.get_emailaddress());
        this.textfield_username.setText(acc.get_username());
        this.textfield_password.setText(acc.get_password());

        textfield_imap_changed = false;
        combobox_imapPort_changed = false;
        combobox_imapSsl_changed = false;
        combobox_imapAuthentication_changed = false;
        textfield_smtp_changed = false;
        combobox_smtpPort_changed = false;
        combobox_smtpSsl_changed = false;
        combobox_smtpAuthentication_changed = false;
        textfield_emailaddress_changed = false;
        textfield_username_changed = false;
        textfield_password_changed = false;

        setUserAndOptionsEnabled(true);
        checkBadAndNotSupportedServerDBs();
        setServerSectionEnabled(EIMUtility.getInstance().containsHost(this.textfield_emailaddress.getText()));
        checkValues();
    }

    public boolean containsData() {
        return !textfield_imap.getText().trim().isEmpty()
                || !textfield_smtp.getText().trim().isEmpty()
                || !textfield_emailaddress.getText().trim().isEmpty()
                || !textfield_username.getText().trim().isEmpty()
                || !String.valueOf(this.textfield_password.getPassword()).trim().isEmpty();
    }

    public boolean changedSomething() {
        return (textfield_imap_changed && !textfield_imap.getText().trim().isEmpty())
                || combobox_imapPort_changed
                || combobox_imapSsl_changed
                || combobox_imapAuthentication_changed
                || (textfield_smtp_changed && !textfield_smtp.getText().trim().isEmpty())
                || combobox_smtpPort_changed
                || combobox_smtpSsl_changed
                || combobox_smtpAuthentication_changed
                || (textfield_emailaddress_changed && !textfield_emailaddress.getText().trim().isEmpty())
                || (textfield_username_changed && !textfield_username.getText().trim().isEmpty())
                || (textfield_password_changed && !String.valueOf(this.textfield_password.getPassword()).trim().isEmpty());
    }

    private void addListener() {
        final JTextField textfield = this.textfield_emailaddress;
        final JButton button = this.button_cancel;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                if (textfield.isEnabled()) {
                    textfield.requestFocus();
                } else {
                    if (button.isEnabled()) {
                        button.requestFocus();
                    }
                }
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
        al.add(button_delete);
        al.add(button_showData);
        al.add(button_login);
        al.add(textfield_imap);
        al.add(combobox_imapPort);
        al.add(combobox_imapSsl);
        al.add(combobox_imapAuthentication);
        al.add(textfield_smtp);
        al.add(combobox_smtpPort);
        al.add(combobox_smtpSsl);
        al.add(combobox_smtpAuthentication);
        al.add(textfield_emailaddress);
        al.add(textfield_username);
        al.add(textfield_password);

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

    private void fillServerData(EIMAccount acc) {
        this.textfield_imap.setText(acc.get_imap());
        this.combobox_imapPort.setSelectedItem(acc.get_imapPort());
        this.combobox_imapSsl.setSelectedItem(acc.get_imapSsl());
        this.combobox_imapAuthentication.setSelectedItem(acc.get_imapAuthentication());
        this.textfield_smtp.setText(acc.get_smtp());
        this.combobox_smtpPort.setSelectedItem(acc.get_smtpPort());
        this.combobox_smtpSsl.setSelectedItem(acc.get_smtpSsl());
        this.combobox_smtpAuthentication.setSelectedItem(acc.get_smtpAuthentication());
        setServerSectionEnabled(EIMUtility.getInstance().containsHost(this.textfield_emailaddress.getText()));
        checkValues();
    }

    private boolean valuesOK() {
        String str_imap = this.textfield_imap.getText();
        String str_smtp = this.textfield_smtp.getText();
        String str_emailaddress = this.textfield_emailaddress.getText();
        String str_username = this.textfield_username.getText();
        String str_password = String.valueOf(this.textfield_password.getPassword());
        return !str_imap.isEmpty()
                && !str_smtp.isEmpty()
                && !str_emailaddress.isEmpty()
                && !str_username.isEmpty()
                && EIMUtility.getInstance().containsHost(this.textfield_emailaddress.getText())
                && !str_password.isEmpty();
    }

    private void setUserAndOptionsEnabled(boolean enabled) {
        // user
        label_user.setEnabled(enabled);
        label_emailaddress.setEnabled(enabled);
        label_username.setEnabled(enabled);
        label_password.setEnabled(enabled);
        textfield_emailaddress.setEnabled(enabled);
        textfield_username.setEnabled(enabled);
        textfield_password.setEnabled(enabled);
        // buttons
        button_cancel.setEnabled(enabled);
        button_delete.setEnabled(enabled);
        button_login.setEnabled(enabled);
    }

    private void setServerSectionEnabled(boolean enabled) {
        label_server.setEnabled(enabled);
        label_imap.setEnabled(enabled);
        label_smtp.setEnabled(enabled);
        textfield_imap.setEnabled(enabled);
        combobox_imapPort.setEnabled(enabled);
        combobox_imapSsl.setEnabled(enabled);
        combobox_imapAuthentication.setEnabled(enabled);
        textfield_smtp.setEnabled(enabled);
        combobox_smtpPort.setEnabled(enabled);
        combobox_smtpSsl.setEnabled(enabled);
        combobox_smtpAuthentication.setEnabled(enabled);
    }

    private void checkBadAndNotSupportedServerDBs() {
        String str = this.textfield_emailaddress.getText();
        for (String s : EIMConstants.NOT_SUPPORTED_SERVER_DB) {
            if (str.contains(s)) {
                this.textfield_emailaddress.setBorder(new EIMBubbleBorder(Color.RED.brighter(), 1, 3, 0));
                this.label_warning.setText(
                        EIMI18N.getInstance().getString("ProviderNotSupported_info"));
                return;
            } else {
                this.textfield_emailaddress.setBorder(default_border);
                this.label_warning.setText("");
            }
        }
        for (String s : EIMConstants.BAD_SERVER_DB) {
            if (str.contains(s)) {
                this.textfield_emailaddress.setBorder(new EIMBubbleBorder(Color.RED.brighter(), 1, 3, 0));
                this.label_warning.setText(
                        EIMI18N.getInstance().getString("PushNotSupported_info")
                        + " "
                        + EIMI18N.getInstance().getString("PushNotSupported"));
                return;
            } else {
                this.textfield_emailaddress.setBorder(default_border);
                this.label_warning.setText("");
            }
        }
    }

    private void checkValues() {
        if (!this.textfield_emailaddress.getText().trim().isEmpty()) {
            this.button_delete.setEnabled(true);
        } else {
            this.button_delete.setEnabled(false);
        }
        if (valuesOK()) {
            this.button_login.setEnabled(true);
        } else {
            this.button_login.setEnabled(false);
        }
    }

    private void checkLogin() {
        if (valuesOK()) {
            login(
                    this.textfield_imap.getText(),
                    (String) this.combobox_imapPort.getSelectedItem(),
                    (String) this.combobox_imapSsl.getSelectedItem(),
                    (String) this.combobox_imapAuthentication.getSelectedItem(),
                    this.textfield_smtp.getText(),
                    (String) this.combobox_smtpPort.getSelectedItem(),
                    (String) this.combobox_smtpSsl.getSelectedItem(),
                    (String) this.combobox_smtpAuthentication.getSelectedItem(),
                    this.textfield_emailaddress.getText(),
                    this.textfield_username.getText(),
                    String.valueOf(this.textfield_password.getPassword()));
        } else {
            onError(
                    this.textfield_emailaddress.getText(),
                    EIMI18N.getInstance().getString("OneValueNotValid_info"),
                    EIMI18N.getInstance().getString("OneValueNotValid"));
        }
    }

    private void delete_internal() {
        delete(this.textfield_emailaddress.getText());
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

        panel_head = new javax.swing.JPanel();
        label_logo = new javax.swing.JLabel();
        label_logo_text = new javax.swing.JLabel();
        panel_main = new javax.swing.JPanel();
        panel_user = new javax.swing.JPanel();
        label_user = new javax.swing.JLabel();
        panel_name = new javax.swing.JPanel();
        label_emailaddress = new javax.swing.JLabel();
        label_password = new javax.swing.JLabel();
        label_username = new javax.swing.JLabel();
        panel_password = new javax.swing.JPanel();
        textfield_emailaddress = new javax.swing.JTextField();
        textfield_password = new javax.swing.JPasswordField();
        textfield_username = new javax.swing.JTextField();
        label_warning = new javax.swing.JLabel();
        panel_server = new javax.swing.JPanel();
        label_server = new javax.swing.JLabel();
        panel_in = new javax.swing.JPanel();
        label_empty1 = new javax.swing.JLabel();
        label_imap = new javax.swing.JLabel();
        label_smtp = new javax.swing.JLabel();
        panel_serverAddress = new javax.swing.JPanel();
        label_serverAddress = new javax.swing.JLabel();
        textfield_imap = new javax.swing.JTextField();
        textfield_smtp = new javax.swing.JTextField();
        panel_port = new javax.swing.JPanel();
        label_port = new javax.swing.JLabel();
        combobox_imapPort = new javax.swing.JComboBox(EIMConstants.IMAP_PORTS);
        combobox_smtpPort = new javax.swing.JComboBox(EIMConstants.SMTP_PORTS);
        panel_ssl = new javax.swing.JPanel();
        label_ssl = new javax.swing.JLabel();
        combobox_imapSsl = new javax.swing.JComboBox(EIMConstants.IMAP_SSL);
        combobox_smtpSsl = new javax.swing.JComboBox(EIMConstants.SMTP_SSL);
        panel_authentication = new javax.swing.JPanel();
        label_authentication = new javax.swing.JLabel();
        combobox_imapAuthentication = new javax.swing.JComboBox(EIMConstants.IMAP_AUTHENTICATION);
        combobox_smtpAuthentication = new javax.swing.JComboBox(EIMConstants.SMTP_AUTHENTICATION);
        panel_buttons = new javax.swing.JPanel();
        button_cancel = new javax.swing.JButton();
        button_delete = new javax.swing.JButton();
        button_showData = new javax.swing.JButton();
        button_login = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Login");
        setMaximumSize(new java.awt.Dimension(581, 302));
        setMinimumSize(new java.awt.Dimension(581, 302));
        setName("login_frame"); // NOI18N
        setPreferredSize(new java.awt.Dimension(581, 302));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        label_logo.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        label_logo.setText("LOGO");
        panel_head.add(label_logo);

        label_logo_text.setFont(new java.awt.Font("Lucida Grande", 1, 18)); // NOI18N
        label_logo_text.setText("Login");
        panel_head.add(label_logo_text);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        getContentPane().add(panel_head, gridBagConstraints);

        panel_main.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel_main.setName(""); // NOI18N
        panel_main.setLayout(new java.awt.GridBagLayout());

        panel_user.setLayout(new java.awt.GridBagLayout());

        label_user.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        label_user.setText("User");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_user.add(label_user, gridBagConstraints);

        panel_name.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        label_emailaddress.setText("Email address:");
        panel_name.add(label_emailaddress);

        label_password.setText("Password:");
        panel_name.add(label_password);

        label_username.setText("Username:");
        panel_name.add(label_username);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panel_user.add(panel_name, gridBagConstraints);

        panel_password.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        textfield_emailaddress.setNextFocusableComponent(textfield_password);
        textfield_emailaddress.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_emailaddressKeyReleased(evt);
            }
        });
        panel_password.add(textfield_emailaddress);

        textfield_password.setNextFocusableComponent(textfield_username);
        textfield_password.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_passwordKeyReleased(evt);
            }
        });
        panel_password.add(textfield_password);

        textfield_username.setNextFocusableComponent(button_login);
        textfield_username.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_usernameKeyReleased(evt);
            }
        });
        panel_password.add(textfield_username);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 5.0;
        gridBagConstraints.weighty = 5.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panel_user.add(panel_password, gridBagConstraints);

        label_warning.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        label_warning.setForeground(new java.awt.Color(255, 0, 0));
        label_warning.setText("WARNING");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        panel_user.add(label_warning, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panel_main.add(panel_user, gridBagConstraints);

        panel_server.setLayout(new java.awt.GridBagLayout());

        label_server.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        label_server.setText("Server");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_server.add(label_server, gridBagConstraints);

        panel_in.setLayout(new java.awt.GridLayout(3, 1, 0, 2));
        panel_in.add(label_empty1);

        label_imap.setText("IMAP:");
        panel_in.add(label_imap);

        label_smtp.setText("SMTP:");
        panel_in.add(label_smtp);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panel_server.add(panel_in, gridBagConstraints);

        panel_serverAddress.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        label_serverAddress.setForeground(new java.awt.Color(153, 153, 153));
        label_serverAddress.setText("Server address");
        panel_serverAddress.add(label_serverAddress);

        textfield_imap.setNextFocusableComponent(combobox_imapPort);
        textfield_imap.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_imapKeyReleased(evt);
            }
        });
        panel_serverAddress.add(textfield_imap);

        textfield_smtp.setNextFocusableComponent(combobox_smtpPort);
        textfield_smtp.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                textfield_smtpKeyReleased(evt);
            }
        });
        panel_serverAddress.add(textfield_smtp);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 8.0;
        gridBagConstraints.weighty = 8.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_server.add(panel_serverAddress, gridBagConstraints);

        panel_port.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        label_port.setForeground(new java.awt.Color(153, 153, 153));
        label_port.setText("Port");
        panel_port.add(label_port);

        combobox_imapPort.setNextFocusableComponent(combobox_imapSsl);
        combobox_imapPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_imapPortActionPerformed(evt);
            }
        });
        panel_port.add(combobox_imapPort);

        combobox_smtpPort.setNextFocusableComponent(combobox_smtpSsl);
        combobox_smtpPort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_smtpPortActionPerformed(evt);
            }
        });
        panel_port.add(combobox_smtpPort);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 11;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_server.add(panel_port, gridBagConstraints);

        panel_ssl.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        label_ssl.setForeground(new java.awt.Color(153, 153, 153));
        label_ssl.setText("SSL");
        panel_ssl.add(label_ssl);

        combobox_imapSsl.setNextFocusableComponent(combobox_imapAuthentication);
        combobox_imapSsl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_imapSslActionPerformed(evt);
            }
        });
        panel_ssl.add(combobox_imapSsl);

        combobox_smtpSsl.setNextFocusableComponent(combobox_smtpAuthentication);
        combobox_smtpSsl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_smtpSslActionPerformed(evt);
            }
        });
        panel_ssl.add(combobox_smtpSsl);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 12;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_server.add(panel_ssl, gridBagConstraints);

        panel_authentication.setLayout(new java.awt.GridLayout(3, 1, 0, 2));

        label_authentication.setForeground(new java.awt.Color(153, 153, 153));
        label_authentication.setText("Authentication");
        panel_authentication.add(label_authentication);

        combobox_imapAuthentication.setNextFocusableComponent(textfield_smtp);
        combobox_imapAuthentication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_imapAuthenticationActionPerformed(evt);
            }
        });
        panel_authentication.add(combobox_imapAuthentication);

        combobox_smtpAuthentication.setNextFocusableComponent(button_login);
        combobox_smtpAuthentication.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                combobox_smtpAuthenticationActionPerformed(evt);
            }
        });
        panel_authentication.add(combobox_smtpAuthentication);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 13;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
        panel_server.add(panel_authentication, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        panel_main.add(panel_server, gridBagConstraints);

        panel_buttons.setLayout(new java.awt.GridLayout(1, 4));

        button_cancel.setText("Cancel");
        button_cancel.setNextFocusableComponent(textfield_emailaddress);
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

        button_delete.setText("Delete");
        button_delete.setNextFocusableComponent(button_showData);
        button_delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_deleteActionPerformed(evt);
            }
        });
        button_delete.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_deleteKeyReleased(evt);
            }
        });
        panel_buttons.add(button_delete);

        button_showData.setText("Show data");
        button_showData.setNextFocusableComponent(button_cancel);
        button_showData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_showDataActionPerformed(evt);
            }
        });
        button_showData.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_showDataKeyReleased(evt);
            }
        });
        panel_buttons.add(button_showData);

        button_login.setText("Log in");
        button_login.setNextFocusableComponent(button_showData);
        button_login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_loginActionPerformed(evt);
            }
        });
        button_login.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_loginKeyReleased(evt);
            }
        });
        panel_buttons.add(button_login);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        panel_main.add(panel_buttons, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 10.0;
        gridBagConstraints.weighty = 10.0;
        getContentPane().add(panel_main, gridBagConstraints);

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

    private void button_loginKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_loginKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            checkLogin();
        }
    }//GEN-LAST:event_button_loginKeyReleased

    private void button_loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_loginActionPerformed
        checkLogin();
    }//GEN-LAST:event_button_loginActionPerformed

    private void textfield_emailaddressKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_emailaddressKeyReleased
        if (evt.getKeyCode() != KeyEvent.VK_ESCAPE) {
            textfield_emailaddress_changed = true;
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                checkLogin();
            } else {
                textfield_username.setText(EIMUtility.getInstance().getUsernameWithoutHost(textfield_emailaddress.getText()));
                checkServerDB();
                checkBadAndNotSupportedServerDBs();
                checkValues();
            }
        }
    }//GEN-LAST:event_textfield_emailaddressKeyReleased

    private void textfield_imapKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_imapKeyReleased
        if (evt.getKeyCode() != KeyEvent.VK_ESCAPE) {
            textfield_imap_changed = true;
            checkValues();
        }
    }//GEN-LAST:event_textfield_imapKeyReleased

    private void textfield_smtpKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_smtpKeyReleased
        if (evt.getKeyCode() != KeyEvent.VK_ESCAPE) {
            textfield_smtp_changed = true;
            checkValues();
        }
    }//GEN-LAST:event_textfield_smtpKeyReleased

    private void textfield_passwordKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_passwordKeyReleased
        if (evt.getKeyCode() != KeyEvent.VK_ESCAPE) {
            textfield_password_changed = true;
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                checkLogin();
            } else {
                checkValues();
            }
        }
    }//GEN-LAST:event_textfield_passwordKeyReleased

    private void combobox_imapPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_imapPortActionPerformed
        combobox_imapPort_changed = true;
    }//GEN-LAST:event_combobox_imapPortActionPerformed

    private void combobox_imapSslActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_imapSslActionPerformed
        combobox_imapSsl_changed = true;
    }//GEN-LAST:event_combobox_imapSslActionPerformed

    private void combobox_imapAuthenticationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_imapAuthenticationActionPerformed
        combobox_imapAuthentication_changed = true;
    }//GEN-LAST:event_combobox_imapAuthenticationActionPerformed

    private void combobox_smtpPortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_smtpPortActionPerformed
        combobox_smtpPort_changed = true;
    }//GEN-LAST:event_combobox_smtpPortActionPerformed

    private void combobox_smtpSslActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_smtpSslActionPerformed
        combobox_smtpSsl_changed = true;
    }//GEN-LAST:event_combobox_smtpSslActionPerformed

    private void combobox_smtpAuthenticationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_combobox_smtpAuthenticationActionPerformed
        combobox_smtpAuthentication_changed = true;
    }//GEN-LAST:event_combobox_smtpAuthenticationActionPerformed

    private void textfield_usernameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_textfield_usernameKeyReleased
        if (evt.getKeyCode() != KeyEvent.VK_ESCAPE) {
            textfield_username_changed = true;
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                checkLogin();
            } else {
                checkValues();
            }
        }
    }//GEN-LAST:event_textfield_usernameKeyReleased

    private void button_deleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_deleteActionPerformed
        delete_internal();
    }//GEN-LAST:event_button_deleteActionPerformed

    private void button_deleteKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_deleteKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            delete_internal();
        }
    }//GEN-LAST:event_button_deleteKeyReleased

    private void button_showDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_showDataActionPerformed
        setServerSectionVisible(!serverSectionVisible);
    }//GEN-LAST:event_button_showDataActionPerformed

    private void button_showDataKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_showDataKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            setServerSectionVisible(!serverSectionVisible);
        }
    }//GEN-LAST:event_button_showDataKeyReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_cancel;
    private javax.swing.JButton button_delete;
    private javax.swing.JButton button_login;
    private javax.swing.JButton button_showData;
    private javax.swing.JComboBox combobox_imapAuthentication;
    private javax.swing.JComboBox combobox_imapPort;
    private javax.swing.JComboBox combobox_imapSsl;
    private javax.swing.JComboBox combobox_smtpAuthentication;
    private javax.swing.JComboBox combobox_smtpPort;
    private javax.swing.JComboBox combobox_smtpSsl;
    private javax.swing.JLabel label_authentication;
    private javax.swing.JLabel label_emailaddress;
    private javax.swing.JLabel label_empty1;
    private javax.swing.JLabel label_imap;
    private javax.swing.JLabel label_logo;
    private javax.swing.JLabel label_logo_text;
    private javax.swing.JLabel label_password;
    private javax.swing.JLabel label_port;
    private javax.swing.JLabel label_server;
    private javax.swing.JLabel label_serverAddress;
    private javax.swing.JLabel label_smtp;
    private javax.swing.JLabel label_ssl;
    private javax.swing.JLabel label_user;
    private javax.swing.JLabel label_username;
    private javax.swing.JLabel label_warning;
    private javax.swing.JPanel panel_authentication;
    private javax.swing.JPanel panel_buttons;
    private javax.swing.JPanel panel_head;
    private javax.swing.JPanel panel_in;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_name;
    private javax.swing.JPanel panel_password;
    private javax.swing.JPanel panel_port;
    private javax.swing.JPanel panel_server;
    private javax.swing.JPanel panel_serverAddress;
    private javax.swing.JPanel panel_ssl;
    private javax.swing.JPanel panel_user;
    private javax.swing.JTextField textfield_emailaddress;
    private javax.swing.JTextField textfield_imap;
    private javax.swing.JPasswordField textfield_password;
    private javax.swing.JTextField textfield_smtp;
    private javax.swing.JTextField textfield_username;
    // End of variables declaration//GEN-END:variables
}
