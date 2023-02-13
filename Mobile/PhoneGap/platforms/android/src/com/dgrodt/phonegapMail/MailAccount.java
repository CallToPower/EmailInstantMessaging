package com.dgrodt.phonegapMail;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MailAccount {
	public final static int ENCRYPTION_NONE = 0;
	public final static int ENCRYPTION_SSL_TLS = 1;
	public final static int ENCRYPTION_STARTTLS = 2;
	public final static int AUTH_NONE = 0;
	public final static int AUTH_PASSWORD = 1;
	public final static int AUTH_PASSWORD_ENCRYPTED = 2;
	public final static int TOKEN_NONE = 0;
	public final static int TOKEN_VALID = 1;
	public final static int TOKEN_INVALID = 2;
	private static final String TAG = "MailAccount";
	private long id = -1;
	private String username;
	private String password;
	private int imap_port;
	private boolean imap_ssl;
	private boolean imap_starttls;
	private String imap_address;
	private int imap_auth;
	private int smtp_port;
	private boolean smtp_ssl;
	private boolean smtp_starttls;
	private String smtp_address;
	private int smtp_auth;
	private long last_update;
	private long last_uid;
	private String sentFolder;
	private int token_state;
	
	public MailAccount(JSONObject json) {
		setJSON(json);
	}
	public MailAccount(){};
	
	public void setJSON(JSONObject json) {
		try {
			if(json.has("id")) {
				setId(json.getInt("id"));
			}
			setUsername(json.getString("username"));
			setPassword(json.getString("password"));
			setImap_port(json.getInt("imap_port"));
			setImap_ssl(json.getBoolean("imap_ssl"));
			setImap_address(json.getString("imap_address"));
			setImap_auth(json.getInt("imap_auth"));
			setSmtp_port(json.getInt("smtp_port"));
			setSmtp_ssl(json.getBoolean("smtp_ssl"));
			setSmtp_address(json.getString("smtp_address"));
			setSmtp_auth(json.getInt("smtp_auth"));	
			setLast_update(json.getInt("last_update"));
			setSmtp_starttls(json.getBoolean("smtp_starttls"));
			setImap_starttls(json.getBoolean("imap_starttls"));
			setToken_state(json.getInt("token_state"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public JSONObject getJSON() {
		JSONObject result = new JSONObject();
		try {
			result.put("id", getId());
			result.put("username", getUsername());
			result.put("password", getPassword());
			result.put("imap_port", getImap_port());
			result.put("imap_ssl", isImap_ssl());
			result.put("imap_address", getImap_address());
			result.put("imap_auth", getImap_auth());
			result.put("smtp_port", getSmtp_port());
			result.put("smtp_ssl", isSmtp_ssl());
			result.put("smtp_address", getSmtp_address());
			result.put("smtp_auth", getSmtp_auth());
			result.put("last_update", getLast_update());
			result.put("smtp_starttls", isSmtp_starttls());
			result.put("imap_starttls", isImap_starttls());
			result.put("token_state", getToken_state());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param l the id to set
	 */
	public void setId(long l) {
		this.id = l;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the imap_port
	 */
	public int getImap_port() {
		return imap_port;
	}
	/**
	 * @param imap_port the imap_port to set
	 */
	public void setImap_port(int imap_port) {
		this.imap_port = imap_port;
	}
	/**
	 * @return the imap_ssl
	 */
	public boolean isImap_ssl() {
		return imap_ssl;
	}
	/**
	 * @param imap_ssl the imap_ssl to set
	 */
	public void setImap_ssl(boolean imap_ssl) {
		this.imap_ssl = imap_ssl;
	}
	/**
	 * @return the imap_address
	 */
	public String getImap_address() {
		return imap_address;
	}
	/**
	 * @param imap_address the imap_address to set
	 */
	public void setImap_address(String imap_address) {
		this.imap_address = imap_address;
	}
	/**
	 * @return the imap_auth
	 */
	public int getImap_auth() {
		return imap_auth;
	}
	/**
	 * @param imap_auth the imap_auth to set
	 */
	public void setImap_auth(int imap_auth) {
		this.imap_auth = imap_auth;
	}
	/**
	 * @return the smtp_port
	 */
	public int getSmtp_port() {
		return smtp_port;
	}
	/**
	 * @param smtp_port the smtp_port to set
	 */
	public void setSmtp_port(int smtp_port) {
		this.smtp_port = smtp_port;
	}
	/**
	 * @return the smtp_ssl
	 */
	public boolean isSmtp_ssl() {
		return smtp_ssl;
	}
	/**
	 * @param smtp_ssl the smtp_ssl to set
	 */
	public void setSmtp_ssl(boolean smtp_ssl) {
		this.smtp_ssl = smtp_ssl;
	}
	/**
	 * @return the smtp_address
	 */
	public String getSmtp_address() {
		return smtp_address;
	}
	/**
	 * @param smtp_address the smtp_address to set
	 */
	public void setSmtp_address(String smtp_address) {
		this.smtp_address = smtp_address;
	}
	/**
	 * @return the smtp_auth
	 */
	public int getSmtp_auth() {
		return smtp_auth;
	}
	/**
	 * @param smtp_auth the smtp_auth to set
	 */
	public void setSmtp_auth(int smtp_auth) {
		this.smtp_auth = smtp_auth;
	}
	/**
	 * @return the last_update
	 */
	public long getLast_update() {
		return last_update;
	}
	/**
	 * @param last_update the last_update to set
	 */
	public void setLast_update(long last_update) {
		this.last_update = last_update;
	}
	/**
	 * @return the last_uid
	 */
	public long getLast_uid() {
		return last_uid;
	}
	/**
	 * @param last_uid the last_uid to set
	 */
	public void setLast_uid(long last_uid) {
		this.last_uid = last_uid;
	}
	public void save(MailSQLiteHelper helper) {
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put("username", getUsername());
		values.put("password", getPassword());
		values.put("imap_port", getImap_port());
		values.put("imap_ssl", isImap_ssl());
		values.put("imap_starttls", isImap_starttls()?1:0);
		values.put("imap_address", getImap_address());
		values.put("imap_auth", getImap_auth());
		values.put("smtp_port", getSmtp_port());
		values.put("smtp_ssl", isSmtp_ssl());
		values.put("smtp_starttls", isSmtp_starttls()?1:0);
		values.put("smtp_address", getSmtp_address());
		values.put("smtp_auth", getSmtp_auth());
		values.put("last_update", 0);
		values.put("sent_folder", getSentFolder());
		values.put("token_state", getToken_state());
		
		if (getId() == -1) {
			if(helper.getAccount(getUsername()) != null) {
				setId(db.insert("accounts", null, values));
			} else {
				Log.e(TAG, "Not saving account "+getUsername()+": duplicate");
			}
		} else {
			db.update("accounts", values, "id = ?", new String[] { String.valueOf(getId()) });
		}
		Log.v(TAG, "Saving Account: "+getJSON().toString());
		db.close();
	}
	public void load(Cursor cursor) {
		setId(cursor.getInt(0));
		setUsername(cursor.getString(1));
		setPassword(cursor.getString(2));
		setImap_port(cursor.getInt(3));
		setImap_ssl(cursor.getInt(4)==1);
		setImap_address(cursor.getString(5));
		setImap_auth(cursor.getInt(6));
		setSmtp_port(cursor.getInt(7));
		setSmtp_ssl(cursor.getInt(8)==1);
		setSmtp_address(cursor.getString(9));
		setSmtp_auth(cursor.getInt(10));
		setLast_update(cursor.getInt(11));
		setSentFolder(cursor.getString(cursor.getColumnIndex("sent_folder")));
		setImap_starttls(cursor.getInt(cursor.getColumnIndex("imap_starttls"))==1);
		setSmtp_starttls(cursor.getInt(cursor.getColumnIndex("smtp_starttls"))==1);
		setToken_state(cursor.getInt(cursor.getColumnIndex("token_state")));
	}
	
	public boolean canSend() {
		return getSmtp_address() != null && getSmtp_address().length() > 0 &&
			getSmtp_port() > 0 &&
			getImap_address() != null && getImap_address().length() > 0 &&
			getImap_port() > 0;
			
	}
	/**
	 * @return the sentFolder
	 */
	public String getSentFolder() {
		return sentFolder;
	}
	/**
	 * @param sentFolder the sentFolder to set
	 */
	public void setSentFolder(String sentFolder) {
		this.sentFolder = sentFolder;
	}
	/**
	 * @return the imap_starttls
	 */
	public boolean isImap_starttls() {
		return imap_starttls;
	}
	/**
	 * @param imap_starttls the imap_starttls to set
	 */
	public void setImap_starttls(boolean imap_starttls) {
		this.imap_starttls = imap_starttls;
	}
	/**
	 * @return the smtp_starttls
	 */
	public boolean isSmtp_starttls() {
		return smtp_starttls;
	}
	/**
	 * @param smtp_starttls the smtp_starttls to set
	 */
	public void setSmtp_starttls(boolean smtp_starttls) {
		this.smtp_starttls = smtp_starttls;
	}
	/**
	 * @return the token_state
	 */
	public int getToken_state() {
		return token_state;
	}
	/**
	 * @param token_state the token_state to set
	 */
	public void setToken_state(int token_state) {
		this.token_state = token_state;
	}
}
