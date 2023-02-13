/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import java.util.ArrayList;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailNotifier
 *
 * @author Denis Meyer
 */
public class EIMEmailNotifier {

    private static final Logger logger = LogManager.getLogger(EIMEmailNotifier.class.getName());
    private MessageCountListener messageCountListener;
    private MessageChangedListener messageChangedListener;
    private final EIMEmailAccount mail;

    public EIMEmailNotifier(EIMEmailAccount mail) {
        if (logger.isDebugEnabled()) {
            logger.debug("Initializing EIMEmailNotifier");
            logger.debug("For account: " + mail.getAccount().get_emailaddress());
        }
        this.mail = mail;
        initializeListeners();
        addListeners();
    }

    private void addListeners() {
        mail.setMessageCounterListerer(messageCountListener);
        mail.setMessageChangedListerer(messageChangedListener);
    }

    private void initializeListeners() {
        messageCountListener = new MessageCountListener() {
            @Override
            public void messagesAdded(final MessageCountEvent evt) {
                try {
                    Message[] msgs = evt.getMessages();
                    if (logger.isDebugEnabled()) {
                        logger.debug(msgs.length + " Messages received");
                    }
                    ArrayList<EIMEmailMessage> msgAL = new ArrayList<>();
                    for (Message msg : msgs) {
                        msgAL.add(new EIMEmailMessage(msg, 0l));
                    }
                    mail.gotNewMessages(msgAL);
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
            }

            @Override
            public void messagesRemoved(MessageCountEvent evt) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message Removed: " + evt.getMessages()[0].getSubject());
                    }
                } catch (MessagingException e) {
                    logger.error("MessagingException: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
            }
        };
        messageChangedListener = new MessageChangedListener() {
            @Override
            public void messageChanged(MessageChangedEvent evt) {
                try {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Message Changed: " + evt.getMessage().getSubject());
                    }
                    if (evt.getMessage().isSet(Flag.SEEN)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Flag changed to seen");
                        }
                    }
                } catch (MessagingException e) {
                    logger.error("MessagingException: " + e.getMessage());
                } catch (Exception e) {
                    logger.error("Exception: " + e.getMessage());
                }
            }
        };
    }
}
