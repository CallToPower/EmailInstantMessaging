/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.util;

import com.eim.db.EIMI18N;
import com.eim.mail.EIMEmailMessage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMUtility
 *
 * @author Denis Meyer
 */
public class EIMUtility {

    private static final Logger logger = LogManager.getLogger(EIMUtility.class.getName());
    private static EIMUtility instance = null;

    protected EIMUtility() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMUtility");
        }
    }

    public static EIMUtility getInstance() {
        if (instance == null) {
            instance = new EIMUtility();
        }
        return instance;
    }

    public boolean messageSentFromEIM(EIMEmailMessage msg) {
        return msg.getEnvelope().getSubject().contains(EIMConstants.SENDMAIL_SUBJECT);
    }

    public String getCustomTextSendingEmail() {
        return "\n\n"
                + EIMConstants.CUSTOM_TEXT_SENDING_EMAIL_SEPARATOR
                + "\n"
                + EIMI18N.getInstance().getString("CustomTextSendingEmail")
                + "\n"
                + EIMI18N.getInstance().getString("CustomTextSendingEmailMoreInfo")
                + " https://sites.google.com/site/calltopowersoftware/software/eim";
    }

    public boolean isAlias(String emailAddr, String emailAddrAlias) {
        if (!getUsername(emailAddr).equals(getUsername(emailAddrAlias))) {
            return false;
        }
        String host = EIMUtility.getInstance().getHost(emailAddrAlias);
        for (String s : EIMConstants.getServerAliases(EIMUtility.getInstance().getHost(emailAddr))) {
            if (host.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public String getMainAlias(String emailAddr) {
        String host = EIMUtility.getInstance().getHost(emailAddr);
        ArrayList<String> al = EIMConstants.getServerAliases(host);
        if (al.size() > 0) {
            return getUsername(emailAddr) + al.get(0);
        } else {
            return emailAddr;
        }
    }

    public ArrayList<String> getAliases(String emailAddr) {
        ArrayList<String> al_return = new ArrayList<>();
        al_return.add(emailAddr);
        String host = EIMUtility.getInstance().getHost(emailAddr);
        for (String s : EIMConstants.getServerAliases(host)) {
            String tmp = getUsername(emailAddr) + s;
            if (!al_return.contains(tmp)) {
                al_return.add(tmp);
            }
        }
        return al_return;
    }

    public String[] parseEmailAddress(String addr) {
        addr = addr.trim();

        String[] arr = new String[2];
        if (addr.contains("<") && addr.endsWith(">")) {
            arr[0] = addr.substring(0, addr.indexOf("<"));
            arr[1] = addr.substring(addr.indexOf("<") + 1, addr.indexOf(">"));
            arr[1] = (containsHost(arr[1])) ? arr[1] : addr;
        } else {
            arr[0] = "";
            arr[1] = addr;
        }
        arr[0] = arr[0].trim();
        arr[1] = arr[1].trim();

        /*
         if(logger.isDebugEnabled()) {
         logger.debug("Parsed " + addr + ", got '" + arr[0] + "' and '" + arr[1] + "'");
         }
         */
        return arr;
    }

    public String getCurrPathFile() {
        String resourcePath = "";
        try {
            resourcePath = new File(".").getCanonicalPath();
            resourcePath = resourcePath.endsWith(File.separator) ? resourcePath.substring(0, resourcePath.length() - 1) : resourcePath;
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
        }
        return resourcePath;
    }

    public String getResourcePath() {
        String resourcePath;
        if (platformIsMac()) {
            resourcePath = System.getProperty("user.home") + "/Library/" + EIMConstants.NAME;
        } else {
            resourcePath = getCurrPathFile();
            resourcePath += File.separator + EIMConstants.NAME;
        }
        File resourcePathFile = new File(resourcePath);
        if (!resourcePathFile.exists() && !resourcePathFile.mkdirs()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not create resource path '" + resourcePath + "'");
            }
            resourcePath = getCurrPathFile();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Resource path is '" + resourcePath + "'");
        }
        return resourcePath;
    }

    public String getDatabasePath() {
        String rp = getResourcePath();
        rp = rp.isEmpty() ? rp : (rp + File.separator);
        return EIMConstants.DB_URL_PREFIX + rp + EIMConstants.DB_NAME;
    }

    public String escapeHTML(String html) {
        return html.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    public boolean containsHost(String str) {
        if ((str != null) && str.contains("@")) {
            int ioa = str.lastIndexOf("@") + 1;
            if (ioa < str.length()) {
                return true;
            }
        }
        return false;
    }

    public String getUsername(String str) {
        if ((str != null) && str.contains("@")) {
            int ioa = str.lastIndexOf("@");
            if ((ioa + 1) < str.length()) {
                return str.substring(0, ioa);
            }
        }
        return str;
    }

    public String getHost(String str) {
        if ((str != null) && str.contains("@")) {
            str = str.toLowerCase();
            int ioa = str.lastIndexOf("@");
            if ((ioa + 1) < str.length()) {
                return str.substring(ioa, str.length()).toLowerCase();
            }
        }
        return str;
    }

    public String hostToLowerCase(String str) {
        if ((str != null) && str.contains("@")) {
            int ioa = str.lastIndexOf("@") + 1;
            if (ioa < str.length()) {
                return str.substring(0, ioa) + str.substring(ioa, str.length()).toLowerCase();
            }
        }
        return str;
    }

    public boolean platformIsMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    public boolean fetchFromImapFolder(String name) {
        for (String s : EIMConstants.NOT_ALLOWED_IMAP_FOLDERS) {
            if (name.contains(s)) {
                return false;
            }
        }
        return true;
    }

    public boolean fetchFromImapAttribute(String attr) {
        for (String s : EIMConstants.NOT_ALLOWED_IMAP_ATTRIBUTES) {
            if (attr.equalsIgnoreCase(s)) {
                return false;
            }
        }
        return true;
    }

    public String getUsernameWithoutHost(String username) {
        for (String s : EIMConstants.SERVER_ADDRESS_USERNAME_EXCEPTIONS) {
            if (username.contains(s)) {
                int ioa = username.indexOf("@");
                String u = username.substring(0, ioa);
                if (u.length() > 0) {
                    return u;
                }
            }
        }
        return username;
    }

    public String toUTF8Values(String s) {
        ByteBuffer buffer = EIMConstants.CHARSET.encode(s);
        String t = "";
        while (buffer.hasRemaining()) {
            t += asUnsigned(buffer.get()) + " ";
        }
        return t;
    }

    private int asUnsigned(byte b) {
        return ((int) b) & 0xFF;
    }
}
