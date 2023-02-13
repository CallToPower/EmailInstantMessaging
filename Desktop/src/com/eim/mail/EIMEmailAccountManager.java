/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.exceptions.EIMDatabaseException;
import com.eim.exceptions.EIMFetchException;
import com.eim.db.EIMDatabase;
import com.eim.exceptions.EIMServerException;
import com.eim.util.EIMUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailAccountManager
 *
 * @author Denis Meyer
 */
public abstract class EIMEmailAccountManager {

    private static final Logger logger = LogManager.getLogger(EIMEmailAccountManager.class.getName());
    private final EIMEmailAccountList emailaccount_list;
    private final ArrayList<EIMAccount> database_account_list;
    private final ArrayList<EIMEmailMessage> database_mails_list;
    private final ArrayList<EIMEmailNotifier> notifiers;
    private HashMap<String, EIMEmailMessage> mails = null;
    private ArrayList<EIMEmailMessage> newInitialMails = null;

    public EIMEmailAccountManager() throws EIMDatabaseException {
        emailaccount_list = new EIMEmailAccountList() {
            @Override
            public synchronized void onChange(EIMEmailAccount account, EIMEmailAccountList.STATUS status) {
                onModelChange(account, status);
            }
        };
        this.database_account_list = EIMDatabase.getInstance().loadAccounts();
        this.database_mails_list = EIMDatabase.getInstance().loadMails();
        notifiers = new ArrayList<>();

        mails = new HashMap<>();
        newInitialMails = new ArrayList<>();

        for (EIMAccount acc : database_account_list) {
            addAccount_internal(acc);
        }

        loadAllMails();
    }

    public abstract void handleError(EIMEmailAccount acc, Exception ex);

    public abstract void onModelChange(EIMEmailAccount acc, EIMEmailAccountList.STATUS status);

    public abstract void onNewMessage(EIMEmailMessage msg);

    public abstract void onPushNotAvailable(String accountName);

    public abstract void onStateChange(EIMEmailAccount acc, boolean connected);

    public void removeAllAccounts() throws EIMDatabaseException {
        if (!EIMDatabase.getInstance().dropTableAccounts()) {
            logger.error("Could not drop table Accounts");
            throw new EIMDatabaseException("Could not drop table Accounts");
        } else {
            logger.error("Dropped table Accounts");
        }
        if (!EIMDatabase.getInstance().dropTableMails()) {
            logger.error("Could not drop table Mails");
            throw new EIMDatabaseException("Could not drop table Mails");
        } else {
            logger.error("Dropped table Mails");
        }
        for (int i = 0; i < emailaccount_list.size(); ++i) {
            removeAccount(emailaccount_list.get(i).getAccount().get_emailaddress());
        }
        emailaccount_list.clear();
        database_account_list.clear();
        database_mails_list.clear();
        notifiers.clear();
    }

    public HashMap<String, EIMEmailMessage> getEmails() {
        return mails;
    }

    public int getEmailCount() {
        return mails.size();
    }

    public void addMail(EIMEmailMessage msg) {
        mails.put(msg.getEnvelope().getID(), msg);
    }

