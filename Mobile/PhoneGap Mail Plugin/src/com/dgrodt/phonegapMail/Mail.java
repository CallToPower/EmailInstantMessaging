package com.dgrodt.phonegapMail;

import java.io.IOException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Mail {
	private String id;
	private String partner;
	private String partnerEmail;
	private long timestamp_received;
	private String message_id;
	private String content;
	private boolean own;
	private transient boolean textIsHtml = false;
	private Whitelist htmlWhitelist;
	
	private final static String TAG = "Mail";
	
	public Mail() {
		htmlWhitelist = Whitelist.simpleText().addTags("br");
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the from
	 */
	public String getPartner() {
		return partner;
	}

	/**
	 * @param from
	 *            the from to set
	 */
	public void setPartner(String partner) {
		this.partner = partner;
	}

	/**
	 * @return the timestamp_received
	 */
	public long getTimestamp_received() {
		return timestamp_received;
	}

	/**
	 * @param timestamp_received
	 *            the timestamp_received to set
	 */
	public void setTimestamp_received(long timestamp_received) {
		this.timestamp_received = timestamp_received;
	}

	/**
	 * @return the message_id
	 */
	public String getMessage_id() {
		return message_id;
	}

	/**
	 * @param message_id
	 *            the message_id to set
	 */
	public void setMessage_id(String message_id) {
		this.message_id = message_id;
	}

	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * @param content
	 *            the content to set
	 */
	public void setContent(Object content) {
		try {
			if(content instanceof String) {
				this.content = Jsoup.clean((String)content, htmlWhitelist);
			} else {
				this.content = Jsoup.clean(getText((Part)content), htmlWhitelist);
			}
			
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getText(Part p) throws MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}

		return null;
	}
	public JSONObject getJSON() {
		JSONObject result = new JSONObject();
		try {
			result.put("id", getId());
			result.put("partner", getPartner());
			result.put("timestamp_received", getTimestamp_received());
			result.put("message_id", getMessage_id());
			result.put("content", getContent());
			result.put("own", isOwn());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public void load(Cursor cursor) {
		setId(cursor.getString(cursor.getColumnIndex("id")));
		setPartner(cursor.getString(cursor.getColumnIndex("partner")));
		setTimestamp_received(cursor.getLong(cursor.getColumnIndex("timestamp_received")));
		setMessage_id(cursor.getString(cursor.getColumnIndex("message_id")));
		setContent(cursor.getString(cursor.getColumnIndex("content")));
		setOwn(cursor.getInt(cursor.getColumnIndex("own"))>0?true:false);
		setPartnerEmail(cursor.getString(cursor.getColumnIndex("partner_email")));
	}
	
	public void save(SQLiteOpenHelper helper) {
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put("id", getId());
		values.put("partner", getPartner());
		values.put("timestamp_received", getTimestamp_received());
		values.put("message_id", getMessage_id());
		values.put("content", getContent());
		values.put("own", isOwn()?1:0);
		values.put("partner_email", getPartnerEmail());
		

		if (getId() != null) {
			try {
				db.insertWithOnConflict("mails", null, values, SQLiteDatabase.CONFLICT_FAIL);
				MailContact contact = new MailContact();
				contact.load(db, getPartner());
				contact.setContact_id(getPartner());
				contact.setLast_email(getPartnerEmail());
				contact.save(helper);
				db.close();
				Log.v(TAG, "email saved.");
			}catch(Exception e) {
				Log.e(TAG, "mail already exists, not inserting: "+getPartner()+" "+new Date(getTimestamp_received()).toLocaleString());
			}
		}
		
	}

	/**
	 * @return the own
	 */
	public boolean isOwn() {
		return own;
	}

	/**
	 * @param own the own to set
	 */
	public void setOwn(boolean own) {
		this.own = own;
	}

	/**
	 * @return the partnerEmail
	 */
	public String getPartnerEmail() {
		return partnerEmail;
	}

	/**
	 * @param partnerEmail the partnerEmail to set
	 */
	public void setPartnerEmail(String partnerEmail) {
		this.partnerEmail = partnerEmail;
	}
}
