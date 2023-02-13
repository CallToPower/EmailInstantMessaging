/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.ui;

import com.eim.eim.EIMController;
import com.eim.exceptions.EIMTrayIconNotSupportedException;
import com.eim.mail.EIMAccount;
import com.eim.mail.EIMEmailAccount;
import com.eim.mail.EIMEmailMessage;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eim.util.EIMConstants;
import com.eim.db.EIMI18N;
import com.eim.img.EIMImage;
import com.eim.types.EIMPair;
import com.eim.util.EIMUtility;
import java.awt.AWTException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

/**
 * EIMDesktopUI
 *
 * @author Denis Meyer
 */
public class EIMDesktopUI extends JFrame {

    private static final Logger logger = LogManager.getLogger(EIMDesktopUI.class.getName());
    private JPopupMenu mainMenu = null;
    private final HashMap<String, JMenuItem> accountMap;
    private final HashMap<String, EIMPair<String, Integer>> missedMessagesMap;
    private JMenuItem About;
    private JMenuItem Preferences;
    private JMenuItem LoginUI;
    private JMenuItem NewMessage;
    private JMenuItem Quit;
    private EIMAboutUI aboutui = null;
    private EIMPreferencesUI preferencesui = null;
    private TrayIcon trayIcon = null;
    private EIMController controller = null;
    private ArrayList<EIMContactListUI> contactListUIs = null;
    private boolean mainMenuVisible = false;
    private ImageIcon iconImage_tray;
    private ImageIcon iconImage_tray_inverted;
    private boolean mainMenuFirstStart = true;
    private Timer mainMenuTimer = null;

