/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.util.EIMConstants;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import com.sun.mail.imap.IMAPFolder;
import javax.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailPoller
 *
 * @author Denis Meyer
 */
public abstract class EIMEmailPoller {

    private static final Logger logger = LogManager.getLogger(EIMEmailPoller.class.getName());
    private IMAPFolder folder;
    private String name = "EIMMailPoller";
    protected Timer timer;
    private int previousCount = -1;
    private int diffCount = -1;

    public EIMEmailPoller(IMAPFolder folder) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMMailPoller");
        }
        this.folder = folder;
    }

    public abstract void onNewMessage();

    public synchronized void start(String name) {
        this.name = name + "-" + name;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                periodicPoller();
            }
        };
        Thread t = new Thread(r, this.name);
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public int getDiffCount() {
        return diffCount;
    }

    public void setFolder(IMAPFolder folder) {
        this.folder = folder;
    }

    private boolean poll() {
        try {
            int totalMessages = folder.getMessageCount();
            if (previousCount == -1) {
                diffCount = 0;
                previousCount = totalMessages;
            } else {
                if (previousCount < totalMessages) {
                    diffCount = totalMessages - previousCount;
                    previousCount = totalMessages;
                    return true;
                }
            }
            return false;
        } catch (MessagingException e) {
            logger.error("Exception: Poller Error: " + e.getMessage());
            return false;
        }
    }

    private void periodicPoller() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (poll()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Got new messages");
                    }
                    onNewMessage();
                }
            }
        };
        stop();
        timer = new Timer(name, true);
        try {
            timer.scheduleAtFixedRate(task, Calendar.getInstance().getTime(), EIMConstants.POLL_SLEEP_TIME);
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception: " + e.getMessage());
            }
        }
    }
}
