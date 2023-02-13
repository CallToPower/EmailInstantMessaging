/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.util.EIMConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailContent
 *
 * @author Denis Meyer
 */
public class EIMEmailContent {

    private static final Logger logger = LogManager.getLogger(EIMEmailEnvelope.class.getName());
    private Message message = null;
    private final boolean saveAttachments = false; // TODO!
    private int attnum = 0;
    private String content = "";

    public EIMEmailContent(String content) {
        this.content = content;
    }

    public EIMEmailContent(Message message) {
        this.message = message;
    }

    public void parse() throws Exception {
        parsePart(message);
    }

    public String getContent() {
        return content;
    }

    private String checkString(String s) {
        if (s.contains(EIMConstants.CUSTOM_TEXT_SENDING_EMAIL_SEPARATOR)) {
            return s.substring(0, content.indexOf(EIMConstants.CUSTOM_TEXT_SENDING_EMAIL_SEPARATOR));
        } else {
            return s;
        }
    }

    private void parsePart(Part p) throws Exception {
        String ct = p.getContentType();
        try {
            new ContentType(ct);
        } catch (ParseException e) {
            logger.error("BAD CONTENT-TYPE: " + ct + ": " + e.getMessage());
        }
        String filename = p.getFileName();
        /*
         if (filename != null) {
         if(logger.isDebugEnabled()) {
         logger.debug("FILENAME: " + filename);
         }
         }
         */

        boolean isTextPlain = ct.equals("TEXT/PLAIN");

        /*
         * Using isMimeType to determine the content type avoids
         * fetching the actual content data until we need it
         */
        if (p.isMimeType("text/plain")) {
            content = (String) p.getContent();
            content = checkString(content);
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                parsePart(mp.getBodyPart(i));
            }
        } else if (p.isMimeType("message/rfc822")) {
            parsePart((Part) p.getContent());
        } else {
            /*
             * If we actually want to see the data, and it's not a
             * MIME type we know, fetch it and check its Java type.
             */
            Object o = p.getContent();
            if (o instanceof String) {
                if (isTextPlain) {
                    content = (String) p.getContent();
                    content = checkString(content);
                }
            } else if (o instanceof InputStream) {
                /*
                 InputStream is = (InputStream) o;
                 int c;
                 while ((c = is.read()) != -1) {
                 }
                 */
            } else {
                /*
                 if(logger.isDebugEnabled()) {
                 logger.debug("Unknown type");
                 logger.debug(o.toString());
                 }
                 */
            }
        }

        if (saveAttachments && (p instanceof MimeBodyPart)
                && !p.isMimeType("multipart/*")) {
            String disp = p.getDisposition();
            // many mailers don't include a Content-Disposition
            if (disp == null || disp.equalsIgnoreCase(Part.ATTACHMENT)) {
                if (filename == null) {
                    filename = "Attachment" + (++attnum);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Saving attachment to file " + filename);
                }
                try {
                    File f = new File(filename);
                    if (f.exists()) {
                        throw new IOException("file exists");
                    }
                    ((MimeBodyPart) p).saveFile(f);
                } catch (IOException ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to save attachment: " + ex);
                    }
                }
            }
        }
    }
}
