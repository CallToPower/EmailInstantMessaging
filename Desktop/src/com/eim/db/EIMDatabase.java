/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.db;

import com.eim.exceptions.EIMDatabaseException;
import com.eim.mail.EIMAccount;
import com.eim.mail.EIMEmailContent;
import com.eim.mail.EIMEmailEnvelope;
import com.eim.mail.EIMEmailMessage;
import com.eim.util.EIMConstants;
import com.eim.util.EIMUtility;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMDatabase
 *
 * @author Denis Meyer
 */
public final class EIMDatabase {

    private static final Logger logger = LogManager.getLogger(EIMDatabase.class.getName());
    private static EIMDatabase instance = null;
    private Connection connection = null;
    private PreparedStatement table_account_drop = null;
    private PreparedStatement table_account_insert = null;
    private PreparedStatement table_account_update = null;
    private PreparedStatement table_account_delete = null;
    private PreparedStatement table_mails_drop = null;
    private PreparedStatement table_mails_insert = null;
    private PreparedStatement table_mails_update = null;
    private PreparedStatement table_mails_delete = null;
    private PreparedStatement table_mails_delete_old_mails = null;
    private PreparedStatement table_preferences_drop = null;
    private PreparedStatement table_preferences_insert = null;
    private PreparedStatement table_preferences_update = null;
    private int preferences_id = 0;