    public boolean addAccount(EIMAccount acc, boolean saveData) throws EIMDatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to add account " + acc.get_emailaddress() + " (save data: " + saveData + ")");
        }
        if (!isRegisteredAccount(acc)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Account " + acc.get_emailaddress() + " is not registered, yet");
            }
            if (saveData) {
                if (isInDatabase(acc)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + acc.get_emailaddress() + " is in the database");
                    }
                    if (getDatabaseAccount(acc).equals(acc)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Account " + acc.get_emailaddress() + " is the same as the given account");
                        }
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Account " + acc.get_emailaddress() + " is not the same as the given account");
                        }
                        if (updateAccount(acc)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Account " + acc.get_emailaddress() + " has successfully been updated");
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Account " + acc.get_emailaddress() + " could not be updated");
                            }
                            return false;
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + acc.get_emailaddress() + " is not in the database, yet");
                    }
                    if (EIMDatabase.getInstance().insertAccount(acc)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Account " + acc.get_emailaddress() + " has successfully been inserted into the database");
                        }
                        database_account_list.add(acc);
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Account " + acc.get_emailaddress() + " could not be inserted into the database");
                        }
                        return false;
                    }
                }
            }
            addAccount_internal(acc);
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Account " + acc.get_emailaddress() + " has already been registered");
            }
            EIMEmailAccount emailAcc = getEmailAccount(acc.get_emailaddress());
            if (emailAcc != null) {
                if (emailAcc.getAccount().equals(acc)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + acc.get_emailaddress() + " is the same as the given account");
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + acc.get_emailaddress() + " is not the same as the given account");
                    }
                    if (saveData) {
                        if (updateAccount(acc)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Account " + acc.get_emailaddress() + " has successfully been updated");
                            }
                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Account " + acc.get_emailaddress() + " could not be updated in the database");
                            }
                            return false;
                        }
                    } else {
                        emailAcc.getAccount().update(acc);
                    }
                }
            }
        }
        return true;
    }

    public boolean isInDatabase(EIMAccount acc) {
        return isInDatabase(acc.get_emailaddress());
    }

    public boolean isInDatabase(String accName) {
        for (EIMAccount db_acc : database_account_list) {
            if (db_acc.get_emailaddress().equals(accName)) {
                return true;
            }
        }
        return false;
    }

    public EIMAccount getDatabaseAccount(EIMAccount acc) {
        for (EIMAccount db_acc : database_account_list) {
            if (db_acc.get_emailaddress().equals(acc.get_emailaddress())) {
                return db_acc;
            }
        }
        return null;
    }

    public boolean isRegisteredAccount(EIMAccount acc) {
        return isRegisteredAccount(acc.get_emailaddress());
    }

    public boolean isRegisteredAccount(String accName) {
        for (EIMEmailAccount mail : emailaccount_list) {
            if (mail.getAccount().isAlias(accName)) {
                return true;
            }
        }
        return false;
    }

    public String getRegisteredAccountAlias(String accName) {
        for (EIMEmailAccount mail : emailaccount_list) {
            if (mail.getAccount().isAlias(accName)) {
                return mail.getAccount().get_emailaddress();
            }
        }
        return accName;
    }

    public EIMEmailAccount getEmailAccount(String account) {
        for (EIMEmailAccount mail : emailaccount_list) {
            if (mail.getAccount().isAlias(account)) {
                return mail;
            }
        }
        return null;
    }

    public boolean connect(String accName) throws EIMServerException, MessagingException, EIMFetchException {
        String accName_new = getRegisteredAccountAlias(accName);
        if (logger.isDebugEnabled()) {
            logger.debug("Connecting account " + accName_new);
        }
        EIMEmailAccount acc = getEmailAccount(accName_new);
        if (isRegisteredAccount(acc.getAccount()) && !acc.isConnected()) {
            fetchMails(acc);
            return true;
        }
        return false;
    }

    public boolean updateAccount(EIMAccount updateAcc) throws EIMDatabaseException {
        if (logger.isDebugEnabled()) {
            logger.debug("Updating account " + updateAcc.get_emailaddress());
        }
        EIMEmailAccount acc = getEmailAccount(updateAcc.get_emailaddress());
        if (acc != null) {
            if (EIMDatabase.getInstance().updateAccount(updateAcc)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Account " + updateAcc.get_emailaddress() + " has successfully been updated in the database");
                }
                EIMAccount dbAcc = getDatabaseAccount(updateAcc);
                if (dbAcc != null) {
                    dbAcc.update(updateAcc);
                }
                return true;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Account " + updateAcc.get_emailaddress() + " could not be updated in the database");
                }
            }
        }
        return false;
    }

    public synchronized void removeAccount(final String accountName) {
        for (int i = 0; i < emailaccount_list.size(); ++i) {
            final int j = i;
            final EIMAccount acc = emailaccount_list.get(j).getAccount();
            if ((acc != null) && acc.get_emailaddress().equals(accountName)) {
                try {
                    if (EIMDatabase.getInstance().deleteAccount(acc.get_id())) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Removing account (+ its mails) " + acc.get_emailaddress());
                                }
                                for (int j = 0; j < database_account_list.size(); ++j) {
                                    if (database_account_list.get(j).get_id() == acc.get_id()) {
                                        database_account_list.remove(j);
                                    }
                                }
                                Iterator it = mails.entrySet().iterator();
                                int deletedMails = 0;
                                int notDeletedMails = 0;
                                while (it.hasNext()) {
                                    Map.Entry pairs = (Map.Entry) it.next();
                                    String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(((EIMEmailMessage) pairs.getValue()).getEnvelope().getFirstTo())[1]);
                                    if (acc.isAlias(to)) {
                                        try {
                                            if (!EIMDatabase.getInstance().deleteMail((String) pairs.getKey())) {
                                                throw new EIMDatabaseException("Could not delete email");
                                            }
                                        } catch (EIMDatabaseException e) {
                                            logger.error("EIMDatabaseException: " + e.getMessage());
                                            ++notDeletedMails;
                                        }
                                        it.remove();
                                    } else {
                                        ++deletedMails;
                                    }
                                }
                                if (deletedMails > 0) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Succesfully deleted " + deletedMails + " mails from account " + acc.get_username());
                                    }
                                }
                                if (notDeletedMails > 0) {
                                    logger.error("Could not delete " + notDeletedMails + " mails from account " + acc.get_username());
                                }
                                emailaccount_list.get(j).disconnect(false);
                                emailaccount_list.remove(emailaccount_list.get(j));
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Successfully removed account " + accountName + " from database and from the account manager");
                                }

                            }
                        });
                        t.start();
                    } else {
                        logger.error("Could not remove account " + accountName + " from database");
                    }
                } catch (EIMDatabaseException e) {
                    logger.error("EIMDatabaseException: " + e.getMessage());
                }
            }
        }
    }

    public void forceDisconnect(String accountName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Forcing disconnecting of account " + accountName);
        }
        for (EIMEmailAccount acc : emailaccount_list) {
            if (acc.getAccount().get_emailaddress().equals(accountName)) {
                try {
                    acc.forceDisconnect(false);
                } catch (Exception e) {
                    logger.error("Exception: " + e);
                }
            }
        }
    }

    public void disconnect(String accountName) {
        if (logger.isDebugEnabled()) {
            logger.debug("Disconnecting of account " + accountName);
        }
        for (EIMEmailAccount acc : emailaccount_list) {
            if (acc.getAccount().get_emailaddress().equals(accountName)) {
                try {
                    acc.disconnect(false);
                } catch (Exception e) {
                    logger.error("Exception: " + e);
                }
            }
        }
    }

    public void reconnectAllDisconnected() throws EIMServerException, MessagingException, EIMFetchException {
        if (logger.isDebugEnabled()) {
            logger.debug("Reconnecting all accounts");
        }
        for (EIMEmailAccount acc : emailaccount_list) {
            connect(acc.getAccount().get_emailaddress());
        }
    }

    public void disconnectAllAccounts() {
        if (logger.isDebugEnabled()) {
            logger.debug("Disconnecting all accounts");
        }
        for (EIMEmailAccount acc : emailaccount_list) {
            disconnect(acc.getAccount().get_emailaddress());
        }
    }

    public boolean allDisconnected() {
        for (EIMEmailAccount acc : emailaccount_list) {
            if (acc.isConnected()) {
                return false;
            }
        }
        return true;
    }

    public ArrayList<EIMEmailAccount> getAllAccounts() {
        ArrayList<EIMEmailAccount> newArrList = new ArrayList<>();
        for (EIMEmailAccount account : emailaccount_list) {
            newArrList.add(account);
        }
        return newArrList;
    }

    public EIMEmailAccount getFirstAccount() {
        if (emailaccount_list.size() > 0) {
            return emailaccount_list.get(0);
        }
        return null;
    }

    public int getNumberOfAccounts() {
        return emailaccount_list.size();
    }

    public EIMEmailNotifier getNotifier(int i) {
        if (i < notifiers.size()) {
            return notifiers.get(i);
        }
        return null;
    }

    private void addAccount_internal(EIMAccount acc) {
        if (logger.isDebugEnabled()) {
            logger.debug("Adding account to the account manager account list");
        }
        final EIMEmailAccount account = new EIMEmailAccount(acc) {
            @Override
            public synchronized void onError(Exception e) {
                this.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                handleError(this, e);
            }

            @Override
            public synchronized void onConnect() {
                if (logger.isDebugEnabled()) {
                    logger.debug("Connected account " + this.getAccount().get_emailaddress());
                }
                this.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGGED_IN);
                onStateChange(this, true);
            }

            @Override
            public synchronized void onDisconnect() {
                if ((this.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)
                        || (this.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Disconnected account " + this.getAccount().get_emailaddress());
                    }
                    this.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGGED_OUT);
                    onStateChange(this, false);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + this.getAccount().get_emailaddress() + " is already disconnected.");
                    }
                }
            }

            @Override
            public void onConnectionLost() {
                if ((this.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)
                        || (this.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT)
                        || (this.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_IN)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Connection lost for account " + this.getAccount().get_emailaddress());
                    }
                    this.getAccount().setStatus(EIMAccount.LOGIN_STATUS.NO_CONNECTION);
                    onStateChange(this, false);
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + this.getAccount().get_emailaddress() + " is already disconnected.");
                    }
                }
            }

            @Override
            public synchronized void onMailFetched(boolean fetchSuccessful, long uid, boolean firstFetch) {
                if (fetchSuccessful) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Fetched mail");
                    }

                    newInitialMails = this.getAllMail();
                    for (EIMEmailMessage mail : newInitialMails) {
                        try {
                            if (!saveMail(mail)) {
                                logger.error("Could not save mail for account " + getAccount().get_emailaddress());
                            } else {
                                database_mails_list.add(mail);
                            }
                        } catch (EIMDatabaseException e) {
                            logger.error("EIMDatabaseException: " + e.getMessage());
                        }
                    }

                    if (firstFetch) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("First fetch, clearing new initial mails");
                        }
                        newInitialMails.clear();
                    } else {
                        ArrayList<EIMEmailMessage> toBeRemoved = new ArrayList<>();
                        for (EIMEmailMessage mail : newInitialMails) {
                            if (mail.getUID() < getAccount().getUID()) {
                                toBeRemoved.add(mail);
                            }
                        }
                        for (EIMEmailMessage mail : toBeRemoved) {
                            newInitialMails.remove(mail);
                        }
                        toBeRemoved.clear();
                    }

                    getAccount().setUID(uid);
                    try {
                        updateAccount(getAccount());
                    } catch (EIMDatabaseException e) {
                        logger.error("EIMDatabaseException: " + e.getMessage());
                    }

                    String name = getAccount().get_emailaddress();
                    loadAllMails();
                    startMailDaemon(getEmailAccount(name));
                } else {
                    logger.error("Fetch not successful");
                    this.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                    handleError(this, new EIMFetchException("Fetch failed", EIMFetchException.STATUS.NORMAL));
                }
            }

            @Override
            public synchronized void onNewMessages(ArrayList<EIMEmailMessage> msgs) {
                for (EIMEmailMessage msg : msgs) {
                    try {
                        if (!saveMail(msg)) {
                            logger.error("Could not save mail for account " + getAccount().get_emailaddress());
                        } else {
                            database_mails_list.add(msg);
                            addMail(msg);
                            long msgUID = msg.getUID();
                            long accUID = getAccount().getUID();
                            long uidToSet = (msgUID > accUID) ? msgUID : (getAccount().getUID() + 1);
                            getAccount().setUID(uidToSet);
                            try {
                                updateAccount(getAccount());
                            } catch (EIMDatabaseException e) {
                                logger.error("EIMDatabaseException: " + e.getMessage());
                            }
                            onNewMessage(msg);
                        }
                    } catch (EIMDatabaseException e) {
                        logger.error("EIMDatabaseException: " + e.getMessage());
                    }
                }
            }

            @Override
            public synchronized void onPushNotAvailable() {
                EIMEmailAccountManager.this.onPushNotAvailable(getAccount().get_username());
            }
        };
        emailaccount_list.add(account);
        notifiers.add(new EIMEmailNotifier(account));
        onModelChange(account, EIMEmailAccountList.STATUS.ADDED);
    }

    public void fetchInitialMails() {
        for (EIMEmailMessage mail : newInitialMails) {
            onNewMessage(mail);
        }
        newInitialMails.clear();
    }

    private void fetchMails(EIMEmailAccount acc) throws EIMServerException, MessagingException, EIMFetchException {
        if (logger.isDebugEnabled()) {
            logger.debug("Fetching mails for account " + acc.getAccount().get_emailaddress());
        }
        acc.fetchMail();
    }

    private boolean loadAllMails() {
        if (!database_mails_list.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(database_mails_list.size() + " mails found");
            }
            for (EIMEmailMessage mail : database_mails_list) {
                String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(mail.getEnvelope().getFirstTo())[1]);
                String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(mail.getEnvelope().getFirstFrom())[1]);
                for (EIMEmailAccount eea : getAllAccounts()) {
                    if (eea.getAccount().isAlias(to)) {
                        to = eea.getAccount().get_emailaddress();
                        break;
                    }
                }
                for (EIMEmailAccount eea : getAllAccounts()) {
                    if (eea.getAccount().isAlias(from)) {
                        from = eea.getAccount().get_emailaddress();
                        break;
                    }
                }
                EIMEmailAccount emailAccTo = getEmailAccount(to);
                EIMEmailAccount emailAccFrom = getEmailAccount(from);
                if ((emailAccTo != null) || (emailAccFrom != null)) {
                    EIMAccount acc = (emailAccTo == null) ? null : emailAccTo.getAccount();
                    if ((acc != null) && isRegisteredAccount(acc)) {
                        addMail(mail);
                        if (emailAccTo != null) {
                            emailAccTo.setLastMessageUID(mail.getUID());
                        }
                    } else {
                        EIMAccount acc2 = emailAccFrom.getAccount();
                        if ((acc2 != null) && isRegisteredAccount(acc2)) {
                            addMail(mail);
                            emailAccFrom.setLastMessageUID(mail.getUID());
                        } else {
                            try {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Email accounts " + to + " and " + from + " are not available, deleting mail");
                                }
                                EIMDatabase.getInstance().deleteMail(mail.getEnvelope().getID());
                            } catch (EIMDatabaseException e) {
                                logger.error("EIMDatabaseException: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Email accounts " + to + " and " + from + " are null, deleting mail");
                    }
                    try {
                        EIMDatabase.getInstance().deleteMail(mail.getEnvelope().getID());
                    } catch (EIMDatabaseException e) {
                        logger.error("EIMDatabaseException: " + e.getMessage());
                    }
                }
            }
            return true;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No mails found");
            }
            return false;
        }
    }

    private boolean saveMail(EIMEmailMessage mail) throws EIMDatabaseException {
        return EIMDatabase.getInstance().insertMail(mail);
    }

    private synchronized void startMailDaemon(EIMEmailAccount mail) {
        if (!mail.isConnected()) {
            String threadName = "EIM-" + mail.getAccount().get_emailaddress();
            if (logger.isDebugEnabled()) {
                logger.debug("Starting thread " + threadName);
            }
            Thread t = new Thread(mail);
            t.setName(threadName);
            t.start();
        }
    }
}
