/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.util;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.KeyStroke;

/**
 * EIMConstants
 *
 * @author Denis Meyer
 */
public class EIMConstants {

    public static enum IMAGE {

        IMG_LOGO_BIG,
        IMG_LOGO,
        IMG_LOGO_WO_BG,
        IMG_ICON,
        IMG_TRAY_ICON,
        IMG_TRAY_ICON_INVERTED,
        IMG_NETWORK_STATUS_GREEN,
        IMG_NETWORK_STATUS_RED,
        IMG_NETWORK_STATUS_WHITE,
        IMG_NETWORK_STATUS_YELLOW,
        IMG_NETWORK_STATUS_GREEN_HOVER,
        IMG_NETWORK_STATUS_RED_HOVER,
        IMG_NETWORK_STATUS_YELLOW_HOVER,
        IMG_NETWORK_STATUS_WHITE_HOVER,
        IMG_WIZARD_AOL,
        IMG_WIZARD_GMAIL,
        IMG_WIZARD_NEOMAILBOX,
        IMG_WIZARD_OUTLOOK,
        IMG_WIZARD_UOS,
        IMG_NEW_ACCOUNT,
        IMG_PREFERENCES,
        IMG_ABOUT,
        IMG_NEW_MESSAGE,
        IMG_EMPTY,
        IMG_CLOSE,
        IMG_QUIT
    };

    public static enum SOUND {