    public EIMDesktopUI(final EIMController controller) throws EIMTrayIconNotSupportedException, FileNotFoundException {
        this.controller = controller;
        contactListUIs = new ArrayList<>();

        this.setTitle(EIMI18N.getInstance().getString("EIM"));
        UIManager.put("OptionPane.yesButtonText", EIMI18N.getInstance().getString("Yes"));
        UIManager.put("OptionPane.noButtonText", EIMI18N.getInstance().getString("No"));
        UIManager.put("OptionPane.cancelButtonText", EIMI18N.getInstance().getString("Cancel"));

        accountMap = new HashMap<>();
        missedMessagesMap = new HashMap<>();

        iconImage_tray = EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_TRAY_ICON));
        try {
            iconImage_tray_inverted = EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(EIMConstants.IMAGE.IMG_TRAY_ICON_INVERTED));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
            iconImage_tray_inverted = iconImage_tray;
        }

        aboutui = new EIMAboutUI(this);
        aboutui.setIconImage(iconImage_tray.getImage());
        initPreferencesUI();

        if (SystemTray.isSupported()) {
            initMenu();
        } else {
            logger.error("Could not initialize tray icon. Not supported for this operating system.");
            throw new EIMTrayIconNotSupportedException("Tray icon not supported");
        }
    }

    public boolean clickedYes(String title, String message) {
        return (JOptionPane.showConfirmDialog(
                this,
                message, title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                iconImage_tray) == JOptionPane.YES_OPTION);
    }

    public boolean isInMenuBar(String emailAddr) {
        for (String s : EIMUtility.getInstance().getAliases(emailAddr)) {
            if (accountMap.containsKey(s)) {
                return true;
            }
        }
        return false;
    }

    public void stateChanged(final EIMAccount acc) {
        if (!containsAccount(acc.get_emailaddress())) {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding new account to the menu and updating it");
            }
            JMenuItem Status = new JMenuItem(acc.get_emailaddress());
            Status.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        mainMenuClosed();
                        controller.logoutOrDeleteAccount(acc.get_emailaddress());
                    } else {
                        mainMenuClosed();
                        controller.displayContactListOrLoginUI(acc.get_emailaddress());
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    stopMainMenuTimer();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    refreshMainMenuTimer();
                }
            });
            accountMap.put(acc.get_emailaddress(), Status);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Updating existing account " + acc.get_emailaddress() + " in the menu, status '" + acc.getStatus() + "'");
        }
        JMenuItem Status = accountMap.get(acc.get_emailaddress());
        if (Status != null) {
            String strBefore = "";
            String strAfter = "";
            switch (acc.getStatus()) {
                case LOGGED_IN:
                    setIcon(Status, EIMConstants.IMAGE.IMG_NETWORK_STATUS_GREEN);
                    break;
                case LOGGED_OUT:
                    setIcon(Status, EIMConstants.IMAGE.IMG_NETWORK_STATUS_YELLOW);
                    removeNewMessages(acc.get_emailaddress());
                    break;
                case LOGGING_IN:
                    strBefore = EIMI18N.getInstance().getString("LoggingInTo") + " ";
                    setIcon(Status, EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                    break;
                case LOGGING_OUT:
                    strBefore = EIMI18N.getInstance().getString("LoggingOutOf") + " ";
                    setIcon(Status, EIMConstants.IMAGE.IMG_NETWORK_STATUS_WHITE);
                    break;
                case LOGIN_FAILED:
                    setIcon(Status, EIMConstants.IMAGE.IMG_NETWORK_STATUS_RED);
                    break;
                case DELETED:
                    deleteAccount(acc.get_emailaddress());
                    break;
                default:
                    break;
            }
            Status.setText(strBefore + acc.get_emailaddress() + strAfter);
            if (mainMenu.isVisible()) {
                mainMenu.setVisible(false);
                mainMenu.setVisible(true);
                refreshMainMenuTimer();
            }
            for (EIMContactListUI ui : contactListUIs) {
                if (acc.get_emailaddress().equals(ui.getAccountName())) {
                    ui.stateChanged(acc.getStatus());
                }
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not find account menu item for account " + acc.get_emailaddress());
            }
        }
        buildMenu();
    }

    public void deinit() {
        if (aboutui != null) {
            aboutui.dispose();
        }
        if (preferencesui != null) {
            preferencesui.dispose();
        }
        if (contactListUIs != null) {
            for (EIMContactListUI cUI : contactListUIs) {
                cUI.setVisible(false);
                cUI.deinit();
                cUI.dispose();
                break;
            }
        }
        if (SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    public void toggleAboutWindow() {
        if (aboutui != null) {
            aboutui.setVisible(true);
        }
    }

    public void togglePreferencesWindow() {
        if (preferencesui != null) {
            preferencesui.setVisible(true);
        }
    }

    public synchronized void displayMessage(EIMEmailAccount account, String from) {
        if (account != null) {
            checkAndCreateContactList(account, false);
            ArrayList<String> al = EIMUtility.getInstance().getAliases(account.getAccount().get_emailaddress());
            boolean _break = false;
            for (EIMContactListUI cUI : contactListUIs) {
                for (String s : al) {
                    if (cUI.getAccountName().equals(s)) {
                        cUI.showConversation(from);
                        _break = true;
                        break;
                    }
                }
                if (_break) {
                    break;
                }
            }
        }
    }

    public void mailSent(String from, String to) {
        ArrayList<String> al = EIMUtility.getInstance().getAliases(from);
        boolean _break = false;
        for (EIMContactListUI cUI : contactListUIs) {
            for (String s : al) {
                if (cUI.getAccountName().equals(s)) {
                    if (cUI.isVisible()) {
                        cUI.addRecieverToAutocomplete(to);
                    }
                    _break = true;
                    break;
                }
            }
            if (_break) {
                break;
            }
        }
    }

    public void toggleContactListWindow(EIMEmailAccount account) {
        checkAndCreateContactList(account, true);
    }

    public boolean addMailToConversation(EIMEmailAccount account, EIMEmailMessage msg, boolean addFrom) {
        boolean addedToUI = false;
        if (account != null) {
            checkAndCreateContactList(account, false);
            for (EIMContactListUI ui : contactListUIs) {
                addedToUI = ui.addMailToConversation(msg, addFrom);
                if (addedToUI) {
                    break;
                }
            }
            if (!addedToUI && (!controller.displayOnlyEIMMsgs() || (controller.displayOnlyEIMMsgs() && EIMUtility.getInstance().messageSentFromEIM(msg)))) {
                String from = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstFrom())[1]);
                try {
                    if (missedMessagesMap.get(from) == null) {
                        missedMessagesMap.put(from, new EIMPair<>(account.getAccount().get_emailaddress(), 1));
                    } else {
                        missedMessagesMap.put(from, new EIMPair<>(account.getAccount().get_emailaddress(), missedMessagesMap.get(from).right + 1));
                    }
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
                String to = EIMUtility.getInstance().getMainAlias(EIMUtility.getInstance().parseEmailAddress(msg.getEnvelope().getFirstTo())[1]);
                String shortenedContent = msg.getContent().getContent();
                shortenedContent = (shortenedContent.length() >= EIMConstants.NEW_MESSAGE_MAX_SHORTENED_CONTENT_LENGTH)
                        ? shortenedContent.substring(0, EIMConstants.NEW_MESSAGE_MAX_SHORTENED_CONTENT_LENGTH) + " [...]"
                        : shortenedContent;
                if (!controller.accountInAccountManager(from)) {
                    controller.displayNewMessage(from, to, shortenedContent);
                }
                buildMenu();
            }
        }
        return addedToUI;
    }

    public void messageSeen(String from) {
        missedMessagesMap.remove(from);
        buildMenu();
    }

    private void initPreferencesUI() {
        preferencesui = new EIMPreferencesUI(this) {
            @Override
            public void savePreferences(
                    boolean saveData,
                    boolean autoLogin,
                    boolean playSounds,
                    boolean displayNotifications,
                    boolean onlyEIMMsgs) {
                if (controller.savePreferences(
                        saveData,
                        autoLogin,
                        playSounds,
                        displayNotifications,
                        onlyEIMMsgs)) {
                } else {
                    logger.error("Could not set key/value");
                }
            }

            @Override
            public void loadDataRequest() {
                this.setData(
                        controller.saveData(),
                        controller.autoLogin(),
                        controller.playSounds(),
                        controller.displayNotifications(),
                        controller.displayOnlyEIMMsgs());
            }
        };
        preferencesui.setIconImage(iconImage_tray.getImage());
    }

    private JMenuItem deleteAccount(String accountName) {
        if (containsAccount(accountName)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Deleting account " + accountName + " from the menu and updating it");
            }
            JMenuItem menuItem = accountMap.remove(accountName);
            buildMenu();
            for (EIMContactListUI cUI : contactListUIs) {
                if (cUI.getAccountName().equals(accountName)) {
                    cUI.setVisible(false);
                    cUI.deinit();
                    cUI.dispose();
                    contactListUIs.remove(cUI);
                    break;
                }
            }
            return menuItem;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not find " + accountName + " in the menu");
            }
        }
        return null;
    }

    private void initMenu() throws FileNotFoundException, EIMTrayIconNotSupportedException {
        About = new JMenuItem(EIMI18N.getInstance().getString("About"));
        About.addActionListener(toggleAboutWindowListener);
        About.addMouseListener(refreshMouseListener);
        setIcon(About, EIMConstants.IMAGE.IMG_ABOUT);
        Preferences = new JMenuItem(EIMI18N.getInstance().getString("Preferences"));
        Preferences.addActionListener(togglePreferencesWindowListener);
        Preferences.addMouseListener(refreshMouseListener);
        setIcon(Preferences, EIMConstants.IMAGE.IMG_PREFERENCES);
        LoginUI = new JMenuItem(EIMI18N.getInstance().getString("AddNewAccount"));
        LoginUI.addActionListener(toggleLoginUIWindowListener);
        LoginUI.addMouseListener(refreshMouseListener);
        setIcon(LoginUI, EIMConstants.IMAGE.IMG_NEW_ACCOUNT);
        NewMessage = new JMenuItem(EIMI18N.getInstance().getString("NewMessage"));
        setIcon(NewMessage, EIMConstants.IMAGE.IMG_NEW_MESSAGE);
        NewMessage.setEnabled(false);
        Quit = new JMenuItem(EIMI18N.getInstance().getString("Quit"));
        setIcon(Quit, EIMConstants.IMAGE.IMG_QUIT);
        Quit.addActionListener(quitApplicationListener);
        Quit.addMouseListener(refreshMouseListener);

        mainMenu = new JPopupMenu();
        mainMenu.setInvoker(mainMenu);
        buildMenu();

        try {
            trayIcon = new TrayIcon(iconImage_tray.getImage(), "", null);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(toggleMainMenuMouseListener);
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(trayIcon);
        } catch (AWTException e) {
            logger.error("Exception: " + e.getMessage());
            throw new EIMTrayIconNotSupportedException("Tray icon not supported");
        }
    }

    private void stopMainMenuTimer() {
        if (mainMenuTimer != null) {
            mainMenuTimer.cancel();
            mainMenuTimer.purge();
            mainMenuTimer = null;
        } else {
            mainMenuTimer = null;
        }
    }

    private void refreshMainMenuTimer() {
        if (mainMenuTimer != null) {
            mainMenuTimer.cancel();
            mainMenuTimer.purge();
            mainMenuTimer = new Timer();
        } else {
            mainMenuTimer = new Timer();
        }
        mainMenuTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainMenu.setVisible(false);
                mainMenuClosed();
            }
        }, EIMConstants.AUTO_CLOSE_MENU_TIME);
    }

    private synchronized void checkAndCreateContactList(final EIMEmailAccount account, final boolean setVisible) {
        if (account != null) {
            boolean foundAccount = false;
            ArrayList<String> al = EIMUtility.getInstance().getAliases(account.getAccount().get_emailaddress());
            boolean _break = false;
            for (EIMContactListUI cUI : contactListUIs) {
                for (String s : al) {
                    if (cUI.getAccountName().equals(s)) {
                        if (!cUI.isVisible()) {
                            cUI.setVisible(setVisible);
                        }
                        foundAccount = true;
                        _break = true;
                        break;
                    }
                }
                if (_break) {
                    break;
                }
            }
            if (!foundAccount) {
                EIMContactListUI cUI = new EIMContactListUI(controller, account) {
                    @Override
                    public synchronized void aMailSent(String from, String to) {
                        mailSent(from, to);
                    }

                    @Override
                    public synchronized void aMailNotSent(String info, String msg, String content) {
                        controller.displayError(info, msg, content);
                    }

                    @Override
                    public synchronized void messageSeen(String from) {
                        EIMDesktopUI.this.messageSeen(from);
                    }
                };
                contactListUIs.add(cUI);
                cUI.setVisible(setVisible);
            }
        }
    }

    private void removeNewMessages(String accountName) {
        for (final Map.Entry<String, EIMPair<String, Integer>> entry : missedMessagesMap.entrySet()) {
            if (entry.getValue().left.equals(accountName)) {
                missedMessagesMap.remove(entry.getKey());
            }
        }
        buildMenu();
    }

    private void setIcon(JMenuItem item, EIMConstants.IMAGE imgStatus) {
        try {
            item.setIcon(EIMImage.getInstance().getImageIcon(EIMConstants.getImagePath(imgStatus)));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        }
    }

    private void mainMenuClosed() {
        mainMenu.setVisible(false);
        mainMenuVisible = false;
        trayIcon.setImage(iconImage_tray.getImage());
        stopMainMenuTimer();
    }

    private void toggleLoginUIWindow() {
        if ((controller.getLoginUI() != null) && (controller.getWizardUI() != null)) {
            if (controller.getLoginUI().isVisible()) {
                if (!controller.getLoginUI().changedSomething()
                        || (controller.getLoginUI().changedSomething()
                        && clickedYes(EIMI18N.getInstance().getString("CancelAccountCreation_info"),
                                EIMI18N.getInstance().getString("CancelAccountCreation")))) {
                    controller.getLoginUI().setVisible(false);
                    controller.getLoginUI().setServerSectionVisible(false);
                    controller.getLoginUI().clear();
                    controller.getWizardUI().clear();
                    controller.getWizardUI().setVisible(!controller.getWizardUI().isVisible());
                }
            } else {
                controller.getLoginUI().clear();
                controller.getLoginUI().setVisible(false);
                controller.getWizardUI().clear();
                controller.getWizardUI().setVisible(!controller.getWizardUI().isVisible());
            }
        }
    }
    private final MouseListener toggleMainMenuMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            mainMenuVisible = !mainMenuVisible;
            if (mainMenuVisible) {
                trayIcon.setImage(iconImage_tray_inverted.getImage());
                int x = e.getX();
                int y = e.getY();
                if (!EIMUtility.getInstance().platformIsMac()) {
                    y -= mainMenu.getBounds().height;
                }
                mainMenu.setLocation(x, y);
                refreshMainMenuTimer();
            } else {
                mainMenuClosed();
            }
            if (mainMenuFirstStart) {
                mainMenuFirstStart = false;
                mainMenuVisible = !mainMenuVisible;
                mouseClicked(e);
            } else {
                mainMenu.setVisible(mainMenuVisible);
            }
        }
    };
    private final ActionListener toggleAboutWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            mainMenuClosed();
            toggleAboutWindow();
        }
    };
    private final ActionListener togglePreferencesWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            mainMenuClosed();
            togglePreferencesWindow();
        }
    };
    private final ActionListener toggleLoginUIWindowListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            mainMenuClosed();
            toggleLoginUIWindow();
        }
    };
    private final ActionListener quitApplicationListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            controller.quit(false);
        }
    };
    private final MouseAdapter refreshMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            stopMainMenuTimer();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            refreshMainMenuTimer();
        }
    };

    private void buildMenu() {
        boolean menuWasVisible = mainMenu.isVisible();
        if (menuWasVisible) {
            mainMenu.setVisible(false);
        }
        mainMenu.removeAll();
        for (Map.Entry<String, JMenuItem> entry : accountMap.entrySet()) {
            EIMEmailAccount acc = controller.getAccountManager().getEmailAccount(entry.getKey());
            if (acc != null) {
                mainMenu.add(entry.getValue());
                if ((acc.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_IN)
                        || (acc.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.LOGGING_OUT)
                        || (acc.getAccount().getStatus() == EIMAccount.LOGIN_STATUS.DELETED)) {
                    JProgressBar progressbar = new JProgressBar(0, 100);
                    progressbar.setIndeterminate(true);
                    JMenuItem menuItem = new JMenuItem();
                    menuItem.add(progressbar);
                    menuItem.setEnabled(false);
                    mainMenu.add(menuItem);
                }
            }
        }
        if (!accountMap.isEmpty()) {
            mainMenu.addSeparator();
        }
        if (!missedMessagesMap.isEmpty()) {
            NewMessage.setText((missedMessagesMap.size() > 1) ? EIMI18N.getInstance().getString("NewMessages") : EIMI18N.getInstance().getString("NewMessage"));
            mainMenu.add(NewMessage);
        }
        for (final Map.Entry<String, EIMPair<String, Integer>> entry : missedMessagesMap.entrySet()) {
            JMenuItem menuItem = new JMenuItem(entry.getKey() + " (" + entry.getValue().right + ")");
            setIcon(menuItem, EIMConstants.IMAGE.IMG_EMPTY);
            menuItem.addMouseListener(refreshMouseListener);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    mainMenuClosed();
                    displayMessage(controller.getAccountManager().getEmailAccount(entry.getValue().left), entry.getKey());
                }
            });
            mainMenu.add(menuItem);
        }
        if (!missedMessagesMap.isEmpty()) {
            mainMenu.addSeparator();
        }
        mainMenu.add(LoginUI);
        if (!EIMUtility.getInstance().platformIsMac()) {
            mainMenu.addSeparator();
            mainMenu.add(Preferences);
            mainMenu.add(About);
            mainMenu.addSeparator();
            mainMenu.add(Quit);
        }
        if (menuWasVisible) {
            mainMenu.setVisible(true);
            refreshMainMenuTimer();
        }
    }

    private boolean containsAccount(String emailAddr) {
        for (String entry : accountMap.keySet()) {
            for (String s : EIMUtility.getInstance().getAliases(emailAddr)) {
                if (entry.equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
