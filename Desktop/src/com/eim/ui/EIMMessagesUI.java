/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui;

import com.eim.eim.EIMController;
import com.eim.mail.EIMAccount;
import com.eim.mail.EIMEmailAccount;
import com.eim.mail.EIMEmailMessage;
import com.eim.util.EIMConstants;
import com.eim.types.EIMEmailComparable;
import com.eim.db.EIMI18N;
import com.eim.img.EIMImage;
import com.eim.sound.EIMSound;
import com.eim.ui.components.EIMBubbleBorder;
import com.eim.util.EIMUtility;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.mail.MessagingException;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMMessagesUI
 *
 * @author Denis Meyer
 */
public abstract class EIMMessagesUI extends JFrame {

    private static final Logger logger = LogManager.getLogger(EIMMessagesUI.class.getName());
    private final EIMController controller;
    private EIMEmailAccount account = null;
    private String sendTo = "";
    private boolean currentlySending = false;
    private HTMLEditorKit kit = null;
    private HTMLDocument doc = null;
    private HashMap<String, String> toReplyToMap = null;
    private HashMap<String, String> messageIdMap = null;
    private ArrayList<EIMEmailMessage> emailList = null;
    private Date lastReceivedMsg = null;
    private Date lastSentMsg = null;
    private int noOfReceivedMsgs = 0;
    private int noOfSentMsgs = 0;
    private Color color_me = new Color(46, 139, 87);
    private boolean sending = false;