    protected EIMDatabase() throws EIMDatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMDatabase");
        }
        init();
    }

    public static EIMDatabase getInstance() throws EIMDatabaseException {
        if (instance == null) {
            instance = new EIMDatabase();
        }
        return instance;
    }

    public synchronized void disconnect() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Disconnecting");
        }
        if (connection != null) {
            connection.close();
        }
    }

    public synchronized void dropAndCreateAll() throws EIMDatabaseException {
        dropTablePreferences();
        dropTableAccounts();
        dropTableMails();
    }

    public synchronized boolean dropTableAccounts() throws EIMDatabaseException {
        if (table_account_drop != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_account_drop.execute();
                    connection.commit();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Dropped table Accounts");
                        logger.debug("Creating table Accounts");
                    }
                    if (create_database_Accounts()) {
                        try {
                            prepareStatementsAccounts();
                        } catch (SQLException e) {
                            throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
                        }
                    } else {
                        throw new EIMDatabaseException("Could not create database Accounts");
                    }

                    return true;
                }
            } catch (SQLException ex) {
                logger.error("SQLException: " + ex.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_account_drop is null");
        }
        return false;
    }

    public synchronized boolean dropTablePreferences() throws EIMDatabaseException {
        if (table_preferences_drop != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_preferences_drop.execute();
                    connection.commit();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Dropped table Preferences");
                        logger.debug("Creating table Preferences");
                    }
                    if (create_database_Preferences()) {
                        try {
                            prepareStatementsPreferences();
                        } catch (SQLException e) {
                            throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
                        }
                    } else {
                        throw new EIMDatabaseException("Could not create database Preferences");
                    }

                    return true;
                }
            } catch (SQLException ex) {
                logger.error("SQLException: " + ex.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_preferences_drop is null");
        }
        return false;
    }

    public synchronized boolean dropTableMails() throws EIMDatabaseException {
        if (table_mails_drop != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_mails_drop.execute();
                    connection.commit();

                    if (logger.isDebugEnabled()) {
                        logger.debug("Dropped table Mails");
                        logger.debug("Creating table Mails");
                    }
                    if (create_database_Mails()) {
                        try {
                            prepareStatementsMails();
                        } catch (SQLException e) {
                            throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
                        }
                    } else {
                        throw new EIMDatabaseException("Could not create database Mails");
                    }

                    return true;
                }
            } catch (SQLException ex) {
                logger.error("SQLException: " + ex.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_mails_drop is null");
        }
        return false;
    }

    public synchronized boolean insertAccount(EIMAccount acc) throws EIMDatabaseException {
        if (table_account_insert != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Adding account to database");
                    }
                    table_account_insert.setString(1, acc.get_imap());
                    table_account_insert.setString(2, acc.get_imapPort());
                    table_account_insert.setString(3, acc.get_imapSsl());
                    table_account_insert.setString(4, acc.get_imapAuthentication());
                    table_account_insert.setString(5, acc.get_smtp());
                    table_account_insert.setString(6, acc.get_smtpPort());
                    table_account_insert.setString(7, acc.get_smtpSsl());
                    table_account_insert.setString(8, acc.get_smtpAuthentication());
                    table_account_insert.setString(9, acc.get_emailaddress());
                    table_account_insert.setString(10, acc.get_username());
                    table_account_insert.setString(11, acc.get_password());
                    table_account_insert.setLong(12, acc.getUID());
                    table_account_insert.executeUpdate();
                    connection.commit();
                    ResultSet rs = table_account_insert.getGeneratedKeys();
                    rs.next();
                    int id = rs.getInt(1);
                    acc.set_id(id);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully added ID " + id + ", Account " + acc.get_emailaddress() + " to the database");
                    }
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to add account " + acc.get_emailaddress());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_account_insert is null");
        }
        return false;
    }

    public synchronized boolean insertMail(EIMEmailMessage msg) throws EIMDatabaseException {
        if (table_mails_insert != null) {
            String to = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1];
            String from = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1];
            String replyTo = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstReplyTo())[1];
            try {
                for (EIMEmailMessage m : loadMails()) {
                    if (m.getUID() == msg.getUID()) {
                        return updateMail(msg);
                    }
                }
                if (!(connection == null) && !connection.isClosed()) {
                    table_mails_insert.setString(1, msg.getEnvelope().getID());
                    table_mails_insert.setString(2, from); // TODO: Change when supporting group messages
                    table_mails_insert.setString(3, replyTo); // TODO: Change when supporting group messages
                    table_mails_insert.setString(4, to); // TODO: Change when supporting group messages
                    table_mails_insert.setString(5, msg.getEnvelope().getSubject());
                    table_mails_insert.setLong(6, msg.getEnvelope().getDateSent().getTime());
                    table_mails_insert.setLong(7, msg.getUID());
                    table_mails_insert.setString(8, msg.getContent().getContent());
                    table_mails_insert.executeUpdate();
                    connection.commit();
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to add email to account " + to);
                logger.error("SQLException: " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_mails_insert is null");
        }
        return false;
    }

    public synchronized boolean updateAccount(EIMAccount acc) throws EIMDatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to update account " + acc.get_emailaddress());
        }
        if (table_account_update != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Updating account " + acc.get_emailaddress() + " with ID " + acc.get_id());
                    }
                    table_account_update.setString(1, acc.get_imap());
                    table_account_update.setString(2, acc.get_imapPort());
                    table_account_update.setString(3, acc.get_imapSsl());
                    table_account_update.setString(4, acc.get_imapAuthentication());
                    table_account_update.setString(5, acc.get_smtp());
                    table_account_update.setString(6, acc.get_smtpPort());
                    table_account_update.setString(7, acc.get_smtpSsl());
                    table_account_update.setString(8, acc.get_smtpAuthentication());
                    table_account_update.setString(9, acc.get_emailaddress());
                    table_account_update.setString(10, acc.get_username());
                    table_account_update.setString(11, acc.get_password());
                    table_account_update.setLong(12, acc.getUID());
                    table_account_update.setInt(13, acc.get_id());
                    table_account_update.executeUpdate();
                    connection.commit();
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to update ID " + acc.get_id() + ", Account " + acc.get_emailaddress() + ": " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_account_update is null");
        }
        return false;
    }

    public synchronized boolean updateMail(EIMEmailMessage msg) throws EIMDatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to update mail ");
        }
        if (table_mails_update != null) {
            try {
                for (EIMEmailMessage m : loadMails()) {
                    if (m.getUID() == msg.getUID()) {
                        if (!(connection == null) && !connection.isClosed()) {
                            String to = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1];
                            String from = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1];
                            String replyTo = EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstReplyTo())[1];
                            table_mails_update.setString(1, from);
                            table_mails_update.setString(2, replyTo);
                            table_mails_update.setString(3, to);
                            table_mails_update.setString(4, msg.getEnvelope().getSubject());
                            table_mails_update.setLong(5, m.getEnvelope().getDateSent().getTime()); // don't set the new time, take the old
                            table_mails_update.setLong(6, msg.getUID());
                            table_mails_update.setString(7, msg.getContent().getContent());
                            table_mails_update.setLong(8, msg.getUID());
                            table_mails_update.executeUpdate();
                            connection.commit();
                            return true;
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Could not update email with UID " + msg.getUID());
                            }
                            return false;
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("No email with UID " + msg.getUID() + " is in the database");
                }
            } catch (SQLException e) {
                logger.error("Failed to updated email with UID " + msg.getUID() + ": " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_mails_update is null");
        }
        return false;
    }

    public synchronized boolean updatePreference(boolean saveData, boolean autoLogin, boolean playSounds, boolean displayNotifications, boolean onlyEIMMsgs) throws EIMDatabaseException {
        if (table_preferences_update != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_preferences_update.setString(1, String.valueOf(saveData));
                    table_preferences_update.setString(2, String.valueOf(autoLogin));
                    table_preferences_update.setString(3, String.valueOf(playSounds));
                    table_preferences_update.setString(4, String.valueOf(displayNotifications));
                    table_preferences_update.setString(5, String.valueOf(onlyEIMMsgs));
                    table_preferences_update.setInt(6, preferences_id);
                    table_preferences_update.executeUpdate();
                    connection.commit();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully updated preferences ID " + preferences_id);
                    }
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to update preferences ID " + preferences_id);
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_preferences_update is null");
        }
        return false;
    }

    public synchronized boolean deleteAccount(int id) throws EIMDatabaseException {
        if (table_account_delete != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_account_delete.setInt(1, id);
                    table_account_delete.executeUpdate();
                    connection.commit();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully deleted ID " + id);
                    }
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to delete ID " + id);
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_account_delete is null");
        }
        return false;
    }

    public synchronized boolean deleteOldMails() throws EIMDatabaseException {
        if (table_mails_delete_old_mails != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.add(Calendar.DAY_OF_MONTH, -1 * EIMConstants.DEFAULT_DAYS_TO_KEEP);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Deleting mails older than " + EIMConstants.DEFAULT_DAYS_TO_KEEP + " days (= " + cal.getTimeInMillis() + "ms, current ms: " + (new Date().getTime()) + ")");
                    }
                    table_mails_delete_old_mails.setLong(1, cal.getTimeInMillis());
                    table_mails_delete_old_mails.executeUpdate();
                    connection.commit();
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to delete old mails");
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_mails_delete_old_mails is null");
        }

        return false;
    }

    public synchronized boolean deleteMail(String id) throws EIMDatabaseException {
        if (table_mails_delete != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_mails_delete.setString(1, id);
                    table_mails_delete.executeUpdate();
                    connection.commit();
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to delete mail with id " + id);
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_mails_delete is null");
        }
        return false;
    }

    public synchronized boolean insertPreferences(boolean saveData, boolean autoLogin, boolean playSounds, boolean displayNotifications, boolean onlyEIMMsgs) throws EIMDatabaseException {
        if (table_preferences_insert != null) {
            try {
                if (!(connection == null) && !connection.isClosed()) {
                    table_preferences_insert.setString(1, saveData ? "1" : "0");
                    table_preferences_insert.setString(2, autoLogin ? "1" : "0");
                    table_preferences_insert.setString(3, playSounds ? "1" : "0");
                    table_preferences_insert.setString(4, displayNotifications ? "1" : "0");
                    table_preferences_insert.setString(5, onlyEIMMsgs ? "1" : "0");
                    table_preferences_insert.executeUpdate();
                    connection.commit();
                    ResultSet rs = table_preferences_insert.getGeneratedKeys();
                    rs.next();
                    preferences_id = rs.getInt(1);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Successfully added preferences ID " + preferences_id);
                    }
                    return true;
                }
            } catch (SQLException e) {
                logger.error("Failed to add preferences ID " + preferences_id);
            }
        } else {
            throw new EIMDatabaseException("Prepare statement table_preferences_insert is null");
        }
        return false;
    }

    public synchronized HashMap<String, String> loadPreferences() throws EIMDatabaseException {
        Statement statement = null;
        HashMap<String, String> prefs = new HashMap<>();
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading preferences");
            }
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(EIMConstants.SQL_PREFERENCES_LOADALL);
            while (rs.next()) {
                preferences_id = rs.getInt("ID");
                boolean saveData = !rs.getString("SAVEDATA").equals("false");
                boolean autoLogin = !rs.getString("AUTOLOGIN").equals("false");
                boolean playSounds = !rs.getString("PLAYSOUNDS").equals("false");
                boolean displayNotifications = !rs.getString("DISPLAYNOTIFICATIONS").equals("false");
                boolean onlyEIMMsgs = !rs.getString("ONLYEIMMSGS").equals("false");
                prefs.put(EIMConstants.PREFERENCE_SAVEDATA, String.valueOf(saveData));
                prefs.put(EIMConstants.PREFERENCE_AUTOLOGIN, String.valueOf(autoLogin));
                prefs.put(EIMConstants.PREFERENCE_PLAYSOUNDS, String.valueOf(playSounds));
                prefs.put(EIMConstants.PREFERENCE_DISPLAYNOTIFICATIONS, String.valueOf(displayNotifications));
                prefs.put(EIMConstants.PREFERENCE_ONLYEIMMSGS, String.valueOf(onlyEIMMsgs));
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        if (prefs.isEmpty()) {
            insertPreferences(true, true, true, true, true);
            return loadPreferences();
        }
        return prefs;
    }

    public synchronized ArrayList<EIMAccount> loadAccounts() {
        Statement statement = null;
        ArrayList<EIMAccount> account_list = new ArrayList<>();
        try {
            if (!(connection == null) && !connection.isClosed()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading accounts from database");
                }
                statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(EIMConstants.SQL_ACCOUNT_LOADALL);
                while (rs.next()) {
                    int id = rs.getInt("ID");
                    String imap = rs.getString("IMAP");
                    String imap_port = rs.getString("IMAP_PORT");
                    String imap_ssl = rs.getString("IMAP_SSL");
                    String imap_authentication = rs.getString("IMAP_AUTHENTICATION");
                    String smtp = rs.getString("SMTP");
                    String smtp_port = rs.getString("SMTP_PORT");
                    String smtp_ssl = rs.getString("SMTP_SSL");
                    String smtp_authentication = rs.getString("SMTP_AUTHENTICATION");
                    String emailaddress = rs.getString("EMAILADDRESS");
                    String username = rs.getString("USERNAME");
                    String password = rs.getString("PASSWORD");
                    long uid = rs.getLong("UID");
                    if (logger.isDebugEnabled()) {
                        logger.debug("ID: " + id + ", Account: " + username);
                    }
                    EIMAccount acc = new EIMAccount(id, imap, imap_port, imap_ssl, imap_authentication,
                            smtp, smtp_port, smtp_ssl, smtp_authentication,
                            emailaddress, username, password, uid);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Loaded account " + username + " from database");
                    }
                    account_list.add(acc);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        return account_list;
    }

    public synchronized ArrayList<EIMEmailMessage> loadMails() {
        Statement statement = null;
        ArrayList<EIMEmailMessage> mails_list = new ArrayList<>();
        try {
            if (!(connection == null) && !connection.isClosed()) {
                statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(EIMConstants.SQL_MAILS_LOADALL);
                while (rs.next()) {
                    String id = rs.getString("ID");
                    String from = rs.getString("SENDER");
                    String reply_to = rs.getString("REPLY_TO");
                    String to = rs.getString("RECEIVER");
                    String subject = rs.getString("SUBJECT");
                    long dateSent = rs.getLong("DATE_SENT");
                    long uid = rs.getLong("UID");
                    String content = rs.getString("CONTENT");

                    EIMEmailEnvelope em_envelope = new EIMEmailEnvelope(id, from, reply_to, to, subject, new Date(dateSent));
                    EIMEmailContent em_content = new EIMEmailContent(content);
                    EIMEmailMessage message = new EIMEmailMessage(uid, em_envelope, em_content);
                    mails_list.add(message);
                }
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        return mails_list;
    }

    private synchronized void init() throws EIMDatabaseException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading database drivers");
            }
            Class.forName(EIMConstants.DB_DRIVER);

            if (logger.isDebugEnabled()) {
                logger.debug("Connecting to database");
            }
            connection = DriverManager.getConnection(EIMUtility.getInstance().getDatabasePath());
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException: " + e.getMessage());
            throw new EIMDatabaseException("Could not load drivers: " + e.getMessage());
        } catch (SQLException e) {
            throw new EIMDatabaseException("Could not connect to the database: " + e.getMessage());
        }

        if (create_database_Preferences()) {
            try {
                prepareStatementsPreferences();
            } catch (SQLException e) {
                throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Could not create database Preferences");
        }

        if (create_database_Accounts()) {
            try {
                prepareStatementsAccounts();
            } catch (SQLException e) {
                throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Could not create database Accounts");
        }

        if (create_database_Mails()) {
            try {
                prepareStatementsMails();
            } catch (SQLException e) {
                throw new EIMDatabaseException("Could not prepare statements: " + e.getMessage());
            }
        } else {
            throw new EIMDatabaseException("Could not create database Mails");
        }
    }

    private void prepareStatementsPreferences() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing statements for Preferences");
        }
        if (!(connection == null) && !connection.isClosed()) {
            table_preferences_drop = connection.prepareStatement(EIMConstants.SQL_PREFERENCES_DROP);
            table_preferences_insert = connection.prepareStatement(EIMConstants.SQL_PREFERENCES_INSERT);
            table_preferences_update = connection.prepareStatement(EIMConstants.SQL_PREFERENCES_UPDATE);
        }
    }

    private void prepareStatementsMails() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing statements for Mails");
        }
        if (!(connection == null) && !connection.isClosed()) {
            table_mails_insert = connection.prepareStatement(EIMConstants.SQL_MAILS_INSERT);
            table_mails_update = connection.prepareStatement(EIMConstants.SQL_MAILS_UPDATE);
            table_mails_drop = connection.prepareStatement(EIMConstants.SQL_MAILS_DROP);
            table_mails_delete = connection.prepareStatement(EIMConstants.SQL_MAILS_DELETE);
            table_mails_delete_old_mails = connection.prepareStatement(EIMConstants.SQL_MAILS_DELETE_OLD_MAILS);
        }
    }

    private void prepareStatementsAccounts() throws SQLException {
        if (logger.isDebugEnabled()) {
            logger.debug("Preparing statements for Accounts");
        }
        if (!(connection == null) && !connection.isClosed()) {
            table_account_insert = connection.prepareStatement(EIMConstants.SQL_ACCOUNT_INSERT);
            table_account_drop = connection.prepareStatement(EIMConstants.SQL_ACCOUNT_DROP);
            table_account_update = connection.prepareStatement(EIMConstants.SQL_ACCOUNT_UPDATE);
            table_account_delete = connection.prepareStatement(EIMConstants.SQL_ACCOUNT_DELETE);
        }
    }

    private synchronized boolean create_database_Accounts() {
        Statement statement = null;
        try {
            if (!(connection == null) && !connection.isClosed()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating database Accounts");
                }
                statement = connection.createStatement();
                statement.executeUpdate(EIMConstants.SQL_ACCOUNT_CREATE);
                return true;
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        return false;
    }

    private synchronized boolean create_database_Mails() {
        Statement statement = null;
        try {
            if (!(connection == null) && !connection.isClosed()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating database Mails");
                }
                statement = connection.createStatement();
                statement.executeUpdate(EIMConstants.SQL_MAILS_CREATE);
                return true;
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        return false;
    }

    private synchronized boolean create_database_Preferences() {
        Statement statement = null;
        try {
            if (!(connection == null) && !connection.isClosed()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating database Preferences");
                }
                statement = connection.createStatement();
                statement.executeUpdate(EIMConstants.SQL_PREFERENCES_CREATE);
                return true;
            }
        } catch (SQLException e) {
            logger.error("SQLException: " + e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            }
        }
        return false;
    }
}
