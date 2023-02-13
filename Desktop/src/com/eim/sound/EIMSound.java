/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.sound;

import com.eim.util.EIMConstants;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMSound
 *
 * @author Denis Meyer
 */
public class EIMSound {

    private static final Logger logger = LogManager.getLogger(EIMSound.class.getName());
    private static EIMSound instance = null;
    private static HashMap<String, URL> list_sounds;

    protected EIMSound() {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMSound");
        }
        list_sounds = new HashMap<>();
    }

    public static EIMSound getInstance() {
        if (instance == null) {
            instance = new EIMSound();
        }
        return instance;
    }

    public URL loadSound(EIMConstants.SOUND sound) {
        String path = "";
        try {
            path = EIMConstants.getSoundPath(sound);

            if (!list_sounds.containsKey(path)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading sound '" + path + "'");
                }
                URL url = getClass().getClassLoader().getResource(path);
                if (url != null) {
                    list_sounds.put(path, url);
                } else {
                    throw new FileNotFoundException("File not found: '" + path + "'");
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
        }
        return list_sounds.get(path);
    }

    public synchronized void playSound(final EIMConstants.SOUND sound) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                URL soundFile = loadSound(sound);
                if (soundFile != null) {
                    AudioInputStream audioInputStream = null;
                    try {
                        EIMAudioListener listener = new EIMAudioListener();
                        audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                        try {
                            Clip clip = AudioSystem.getClip();
                            clip.addLineListener(listener);
                            clip.open(audioInputStream);
                            try {
                                clip.start();
                                listener.waitUntilDone();
                            } catch (InterruptedException e) {
                                logger.error("InterruptedException: " + e.getMessage());
                            } finally {
                                clip.close();
                            }
                        } catch (LineUnavailableException e) {
                            logger.error("LineUnavailableException: " + e.getMessage());
                        } finally {
                            audioInputStream.close();
                        }
                    } catch (IOException e) {
                        logger.error("IOException: " + e.getMessage());
                    } catch (UnsupportedAudioFileException e) {
                        logger.error("UnsupportedAudioFileException: " + e.getMessage());
                    } finally {
                        if (audioInputStream != null) {
                            try {
                                audioInputStream.close();
                            } catch (IOException e) {
                                logger.error("IOException: " + e.getMessage());
                            }
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sound file is null");
                    }
                }
            }
        });
        t.start();
    }
}
