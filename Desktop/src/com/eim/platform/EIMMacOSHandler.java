/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.platform;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;
import com.eim.eim.EIMController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMMacOSHandler
 *
 * @author Denis Meyer
 */
@SuppressWarnings("deprecation")
public class EIMMacOSHandler extends Application {

    private static final Logger logger = LogManager.getLogger(EIMMacOSHandler.class.getName());

    public EIMMacOSHandler(final EIMController controller) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMMacOSHandler");
        }
        this.setEnabledPreferencesMenu(true);

        addApplicationListener(new ApplicationListener() {
            @Override
            public void handleAbout(ApplicationEvent event) {
                event.setHandled(true);
                controller.getDesktopUI().toggleAboutWindow();
            }

            @Override
            public void handleOpenApplication(ApplicationEvent event) {
                // Do nothing
            }

            @Override
            public void handleOpenFile(ApplicationEvent event) {
                // Do nothing
            }

            @Override
            public void handlePreferences(ApplicationEvent event) {
                event.setHandled(true);
                controller.getDesktopUI().togglePreferencesWindow();
            }

            @Override
            public void handlePrintFile(ApplicationEvent event) {
                // Do nothing
            }

            @Override
            public void handleQuit(ApplicationEvent event) {
                if (controller.quit(true)) {
                    event.setHandled(true);
                    System.exit(0);
                }
            }

            @Override
            public void handleReOpenApplication(ApplicationEvent event) {
                // Do nothing
            }
        });
    }
}
