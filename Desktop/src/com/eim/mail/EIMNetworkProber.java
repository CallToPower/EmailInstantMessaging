/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.util.EIMConstants;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMNetworkProber
 *
 * @author Denis Meyer
 */
public abstract class EIMNetworkProber {

    private static final Logger logger = LogManager.getLogger(EIMNetworkProber.class.getName());
    private final EIMEmailAccount mail;
    private final String host;
    private int port = 993;
    private String name = "EIMNetworkProber";
    private int pingFailureCount = 0;
    private int sessionFailureCount = 0;
    private long lastBeat = -1;
    private boolean netConnectivity = false;
    private Timer timer;

    public EIMNetworkProber(EIMEmailAccount mail) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMNetworkProber");
        }
        this.mail = mail;
        this.host = mail.getAccount().get_imap();
        this.port = Integer.parseInt(mail.getAccount().get_imapPort());
        this.name = name + "-" + mail.getAccount().get_emailaddress();
    }

    public abstract void onNetworkChange(boolean status);

    public abstract void missedBeat();

    public synchronized void start() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                periodicProber();
            }
        };
        sessionFailureCount = 0;
        pingFailureCount = 0;
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (timer == null) {
            return;
        }

        timer.cancel();
        timer.purge();
        timer = null;
    }

    public int getPingFailureCount() {
        return pingFailureCount;
    }

    public int getSessionFailureCount() {
        return sessionFailureCount;
    }

    public boolean getNetConnectivity() {
        return netConnectivity;
    }

    private boolean probe() {
        boolean status = true;

        Socket socket = null;
        try {
            socket = new Socket(host, port);
            status = true;
            pingFailureCount = 0;
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
            status = false;
            ++pingFailureCount;
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            status = false;
            ++pingFailureCount;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("Exception: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
            }
        }
        netConnectivity = status;
        return status;
    }

    private boolean probeWithSessionCheck() {
        boolean status = probe();
        if (status) {
            if (mail.serverIsConnected()) {
                sessionFailureCount = 0;
                return true;
            } else {
                ++sessionFailureCount;
                return false;
            }
        }
        return false;
    }

    private void periodicProber() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (lastBeat != -1) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBeat > (EIMConstants.PROBER_SLEEP_TIME + 1)) { // missed beat
                        lastBeat = currentTime;
                        missedBeat();
                        return;
                    }
                    lastBeat = currentTime;
                } else {
                    lastBeat = System.currentTimeMillis();
                }

                if (mail == null) {
                    onNetworkChange(probe());
                } else {
                    onNetworkChange(probeWithSessionCheck());
                }
            }
        };
        stop();
        timer = new Timer(name, true);
        timer.scheduleAtFixedRate(task, Calendar.getInstance().getTime(), EIMConstants.PROBER_SLEEP_TIME);
    }
}
