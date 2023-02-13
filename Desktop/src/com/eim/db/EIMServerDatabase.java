/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.db;

import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMServerDatabase
 *
 * @author Denis Meyer
 */
public class EIMServerDatabase {

    private static final Logger logger = LogManager.getLogger(EIMServerDatabase.class.getName());
    private static EIMServerDatabase instance = null;

    protected EIMServerDatabase() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMServerDatabase");
        }
    }

    public static EIMServerDatabase getInstance() {
        if (instance == null) {
            instance = new EIMServerDatabase();
        }
        return instance;
    }

    public String getData(String server) {
        try {
            return ResourceBundle.getBundle("com.eim.resources.server").getString(server);
        } catch (Exception e) {
            // logger.warn("Did not find server data for string " + server + "'");
        }
        return server;
    }
}
