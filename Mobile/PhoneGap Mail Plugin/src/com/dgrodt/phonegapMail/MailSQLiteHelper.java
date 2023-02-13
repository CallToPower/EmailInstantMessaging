package com.dgrodt.phonegapMail;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MailSQLiteHelper extends SQLiteOpenHelper {
	private final static String TAG = "MailSQLHelper";

	// Database Version
	private static final int DATABASE_VERSION = 30;
	// Database Name
	private static final String DATABASE_NAME = "MailDB";

	public MailSQLiteHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// SQL statement to create book table
		String CREATE_ACCOUNTS_TABLE = "CREATE TABLE accounts ( " + "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "username TEXT, " + "password TEXT, "
				+ "imap_port INTEGER, " + "imap_ssl INTEGER, " + "imap_address TEXT, " + "imap_auth INTEGER, " + "smtp_port INTEGER, " + "smtp_ssl INTEGER, "
				+ "smtp_address TEXT, " + "smtp_auth INTEGER, "+ "last_update INTEGER, " + "imap_starttls INTEGER, " + "smtp_starttls INTEGER, " + "sent_folder TEXT, "+"token_state INTEGER)";

		// create books table
		db.execSQL(CREATE_ACCOUNTS_TABLE);
		
		String CREATE_FOLDERS_TABLE = "CREATE TABLE folders ( " + "id INTEGER PRIMARY KEY AUTOINCREMENT, " + "account_id INTEGER, "+"name TEXT, " + "last_uid INTEGER)";

		db.execSQL(CREATE_FOLDERS_TABLE);
		
		String CREATE_MAIL_TABLE = "CREATE TABLE mails ( " + "id TEXT PRIMARY KEY, " + "partner TEXT, "+"partner_email TEXT, "+"timestamp_received INTEGER, " + "message_id TEXT, " + "content TEXT, " + "'own' INTEGER)";
		
		db.execSQL(CREATE_MAIL_TABLE);
		
		String CREATE_CONTACT_TABLE = "CREATE TABLE contacts ( " + "id INTEGER PRIMARY KEY AUTOINCREMENT, "+ "contact_id TEXT, " + "preferred_email TEXT, " + "last_email INTEGER)";
		
		db.execSQL(CREATE_CONTACT_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older books table if existed
		db.execSQL("DROP TABLE IF EXISTS accounts");
		
		db.execSQL("DROP TABLE IF EXISTS folders");
		
		db.execSQL("DROP TABLE IF EXISTS mails");
		
		db.execSQL("DROP TABLE IF EXISTS contacts");

		// create fresh books table
		this.onCreate(db);
	}

	public void addAccount(MailAccount acc) {
		acc.save(this);
	}
	
	public MailAccount getAccount(String username) {
		String query = "SELECT  * FROM " + "accounts WHERE username='"+username+"' LIMIT 1";

		// 2. get reference to writable DB
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		// 3. go over each row, build book and add it to list
		MailAccount account = null;
		if (cursor.moveToFirst()) {
				account = new MailAccount();
				account.load(cursor);
		}
		db.close();
		return account;
	}

	public List<MailAccount> getAccounts() {
		List<MailAccount> accounts = new LinkedList<MailAccount>();

		// 1. build the query
		String query = "SELECT  * FROM " + "accounts";

		// 2. get reference to writable DB
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		// 3. go over each row, build book and add it to list
		MailAccount account = null;
		if (cursor.moveToFirst()) {
			do {
				account = new MailAccount();
				account.load(cursor);

				// Add book to books
				accounts.add(account);
			} while (cursor.moveToNext());
		}
		db.close();

		// return books
		return accounts;
	}
	
	public List<Mail> getMails() {
		List<Mail> mails = new LinkedList<Mail>();

		// 1. build the query
		String query = "SELECT  * FROM " + "mails" + " ORDER BY timestamp_received asc";

		// 2. get reference to writable DB
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		// 3. go over each row, build book and add it to list
		Mail mail = null;
		Log.v(TAG, "messages: "+cursor.getCount());
		if (cursor.moveToFirst()) {
			do {
				mail = new Mail();
				mail.load(cursor);
				// Add book to books
				mails.add(mail);
			} while (cursor.moveToNext());
		}
		db.close();

		// return books
		return mails;
	}
	
	public void clearOldMails() {
		SQLiteDatabase db = this.getWritableDatabase();

		// 2. delete
		int deleted = db.delete("mails", "timestamp_received" + " <= ?", new String[] { String.valueOf((int)(System.currentTimeMillis()/1000 - MailUtils.MESSAGE_MAX_AGE_SECONDS)) });
		Log.i(TAG, "deleted "+deleted+" old emails from cache");
		// 3. close
		db.close();
	}

	public void deleteAccount(long id) {
		if (id != -1) {
			// 1. get reference to writable DB
			SQLiteDatabase db = this.getWritableDatabase();

			// 2. delete
			db.delete("accounts", "id" + " = ?", new String[] { String.valueOf(id) });

			// 3. close
			db.close();
		}
	}
	
	public HashMap<String, Long> getFolders(long accountID) {
		HashMap<String, Long> folders = new HashMap<String, Long>();
		// 1. build the query
		String query = "SELECT  * FROM " + "folders"+" where account_id='"+accountID+"'";

		// 2. get reference to writable DB
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);

		if (cursor.moveToFirst()) {
			do {
				folders.put(cursor.getString(2), cursor.getLong(3));
			} while (cursor.moveToNext());
		}
		db.close();
		for(String s: folders.keySet()) {
			Log.v(TAG, "receiving folder UID: folder="+s+" uid="+folders.get(s));
		}
		return folders;
	}
	public long getFolder(long accountID, String folder) {

		String query = "SELECT  * FROM " + "folders"+" where account_id='"+accountID+"' AND name='"+folder+"' LIMIT 1";

		// 2. get reference to writable DB
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(query, null);
		
		long result = 0;
		
		if (cursor.moveToFirst()) {
			result = cursor.getLong(3);
		}
		db.close();
		return result;
	}
	public void setFolders(long accountID, HashMap<String, Long> folders) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("folders", "account_id" + " = ?", new String[] { String.valueOf(accountID) });
		for(String folder: folders.keySet()) {
			ContentValues values = new ContentValues();
			values.put("account_id", accountID);
			values.put("name", folder);
			values.put("last_uid", folders.get(folder));
			Log.v(TAG, "updating folder UID: "+values.toString());
			db.insert("folders", null, values);
		}
		db.close();
	}
	public void setFolder(long accountID, String folder, long uid) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete("folders", "account_id" + " = ? AND name = ?", new String[] { String.valueOf(accountID), folder });
		ContentValues values = new ContentValues();
		values.put("account_id", accountID);
		values.put("name", folder);
		values.put("last_uid", uid);
		Log.v(TAG, "updating folder UID: "+values.toString());
		db.insert("folders", null, values);
		db.close();
	}

}