/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.mail;

import com.eim.util.EIMUtility;
import java.util.ArrayList;
import java.util.Date;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * EIMEmailEnvelope
 *
 * @author Denis Meyer
 */
public class EIMEmailEnvelope {

    private static final Logger logger = LogManager.getLogger(EIMEmailEnvelope.class.getName());
    private Message message = null;
    private final ArrayList<String> from;
    private final ArrayList<String> replyTo;
    private final ArrayList<String> to;
    private String subject = "";
    private Date date_sent = null;
    private String ID = "";

    public EIMEmailEnvelope(String _ID, String _from, String _replyTo, String _to, String _subject, Date _date_sent) {
        this(_from, _replyTo, _to, _subject, _date_sent);
        ID = _ID;
    }

    public EIMEmailEnvelope(String _from, String _replyTo, String _to, String _subject, Date _date_sent) {

        from = new ArrayList<>();
        replyTo = new ArrayList<>();
        to = new ArrayList<>();

        from.add(_from);
        replyTo.add(_replyTo);
        to.add(_to);
        subject = _subject;
        date_sent = _date_sent;
        generateID();
    }

    public EIMEmailEnvelope(Message message) {
        this.message = message;

        from = new ArrayList<>();
        replyTo = new ArrayList<>();
        to = new ArrayList<>();
    }

    public void parse() throws MessagingException {
        Address[] address;

        from.clear();
        try {
            if ((address = message.getFrom()) != null) {
                for (Address a : address) {
                    String addr = a.toString();
                    if (!addr.trim().isEmpty() && EIMUtility.getInstance().containsHost(addr)) {
                        from.add(a.toString());
                    }
                }
            }
        } catch (MessagingException e) {
            logger.error("Error parsing FROM: MessagingException: " + e.getMessage());
            throw e;
        }

        replyTo.clear();
        try {
            if ((address = message.getReplyTo()) != null) {
                for (Address a : address) {
                    String addr = a.toString();
                    if (!addr.trim().isEmpty() && EIMUtility.getInstance().containsHost(addr)) {
                        replyTo.add(a.toString());
                    }
                }
            }
        } catch (MessagingException e) {
            logger.error("Error parsing REPLY TO: MessagingException: " + e.getMessage());
            throw e;
        }

        to.clear();
        try {
            if ((address = message.getRecipients(Message.RecipientType.TO)) != null) {
                for (Address a : address) {
                    String addr = a.toString();
                    if (!addr.trim().isEmpty() && EIMUtility.getInstance().containsHost(addr)) {
                        to.add(a.toString());
                        /*
                         // TODO: Save group
                         InternetAddress ia = (InternetAddress) a1;
                         if (ia.isGroup()) {
                         InternetAddress[] aa = ia.getGroup(false);
                         for (InternetAddress aa1 : aa) {
                         if(logger.isDebugEnabled()) {
                         logger.debug("GROUP: " + aa1.toString());
                         }
                         }
                         }
                         */
                    }
                }
            }
        } catch (MessagingException e) {
            logger.error("Error parsing TO: MessagingException: " + e.getMessage());
            throw e;
        }

        try {
            subject = message.getSubject();
            subject = (subject == null) ? "" : subject;
        } catch (MessagingException e) {
            logger.error("Error parsing subject: MessagingException: " + e.getMessage());
            throw e;
        }

        try {
            date_sent = message.getSentDate();
            date_sent = (date_sent == null) ? new Date() : date_sent;
        } catch (MessagingException e) {
            logger.error("Error parsing date sent: MessagingException: " + e.getMessage());
            throw e;
        }

        try {
            String[] header = message.getHeader("Message-ID");
            ID = ((header != null) && (header.length > 0)) ? message.getHeader("Message-ID")[0] : null;
        } catch (MessagingException e) {
            logger.error("Error parsing message ID: MessagingException: " + e.getMessage());
            generateID();
        }
        if ((ID == null) || (ID.trim().isEmpty())) {
            generateID();
        }

        // FLAGS
         /*
         Flags flags = m.getFlags();
         StringBuffer sb = new StringBuffer();
         Flags.Flag[] sf = flags.getSystemFlags(); // get the system flags

         boolean first = true;
         for (int i = 0; i < sf.length; i++) {
         String s;
         Flags.Flag f = sf[i];
         if (f == Flags.Flag.ANSWERED) {
         s = "\\Answered";
         } else if (f == Flags.Flag.DELETED) {
         s = "\\Deleted";
         } else if (f == Flags.Flag.DRAFT) {
         s = "\\Draft";
         } else if (f == Flags.Flag.FLAGGED) {
         s = "\\Flagged";
         } else if (f == Flags.Flag.RECENT) {
         s = "\\Recent";
         } else if (f == Flags.Flag.SEEN) {
         s = "\\Seen";
         } else {
         continue; // skip it
         }
         if (first) {
         first = false;
         } else {
         sb.append(' ');
         }
         sb.append(s);
         }

         String[] uf = flags.getUserFlags(); // get the user flag strings
         for (int i = 0; i < uf.length; i++) {
         if (first) {
         first = false;
         } else {
         sb.append(' ');
         }
         sb.append(uf[i]);
         }
         if(logger.isDebugEnabled()) {
         logger.debug("FLAGS: " + sb.toString());
         }

         // X-MAILER
         String[] hdrs = m.getHeader("X-Mailer");
         if (hdrs != null) {
         if(logger.isDebugEnabled()) {
         logger.debug("X-Mailer: " + hdrs[0]);
         }
         } else {
         if(logger.isDebugEnabled()) {
         logger.debug("X-Mailer NOT available");
         }
         }
         */
    }

    public ArrayList<String> getFrom() {
        return from;
    }

    public ArrayList<String> getReplyTo() {
        return replyTo;
    }

    public ArrayList<String> getTo() {
        return to;
    }

    public String getFirstFrom() {
        if (from.size() > 0) {
            return from.get(0);
        }
        return "";
    }

    public String getFirstTo() {
        if (to.size() > 0) {
            return to.get(0);
        }
        return "";
    }

    public String getFirstReplyTo() {
        if (replyTo.size() > 0) {
            return replyTo.get(0);
        }
        return "";
    }

    public String getSubject() {
        return subject;
    }

    public Date getDateSent() {
        return date_sent;
    }

    public void setDateSent(Date date) {
        date_sent = date;
    }

    public String getID() {
        if ((ID == null) || ID.isEmpty()) {
            generateID();
        }
        return ID;
    }

    private void generateID() {
        ID = to + "-" + date_sent.getTime();
    }
}
