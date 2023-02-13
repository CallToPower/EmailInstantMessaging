/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Properties;
import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.StoreClosedException;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import com.eim.util.EIMConstants;
import com.eim.exceptions.EIMEmptyIMAPFolderException;
import com.eim.exceptions.EIMFetchException;
import com.eim.exceptions.EIMInvalidIMAPFolderException;
import com.eim.exceptions.EIMMessagingException;
import com.eim.exceptions.EIMServerException;
import com.eim.util.EIMUtility;
import java.util.Date;
import javax.mail.FetchProfile;
import javax.mail.UIDFolder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.mail.Flags.Flag;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

/**
 * EIMEmailAccount
 *
 * @author Denis Meyer
 */
public abstract class EIMEmailAccount implements Runnable {

    private static final Logger logger = LogManager.getLogger(EIMEmailAccount.class.getName());
    private final ArrayList<EIMEmailMessage> email_list;
    private Message[] msgs = null;
    private static final String inboxFolderName = "INBOX";
    private Folder[] allStoreFolders = null;
    private boolean connected = false;
    private boolean usePush = true;
    private IMAPStore server;
    private Session session;
    private IMAPFolder folder;
    private MessageCountListener messageCountListener, externalCountListener;
    private MessageChangedListener messageChangedListener, externalChangedListener;
    private EIMNetworkProber prober;
    private EIMEmailPoller poller;
    private Thread pushThread;
    private final EIMAccount account;
    private long lastMessageUID = -1l;
    private long fetchedTo = -1l;
    private boolean firstFetch = false;
    private String firstFetchedFolder = "";
    private boolean disconnected = false;

    public EIMEmailAccount(EIMAccount account) {
        this.account = account;
        this.email_list = new ArrayList<>();
    }

    public abstract void onError(Exception e);

    public abstract void onDisconnect();

    public abstract void onConnectionLost();

    public abstract void onConnect();

    public abstract void onNewMessages(ArrayList<EIMEmailMessage> msgs);

    public abstract void onMailFetched(boolean fetchSuccessful, long uid, boolean firstFetch);

    public abstract void onPushNotAvailable();