    public EIMMessagesUI(EIMController controller, EIMEmailAccount from, String to) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMMessagesUI");
        }
        this.controller = controller;
        this.account = from;
        this.sendTo = EIMUtility.getInstance().getMainAlias(to);
        toReplyToMap = new HashMap<>();
        messageIdMap = new HashMap<>();
        emailList = new ArrayList<>();

        try {
            this.setIconImage(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_ICON)).getImage());
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }

        initComponents();

        this.setTitle(EIMI18N.getInstance().getString("Conversation"));
        this.button_send.setText(EIMI18N.getInstance().getString("Send"));
        this.label_from_text.setText(EIMI18N.getInstance().getString("From") + ":");
        this.label_to_text.setText(EIMI18N.getInstance().getString("To") + ":");
        this.label_information.setText("");
        this.label_from.setText(account.getAccount().get_emailaddress());
        this.label_to.setText(to);

        this.scrollpane_text_conversation.setBorder(new EIMBubbleBorder(new Color(92, 172, 238), 1, 3, 4));
        this.editorpane.setEditable(false);

        this.label_from.setForeground(color_me.darker());
        this.label_to.setForeground(new Color(100, 149, 237).darker());

        this.button_send.setEnabled(false);
        this.text_write.requestFocus();

        prepareHTML();
        addListener();

        this.setLocationRelativeTo(null);
    }

    public abstract void mailSent(String from, String to);

    public abstract void couldNotSendMail(String info, String msg, String content);

    public void deinit() {
        toReplyToMap.clear();
        messageIdMap.clear();
        emailList.clear();
    }

    public String getAccountName() {
        return account.getAccount().get_emailaddress();
    }

    public String getToName() {
        return sendTo;
    }

    public synchronized boolean addMailToConversation(EIMEmailMessage msg, boolean addFrom) {
        if (controller.displayOnlyEIMMsgs() && !EIMUtility.getInstance().messageSentFromEIM(msg)) {
            return false;
        }
        boolean added = false;
        if (controller.getAccountManager() != null) {
            if ((doc != null) && (kit != null)) {
                try {
                    if (account != null) {
                        emailList.add(msg);
                        String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1]);
                        String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1]);
                        String replyTo = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstReplyTo())[1]);
                        /*
                         if(logger.isDebugEnabled()) {
                         logger.debug("FROM: " + from + ", TO: " + to + ", IN: " + ((from.equals(sendTo) && account.getAccount().isAlias(to)) || (addFrom && account.getAccount().isAlias(from) && to.equals(sendTo))));
                         }
                         */
                        replyTo = replyTo.isEmpty() ? to : replyTo;
                        Date d = msg.getEnvelope().getDateSent();
                        messageIdMap.put(to, msg.getEnvelope().getID());
                        messageIdMap.put(from, msg.getEnvelope().getID());
                        if (from.equals(sendTo) && account.getAccount().isAlias(to)) {
                            lastReceivedMsg = msg.getEnvelope().getDateSent();
                            ++noOfReceivedMsgs;
                            toReplyToMap.put(from, replyTo);
                            kit.insertHTML(
                                    doc,
                                    doc.getLength(),
                                    "<div class='speech_left'>"
                                    + "<div class='from'> "
                                    + EIMConstants.DATE_FORMAT_CHAT.format(d) + " (" + (noOfSentMsgs + noOfReceivedMsgs) + ")"
                                    + "</div>"
                                    + "<div class='content'> "
                                    + EIMUtility.getInstance().escapeHTML(msg.getContent().getContent())
                                    + "</div>"
                                    + "</div>"
                                    + "<div class='normal'></div>",
                                    0,
                                    0,
                                    null);
                            added = true;
                        } else if (addFrom && account.getAccount().isAlias(from) && to.equals(sendTo)) {
                            lastSentMsg = msg.getEnvelope().getDateSent();
                            ++noOfSentMsgs;
                            kit.insertHTML(
                                    doc,
                                    doc.getLength(),
                                    "<div class='speech_right'>"
                                    + "<div class='from'> "
                                    + EIMConstants.DATE_FORMAT_CHAT.format(d) + " (" + (noOfSentMsgs + noOfReceivedMsgs) + ")"
                                    + "</div>"
                                    + "<div class='content'> "
                                    + EIMUtility.getInstance().escapeHTML(msg.getContent().getContent())
                                    + "</div>"
                                    + "</div>"
                                    + "<div class='normal'></div>",
                                    0,
                                    0,
                                    null);
                            added = true;
                        }
                        updateInformation();
                        updateScrollpanePosition();
                    }
                } catch (BadLocationException e) {
                    logger.error("BadLocationException: " + e.getMessage());
                } catch (IOException e) {
                    logger.error("IOException: " + e.getMessage());
                }
            }
        }
        return added;
    }

    public boolean currentlySending() {
        return currentlySending;
    }

    public void setAllEnabled(boolean enabled) {
        this.label_from_text.setEnabled(enabled);
        this.label_from.setEnabled(enabled);
        this.label_to_text.setEnabled(enabled);
        this.label_to.setEnabled(enabled);
        this.text_write.setEnabled(enabled);
        this.button_send.setEnabled(enabled);
    }

    private void addListener() {
        final EIMEmailAccount _from = account;
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                loadConversation();
                text_writeKeyReleased(null);
                if (!sending && (_from.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)) {
                    setAllEnabled(true);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateScrollpanePosition();
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
        this.addKeyListener(escapePressed);
        this.label_from.addKeyListener(escapePressed);
        this.label_to.addKeyListener(escapePressed);
        this.text_write.addKeyListener(escapePressed);
        this.button_send.addKeyListener(escapePressed);
        this.editorpane.addKeyListener(escapePressed);

        this.text_write.getInputMap().put(
                EIMUtility.getInstance().platformIsMac() ? EIMConstants.KEYSTROKE_SEND_MESSAGE_MAC : EIMConstants.KEYSTROKE_SEND_MESSAGE_WINDOWS,
                "sendMessage");
        this.text_write.getInputMap().put(
                EIMUtility.getInstance().platformIsMac() ? EIMConstants.KEYSTROKE_CLOSE_WINDOW_MAC : EIMConstants.KEYSTROKE_CLOSE_WINDOW_WINDOWS,
                "closeWindow");
        this.button_send.getInputMap().put(
                EIMUtility.getInstance().platformIsMac() ? EIMConstants.KEYSTROKE_CLOSE_WINDOW_MAC : EIMConstants.KEYSTROKE_CLOSE_WINDOW_WINDOWS,
                "closeWindow");

        this.text_write.getActionMap().put(
                "sendMessage",
                new SendMessageAction());
        this.text_write.getActionMap().put(
                "closeWindow",
                new CloseWindowAction());
        this.button_send.getActionMap().put(
                "closeWindow",
                new CloseWindowAction());
    }

    private void updateInformation() {
        if (lastSentMsg != null) {
            this.label_from.setToolTipText("Last sent message: " + EIMConstants.DATE_FORMAT.format(lastSentMsg));
        } else {
            this.label_from.setToolTipText("");
        }
        if (lastReceivedMsg != null) {
            this.label_to.setToolTipText("Last received message: " + EIMConstants.DATE_FORMAT.format(lastReceivedMsg));
        } else {
            this.label_to.setToolTipText("");
        }
        this.label_information.setText("Received messages: " + noOfReceivedMsgs + ", sent messages: " + noOfSentMsgs);
    }

    private void prepareHTML() {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing HTML");
        }
        kit = new HTMLEditorKit();
        editorpane.setEditorKit(kit);
        editorpane.setDocument(kit.createDefaultDocument());
        editorpane.setText("");
        editorpane.setContentType("text/html");
        doc = (HTMLDocument) editorpane.getDocument();

        StyleSheet css = kit.getStyleSheet();
        css.addRule(".normal {"
                + "color: #000000;"
                + "font-size: 0%;"
                + "}");
        css.addRule(".from {"
                + "color: #808080;"
                + "font-size: 95%;"
                + "}");
        css.addRule(".content {"
                + "font-size: 100%;"
                + "}");
        css.addRule(".speech_left {"
                + "position: relative;"
                + "color: #000000;"
                + "background: #C6E2FF;"
                + "margin: 2px 30px 2px 0px;"
                + "text-align: left;"
                + "border: 1px solid #808080;"
                + "padding: 2px 2px 2px 2px;"
                + "}");
        css.addRule(".speech_right {"
                + "position: relative;"
                + "color: #000000;"
                + "background: #BDFCC9;"
                + "margin: 2px 0px 2px 30px;"
                + "text-align: left;"
                + "border: 1px solid #808080;"
                + "padding: 2px 2px 2px 2px;"
                + "}");
    }

    private void updateScrollpanePosition() {
        editorpane.setCaretPosition(doc.getLength());
    }

    private synchronized void loadConversation() {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading conversation");
            }
            editorpane.setText("");
            emailList.clear();
            noOfReceivedMsgs = 0;
            noOfSentMsgs = 0;
            if (account != null) {
                ArrayList<EIMEmailMessage> emails = new ArrayList<>();
                for (Map.Entry pairs : controller.getAccountManager().getEmails().entrySet()) {
                    EIMEmailMessage msg = (EIMEmailMessage) pairs.getValue();
                    emails.add(msg);
                }
                Collections.sort(emails, new EIMEmailComparable());
                for (EIMEmailMessage msg : emails) {
                    addMailToConversation(msg, true);
                }
                updateScrollpanePosition();
                // this.text_write.requestFocus();
            }
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
    }

    private void cancel() {
        this.setVisible(false);
    }

    private synchronized void sendMessage() {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to send message");
        }
        String text = text_write.getText();
        if (!text.trim().isEmpty()) {
            currentlySending = true;
            setAllEnabled(false);

            final JTextArea textfield = this.text_write;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Send message thread running");
                    }
                    boolean sent = false;
                    EIMEmailMessage msg = null;
                    try {
                        sending = true;
                        if (account != null) {
                            if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN) {
                                Callable<EIMEmailMessage> sendMessage = new Callable<EIMEmailMessage>() {
                                    @Override
                                    public EIMEmailMessage call() throws Exception {
                                        EIMEmailMessage msg = null;
                                        String replyTo = toReplyToMap.get(sendTo);
                                        if (replyTo != null) {
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("Using reply-to address");
                                            }
                                        } else {
                                            if (logger.isDebugEnabled()) {
                                                logger.debug("Using to address");
                                            }
                                            replyTo = sendTo;
                                        }
                                        if (EIMUtility.getInstance().containsHost(replyTo)) {
                                            Date date = new Date();
                                            String subject = EIMConstants.SENDMAIL_SUBJECT + " " + EIMConstants.DATE_FORMAT.format(date);
                                            String text = text_write.getText();
                                            if (EIMConstants.ADD_CUSTOM_TEXT_TO_EMAIL) {
                                                text += EIMUtility.getInstance().getCustomTextSendingEmail();
                                            }

                                            try {
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("Sending message: (REPLY-)TO: " + replyTo + ", Subject: " + subject /* + ", Text: " + text */);
                                                }
                                                String msgId = messageIdMap.get(sendTo); // replyTo
                                                msgId = (msgId == null) ? "" : msgId;
                                                msg = account.sendMail(replyTo, subject, text, msgId);
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("Successfully sent message");
                                                }
                                                text_write.setText("");
                                                text_write.requestFocus();
                                                if (controller.playSounds()) {
                                                    EIMSound.getInstance().playSound(EIMConstants.SOUND.SOUND_SENT);
                                                }
                                                mailSent(account.getAccount().get_emailaddress(), replyTo);
                                            } catch (MessagingException e) {
                                                logger.error("Account not logged in: MessagingException: " + e.getMessage());
                                                couldNotSendMail(
                                                        account.getAccount().get_emailaddress(),
                                                        EIMI18N.getInstance().getString("CouldNotSendMessage_info"),
                                                        EIMI18N.getInstance().getString("CouldNotSendMessage"));
                                            }
                                        } else {
                                            logger.error("To address does not contain host: " + replyTo);
                                            couldNotSendMail(
                                                    account.getAccount().get_emailaddress(),
                                                    EIMI18N.getInstance().getString("RecipientNotValid_info"),
                                                    EIMI18N.getInstance().getString("RecipientNotValid"));
                                        }
                                        return msg;
                                    }
                                };
                                ExecutorService service = Executors.newSingleThreadExecutor();
                                try {
                                    final Future<EIMEmailMessage> f = service.submit(sendMessage);

                                    msg = f.get(EIMConstants.MAX_MESSAGE_SEND_TIME, TimeUnit.SECONDS);
                                    sent = true;
                                } catch (final TimeoutException e) {
                                    logger.error("TimeoutException: Sending the message took too long");
                                    couldNotSendMail(
                                            account.getAccount().get_emailaddress(),
                                            EIMI18N.getInstance().getString("DeliveringTooLong"),
                                            EIMI18N.getInstance().getString("TryAgain"));
                                } catch (InterruptedException e) {
                                    logger.error("InterruptedException: " + e.getMessage());
                                } catch (ExecutionException e) {
                                    logger.error("ExecutionException: " + e.getMessage());
                                } catch (Exception e) {
                                    logger.error("Exception: " + e.getMessage());
                                } finally {
                                    service.shutdown();
                                }
                            } else {
                                logger.error("Account not logged in");
                                couldNotSendMail(
                                        account.getAccount().get_emailaddress(),
                                        EIMI18N.getInstance().getString("AccountNotLoggedIn_info"),
                                        EIMI18N.getInstance().getString("AccountNotLoggedIn"));
                            }
                        } else {
                            logger.error("Account not found");
                        }
                    } catch (Exception e) {
                        logger.error("Exception: " + e.getMessage());
                    } finally {
                        setAllEnabled(true);
                        currentlySending = false;
                        if (sent && (msg != null)) {
                            controller.getAccountManager().addMail(msg);
                            addMailToConversation(msg, true);
                        }
                        textfield.requestFocus();
                        sending = false;
                    }
                }
            });
            t.start();
        }
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
        panel_display = new javax.swing.JPanel();
        label_from_text = new javax.swing.JLabel();
        label_from = new javax.swing.JLabel();
        label_to_text = new javax.swing.JLabel();
        label_to = new javax.swing.JLabel();
        label_information = new javax.swing.JLabel();
        panel_text = new javax.swing.JPanel();
        panel_write = new javax.swing.JPanel();
        scrollpane_write = new javax.swing.JScrollPane();
        text_write = new javax.swing.JTextArea();
        button_send = new javax.swing.JButton();
        scrollpane_text_conversation = new javax.swing.JScrollPane();
        editorpane = new javax.swing.JEditorPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Conversation");
        setMinimumSize(new java.awt.Dimension(540, 456));
        setPreferredSize(new java.awt.Dimension(540, 456));
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        panel_main.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel_main.setLayout(new java.awt.GridBagLayout());

        panel_display.setLayout(new java.awt.GridBagLayout());

        label_from_text.setForeground(new java.awt.Color(102, 102, 102));
        label_from_text.setText("From:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        panel_display.add(label_from_text, gridBagConstraints);

        label_from.setText("FROM");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 5.0;
        panel_display.add(label_from, gridBagConstraints);

        label_to_text.setForeground(new java.awt.Color(102, 102, 102));
        label_to_text.setText("To:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        panel_display.add(label_to_text, gridBagConstraints);

        label_to.setText("TO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 5.0;
        panel_display.add(label_to, gridBagConstraints);

        label_information.setForeground(new java.awt.Color(102, 102, 102));
        label_information.setText("INFO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 5.0;
        panel_display.add(label_information, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 2, 0);
        panel_main.add(panel_display, gridBagConstraints);

        panel_text.setLayout(new java.awt.BorderLayout());

        panel_write.setLayout(new java.awt.GridBagLayout());

        text_write.setBackground(new java.awt.Color(248, 255, 248));
        text_write.setColumns(20);
        text_write.setLineWrap(true);
        text_write.setRows(4);
        text_write.setTabSize(4);
        text_write.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                text_writeKeyReleased(evt);
            }
        });
        scrollpane_write.setViewportView(text_write);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panel_write.add(scrollpane_write, gridBagConstraints);

        button_send.setForeground(new java.awt.Color(46, 139, 87));
        button_send.setText("Send");
        button_send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                button_sendActionPerformed(evt);
            }
        });
        button_send.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                button_sendKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 0);
        panel_write.add(button_send, gridBagConstraints);

        panel_text.add(panel_write, java.awt.BorderLayout.PAGE_END);

        scrollpane_text_conversation.setViewportView(editorpane);

        panel_text.add(scrollpane_text_conversation, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panel_main.add(panel_text, gridBagConstraints);

        getContentPane().add(panel_main);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void button_sendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_button_sendActionPerformed
        sendMessage();
        this.text_write.requestFocus();
    }//GEN-LAST:event_button_sendActionPerformed

    private void button_sendKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_button_sendKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            sendMessage();
        }
    }//GEN-LAST:event_button_sendKeyReleased

    private void text_writeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_text_writeKeyReleased
        if (this.text_write.getText().trim().isEmpty()) {
            this.button_send.setEnabled(false);
            this.button_send.setForeground(Color.GRAY);
        } else {
            this.button_send.setEnabled(true);
            this.button_send.setForeground(color_me);
        }
    }//GEN-LAST:event_text_writeKeyReleased

    private class SendMessageAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent tf) {
            sendMessage();
        }
    }

    private class CloseWindowAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent tf) {
            cancel();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton button_send;
    private javax.swing.JEditorPane editorpane;
    private javax.swing.JLabel label_from;
    private javax.swing.JLabel label_from_text;
    private javax.swing.JLabel label_information;
    private javax.swing.JLabel label_to;
    private javax.swing.JLabel label_to_text;
    private javax.swing.JPanel panel_display;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_text;
    private javax.swing.JPanel panel_write;
    private javax.swing.JScrollPane scrollpane_text_conversation;
    private javax.swing.JScrollPane scrollpane_write;
    private javax.swing.JTextArea text_write;
    // End of variables declaration//GEN-END:variables
}
