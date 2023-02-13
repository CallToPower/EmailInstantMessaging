package com.dgrodt.phonegapMail;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MailContact {
	private final static String TAG = "MailContact";
	private long id = -1;
	private String contact_id;
	private String preferred_email;
	private String last_email;
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return the contact_id
	 */
	public String getContact_id() {
		return contact_id;
	}
	/**
	 * @param contact_id the contact_id to set
	 */
	public void setContact_id(String contact_id) {
		this.contact_id = contact_id;
	}
	/**
	 * @return the preferred_email
	 */
	public String getPreferred_email() {
		return preferred_email;
	}
	/**
	 * @param preferred_email the preferred_email to set
	 */
	public void setPreferred_email(String preferred_email) {
		this.preferred_email = preferred_email;
	}
	/**
	 * @return the last_email
	 */
	public String getLast_email() {
		return last_email;
	}
	/**
	 * @param last_email the last_email to set
	 */
	public void setLast_email(String last_email) {
		this.last_email = last_email;
	}
	public JSONObject getJSON() {
		JSONObject result = new JSONObject();
		try {
			result.put("id", getId());
			result.put("contact_id", getContact_id());
			result.put("preferred_email", getPreferred_email());
			result.put("last_email", getLast_email());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public void setJSON(JSONObject json) {
		try {
			setId(json.getLong("id"));
		} catch (JSONException e) {}
		try {
			setContact_id(json.getString("contact_id"));
		} catch (JSONException e) {}
		try {
			setPreferred_email(json.getString("preferred_email"));
		} catch (JSONException e) {}
		try {
			setLast_email(json.getString("last_email"));
		} catch (JSONException e) {}
	}
	public void load(Cursor cursor) {
		setId(cursor.getLong(cursor.getColumnIndex("id")));
		setContact_id(cursor.getString(cursor.getColumnIndex("contact_id")));
		setPreferred_email(cursor.getString(cursor.getColumnIndex("preferred_email")));
		setLast_email(cursor.getString(cursor.getColumnIndex("last_email")));
		Log.v(TAG, "loaded contact: "+getContact_id()+" "+getPreferred_email()+" "+getLast_email());
	}
	public void load(SQLiteDatabase db, String contactID) {
		// 1. build the query
		String query = "SELECT  * FROM " + "contacts" + " where contact_id='" + contactID +"' LIMIT 1";

		// 2. get reference to writable DB
		Cursor cursor = db.rawQuery(query, null);

		// 3. go over each row, build book and add it to list
		if (cursor.moveToFirst()) {
			load(cursor);
		}
	}
	
	public void save(SQLiteOpenHelper helper) {
		SQLiteDatabase db = helper.getWritableDatabase();

		ContentValues values = new ContentValues();
		
		values.put("contact_id", getContact_id());
		values.put("preferred_email", getPreferred_email());
		values.put("last_email", getLast_email());
		Log.v(TAG, "Saving contact "+values.toString());

		if (getId() == -1) {
			setId(db.insert("contacts", null, values));
		} else {
			db.update("contacts", values, "id = ?", new String[] { String.valueOf(getId()) });
		}
		db.close();
	}
}
