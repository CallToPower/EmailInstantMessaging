/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import javax.mail.Message;
import javax.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailMessage
 *
 * @author Denis Meyer
 */
public final class EIMEmailMessage {

    private static final Logger logger = LogManager.getLogger(EIMEmailMessage.class.getName());
    private long uid = 0l;
    private EIMEmailEnvelope envelope = null;
    private EIMEmailContent content = null;

    public EIMEmailMessage(long uid, EIMEmailEnvelope envelope, EIMEmailContent content) {
        this.envelope = envelope;
        this.content = content;
        this.uid = uid;
    }

    public EIMEmailMessage(Message message, long uid) {
        this.uid = uid;
        try {
            envelope = new EIMEmailEnvelope(message);
            envelope.parse();
            content = new EIMEmailContent(message);
            content.parse();
        } catch (MessagingException e) {
            logger.error("MessagingException: " + e.getMessage());
            envelope = null;
            content = null;
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage());
            envelope = null;
            content = null;
        }
    }

    public void setUID(long uid) {
        this.uid = uid;
    }

    public long getUID() {
        return uid;
    }

    public EIMEmailEnvelope getEnvelope() {
        return envelope;
    }

    public EIMEmailContent getContent() {
        return content;
    }
}
