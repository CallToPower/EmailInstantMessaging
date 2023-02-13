/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.eim;

import com.eim.platform.EIMMacOSHandler;
import com.eim.exceptions.EIMDatabaseException;
import com.eim.exceptions.EIMFetchException;
import com.eim.exceptions.EIMTrayIconNotSupportedException;
import com.eim.mail.EIMAccount;
import com.eim.mail.EIMEmailAccount;
import com.eim.mail.EIMEmailAccountList;
import java.io.FileNotFoundException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.eim.mail.EIMEmailAccountManager;
import com.eim.mail.EIMEmailMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eim.ui.EIMDesktopUI;
import com.eim.ui.EIMLoginUI;
import com.eim.util.EIMConstants;
import com.eim.db.EIMDatabase;
import com.eim.db.EIMI18N;
import com.eim.exceptions.EIMMessagingException;
import com.eim.exceptions.EIMServerException;
import com.eim.img.EIMImage;
import com.eim.util.EIMNotification;
import com.eim.sound.EIMSound;
import com.eim.ui.EIMNotificationUI;
import com.eim.ui.EIMSplashscreenUI;
import com.eim.ui.EIMWizardUI;
import com.eim.util.EIMUtility;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
/*
 import com.eim.util.EIMCrypto;
 import com.eim.exceptions.EIMCouldNotWriteToFileException;
 import com.eim.util.EIMFileUtils;
 import java.io.IOException;
 import java.security.InvalidKeyException;
 import java.security.NoSuchAlgorithmException;
 import java.security.spec.InvalidKeySpecException;
 */

/**
 * EIMController
 *
 * @author Denis Meyer
 */
public final class EIMController {

    private static final Logger logger = LogManager.getLogger(EIMController.class.getName());
    private EIMWizardUI wizardui = null;
    private EIMLoginUI loginui = null;
    private EIMDesktopUI desktopui = null;
    private EIMEmailAccountManager accountManager = null;
    private HashMap<String, String> preferences_list;
    private EIMSplashscreenUI splashscreenui = null;
    // private EIMCrypto crypto;

