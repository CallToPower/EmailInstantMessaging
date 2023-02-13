/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.util.EIMConstants;
import com.eim.util.EIMUtility;
import java.util.ArrayList;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMAccount
 *
 * @author Denis Meyer
 */
public final class EIMAccount {

    private static final Logger logger = LogManager.getLogger(EIMAccount.class.getName());
    private LOGIN_STATUS status;
    private int id;
    private String imap;
    private String imap_port;
    private String imap_ssl;
    private String imap_authentication;
    private String smtp;
    private String smtp_port;
    private String smtp_ssl;
    private String smtp_authentication;
    private String emailaddress;
    private String username;
    private String password;
    private long uid;
    private ArrayList<String> aliases;

    public static enum LOGIN_STATUS {

        LOGGED_OUT,
        LOGGED_IN,
        LOGGING_IN,
        LOGGING_OUT,
        LOGIN_FAILED,
        NO_CONNECTION,
        DELETED
    };

    public EIMAccount() {
        this(-1, "", "", "", "", "", "", "", "", "", "", "", -1l);
    }

    public EIMAccount(int id,
            String imap, String imap_port, String imap_ssl, String imap_authentication,
            String smtp, String smtp_port, String smtp_ssl, String smtp_authentication,
            String emailaddress,
            String username,
            String password,
            long uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMAccount");
        }
        this.uid = -1;
        this.id = id;
        status = LOGIN_STATUS.LOGGED_OUT;
        setServerData(imap, imap_port, imap_ssl, imap_authentication, smtp, smtp_port, smtp_ssl, smtp_authentication);
        setCredentials(EIMUtility.getInstance().hostToLowerCase(emailaddress), username, password);
        setUID(uid);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EIMAccount) {
            EIMAccount acc = (EIMAccount) o;
            ArrayList<String> al = EIMUtility.getInstance().getAliases(this.emailaddress);
            for (String s : al) {
                if (hashCode(s) == acc.hashCode()) {
                    return true;
                }
            }
        }

        return false;
        /*
         EIMAccount acc = (EIMAccount) o;
         return (id == acc.id)
         && (uid == acc.getUID())
         && (imap.equals(acc.get_imap()))
         && (imap_port.equals(acc.get_imapPort()))
         && (imap_ssl.equals(acc.get_imapSsl()))
         && (imap_authentication.equals(acc.get_imapAuthentication()))
         && (smtp.equals(acc.get_smtp()))
         && (smtp_port.equals(acc.get_smtpPort()))
         && (smtp_ssl.equals(acc.get_smtpSsl()))
         && (smtp_authentication.equals(acc.get_smtpAuthentication()))
         && (emailaddress.equals(acc.get_emailaddress()))
         && (username.equals(acc.get_username()))
         && (password.equals(acc.get_password()));
         */
    }

    public int hashCode(String emailAddr) {
        int hash = 7;
        hash = 53 * hash + this.id;
        hash = 53 * hash + Objects.hashCode(this.imap);
        hash = 53 * hash + Objects.hashCode(this.imap_port);
        hash = 53 * hash + Objects.hashCode(this.imap_ssl);
        hash = 53 * hash + Objects.hashCode(this.imap_authentication);
        hash = 53 * hash + Objects.hashCode(this.smtp);
        hash = 53 * hash + Objects.hashCode(this.smtp_port);
        hash = 53 * hash + Objects.hashCode(this.smtp_ssl);
        hash = 53 * hash + Objects.hashCode(this.smtp_authentication);
        hash = 53 * hash + Objects.hashCode(emailAddr);
        hash = 53 * hash + Objects.hashCode(this.username);
        hash = 53 * hash + Objects.hashCode(this.password);
        hash = 53 * hash + (int) (this.uid ^ (this.uid >>> 32));
        return hash;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.id;
        hash = 53 * hash + Objects.hashCode(this.imap);
        hash = 53 * hash + Objects.hashCode(this.imap_port);
        hash = 53 * hash + Objects.hashCode(this.imap_ssl);
        hash = 53 * hash + Objects.hashCode(this.imap_authentication);
        hash = 53 * hash + Objects.hashCode(this.smtp);
        hash = 53 * hash + Objects.hashCode(this.smtp_port);
        hash = 53 * hash + Objects.hashCode(this.smtp_ssl);
        hash = 53 * hash + Objects.hashCode(this.smtp_authentication);
        hash = 53 * hash + Objects.hashCode(this.emailaddress);
        hash = 53 * hash + Objects.hashCode(this.username);
        hash = 53 * hash + Objects.hashCode(this.password);
        hash = 53 * hash + (int) (this.uid ^ (this.uid >>> 32));
        return hash;
    }

    public void update(EIMAccount acc) {
        setServerData(acc.get_imap(), acc.get_imapPort(), acc.get_imapSsl(), acc.get_imapAuthentication(),
                acc.get_smtp(), acc.get_smtpPort(), acc.get_smtpSsl(), acc.get_smtpAuthentication());
        setCredentials(acc.get_emailaddress(), acc.get_username(), acc.get_password());
        setUID(acc.getUID());
        this.id = acc.get_id();
    }

    public LOGIN_STATUS getStatus() {
        return status;
    }

    public boolean isAlias(String emailAddress) {
        if (!EIMUtility.getInstance().getUsername(this.emailaddress).equals(EIMUtility.getInstance().getUsername(emailAddress))) {
            return false;
        }
        String host = EIMUtility.getInstance().getHost(emailAddress);
        for (String s : aliases) {
            if (host.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public void setStatus(LOGIN_STATUS status) {
        this.status = status;
    }

    public final void setCredentials(String emailaddress, String username, String password) {
        this.emailaddress = emailaddress;
        this.username = username;
        this.password = password;

        aliases = EIMConstants.getServerAliases(EIMUtility.getInstance().getHost(this.emailaddress));
        if (!aliases.contains(EIMUtility.getInstance().getHost(this.emailaddress))) {
            aliases.add(EIMUtility.getInstance().getHost(this.emailaddress));
        }
    }

    public final void setServerData(
            String imap, String imap_port, String imap_ssl, String imap_authentication,
            String smtp, String smtp_port, String smtp_ssl, String smtp_authentication) {
        this.imap = imap;
        this.imap_port = imap_port;
        this.imap_ssl = imap_ssl;
        this.imap_authentication = imap_authentication;
        this.smtp = smtp;
        this.smtp_port = smtp_port;
        this.smtp_ssl = smtp_ssl;
        this.smtp_authentication = smtp_authentication;
    }

    public boolean use_imap_STARTTLS() {
        return get_imapSsl().equals(EIMConstants.IMAP_SSL[1]);
    }

    public boolean use_imap_Ssl() {
        return get_imapSsl().equals(EIMConstants.IMAP_SSL[2]);
    }

    public boolean use_imap_SslTls() {
        return get_imapSsl().equals(EIMConstants.IMAP_SSL[3]);
    }

    public boolean use_imap_encryptedPassword() {
        return get_imapAuthentication().equals(EIMConstants.IMAP_AUTHENTICATION[1]);
    }

    public boolean use_smtp_STARTTLS() {
        return get_smtpSsl().equals(EIMConstants.SMTP_SSL[1]);
    }

    public boolean use_smtp_Ssl() {
        return get_smtpSsl().equals(EIMConstants.SMTP_SSL[2]);
    }

    public boolean use_smtp_SslTls() {
        return get_smtpSsl().equals(EIMConstants.SMTP_SSL[3]);
    }

    public boolean use_smtp_password() {
        return get_smtpAuthentication().equals(EIMConstants.SMTP_AUTHENTICATION[1]);
    }

    public boolean use_smtp_encryptedPassword() {
        return get_smtpAuthentication().equals(EIMConstants.SMTP_AUTHENTICATION[2]);
    }

    public int get_id() {
        return id;
    }

    public String get_imap() {
        return imap;
    }

    public String get_imapPort() {
        return imap_port;
    }

    public String get_imapSsl() {
        return imap_ssl;
    }

    public String get_imapAuthentication() {
        return imap_authentication;
    }

    public String get_smtp() {
        return smtp;
    }

    public String get_smtpPort() {
        return smtp_port;
    }

    public String get_smtpSsl() {
        return smtp_ssl;
    }

    public String get_smtpAuthentication() {
        return smtp_authentication;
    }

    public String get_emailaddress() {
        return emailaddress;
    }

    public String get_username() {
        return username;
    }

    public String get_password() {
        return password;
    }

    public long getUID() {
        return uid;
    }

    public void set_id(int id) {
        this.id = id;
    }

    public void set_imap(String imap) {
        this.imap = imap;
    }

    public void set_imapPort(String imap_port) {
        this.imap_port = imap_port;
    }

    public void set_imapSsl(String imap_ssl) {
        this.imap_ssl = imap_ssl;
    }

    public void set_imapAuthentication(String imap_authentication) {
        this.imap_authentication = imap_authentication;
    }

    public void set_smtp(String smtp) {
        this.smtp = smtp;
    }

    public void set_smtpPort(String smtp_port) {
        this.smtp_port = smtp_port;
    }

    public void set_smtpSsl(String smtp_ssl) {
        this.smtp_ssl = smtp_ssl;
    }

    public void set_smtpAuthentication(String smtp_authentication) {
        this.smtp_authentication = smtp_authentication;
    }

    public void set_emailaddress(String emailaddress) {
        this.emailaddress = emailaddress;
    }

    public void set_username(String username) {
        this.username = username;
    }

    public void set_password(String password) {
        this.password = password;
    }

    public void setUID(long uid) {
        if (logger.isDebugEnabled()) {
            logger.debug("Current UID: " + this.uid + ", got UID: " + uid);
        }
        if (this.uid < uid) {
            this.uid = uid;
        }
    }
}