        SOUND_NEW_OPEN,
        SOUND_NEW_NOT_OPEN,
        SOUND_SENT,
        ERROR,
        LOGGED_IN,
        LOGGED_OUT,
        INFORMATION
    };
    // data
    public final static String NAME = "EIM";
    public final static String BUILD = "1";
    public final static String VERSION = "0.13.2 beta" + " (build " + BUILD + ")";
    // settings
    public final static boolean ASK_TO_QUIT = false;
    public final static boolean SHOW_SPLASHSCREEN = true;
    public final static boolean SPLASHSCREEN_SHOW_PERCENTAGE = true;
    public final static boolean ADD_CUSTOM_TEXT_TO_EMAIL = true;
    public final static int DEFAULT_DAYS_TO_FETCH = 4;
    public final static int DEFAULT_DAYS_TO_KEEP = 12;
    public final static int DEFAULT_NUMBER_OF_MAILS_TO_FETCH = 20;
    public final static int POLL_SLEEP_TIME = 5000; // ms
    public final static int PROBER_SLEEP_TIME = 10000; // ms
    public final static int NEW_MESSAGE_DISPLAY_TIME = 6000; // ms
    public final static int AUTO_CLOSE_MENU_TIME = 800; // ms
    public final static int SPLASHSCREEN_QUIT = 500; // ms
    public final static int NEW_MESSAGE_MAX_SHORTENED_CONTENT_LENGTH = 48;
    public final static int MAC_OS_X_MENUBAR_HEIGHT = 22;
    public final static int MAX_MESSAGE_SEND_TIME = 20; // s
    public final static int MAX_DISCONNECTING_TIME = 8; // s
    public final static int MAX_FETCHMAIL_TIME = 300; // s
    public final static int MAX_PING_FAILURE_COUNT = 2;
    public final static int MAX_SESSION_FAILURE_COUNT = 2;
    // key strokes
    public final static KeyStroke KEYSTROKE_SEND_MESSAGE_WINDOWS = KeyStroke.getKeyStroke(
            KeyEvent.VK_ENTER,
            InputEvent.CTRL_DOWN_MASK,
            true);
    public final static KeyStroke KEYSTROKE_SEND_MESSAGE_MAC = KeyStroke.getKeyStroke(
            KeyEvent.VK_ENTER,
            InputEvent.CTRL_DOWN_MASK,
            true);
    public final static KeyStroke KEYSTROKE_CLOSE_WINDOW_WINDOWS = KeyStroke.getKeyStroke(
            KeyEvent.VK_W,
            InputEvent.CTRL_DOWN_MASK,
            true);
    public final static KeyStroke KEYSTROKE_CLOSE_WINDOW_MAC = KeyStroke.getKeyStroke(
            KeyEvent.VK_W,
            InputEvent.CTRL_DOWN_MASK, // InputEvent.META_DOWN_MASK does _not_ work!
            true);
    // server
    private final static ArrayList<ArrayList<String>> SERVER_ALIASES;
    public static String[] SERVER_ADDRESS_USERNAME_EXCEPTIONS = {
        "@aol.com", "@uni-osnabrueck.de", "@uos.de", "@neomailbox.ch", "@neomailbox.net", "@neomailbox.com"
    };
    public static String[] BAD_SERVER_DB = {
        "@outlook.com", "@hotmail.com", "@hotmail.de"
    };
    public static String[] NOT_SUPPORTED_SERVER_DB = {
        "@yahoo.com"
    };
    public static String[] NOT_ALLOWED_IMAP_FOLDERS = {
        "All Mail", "Drafts", "Important",
        "Sent Mail", "Sent", "SavedIMs",
        "Notebook", "Saved", "Starred",
        "Trash", "[Gmail]", "Deleted",
        "Archive", "Notes"
    };
    public static String[] NOT_ALLOWED_IMAP_ATTRIBUTES = {
        "\\All", "\\Drafts", "\\Trash"
    };
    // misc
    public final static Charset CHARSET = StandardCharsets.UTF_8;
    public final static String LINESEPARATOR = System.getProperty("line.separator");
    public static final String SERVER_DATABASE_SEPARATOR = ";;";
    public static final String SENDMAIL_SUBJECT = "[EIM]";
    public static final String CUSTOM_TEXT_SENDING_EMAIL_SEPARATOR = "_EIM-Email_Instant_Messaging_";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static final DateFormat DATE_FORMAT_CHAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    // imap + smtp server data
    public final static String[] IMAP_PORTS = {"143", "993"};
    public final static String[] IMAP_SSL = {"No", "STARTTLS", "SSL", "SSL/TLS"};
    public final static String[] IMAP_AUTHENTICATION = {"Password", "Password (encrypted)"};
    public final static String[] SMTP_PORTS = {"587", "25", "465"};
    public final static String[] SMTP_SSL = {"No", "STARTTLS", "SSL", "SSL/TLS"};
    public final static String[] SMTP_AUTHENTICATION = {"No", "Password", "Password (encrypted)"};
    // database
    // public final static byte[] DB_ENCRYPTION_KEY = "eim_db_encryption_key_12".getBytes(CHARSET);
    // public final static String DB_NAME_ENCRYPTED = "_eim.db";
    public final static String DB_NAME = "eim.db";
    public final static String DB_DRIVER = "org.sqlite.JDBC";
    public final static String DB_URL_PREFIX = "jdbc:sqlite:";
    public final static String PREFERENCE_SAVEDATA = "saveData";
    public final static String PREFERENCE_AUTOLOGIN = "autoLogin";
    public final static String PREFERENCE_PLAYSOUNDS = "playSounds";
    public final static String PREFERENCE_DISPLAYNOTIFICATIONS = "displayNotifications";
    public final static String PREFERENCE_ONLYEIMMSGS = "onlyEIMMsgs";
    public final static String SQL_PREFERENCES_CREATE = "CREATE TABLE IF NOT EXISTS Preferences "
            + "(ID                      INTEGER PRIMARY KEY, "
            + " SAVEDATA                INTEGER NOT NULL, "
            + " AUTOLOGIN               INTEGER NOT NULL, "
            + " PLAYSOUNDS              INTEGER NOT NULL, "
            + " DISPLAYNOTIFICATIONS    INTEGER NOT NULL, "
            + " ONLYEIMMSGS             INTEGER NOT NULL)";
    public static final String SQL_PREFERENCES_DROP = "DROP TABLE IF EXISTS Preferences;";
    public final static String SQL_PREFERENCES_INSERT = "INSERT INTO Preferences "
            + "(SAVEDATA, AUTOLOGIN, PLAYSOUNDS, DISPLAYNOTIFICATIONS, ONLYEIMMSGS) "
            + " VALUES (?,?,?,?,?);";
    public static final String SQL_PREFERENCES_UPDATE = "UPDATE Preferences SET "
            + "SAVEDATA=?,AUTOLOGIN=?,PLAYSOUNDS=?,DISPLAYNOTIFICATIONS=?,ONLYEIMMSGS=? "
            + "WHERE ID=?;";
    public static final String SQL_PREFERENCES_LOADALL = "SELECT * FROM Preferences WHERE 1;";
    public final static String SQL_ACCOUNT_CREATE = "CREATE TABLE IF NOT EXISTS Account "
            + "(ID                  INTEGER PRIMARY KEY, "
            + " IMAP                TEXT    NOT NULL, "
            + " IMAP_PORT           TEXT    NOT NULL, "
            + " IMAP_SSL            TEXT    NOT NULL, "
            + " IMAP_AUTHENTICATION TEXT    NOT NULL, "
            + " SMTP                TEXT    NOT NULL, "
            + " SMTP_PORT           TEXT    NOT NULL, "
            + " SMTP_SSL            TEXT    NOT NULL, "
            + " SMTP_AUTHENTICATION TEXT    NOT NULL, "
            + " EMAILADDRESS        TEXT    NOT NULL, "
            + " USERNAME            TEXT    NOT NULL, "
            + " PASSWORD            TEXT    NOT NULL, "
            + " UID                 LONG    NULL)";
    public static final String SQL_ACCOUNT_DROP = "DROP TABLE IF EXISTS Account;";
    public final static String SQL_ACCOUNT_INSERT = "INSERT OR IGNORE INTO Account "
            + "(IMAP, IMAP_PORT, IMAP_SSL, IMAP_AUTHENTICATION, SMTP, SMTP_PORT, SMTP_SSL, SMTP_AUTHENTICATION, EMAILADDRESS, USERNAME, PASSWORD, UID) "
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?);";
    public static final String SQL_ACCOUNT_UPDATE = "UPDATE Account SET "
            + "IMAP=?,IMAP_PORT=?,IMAP_SSL=?,IMAP_AUTHENTICATION=?,SMTP=?,SMTP_PORT=?,SMTP_SSL=?,SMTP_AUTHENTICATION=?,EMAILADDRESS=?,USERNAME=?,PASSWORD=?,UID=? "
            + "WHERE ID=?;";
    public static final String SQL_ACCOUNT_DELETE = "DELETE FROM Account WHERE ID=?;";
    public static final String SQL_ACCOUNT_LOADALL = "SELECT * FROM Account WHERE 1;";
    public final static String SQL_MAILS_CREATE = "CREATE TABLE IF NOT EXISTS Mails "
            + "(ID                  TEXT    PRIMARY KEY, "
            + " SENDER              TEXT    NOT NULL, "
            + " REPLY_TO            TEXT    NOT NULL, "
            + " RECEIVER            TEXT    NOT NULL, "
            + " SUBJECT             TEXT    NOT NULL, "
            + " DATE_SENT           LONG    NOT NULL, "
            + " UID                 LONG    NOT NULL, "
            + " CONTENT             TEXT    NOT NULL)";
    public static final String SQL_MAILS_DROP = "DROP TABLE IF EXISTS Mails;";
    public final static String SQL_MAILS_INSERT = "INSERT OR IGNORE INTO Mails "
            + "(ID, SENDER, REPLY_TO, RECEIVER, SUBJECT, DATE_SENT, UID, CONTENT) "
            + " VALUES (?,?,?,?,?,?,?,?);";
    public static final String SQL_MAILS_UPDATE = "UPDATE Mails SET "
            + "SENDER=?,REPLY_TO=?,RECEIVER=?,SUBJECT=?,DATE_SENT=?,UID=?,CONTENT=? "
            + "WHERE ID=?;";
    public static final String SQL_MAILS_DELETE_OLD_MAILS = "DELETE FROM Mails WHERE DATE_SENT<?;";
    public static final String SQL_MAILS_DELETE = "DELETE FROM Mails WHERE ID=?;";
    public static final String SQL_MAILS_LOADALL = "SELECT * FROM Mails WHERE 1 ORDER BY DATE_SENT asc;";
    // images and sounds
    private final static HashMap<IMAGE, String> IMAGES_MAP;
    private final static HashMap<SOUND, String> SOUNDS_MAP;

    static {
        IMAGES_MAP = new HashMap<>();
        IMAGES_MAP.put(IMAGE.IMG_LOGO_BIG, "com/eim/resources/img/logo-big.png");
        IMAGES_MAP.put(IMAGE.IMG_LOGO, "com/eim/resources/img/logo.png");
        IMAGES_MAP.put(IMAGE.IMG_LOGO_WO_BG, "com/eim/resources/img/logo-wo-bg.png");
        IMAGES_MAP.put(IMAGE.IMG_ICON, "com/eim/resources/img/icon.png");
        IMAGES_MAP.put(IMAGE.IMG_TRAY_ICON, "com/eim/resources/img/tray-icon.png");
        IMAGES_MAP.put(IMAGE.IMG_TRAY_ICON_INVERTED, "com/eim/resources/img/tray-icon-inverted.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_GREEN, "com/eim/resources/img/network_status/green.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_RED, "com/eim/resources/img/network_status/red.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_WHITE, "com/eim/resources/img/network_status/white.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_YELLOW, "com/eim/resources/img/network_status/yellow.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_GREEN_HOVER, "com/eim/resources/img/network_status/green_hover.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_RED_HOVER, "com/eim/resources/img/network_status/red_hover.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_YELLOW_HOVER, "com/eim/resources/img/network_status/yellow_hover.png");
        IMAGES_MAP.put(IMAGE.IMG_NETWORK_STATUS_WHITE_HOVER, "com/eim/resources/img/network_status/white_hover.png");
        IMAGES_MAP.put(IMAGE.IMG_WIZARD_AOL, "com/eim/resources/img/wizard/aol.png");
        IMAGES_MAP.put(IMAGE.IMG_WIZARD_GMAIL, "com/eim/resources/img/wizard/gmail.png");
        IMAGES_MAP.put(IMAGE.IMG_WIZARD_NEOMAILBOX, "com/eim/resources/img/wizard/neomailbox.png");
        IMAGES_MAP.put(IMAGE.IMG_WIZARD_OUTLOOK, "com/eim/resources/img/wizard/outlook.png");
        IMAGES_MAP.put(IMAGE.IMG_WIZARD_UOS, "com/eim/resources/img/wizard/uos.png");
        IMAGES_MAP.put(IMAGE.IMG_NEW_ACCOUNT, "com/eim/resources/img/menuitems/new_account.png");
        IMAGES_MAP.put(IMAGE.IMG_PREFERENCES, "com/eim/resources/img/menuitems/preferences.png");
        IMAGES_MAP.put(IMAGE.IMG_ABOUT, "com/eim/resources/img/menuitems/about.png");
        IMAGES_MAP.put(IMAGE.IMG_NEW_MESSAGE, "com/eim/resources/img/menuitems/new_message.png");
        IMAGES_MAP.put(IMAGE.IMG_EMPTY, "com/eim/resources/img/menuitems/empty.png");
        IMAGES_MAP.put(IMAGE.IMG_CLOSE, "com/eim/resources/img/menuitems/close.png");
        IMAGES_MAP.put(IMAGE.IMG_QUIT, "com/eim/resources/img/menuitems/quit.png");
        SOUNDS_MAP = new HashMap<>();
        SOUNDS_MAP.put(SOUND.SOUND_NEW_OPEN, "com/eim/resources/sound/new_open.wav");
        SOUNDS_MAP.put(SOUND.SOUND_NEW_NOT_OPEN, "com/eim/resources/sound/new_not_open.wav");
        SOUNDS_MAP.put(SOUND.SOUND_SENT, "com/eim/resources/sound/sent.wav");
        SOUNDS_MAP.put(SOUND.ERROR, "com/eim/resources/sound/error.wav");
        SOUNDS_MAP.put(SOUND.LOGGED_IN, "com/eim/resources/sound/logged_in.wav");
        SOUNDS_MAP.put(SOUND.LOGGED_OUT, "com/eim/resources/sound/logged_out.wav");
        SOUNDS_MAP.put(SOUND.INFORMATION, "com/eim/resources/sound/information.wav");
        SERVER_ALIASES = new ArrayList<>();
        SERVER_ALIASES.add(new ArrayList<String>() {
            {
                add("@gmail.com");
                add("@googlemail.com");
            }
        });
        SERVER_ALIASES.add(new ArrayList<String>() {
            {
                add("@uni-osnabrueck.de");
                add("@uos.de");
            }
        });
        SERVER_ALIASES.add(new ArrayList<String>() {
            {
                add("@neomailbox.ch");
                add("@neomailbox.net");
                add("@neomailbox.com");
            }
        });
    }

    public static ArrayList<String> getServerAliases(String host) {
        for (ArrayList<String> al : SERVER_ALIASES) {
            for (String s : al) {
                if (s.equalsIgnoreCase(host)) {
                    return al;
                }
            }
        }
        return new ArrayList<>();
    }

    public static String getImagePath(IMAGE img) throws FileNotFoundException {
        if (!IMAGES_MAP.containsKey(img)) {
            throw new FileNotFoundException("File not found: " + img);
        }
        return IMAGES_MAP.get(img);
    }

    public static String getSoundPath(SOUND sound) throws FileNotFoundException {
        if (!SOUNDS_MAP.containsKey(sound)) {
            throw new FileNotFoundException("File not found: " + sound);
        }
        return SOUNDS_MAP.get(sound);
    }
}