    @Override
    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Starting thread");
        }
        this.initConnection();
    }

    public EIMAccount getAccount() {
        return account;
    }

    public void connect() {
        if (!connected && !server.isConnected()) {
            try {
                server.connect(account.get_imap(), Integer.parseInt(account.get_imapPort()), account.get_username(), account.get_password());
                selectFolder("");
                prober.start();
                connected = true;
                if (logger.isDebugEnabled()) {
                    logger.debug(account.get_emailaddress() + " connected!");
                }
                disconnected = false;
                onConnect();
            } catch (AuthenticationFailedException e) {
                connected = false;
                logger.error("AuthenticationFailedException for username " + account.get_emailaddress() + ": " + e.getMessage());
                onError(e);
            } catch (MessagingException e) {
                connected = false;
                folder = null;
                messageChangedListener = null;
                messageCountListener = null;
                logger.error("MessagingException for username " + account.get_emailaddress() + ": " + e.getMessage());
                onError(new EIMMessagingException(e.getMessage(), EIMMessagingException.STATUS.NO_CONNECTION_TO_STORE));
            } catch (IllegalStateException e) {
                logger.error("IllegalStateException for username " + account.get_emailaddress() + ": " + e.getMessage());
                connected = true;
                onError(e);
            }
        }
    }

    private synchronized void forceDisconnect_helper(final boolean connectionLost) {
        connected = false;
        if (logger.isDebugEnabled()) {
            logger.debug(account.get_emailaddress() + " disconnected");
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (prober != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Stopping prober");
                    }
                    prober.stop();
                }
                if (poller != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Stopping poller");
                    }
                    poller.stop();
                }
            }
        });
        t.start();
        if (!connectionLost) {
            onDisconnect();
        } else {
            onConnectionLost();
        }
    }

    public synchronized void forceDisconnect(final boolean connectionLost) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Callable<Boolean> disconnect = new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        disconnected = true;
                        MessagingException me = null;
                        try {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Closing folder");
                            }
                            closeFolder();
                        } catch (MessagingException e) {
                            logger.error("MessagingException: " + e);
                            me = e;
                        }
                        if ((server != null) && (!server.isConnected())) {
                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Closing connection to server");
                                }
                                server.close();
                            } catch (MessagingException e) {
                                logger.error("MessagingException: " + e);
                                me = e;
                            }
                        }
                        if (prober != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Stopping prober");
                            }
                            prober.stop();
                        }
                        if (poller != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Stopping poller");
                            }
                            poller.stop();
                        }
                        forceDisconnect_helper(connectionLost);
                        return true;
                    }
                };
                ExecutorService service = Executors.newSingleThreadExecutor();
                try {
                    final Future<Boolean> f = service.submit(disconnect);

                    f.get(EIMConstants.MAX_DISCONNECTING_TIME / 2, TimeUnit.SECONDS);
                } catch (final TimeoutException e) {
                    logger.error("TimeoutException: Sending the message took too long");
                    forceDisconnect_helper(connectionLost);
                } catch (InterruptedException e) {
                    logger.error("InterruptedException: " + e.getMessage());
                    forceDisconnect_helper(connectionLost);
                } catch (ExecutionException e) {
                    logger.error("ExecutionException: " + e.getMessage());
                    forceDisconnect_helper(connectionLost);
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                    forceDisconnect_helper(connectionLost);
                } finally {
                    service.shutdown();
                }
            }
        });
        t.start();
    }

    public synchronized void disconnect(final boolean connectionLost) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                Callable<Boolean> disconnect = new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        disconnected = true;
                        MessagingException me = null;
                        try {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Closing folder");
                            }
                            closeFolder();
                        } catch (MessagingException e) {
                            logger.error("MessagingException: " + e);
                            me = e;
                        }
                        if ((server != null) && (!server.isConnected())) {
                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Closing connection to server");
                                }
                                server.close();
                            } catch (MessagingException e) {
                                logger.error("MessagingException: " + e);
                                me = e;
                            }
                        }
                        if (prober != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Stopping prober");
                            }
                            prober.stop();
                        }
                        if (poller != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Stopping poller");
                            }
                            poller.stop();
                        }
                        // boolean wasConnected = connected;
                        connected = false;
                        if (logger.isDebugEnabled()) {
                            logger.debug(account.get_emailaddress() + " disconnected");
                        }
                        // if (wasConnected || !usePush) {
                        if (!connectionLost) {
                            onDisconnect();
                        } else {
                            onConnectionLost();
                        }
                        if (me != null) {
                            onError(me);
                        }
                        return true;
                    }
                };
                ExecutorService service = Executors.newSingleThreadExecutor();
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Disconnecting...");
                    }
                    final Future<Boolean> f = service.submit(disconnect);

                    f.get(EIMConstants.MAX_DISCONNECTING_TIME, TimeUnit.SECONDS);
                } catch (final TimeoutException e) {
                    logger.error("TimeoutException: Sending the message took too long");
                    forceDisconnect(connectionLost);
                } catch (InterruptedException e) {
                    logger.error("InterruptedException: " + e.getMessage());
                    forceDisconnect(connectionLost);
                } catch (ExecutionException e) {
                    logger.error("ExecutionException: " + e.getMessage());
                    forceDisconnect(connectionLost);
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                    forceDisconnect(connectionLost);
                } finally {
                    service.shutdown();
                }
            }
        });
        t.start();
    }

    public long getLastMessageUID() {
        return lastMessageUID;
    }

    public void setLastMessageUID(long uid) {
        if (uid > lastMessageUID) {
            lastMessageUID = uid;
        }
    }

    public void setMessageChangedListerer(MessageChangedListener listener) {
        removeListener(externalChangedListener);
        externalChangedListener = listener;
        addListener(externalChangedListener);
    }

    public void setMessageCounterListerer(MessageCountListener listener) {
        removeListener(externalCountListener);
        externalCountListener = listener;
        addListener(externalCountListener);
    }

    public Message[] getNewMessages() throws MessagingException {
        ArrayList<Message> mess = new ArrayList<>();
        Message[] allMessages;
        Message[] messages;

        try {
            // allMessages = folder.getSortedMessages(new SortTerm[]{SortTerm.ARRIVAL, SortTerm.DATE});
            allMessages = folder.getMessages();
            for (int i = 0; i < poller.getDiffCount(); i++) {
                if (allMessages[i].isSet(Flags.Flag.SEEN) == false) {
                    mess.add(allMessages[i]);
                }
            }
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }

        messages = new Message[mess.size()];
        for (int i = 0; i < mess.size(); i++) {
            messages[i] = mess.get(i);
        }

        return messages;
    }

    public Message[] getMessages() throws MessagingException {
        return folder.getMessages();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean serverIsConnected() {
        return server.isConnected();
    }

    private void copyMessageToSent(Message msg) throws NoSuchProviderException, MessagingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Copying message to sent folder");
        }

        Properties properties = new Properties();

        String imapProtocol = "imap";
        if (account.use_imap_Ssl()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL");
            }
            imapProtocol = "imaps";
            properties.setProperty("mail." + imapProtocol + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail." + imapProtocol + ".socketFactory.fallback", "false");
            // properties.put("mail." + imapProtocol + ".EnableSSL.enable", "true");
        }
        if (account.use_imap_SslTls()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL/TLS");
            }
            imapProtocol = "imaps";
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.imap.socketFactory.fallback", "false");
        }
        if (account.use_imap_STARTTLS()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using STARTTLS");
            }
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
        }
        properties.setProperty("mail.store.protocol", imapProtocol);
        properties.setProperty("mail." + imapProtocol + ".host", account.get_imap());
        properties.setProperty("mail." + imapProtocol + ".port", account.get_imapPort());

        properties.put("mail." + imapProtocol + ".auth", "true");

        final String u = account.get_username();
        final String p = account.get_password();
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(u, p);
            }
        };

        Session _session = Session.getInstance(properties, authenticator);
        IMAPStore store = null;
        IMAPFolder _folder = null;

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Getting session store");
            }
            store = (IMAPStore) _session.getStore();
            if (logger.isDebugEnabled()) {
                logger.debug("Connecting to store");
            }
            store.connect();

            if (logger.isDebugEnabled()) {
                logger.debug("Trying to open default folder");
            }
            _folder = (IMAPFolder) store.getDefaultFolder();
            if (_folder == null) {
                throw new EIMInvalidIMAPFolderException("No default folder");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Looking for the sent folder");
            }
            allStoreFolders = store.getDefaultFolder().list("*");
            IMAPFolder sentFolder = null;
            if (allStoreFolders != null) {
                for (Folder f : allStoreFolders) {
                    IMAPFolder imapFolder = (IMAPFolder) f;
                    for (String attribute : imapFolder.getAttributes()) {
                        if (attribute.equalsIgnoreCase("\\Sent") || imapFolder.getFullName().contains("Sent")) {
                            sentFolder = imapFolder;
                            break;
                        }
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not download store folders");
                }
            }
            if (sentFolder != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Found sent folder, copying message");
                }
                sentFolder.appendMessages(new Message[]{msg});
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not find sent folder");
                }
            }
        } catch (EIMInvalidIMAPFolderException e) {
            logger.error("EIMInvalidIMAPFolderException: " + e);
        } finally {
            if ((_folder != null) && (_folder.isOpen())) {
                _folder.close(false);
            }
            if ((store != null) && (store.isConnected())) {
                store.close();
            }
        }
    }

    public EIMEmailMessage sendMail(String recipient, String subject, String message, String msgId)
            throws MessagingException {
        if (logger.isDebugEnabled()) {
            logger.debug("Sending mail");
        }

        Properties properties = new Properties();

        String smtpProtocol = "smtp";
        if (account.use_smtp_Ssl()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL");
            }
            // smtpProtocol = "smtps";
            properties.put("mail." + smtpProtocol + ".EnableSSL.enable", "true");
        }
        if (account.use_smtp_SslTls()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL/TLS");
            }
            // smtpProtocol = "smtps";
            properties.put("mail." + smtpProtocol + ".starttls.enable", "true");
            properties.put("mail." + smtpProtocol + ".EnableSSL.enable", "true");
        }
        if (account.use_smtp_STARTTLS()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using STARTTLS");
            }
            properties.put("mail." + smtpProtocol + ".starttls.enable", "true");
        }
        properties.setProperty("mail.store.protocol", smtpProtocol);
        properties.setProperty("mail." + smtpProtocol + ".host", account.get_smtp());
        properties.setProperty("mail." + smtpProtocol + ".port", account.get_smtpPort());

        boolean usePassword = false;
        if (account.use_smtp_password() || account.use_smtp_encryptedPassword()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using authentication");
            }
            properties.put("mail." + smtpProtocol + ".auth", "true");
            usePassword = true;
        } else {
            properties.put("mail." + smtpProtocol + ".auth", "false");
        }

        final String u = account.get_username();
        final String p = usePassword ? account.get_password() : "";
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(u, p);
            }
        };

        Session _session = Session.getInstance(properties, authenticator);
        Message msg = new MimeMessage(_session);
        InternetAddress addressFrom = new InternetAddress(account.get_emailaddress());
        if (logger.isDebugEnabled()) {
            logger.debug("Sending from " + addressFrom.getAddress() + "(" + account.get_emailaddress() + ")");
        }
        msg.setFrom(addressFrom);
        InternetAddress addressTo = new InternetAddress(recipient);
        if (logger.isDebugEnabled()) {
            logger.debug("Sending to " + addressTo.getAddress() + "(" + recipient + ")");
        }
        msg.setRecipient(Message.RecipientType.TO, addressTo);
        msg.setSubject(subject);
        msg.setContent(message, "text/plain");

        if (!msgId.trim().isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Setting header 'In-Reply-To' to " + msgId.trim());
            }
            msg.setHeader("In-Reply-To", msgId.trim());
        }

        Transport.send(msg);

        msg.setFlag(Flag.SEEN, true);
        msg.setSentDate(new Date());

        try {
            copyMessageToSent(msg);
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }

        EIMEmailMessage emMsg = new EIMEmailMessage(msg, 0l);
        emMsg.getEnvelope().setDateSent(new Date());

        return emMsg;
    }

    public ArrayList<EIMEmailMessage> getAllMail() {
        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + email_list.size() + " fetched emails");
        }
        return email_list;
    }

    public synchronized void fetchMail() throws EIMServerException, AuthenticationFailedException, MessagingException, EIMFetchException {
        email_list.clear();

        Callable<Boolean> fetchMailCallable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return fetchMail_helper();
            }
        };
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            final Future<Boolean> f = service.submit(fetchMailCallable);

            onMailFetched(f.get(EIMConstants.MAX_FETCHMAIL_TIME, TimeUnit.SECONDS), fetchedTo, firstFetch);
        } catch (final TimeoutException e) {
            logger.error("TimeoutException: Fetching messages took too long");
            disconnect(true);
        } catch (InterruptedException e) {
            logger.error("InterruptedException: " + e.getMessage());
            disconnect(true);
        } catch (ExecutionException e) {
            logger.error("ExecutionException: " + e.getMessage());
            disconnect(true);
            if(e.getMessage().contains("AuthenticationFailedException")) {
                throw new AuthenticationFailedException(e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            disconnect(true);
        } finally {
            service.shutdown();
        }
    }

    private boolean fetchMail_helper() throws EIMServerException, MessagingException, EIMFetchException {
        if (logger.isDebugEnabled()) {
            logger.debug("Fetching mails");
        }

        Properties properties = new Properties();

        String imapProtocol = "imap";
        if (account.use_imap_Ssl()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL");
            }
            imapProtocol = "imaps";
            properties.setProperty("mail." + imapProtocol + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail." + imapProtocol + ".socketFactory.fallback", "false");
            // properties.put("mail." + imapProtocol + ".EnableSSL.enable", "true");
        }
        if (account.use_imap_SslTls()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SSL/TLS");
            }
            imapProtocol = "imaps";
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.imap.socketFactory.fallback", "false");
        }
        if (account.use_imap_STARTTLS()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using STARTTLS");
            }
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
        }
        properties.setProperty("mail.store.protocol", imapProtocol);
        properties.setProperty("mail." + imapProtocol + ".host", account.get_imap());
        properties.setProperty("mail." + imapProtocol + ".port", account.get_imapPort());

        properties.put("mail." + imapProtocol + ".auth", "true");

        final String u = account.get_username();
        final String p = account.get_password();
        Authenticator authenticator = new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(u, p);
            }
        };

        Session _session = Session.getInstance(properties, authenticator);
        IMAPStore store = null;
        IMAPFolder _folder = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Getting session store");
            }
            store = (IMAPStore) _session.getStore();
            if (logger.isDebugEnabled()) {
                logger.debug("Connecting to store");
            }
            store.connect();

            if (logger.isDebugEnabled()) {
                logger.debug("Trying to open default folder");
            }
            _folder = (IMAPFolder) store.getDefaultFolder();
            if (_folder == null) {
                throw new EIMInvalidIMAPFolderException("No default folder");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Looking for folders to fetch");
            }
            allStoreFolders = store.getDefaultFolder().list("*");
            ArrayList<String> folderList = new ArrayList<>();
            ArrayList<String> sentFolderList = new ArrayList<>();
            if (allStoreFolders != null) {
                for (Folder f : allStoreFolders) {
                    IMAPFolder imapFolder = (IMAPFolder) f;
                    for (String attribute : imapFolder.getAttributes()) {
                        if (attribute.equalsIgnoreCase("\\Sent") || imapFolder.getFullName().contains("Sent")) {
                            if (!sentFolderList.contains(imapFolder.getFullName())) {
                                sentFolderList.add(imapFolder.getFullName());
                            }
                        } else if (/*((f.getType() & Folder.HOLDS_MESSAGES) != 0)
                                 && */EIMUtility.getInstance().fetchFromImapFolder(imapFolder.getFullName())
                                && EIMUtility.getInstance().fetchFromImapAttribute(attribute)) {
                            if (!folderList.contains(imapFolder.getFullName())) {
                                folderList.add(imapFolder.getFullName());
                            }
                        }
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not download store folders");
                }
                return false;
            }

            if (folderList.isEmpty()) {
                logger.error("Could not find a folder to fetch from");
                throw new EIMFetchException("Could not find a folder to fetch from", EIMFetchException.STATUS.NO_FOLDER_FOUND);
            }

            if (sentFolderList.isEmpty()) {
                logger.error("Could not find a sent folder");
                throw new EIMFetchException("Could not find a sent folder", EIMFetchException.STATUS.NO_SENT_FOLDER_FOUND);
            }

            for (String s : folderList) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fetching from folder '" + s + "'");
                }
                fetchFromFolder(_folder, s);
            }
            for (String s : sentFolderList) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Fetching from sent folder '" + s + "'");
                }
                fetchFromFolder(_folder, s);
            }

            return true;
        } catch (EIMInvalidIMAPFolderException e) {
            logger.error("EIMInvalidIMAPFolderException: " + e.getMessage());
            throw new EIMServerException("EIMInvalidIMAPFolderException: " + e.getMessage());
        } catch (EIMEmptyIMAPFolderException e) {
            throw new EIMServerException("EIMEmptyIMAPFolderException: " + e.getMessage());
        } catch (NoSuchProviderException e) {
            logger.error("NoSuchProviderException: " + e.getMessage());
            throw new EIMServerException("NoSuchProviderException: " + e.getMessage());
        } catch (MessagingException e) {
            logger.error("MessagingException: " + e.getMessage());
            throw e;
        } finally {
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    logger.error("MessagingException: " + e.getMessage());
                }
            }
            if (allStoreFolders != null) {
                for (Folder f : allStoreFolders) {
                    if (((f != null) && (f.isOpen()))) {
                        try {
                            f.close(false);
                        } catch (MessagingException e) {
                            logger.error("MessagingException: " + e.getMessage());
                        }
                    }
                }
            }
            if (((_folder != null) && (_folder.isOpen()))) {
                try {
                    _folder.close(false);
                } catch (MessagingException e) {
                    logger.error("MessagingException: " + e.getMessage());
                }
            }
        }
    }

    private void fetchFromFolder(IMAPFolder folder, String folderName)
            throws MessagingException, EIMInvalidIMAPFolderException, EIMEmptyIMAPFolderException {
        IMAPFolder _folder = (IMAPFolder) folder.getFolder(folderName);
        if (_folder == null) {
            throw new EIMInvalidIMAPFolderException("Invalid folder: " + folderName);
        }

        try {
            _folder.open(Folder.READ_WRITE);
            if (logger.isDebugEnabled()) {
                logger.debug("Can open in RW");
            }
        } catch (MessagingException e) {
            logger.error("Cannot open in RW, opening in R");
            try {
                _folder.open(Folder.READ_ONLY);
            } catch (MessagingException e2) {
                if (logger.isDebugEnabled()) {
                    logger.debug("MessagingException: " + e2.getMessage());
                }
                return;
            }
        }

        if (!_folder.isOpen()) {
            throw new EIMEmptyIMAPFolderException("Folder is closed");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Checking messages count");
        }
        int totalMessages = _folder.getMessageCount();

        if (totalMessages == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Folder is empty");
            }
            /*
             if (_folder.isOpen()) {
             _folder.close(false);
             }
             store.close();
             throw new EIMEmptyIMAPFolderException("Empty folder");
             */
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Total messages = " + totalMessages);
            logger.debug("Fetching mail");
        }

        FetchProfile fp = new FetchProfile();
        fp.add(FetchProfile.Item.ENVELOPE);
        // fp.add(FetchProfile.Item.FLAGS);
        // fp.add("X-Mailer");

        if (lastMessageUID < 0) {
            Date from = new Date();
            if (logger.isDebugEnabled()) {
                logger.debug("Searching for mails in the last " + EIMConstants.DEFAULT_DAYS_TO_FETCH + " days");
            }
            GregorianCalendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_MONTH, -1 * EIMConstants.DEFAULT_DAYS_TO_FETCH);
            Date to = new Date(cal.getTimeInMillis());
            SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LE, from);
            SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GE, to);
            SearchTerm andTerm = new AndTerm(newerThan, olderThan);
            msgs = _folder.search(andTerm);
            firstFetch = true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Using UID to fetch");
            }
            msgs = _folder.getMessagesByUID(lastMessageUID + 1, UIDFolder.LASTUID);
            if (logger.isDebugEnabled()) {
                logger.debug("Fetching from " + (lastMessageUID + 1) + " to " + UIDFolder.LASTUID);
            }
        }
        try {
            _folder.fetch(msgs, fp);
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }

        if (firstFetch && (totalMessages > 0) && (msgs.length <= 0)) {
            // msgs = _folder.getMessages();
            // _folder.fetch(msgs, fp);
            int nrOfMsgsToFetch = (totalMessages > EIMConstants.DEFAULT_NUMBER_OF_MAILS_TO_FETCH) ? (totalMessages - EIMConstants.DEFAULT_NUMBER_OF_MAILS_TO_FETCH) : 0;
            if (logger.isDebugEnabled()) {
                logger.debug("No messages found by search. Fetching the last " + (totalMessages - nrOfMsgsToFetch) + " messages");
            }
            try {
                msgs = _folder.getMessages(nrOfMsgsToFetch, totalMessages);
                _folder.fetch(msgs, fp);
            } catch (Exception e) {
                logger.error("Exception: " + e.getMessage());
            }
        }

        int mailCount = 0;

        if (msgs != null) {
            for (int i = 0; i < msgs.length; ++i) {
                email_list.add(new EIMEmailMessage(msgs[i], _folder.getUID(msgs[i])));
                fetchedTo = (_folder.getUID(msgs[i]) > fetchedTo) ? _folder.getUID(msgs[i]) : fetchedTo;
                ++mailCount;
                if (firstFetchedFolder.isEmpty()) {
                    firstFetchedFolder = _folder.getFullName();
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Fetched " + mailCount + " mails");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No messages fetched");
            }
        }
    }

    public void gotNewMessages(ArrayList<EIMEmailMessage> msgs) {
        onNewMessages(msgs);
    }

    private void initConnection() {
        prober = new EIMNetworkProber(this) {
            @Override
            public synchronized void onNetworkChange(boolean status) {
                // logger.debug("Network status changed to: " + status);
                if (status != connected) { // if two states do not match, something has truly changed!
                    if (status && !connected) { // if connection up, but not connected...
                        connect();
                    } else if (!status && connected) { //if previously connected, but link down, just disconnect
                        if ((getSessionFailureCount() >= EIMConstants.MAX_SESSION_FAILURE_COUNT) || (getPingFailureCount() >= EIMConstants.MAX_PING_FAILURE_COUNT)) {
                            disconnect(true);
                        }
                    }
                } else { // if link (either session or net connection) and connection down, something gone wrong
                    if (!serverIsConnected() && getNetConnectivity()) { // need to make sure that session is down, but link is up
                        connect();
                    }
                }
            }

            @Override
            public synchronized void missedBeat() { // missed beat
                connected = false;
            }
        };

        poller = new EIMEmailPoller(folder) {
            @Override
            public synchronized void onNewMessage() {
                logger.debug("New message");
                try {
                    if ((externalCountListener != null) && (messageCountListener != null)) {
                        Message[] msgs = getNewMessages();
                        externalCountListener.messagesAdded(new MessageCountEvent(folder, MessageCountEvent.ADDED, false, msgs));
                        messageCountListener.messagesAdded(new MessageCountEvent(folder, MessageCountEvent.ADDED, false, msgs));
                    }
                } catch (MessagingException e) {
                    onError(e);
                }

            }
        };

        logger.debug("Setting properties");

        Properties properties = new Properties();

        String imapProtocol = "imap";
        if (account.use_imap_Ssl()) {
            logger.debug("Using SSL");
            imapProtocol = "imaps";
            properties.setProperty("mail." + imapProtocol + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail." + imapProtocol + ".socketFactory.fallback", "false");
            // properties.put("mail." + imapProtocol + ".EnableSSL.enable", "true");
        }
        if (account.use_imap_SslTls()) {
            logger.debug("Using SSL/TLS");
            imapProtocol = "imaps";
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
            properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.imap.socketFactory.fallback", "false");
        }
        if (account.use_imap_STARTTLS()) {
            logger.debug("Using STARTTLS");
            properties.put("mail." + imapProtocol + ".starttls.enable", "true");
        }
        properties.setProperty("mail.store.protocol", imapProtocol);
        properties.setProperty("mail." + imapProtocol + ".host", account.get_imap());
        properties.setProperty("mail." + imapProtocol + ".port", account.get_imapPort());

        properties.put("mail." + imapProtocol + ".auth", "true");

        session = Session.getDefaultInstance(properties, null);
        try {
            logger.debug("Connecting to store");
            server = (IMAPStore) session.getStore(imapProtocol);
            connect();
        } catch (NoSuchProviderException e) {
            logger.error("NoSuchProviderException: " + e.getMessage());
            onError(e);
        }
    }

    private void selectFolder(String folderName) {
        try {
            closeFolder();
            if (folderName.equalsIgnoreCase("")) {
                folder = (IMAPFolder) server.getFolder(inboxFolderName);
            } else {
                folder = (IMAPFolder) server.getFolder(folderName);
            }
            openFolder();
        } catch (MessagingException e) {
            logger.error("MessagingException: " + e.getMessage());
            onError(e);
        } catch (IllegalStateException e) {
            logger.error("MessagingException: " + e.getMessage());
        }
    }

    private void openFolder() throws MessagingException {
        if (folder != null) {
            folder.open(Folder.READ_ONLY);
            folder.setSubscribed(true);
            removeAllListenersFromFolder();
            addAllListenersFromFolder();
            poller.setFolder(folder);

            if (usePush) {
                usePush();
            } else {
                poller.start(account.get_emailaddress());
            }
        }
    }

    private void closeFolder() throws MessagingException {
        if ((folder != null) && folder.isOpen()) {
            removeAllListenersFromFolder();
            folder.setSubscribed(false);
            folder.close(false);
            folder = null;
        }
    }

    private void usePush() {
        if (folder == null || !usePush) {
            return;
        }

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    folder.idle(false);
                } catch (FolderClosedException e) {
                    logger.error("FolderClosedException: Push Error: [DISCONNECT] " + account.get_emailaddress() + ", " + e.getMessage());
                    usePush = true;
                    messageChangedListener = null;
                    messageCountListener = null;
                    selectFolder(firstFetchedFolder);
                } catch (StoreClosedException e) {
                    logger.error("StoreClosedException: Push Error: [GLOBAL DISCONNECT] " + account.get_emailaddress() + ": " + e.getMessage());
                    usePush = true;
                } catch (MessagingException e) {
                    logger.error("MessagingException: Push Error: [NO IDLE] " + account.get_emailaddress() + ": " + e.getMessage());
                    usePush = false;
                    selectFolder(firstFetchedFolder);
                } catch (Exception e) {
                    logger.error("Exception: Push Error: [UNKNOWN] " + account.get_emailaddress() + ": " + e.getMessage());
                    usePush = false;
                    selectFolder(firstFetchedFolder);
                }
                if (!disconnected && !usePush) {
                    logger.info("IMAP idle not supported, polling");
                    poller.start(account.get_emailaddress());
                    onPushNotAvailable();
                }
            }
        };
        pushThread = new Thread(r, "Push-" + account.get_emailaddress());
        pushThread.setDaemon(true);
        pushThread.start();
    }

    private void removeAllListenersFromFolder() {
        removeListener(externalChangedListener);
        removeListener(externalCountListener);
    }

    private void removeListener(EventListener listener) {
        if (listener == null || folder == null) {
            return;
        }

        if (listener instanceof MessageChangedListener) {
            folder.removeMessageChangedListener((MessageChangedListener) listener);
        } else {
            if (listener instanceof MessageCountListener) {
                folder.removeMessageCountListener((MessageCountListener) listener);
            }
        }
    }

    private void addAllListenersFromFolder() {
        addListener(externalCountListener);
        addListener(externalChangedListener);
    }

    private void addListener(EventListener listener) {
        if (listener == null || folder == null) {
            return;
        }

        if (listener instanceof MessageChangedListener) {
            folder.addMessageChangedListener((MessageChangedListener) listener);
        } else {
            if (listener instanceof MessageCountListener) {
                folder.addMessageCountListener((MessageCountListener) listener);
            }
        }

        addInternalListeners(listener);
    }

    private void addInternalListeners(EventListener listener) {
        if (listener == null || folder == null) {
            return;
        }

        if (listener instanceof MessageChangedListener && messageChangedListener == null) {
            messageChangedListener = new MessageChangedListener() {
                @Override
                public void messageChanged(MessageChangedEvent mce) {
                    usePush();
                }
            };
            folder.addMessageChangedListener(messageChangedListener);
        } else {
            if (listener instanceof MessageCountListener && messageCountListener == null) {
                messageCountListener = new MessageCountListener() {
                    @Override
                    public void messagesAdded(MessageCountEvent mce) {
                        usePush();
                    }

                    @Override
                    public void messagesRemoved(MessageCountEvent mce) {
                        usePush();
                    }
                };
                folder.addMessageCountListener(messageCountListener);
            }
        }
    }
}
