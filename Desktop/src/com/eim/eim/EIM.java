/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.eim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.eim.util.EIMConstants;
import com.eim.db.EIMI18N;
import com.eim.ui.EIMNotificationUI;
import com.eim.util.EIMNotification;
import java.util.Locale;

/**
 * EIM
 *
 * @author Denis Meyer
 */
public class EIM {

    private static final Logger logger = LogManager.getLogger(EIM.class.getName());

    public static void main(String[] args) {
        logger.info("EIM version " + EIMConstants.VERSION);
        logger.info("Java information:");
        logger.info("\t" + "Version " + System.getProperty("java.version"));
        logger.info("\t" + "Vendor: " + System.getProperty("java.vendor"));
        logger.info("\t" + "Vendor URL: " + System.getProperty("java.vendor.url"));
        logger.info("\t" + "Class path: " + System.getProperty("java.class.path"));
        logger.info("\t" + "Home: " + System.getProperty("java.home"));
        logger.info("Operating system information:");
        logger.info("\t" + "Name: " + System.getProperty("os.name"));
        logger.info("\t" + "Arch: " + System.getProperty("os.arch"));
        logger.info("\t" + "Version: " + System.getProperty("os.version"));
        // logger.info("\t" + "File separator: " + System.getProperty("file.separator"));
        // logger.info("\t" + "Line separator: " + System.getProperty("line.separator"));
        // logger.info("\t" + "Path separator: " + System.getProperty("path.separator"));
        logger.info("User information:");
        logger.info("\t" + "Name: " + System.getProperty("user.name"));
        logger.info("\t" + "Language: " + System.getProperty("user.language") + " (" + Locale.getDefault() + ")");
        logger.info("\t" + "Directory: " + System.getProperty("user.dir"));
        logger.info("\t" + "Home: " + System.getProperty("user.home"));

        boolean ok = false;
        try {
            EIMController controller = new EIMController();
            ok = true;
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        if (!ok) {
            logger.error("Something went wrong initializing th controller, quitting...");
            EIMNotification.getInstance().showNotificationAndQuit(
                    null,
                    EIMI18N.getInstance().getString("QuittingTheApplication"),
                    EIMI18N.getInstance().getString("SomethingWentWrong"),
                    "",
                    null,
                    EIMNotificationUI.TYPE.ERROR);
        }
    }
}
