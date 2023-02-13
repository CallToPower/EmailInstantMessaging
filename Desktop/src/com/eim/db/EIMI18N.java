/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.db;

import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMI18N
 *
 * @author Denis Meyer
 */
public class EIMI18N {

    private static final Logger logger = LogManager.getLogger(EIMI18N.class.getName());
    private static EIMI18N instance = null;

    protected EIMI18N() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMI18N");
        }
    }

    public static EIMI18N getInstance() {
        if (instance == null) {
            instance = new EIMI18N();
        }
        return instance;
    }

    public String getString(String str) {
        try {
            return ResourceBundle.getBundle("com.eim.resources.language").getString(str);
        } catch (Exception e) {
            logger.warn("Did not find i18n for string '" + str + "'");
        }
        return str;
    }
}