    public EIMController() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMController");
        }
        initPlatformSpecifications();
        EIMNotification.getInstance();
        startSplashscreen();
        splashscreenui.update(0, EIMI18N.getInstance().getString("STATUS_LoadingImages"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 0
        cacheImages();
        splashscreenui.update(splashscreenui.getProgress() + 10, EIMI18N.getInstance().getString("STATUS_LoadingSounds"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 10
        cacheSounds();
        splashscreenui.update(splashscreenui.getProgress() + 10, EIMI18N.getInstance().getString("STATUS_SettingLookAndFeel"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 20
        setLookAndFeel();
        splashscreenui.update(splashscreenui.getProgress() + 10, EIMI18N.getInstance().getString("STATUS_InitializingDatabase"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 30
        // initCrypto();
        // decryptDatabase();
        initDatabase();
        splashscreenui.update(splashscreenui.getProgress() + 5, EIMI18N.getInstance().getString("STATUS_InitializingDatabase"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 35
        deleteOldMails();
        splashscreenui.update(splashscreenui.getProgress() + 5, EIMI18N.getInstance().getString("STATUS_LoadingPreferences"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 40
        initPreferences();
        splashscreenui.update(splashscreenui.getProgress() + 20, EIMI18N.getInstance().getString("STATUS_InitializingUserInterface"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 60
        initUI();
        splashscreenui.update(splashscreenui.getProgress() + 20, EIMI18N.getInstance().getString("STATUS_InitializingAccounts"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 80
        initAccounts();
        splashscreenui.update(splashscreenui.getProgress() + 20, EIMI18N.getInstance().getString("STATUS_DoneLoading"), EIMConstants.SPLASHSCREEN_SHOW_PERCENTAGE); // 100
        splashscreenui.quit();
    }

    public boolean saveData() {
        String s = preferences_list.get(EIMConstants.PREFERENCE_SAVEDATA);
        s = (s == null) ? "true" : s;
        return Boolean.parseBoolean(s);
    }

    public boolean autoLogin() {
        String s = preferences_list.get(EIMConstants.PREFERENCE_AUTOLOGIN);
        s = (s == null) ? "true" : s;
        return Boolean.parseBoolean(s);
    }

    public boolean playSounds() {
        String s = preferences_list.get(EIMConstants.PREFERENCE_PLAYSOUNDS);
        s = (s == null) ? "true" : s;
        return Boolean.parseBoolean(s);
    }

    public boolean displayNotifications() {
        String s = preferences_list.get(EIMConstants.PREFERENCE_DISPLAYNOTIFICATIONS);
        s = (s == null) ? "true" : s;
        return Boolean.parseBoolean(s);
    }

    public boolean displayOnlyEIMMsgs() {
        String s = preferences_list.get(EIMConstants.PREFERENCE_ONLYEIMMSGS);
        s = (s == null) ? "true" : s;
        return Boolean.parseBoolean(s);
    }

    public EIMEmailAccountManager getAccountManager() {
        return accountManager;
    }

    public EIMDesktopUI getDesktopUI() {
        return desktopui;
    }

    public EIMWizardUI getWizardUI() {
        return wizardui;
    }

    public EIMLoginUI getLoginUI() {
        return loginui;
    }

    public boolean savePreferences(boolean saveData, boolean autoLogin, boolean playSounds, boolean displayNotifications, boolean onlyEIMMsgs) {
        boolean succeeded = false;
        try {
            if (EIMDatabase.getInstance().updatePreference(saveData, autoLogin, playSounds, displayNotifications, onlyEIMMsgs)) {
                preferences_list.put(EIMConstants.PREFERENCE_SAVEDATA, String.valueOf(saveData));
                preferences_list.put(EIMConstants.PREFERENCE_AUTOLOGIN, String.valueOf(autoLogin));
                preferences_list.put(EIMConstants.PREFERENCE_PLAYSOUNDS, String.valueOf(playSounds));
                preferences_list.put(EIMConstants.PREFERENCE_DISPLAYNOTIFICATIONS, String.valueOf(displayNotifications));
                preferences_list.put(EIMConstants.PREFERENCE_ONLYEIMMSGS, String.valueOf(onlyEIMMsgs));
                EIMNotification.getInstance().setDisplay(displayNotifications);
                succeeded = true;
            }
        } catch (EIMDatabaseException e) {
            logger.error("EIMDatabaseException: " + e.getMessage());
        }
        if (!succeeded) {
            preferences_list.put(EIMConstants.PREFERENCE_SAVEDATA, "true");
            preferences_list.put(EIMConstants.PREFERENCE_AUTOLOGIN, "true");
            preferences_list.put(EIMConstants.PREFERENCE_PLAYSOUNDS, "true");
            preferences_list.put(EIMConstants.PREFERENCE_DISPLAYNOTIFICATIONS, "true");
            preferences_list.put(EIMConstants.PREFERENCE_ONLYEIMMSGS, "true");
            EIMNotification.getInstance().setDisplay(true);
        }
        return succeeded;
    }

    public void stateChanged(EIMAccount acc) {
        EIMEmailAccount account = accountManager.getEmailAccount(acc.get_emailaddress());
        if (account != null) {
            account.getAccount().setStatus(acc.getStatus());
            desktopui.stateChanged(account.getAccount());
        } else {
            desktopui.stateChanged(acc);
        }
    }

    public void displayNewMessage(String from, String to, String shortenedContent) {
        for (EIMEmailAccount eea : accountManager.getAllAccounts()) {
            if (eea.getAccount().isAlias(to)) {
                to = eea.getAccount().get_emailaddress();
                break;
            }
        }
        EIMNotification.getInstance().showMessageNotification(
                this,
                EIMI18N.getInstance().getString("NewMessage"),
                from,
                to,
                shortenedContent,
                playSounds() ? EIMConstants.SOUND.SOUND_NEW_NOT_OPEN : null,
                EIMNotificationUI.TYPE.MESSAGE);
    }

    public void displayLoggedInNotification(String accountName) {
        EIMNotification.getInstance().showLoggedInNotification(
                this,
                accountName,
                EIMI18N.getInstance().getString("Account") + " " + accountName,
                EIMI18N.getInstance().getString("LoggedIn"),
                "",
                playSounds() ? EIMConstants.SOUND.LOGGED_IN : null,
                EIMNotificationUI.TYPE.INFORMATION);
    }

    public void displayNotification(String info, String msg, String content, EIMConstants.SOUND sound) {
        EIMNotification.getInstance().showNotification(
                info,
                msg,
                content,
                playSounds() ? sound : null,
                EIMNotificationUI.TYPE.INFORMATION);
    }

    public void displayError(String info, String msg, String content) {
        EIMNotification.getInstance().showNotification(
                info,
                msg,
                content,
                playSounds() ? EIMConstants.SOUND.ERROR : null,
                EIMNotificationUI.TYPE.ERROR);
    }

    public void displayErrorAndQuit(String info, String msg, String content) {
        EIMNotification.getInstance().showNotificationAndQuit(
                this,
                info,
                msg,
                content,
                playSounds() ? EIMConstants.SOUND.ERROR : null,
                EIMNotificationUI.TYPE.ERROR);
    }

    public void displayContactListOrLoginUI(String accountName) {
        EIMEmailAccount account = accountManager.getEmailAccount(accountName);
        if (account != null) {
            if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN) {
                desktopui.toggleContactListWindow(account);
            } else if ((account.getAccount().getStatus() != EIMAccount.LOGIN_STATUS.LOGGING_IN)
                    && (account.getAccount().getStatus() != EIMAccount.LOGIN_STATUS.LOGGING_OUT)) {
                wizardui.cancel();
                loginui.setData(account.getAccount());
                loginui.setServerSectionVisible(false);
                loginui.setVisible(true);
            }
        } else {
            loginui.clear();
            loginui.setVisible(false);
            wizardui.clear();
            wizardui.setVisible(true);
        }
    }

    public void displayMessage(final String accountName, final String from) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EIMEmailAccount account = accountManager.getEmailAccount(accountName);
                    if ((account != null)
                            && (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)) {
                        desktopui.displayMessage(account, from);
                    }
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
            }
        });
        t.start();
    }

    public boolean logoutOrDeleteAccount(String accountName) {
        EIMEmailAccount account = accountManager.getEmailAccount(accountName);
        if (account != null) {
            if ((account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)
                    && desktopui.clickedYes(
                            EIMI18N.getInstance().getString("LogoutQuestion_info"),
                            EIMI18N.getInstance().getString("LogoutQuestion"))) {
                account.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGGING_OUT);
                stateChanged(account.getAccount());
                accountManager.disconnect(accountName);
                return true;
            } else if ((account.getAccount().getStatus() != EIMAccount.LOGIN_STATUS.LOGGED_IN)
                    && (account.getAccount().getStatus() != EIMAccount.LOGIN_STATUS.LOGGING_IN)
                    && (account.getAccount().getStatus() != EIMAccount.LOGIN_STATUS.LOGGING_OUT)
                    && desktopui.clickedYes(
                            EIMI18N.getInstance().getString("DeleteAccountQuestion_info"),
                            EIMI18N.getInstance().getString("DeleteAccountQuestion"))) {
                account.getAccount().setStatus(EIMAccount.LOGIN_STATUS.DELETED);
                desktopui.stateChanged(account.getAccount());
                accountManager.removeAccount(accountName);
                displayNotification(accountName, EIMI18N.getInstance().getString("DeletedAccount"), "", EIMConstants.SOUND.INFORMATION);
                return true;
            }
            /*
             else if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT) {
             accountManager.forceDisconnect(accountName);
             return true;
             }
             */
        }
        return false;
    }

    public boolean accountInAccountManager(String accountName) {
        return (accountManager.getEmailAccount(accountName) != null);
    }

    public void login(final EIMAccount acc, final boolean login) {
        if (acc != null) {
            if (((acc.getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN) || (acc.getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT))
                    && desktopui.isInMenuBar(acc.get_emailaddress())) {
                displayNotification(
                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                        EIMI18N.getInstance().getString("AccountAlreadyAdded_info"),
                        EIMI18N.getInstance().getString("AccountAlreadyAdded"),
                        EIMConstants.SOUND.ERROR);
                return;
            }
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EIMEmailAccount acc_check = accountManager.getEmailAccount(acc.get_emailaddress());
                        if (acc_check != null) {
                            if ((acc_check.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN)
                                    || (acc_check.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_IN)
                                    || (acc_check.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT)) {
                                displayNotification(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("AccountAlreadyAdded_info"),
                                        EIMI18N.getInstance().getString("AccountAlreadyAdded"),
                                        EIMConstants.SOUND.ERROR);
                                return;
                            }
                        }
                        if (login) {
                            if (acc.getUID() <= 0) {
                                displayNotification(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("NowFetchingFirstTime_info"),
                                        EIMI18N.getInstance().getString("NowFetchingFirstTime"),
                                        EIMConstants.SOUND.INFORMATION);
                            } else {
                                displayNotification(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("NowFetching"),
                                        "",
                                        EIMConstants.SOUND.INFORMATION);
                            }
                        }
                        if (!login_internal(acc, login)) {
                            displayError(
                                    EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                    EIMI18N.getInstance().getString("CouldNotLogin"),
                                    "");
                        }
                    } catch (EIMServerException e) {
                        logger.error("EIMServerException: " + e.getMessage());
                        displayError(
                                EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                EIMI18N.getInstance().getString("ServerError"),
                                "");
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                        stateChanged(acc);
                    } catch (MessagingException e) {
                        logger.error("MessagingException: " + e.getMessage());
                        displayError(
                                EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                EIMI18N.getInstance().getString("AuthenticationFailed_info"),
                                EIMI18N.getInstance().getString("AuthenticationFailed"));
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                        stateChanged(acc);
                    } catch (EIMDatabaseException e) {
                        logger.error("EIMDatabaseException: " + e.getMessage());
                        displayError(
                                EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                EIMI18N.getInstance().getString("CouldNotSaveData"),
                                "");
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                        stateChanged(acc);
                    } catch (EIMFetchException e) {
                        logger.error("EIMServerException: " + e.getMessage());
                        switch (e.status) {
                            case NO_FOLDER_FOUND:
                                displayError(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("FetchFailed_info"),
                                        EIMI18N.getInstance().getString("FetchFailedNoFolder"));
                                break;
                            case NO_SENT_FOLDER_FOUND:
                                displayError(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("FetchFailed_info"),
                                        EIMI18N.getInstance().getString("FetchFailedNoSentFolder"));
                                break;
                            default:
                                displayError(
                                        EIMI18N.getInstance().getString("Account") + " " + acc.get_emailaddress(),
                                        EIMI18N.getInstance().getString("FetchFailed_info"),
                                        "");
                                break;
                        }
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                        stateChanged(acc);
                    } catch (Exception e) {
                        logger.error("Exception: " + e.getMessage());
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                        stateChanged(acc);
                    }
                }
            });
            t.start();
        }
    }

    private boolean login_internal(EIMAccount acc, boolean login) throws EIMServerException, MessagingException, EIMDatabaseException, EIMFetchException {
        if (acc != null) {
            try {
                if (accountManager.addAccount(acc, saveData())) {
                    if (login) {
                        acc.setStatus(EIMAccount.LOGIN_STATUS.LOGGING_IN);
                        stateChanged(acc);
                        return accountManager.connect(acc.get_emailaddress());
                    } else {
                        return true;
                    }
                }
                return false;
            } catch (EIMDatabaseException | EIMServerException | MessagingException | EIMFetchException e) {
                acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                stateChanged(acc);
                throw e;
            } catch (Exception e) {
                logger.error("Exception: " + e.getMessage());
                acc.setStatus(EIMAccount.LOGIN_STATUS.LOGIN_FAILED);
                stateChanged(acc);
            }
        }
        return false;
    }

    public boolean quit(boolean fromMacOSApplicationHandler) {
        if (!EIMConstants.ASK_TO_QUIT
                || (EIMConstants.ASK_TO_QUIT
                && desktopui.clickedYes(
                        EIMI18N.getInstance().getString("QuitApplicationQuestion_info"),
                        EIMI18N.getInstance().getString("QuitApplicationQuestion")))) {
            preferences_list.put(EIMConstants.PREFERENCE_PLAYSOUNDS, "false");
            preferences_list.put(EIMConstants.PREFERENCE_DISPLAYNOTIFICATIONS, "false");
            EIMNotification.getInstance().setDisplay(false);
            if (accountManager != null) {
                accountManager.disconnectAllAccounts();
            }
            try {
                if (logger.isDebugEnabled()) {
                    logger.debug("Disconnecting from database");
                }
                EIMDatabase.getInstance().disconnect();
            } catch (SQLException e) {
                logger.error("SQLException: " + e.getMessage());
            } catch (EIMDatabaseException e) {
                logger.error("EIMDatabaseException: " + e.getMessage());
            }
            if (desktopui != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("De-initializing desktop ui");
                }
                desktopui.deinit();
            }
            if (loginui != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Disposing login ui");
                }
                loginui.dispose();
            }
            if (wizardui != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Disposing wizard ui");
                }
                wizardui.dispose();
            }
            // encryptDatabase();
            if (logger.isDebugEnabled()) {
                logger.debug("Quitting application");
            }
            if (fromMacOSApplicationHandler) {
                return true;
            } else {
                System.exit(0);
            }
        }
        return false;
    }

    private void startSplashscreen() {
        splashscreenui = new EIMSplashscreenUI(null, true);
        if (EIMConstants.SHOW_SPLASHSCREEN) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    splashscreenui.setVisible();
                }
            });
            t.start();
        }
    }

    private void setLookAndFeel() {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting look and feel");
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            logger.error("Error setting system look and feel: " + e.getMessage());
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e2) {
                logger.error("Error setting cross-platform look and feel: " + e2.getMessage());
            }
        }
    }

    private void initPlatformSpecifications() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing platform specifications");
        }
        if (EIMUtility.getInstance().platformIsMac()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Platform is Mac");
            }
            try {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("com.apple.mrj.application.apple.menu.about.name", "EIM");
                System.setProperty("com.apple.mrj.application.live-resize", "true");
                System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
                System.setProperty("com.apple.macos.smallTabs", "true");
                EIMMacOSHandler macoshandler = new EIMMacOSHandler(this);
            } catch (Exception e) {
                logger.error("Exception: " + e.getMessage());
            }
        }
    }

    private void cacheImages() {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching images");
        }
        for (EIMConstants.IMAGE img : EIMConstants.IMAGE.values()) {
            try {
                EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(img));
            } catch (FileNotFoundException e) {
                logger.error("FileNotFoundException: " + e.getMessage());
            }
        }
    }

    private void cacheSounds() {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching sounds");
        }
        for (EIMConstants.SOUND sound : EIMConstants.SOUND.values()) {
            EIMSound.getInstance().loadSound(sound);
        }
    }

    private void initWizardUI() {
        wizardui = new EIMWizardUI() {
            @Override
            public void accountSelected(EIMWizardUI.ACCOUNTTYPE accType) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selected account type: " + accType.toString());
                }
                EIMAccount acc = new EIMAccount(-1, "", "", "", "", "", "", "", "", "", "", "", -1);
                switch (accType) {
                    case AOL:
                        acc.set_emailaddress("@aol.com");
                        loginui.clear();
                        loginui.setData(acc);
                        loginui.checkServerDB();
                        loginui.clearEmailAddressField();
                        loginui.setServerSectionVisible(false);
                        loginui.setVisible(true);
                        break;
                    default:
                    case CUSTOM:
                        loginui.clear();
                        loginui.setServerSectionVisible(true);
                        loginui.setVisible(true);
                        break;
                    case GMAIL:
                        acc.set_emailaddress("@gmail.com");
                        loginui.clear();
                        loginui.setData(acc);
                        loginui.checkServerDB();
                        loginui.clearEmailAddressField();
                        loginui.setServerSectionVisible(false);
                        loginui.setVisible(true);
                        break;
                    case NEOMAILBOX:
                        acc.set_emailaddress("@neomailbox.ch");
                        loginui.clear();
                        loginui.setData(acc);
                        loginui.checkServerDB();
                        loginui.clearEmailAddressField();
                        loginui.setServerSectionVisible(false);
                        loginui.setVisible(true);
                        break;
                    case OUTLOOK:
                        acc.set_emailaddress("@outlook.com");
                        loginui.clear();
                        loginui.setData(acc);
                        loginui.checkServerDB();
                        loginui.clearEmailAddressField();
                        loginui.setServerSectionVisible(false);
                        loginui.setVisible(true);
                        break;
                    case UOS:
                        acc.set_emailaddress("@uos.de");
                        loginui.clear();
                        loginui.setData(acc);
                        loginui.checkServerDB();
                        loginui.clearEmailAddressField();
                        loginui.setServerSectionVisible(false);
                        loginui.setVisible(true);
                        break;
                }
            }
        };
    }

    private void initLoginUI() {
        loginui = new EIMLoginUI() {
            @Override
            public void cancel() {
                if (!loginui.changedSomething()
                        || (loginui.changedSomething()
                        && desktopui.clickedYes(EIMI18N.getInstance().getString("CancelAccountCreation_info"),
                                EIMI18N.getInstance().getString("CancelAccountCreation")))) {
                    clear();
                    this.setVisible(false);
                }
            }

            @Override
            public void login(
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
                    String password) {

                setVisible(false);
                long lastUpdate = -1l;
                int id = -1;
                EIMEmailAccount a = accountManager.getEmailAccount(email);
                if (a != null) {
                    lastUpdate = a.getAccount().getUID();
                    id = a.getAccount().get_id();
                }
                EIMAccount acc = new EIMAccount(
                        id,
                        imap,
                        imapPort,
                        imapSsl,
                        imapAuth,
                        smtp,
                        smtpPort,
                        smtpSsl,
                        smtpAuth,
                        email,
                        username,
                        password,
                        lastUpdate);
                EIMController.this.login(acc, true);
            }

            @Override
            public void onError(String emailAddress, String info, String content) {
                displayError(
                        emailAddress,
                        info,
                        content);
            }

            @Override
            public void delete(String emailAddress) {
                EIMEmailAccount acc = accountManager.getEmailAccount(emailAddress);
                if (acc != null) {
                    if (logoutOrDeleteAccount(emailAddress)) {
                        cancel();
                    }
                } else {
                    cancel();
                }
            }
        };
    }

    private boolean initManager() {
        try {
            accountManager = new EIMEmailAccountManager() {
                @Override
                public synchronized void handleError(EIMEmailAccount account, Exception e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("ERROR in account " + account.getAccount().get_emailaddress());
                    }
                    if (e instanceof AuthenticationFailedException) {
                        logger.error("AuthenticationFailedException: " + e.getMessage());
                        stateChanged(account.getAccount());
                        if (!loginui.isVisible()) {
                            loginui.setData(account.getAccount());
                            loginui.setVisible(true);
                        }
                        displayError(
                                EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                EIMI18N.getInstance().getString("AuthenticationFailed_info"),
                                EIMI18N.getInstance().getString("AuthenticationFailed"));
                    } else if (e instanceof EIMFetchException) {
                        logger.error("EIMFetchException: " + e.getMessage());
                        stateChanged(account.getAccount());
                        displayError(
                                EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                EIMI18N.getInstance().getString("FetchFailed_info"),
                                EIMI18N.getInstance().getString("FetchFailed"));
                    } else if (e instanceof EIMMessagingException) {
                        logger.error("EIMMessagingException: " + e.getMessage() + " on account " + account.getAccount().get_emailaddress());
                        stateChanged(account.getAccount());
                        switch (((EIMMessagingException) e).status) {
                            default:
                            case NORMAL:
                                displayError(
                                        EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                        EIMI18N.getInstance().getString("ErrorOccurred"),
                                        "");
                                break;
                            case NO_CONNECTION_TO_STORE:
                                displayError(
                                        EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                        EIMI18N.getInstance().getString("ErrorOccurred"),
                                        EIMI18N.getInstance().getString("NoConnectionToStore"));
                                break;
                        }
                    } else if (e instanceof IllegalStateException) {
                        logger.error("IllegalStateException: " + e.getMessage() + " on account " + account.getAccount().get_emailaddress());
                    }
                }

                @Override
                public synchronized void onModelChange(EIMEmailAccount account, EIMEmailAccountList.STATUS status) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + account.getAccount().get_emailaddress() + " " + ((status == EIMEmailAccountList.STATUS.ADDED) ? "added" : "removed"));
                    }
                }

                @Override
                public synchronized void onStateChange(EIMEmailAccount account, boolean connected) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Account " + account.getAccount().get_emailaddress() + " changed. Connected: " + connected);
                    }
                    if (!(account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.DELETED)) {
                        if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_IN) {
                            displayLoggedInNotification(account.getAccount().get_emailaddress());
                            accountManager.fetchInitialMails();
                        } else if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGED_OUT) {
                            displayNotification(
                                    EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                    EIMI18N.getInstance().getString("LoggedOut"),
                                    "",
                                    EIMConstants.SOUND.LOGGED_OUT);
                        } else if (account.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.NO_CONNECTION) {
                            displayNotification(
                                    EIMI18N.getInstance().getString("Account") + " " + account.getAccount().get_emailaddress(),
                                    EIMI18N.getInstance().getString("NoConnection_info"),
                                    EIMI18N.getInstance().getString("NoConnection"),
                                    EIMConstants.SOUND.LOGGED_OUT);
                            account.getAccount().setStatus(EIMAccount.LOGIN_STATUS.LOGGED_OUT);
                        }
                        stateChanged(account.getAccount());
                    }
                }

                @Override
                public synchronized void onNewMessage(EIMEmailMessage msg) {
                    String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1]);
                    for (EIMEmailAccount eea : accountManager.getAllAccounts()) {
                        if (eea.getAccount().isAlias(to)) {
                            boolean addedToUI = desktopui.addMailToConversation(eea, msg, false);
                            if (addedToUI) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Added message to UI");
                                }
                                if (playSounds()) {
                                    EIMSound.getInstance().playSound(EIMConstants.SOUND.SOUND_NEW_OPEN);
                                }
                            }
                            break;
                        }
                    }
                }

                @Override
                public synchronized void onPushNotAvailable(String accountName) {
                    displayNotification(
                            EIMI18N.getInstance().getString("Account") + " " + accountName,
                            EIMI18N.getInstance().getString("PushNotSupported_info"),
                            EIMI18N.getInstance().getString("PushNotSupported"),
                            EIMConstants.SOUND.INFORMATION);
                }
            };
            return true;
        } catch (EIMDatabaseException e) {
            logger.error("EIMDatabaseException: " + e.getMessage());
        }
        return false;
    }

    private boolean loadAccounts() {
        ArrayList<EIMEmailAccount> accountData = accountManager.getAllAccounts();
        if (!accountData.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(accountData.size() + " accounts found");
            }
            for (EIMEmailAccount acc : accountData) {
                int key = acc.getAccount().get_id();
                if (logger.isDebugEnabled()) {
                    logger.debug(key + ": " + acc.getAccount().get_emailaddress());
                }
                desktopui.stateChanged(acc.getAccount());
                login(acc.getAccount(), autoLogin());
            }
            return true;
        } else {
            logger.debug("No accounts found");
            return false;
        }
    }

    private void initCrypto() {
        /*
         try {
         crypto = new EIMCrypto(EIMConstants.DB_ENCRYPTION_KEY);
         } catch (NoSuchAlgorithmException e) {
         logger.error("NoSuchAlgorithmException: " + e.getMessage());
         crypto = null;
         } catch (InvalidKeyException e) {
         logger.error("InvalidKeyException: " + e.getMessage());
         crypto = null;
         } catch (InvalidKeySpecException e) {
         logger.error("InvalidKeySpecException: " + e.getMessage());
         crypto = null;
         }
         */
    }

    private void encryptDatabase() {
        /*
         if (crypto != null) {
         logger.debug("Encrypting database");
         boolean encrypted = false;
         try {
         String db = EIMFileUtils.getInstance().readFromFile(EIMConstants.DB_NAME);
         // try {
         byte[] encryptedDB = db.getBytes(EIMConstants.CHARSET); // crypto.encrypt(db);
         try {
         EIMFileUtils.getInstance().writeToBinaryFile(EIMConstants.DB_NAME_ENCRYPTED, encryptedDB);
         if (EIMFileUtils.getInstance().deleteFile(EIMConstants.DB_NAME)) {
         encrypted = true;
         }
         } catch (EIMCouldNotWriteToFileException e) {
         logger.error("EIMCouldNotWriteToFileException: " + e.getMessage());
         }
         */
        /*
         }
         catch (NoSuchAlgorithmException e) {
         logger.error("NoSuchAlgorithmException: " + e.getMessage());
         } catch (InvalidKeyException e) {
         logger.error("InvalidKeyException: " + e.getMessage());
         } catch (NoSuchPaddingException e) {
         logger.error("NoSuchPaddingException: " + e.getMessage());
         } catch (IllegalBlockSizeException e) {
         logger.error("IllegalBlockSizeException: " + e.getMessage());
         } catch (BadPaddingException e) {
         logger.error("BadPaddingException: " + e.getMessage());
         }
         */
        /*
         } catch (FileNotFoundException e) {
         logger.error("FileNotFoundException: " + e.getMessage());
         } catch (IOException e) {
         logger.error("IOException: " + e.getMessage());
         } catch (EIMCouldNotWriteToFileException e) {
         logger.error("EIMCouldNotWriteToFileException: " + e.getMessage());
         }
         if (!encrypted) {
         logger.error("Could not encrypt database");
         } else {
         if(logger.isDebugEnabled()) {
         logger.debug("Successfully encrypted the database");
         }
         }
         }
         */
    }

    private void decryptDatabase() {
        /*
         if (crypto != null) {
         if(logger.isDebugEnabled()) {
         logger.debug("Decrypting database");
         }
         boolean decrypted = false;
         try {
         byte[] ba = EIMFileUtils.getInstance().readFromBinaryFile(EIMConstants.DB_NAME_ENCRYPTED);
         if (EIMFileUtils.getInstance().deleteFile(EIMConstants.DB_NAME)) {
         // try {
         String decryptedDB = new String(ba, EIMConstants.CHARSET); // crypto.decrypt(ba);
         EIMFileUtils.getInstance().writeToFile(EIMConstants.DB_NAME, decryptedDB);
         decrypted = true;
         */
        /*
         }
         catch (NoSuchAlgorithmException e) {
         logger.error("NoSuchAlgorithmException: " + e.getMessage());
         } catch (InvalidKeyException e) {
         logger.error("InvalidKeyException: " + e.getMessage());
         } catch (NoSuchPaddingException e) {
         logger.error("NoSuchPaddingException: " + e.getMessage());
         } catch (IllegalBlockSizeException e) {
         logger.error("IllegalBlockSizeException: " + e.getMessage());
         } catch (BadPaddingException e) {
         logger.error("BadPaddingException: " + e.getMessage());
         }
         */
        /*
         }
         } catch (FileNotFoundException e) {
         logger.error("FileNotFoundException: " + e.getMessage());
         } catch (IOException e) {
         logger.error("IOException: " + e.getMessage());
         } catch (EIMCouldNotWriteToFileException e) {
         logger.error("EIMCouldNotWriteToFileException: " + e.getMessage());
         }
         if (!decrypted) {
         EIMFileUtils.getInstance().deleteFile(EIMConstants.DB_NAME_ENCRYPTED);
         EIMFileUtils.getInstance().deleteFile(EIMConstants.DB_NAME);
         logger.error("Could not decrypt database");
         } else {
         if(logger.isDebugEnabled()) {
         logger.debug("Successfully decrypted the database");
         }
         }
         }
         */
    }

    private void initDatabase() {
        try {
            EIMDatabase.getInstance();
        } catch (EIMDatabaseException e) {
            logger.error("EIMDatabaseException: " + e.getMessage());
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline_info"),
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    EIMConstants.SOUND.ERROR,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }

    private void deleteOldMails() {
        if (logger.isDebugEnabled()) {
            logger.debug("Trying to delete old mails ");
        }
        try {
            if (EIMDatabase.getInstance().deleteOldMails()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Successfully deleted old mails");
                }
            } else {
                logger.error("Could not delete old mails");
            }
        } catch (EIMDatabaseException e) {
            logger.error("EIMDatabaseException: " + e.getMessage());
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline_info"),
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    EIMConstants.SOUND.ERROR,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }

    private void initPreferences() {
        try {
            this.preferences_list = EIMDatabase.getInstance().loadPreferences();
            EIMNotification.getInstance().setDisplay(displayNotifications());
        } catch (EIMDatabaseException e) {
            logger.error("EIMDatabaseException: " + e.getMessage());
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline_info"),
                    EIMI18N.getInstance().getString("ErrorDatabaseNotOnline"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    EIMConstants.SOUND.ERROR,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }

    private void initUI() {
        try {
            desktopui = new EIMDesktopUI(this);
        } catch (EIMTrayIconNotSupportedException e) {
            logger.error("EIMTrayIconNotSupportedException: " + e.getMessage());
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorTrayNotSupported_info"),
                    EIMI18N.getInstance().getString("ErrorTrayNotSupported"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    playSounds() ? EIMConstants.SOUND.ERROR : null,
                    EIMNotificationUI.TYPE.ERROR);
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorFileNotFound_info"),
                    EIMI18N.getInstance().getString("ErrorFileNotFound"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    playSounds() ? EIMConstants.SOUND.ERROR : null,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }

    private void initAccounts() {
        if (initManager()) {
            initWizardUI();
            initLoginUI();
            if (!loadAccounts()) {
                wizardui.setVisible(true);
            } else if (!autoLogin()) {
                loginui.setData(accountManager.getFirstAccount().getAccount());
                loginui.setServerSectionVisible(false);
                loginui.setVisible(true);
            }
        } else {
            EIMNotification.getInstance().showNotificationAndQuit(
                    this,
                    EIMI18N.getInstance().getString("ErrorAccountManagerNotOnline_info"),
                    EIMI18N.getInstance().getString("ErrorAccountManagerNotOnline"),
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    playSounds() ? EIMConstants.SOUND.ERROR : null,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }
}
