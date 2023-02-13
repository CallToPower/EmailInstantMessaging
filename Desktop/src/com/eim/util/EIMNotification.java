/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.util;

import com.eim.eim.EIMController;
import com.eim.sound.EIMSound;
import com.eim.ui.EIMNotificationUI;
import com.eim.ui.EIMNotificationUI.TYPE;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMNotification
 *
 * @author Denis Meyer
 */
public class EIMNotification {

    private static final Logger logger = LogManager.getLogger(EIMNotification.class.getName());
    private static EIMNotification instance = null;
    private static int uiCount = 0;
    private static boolean display = true;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    protected EIMNotification() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMNotification");
        }
    }

    public static EIMNotification getInstance() {
        if (instance == null) {
            instance = new EIMNotification();
        }
        return instance;
    }

    public void setDisplay(boolean display) {
        EIMNotification.display = display;
    }

    public void showNotification(
            final String header,
            final String mid,
            final String msg,
            final EIMConstants.SOUND soundToPlay,
            final TYPE type) {
        if (display) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (soundToPlay != null) {
                        EIMSound.getInstance().playSound(soundToPlay);
                    }
                    ++uiCount;
                    EIMNotificationUI ui = new EIMNotificationUI(header, mid, msg, uiCount, type) {

                        @Override
                        public void onClick() {
                            this.setVisible(false);
                        }

                        @Override
                        public void onWindowClose() {
                            --uiCount;
                        }
                    };
                    ui.setVisible(true);
                }
            });
            executor.execute(t);
        }
    }

    public void showNotificationAndQuit(
            final EIMController controller,
            final String header,
            final String mid,
            final String msg,
            final EIMConstants.SOUND soundToPlay,
            final TYPE type) {
        if (display) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (soundToPlay != null) {
                        EIMSound.getInstance().playSound(soundToPlay);
                    }
                    EIMNotificationUI ui = new EIMNotificationUI(header, mid, msg, uiCount, type) {

                        @Override
                        public void onClick() {
                            if (controller != null) {
                                controller.quit(false);
                            } else {
                                System.exit(0);
                            }
                            this.setVisible(false);
                        }

                        @Override
                        public void onWindowClose() {
                            if (controller != null) {
                                controller.quit(false);
                            } else {
                                System.exit(0);
                            }
                        }
                    };
                    ui.setVisible(true);
                }
            });
            executor.execute(t);
        }
    }

    public void showLoggedInNotification(
            final EIMController controller,
            final String accountName,
            final String accountNameStr,
            final String msg,
            final String msg2,
            final EIMConstants.SOUND soundToPlay,
            final TYPE type) {
        if (display) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (soundToPlay != null) {
                        EIMSound.getInstance().playSound(soundToPlay);
                    }
                    ++uiCount;
                    EIMNotificationUI ui = new EIMNotificationUI(accountNameStr, msg, msg2, uiCount, type) {

                        @Override
                        public void onClick() {
                            if (controller != null) {
                                controller.displayContactListOrLoginUI(accountName);
                            }
                            this.setVisible(false);
                        }

                        @Override
                        public void onWindowClose() {
                            --uiCount;
                        }
                    };
                    ui.setVisible(true);
                }
            });
            executor.execute(t);
        }
    }

    public void showMessageNotification(
            final EIMController controller,
            final String header,
            final String from,
            final String to,
            final String msg,
            final EIMConstants.SOUND soundToPlay,
            final TYPE type) {
        if (display) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (soundToPlay != null) {
                        EIMSound.getInstance().playSound(soundToPlay);
                    }
                    ++uiCount;
                    EIMNotificationUI ui = new EIMNotificationUI(header, from, msg, uiCount, type) {

                        @Override
                        public void onClick() {
                            if (controller != null) {
                                controller.displayMessage(to, from);
                            }
                            this.setVisible(false);
                        }

                        @Override
                        public void onWindowClose() {
                            --uiCount;
                        }
                    };
                    ui.setVisible(true);
                }
            });
            executor.execute(t);
        }
    }
}
