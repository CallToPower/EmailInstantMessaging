/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.img;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMImage
 *
 * @author Denis Meyer
 */
public class EIMImage {

    private static final Logger logger = LogManager.getLogger(EIMImage.class.getName());
    private static EIMImage instance = null;
    private static HashMap<String, ImageIcon> list_imageIcon;

    protected EIMImage() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMImage");
        }
        list_imageIcon = new HashMap<>();
    }

    public static EIMImage getInstance() {
        if (instance == null) {
            instance = new EIMImage();
        }
        return instance;
    }

    public ImageIcon getImageIcon(String path) throws FileNotFoundException {
        if (!list_imageIcon.containsKey(path)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading image '" + path + "'");
            }
            URL url = getClass().getClassLoader().getResource(path);
            if (url != null) {
                list_imageIcon.put(path, new ImageIcon(url));
            } else {
                throw new FileNotFoundException("File not found: '" + path + "'");
            }
        }
        /*
         else {
         if(logger.isDebugEnabled()) {
         logger.debug("Found image '" + path + "'");
         }
         }
         */
        return list_imageIcon.get(path);
    }
}
