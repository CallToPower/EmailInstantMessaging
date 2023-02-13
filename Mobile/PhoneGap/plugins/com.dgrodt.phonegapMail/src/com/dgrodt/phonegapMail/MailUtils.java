package com.dgrodt.phonegapMail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.UIDFolder.FetchProfileItem;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.sun.mail.imap.IMAPFolder;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

public class MailUtils extends CordovaPlugin {
	private final static String TAG = "MailUtils";

	private PendingIntent serviceIntent;

	public final static long MESSAGE_MAX_AGE_SECONDS = 60 * 60 * 24 * 4;
	public final static long MESSAGE_MAX_OLD_UID = 20;

	public final static int RESULT_AUTH = 153215;
	private final static String BROADCAST = "com.dgrodt.phonegapMail.android.action.broadcast";
	private BroadcastReceiver broadcast;
	private final Handler handler = new Handler();

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		try {
			if (action.equals("setAccounts")) {
				this.setAccounts(args, callbackContext);
				return true;
			} else if (action.equals("getAccounts")) {
				this.getAccounts(callbackContext);
				return true;
			} else if (action.equals("deleteAccount")) {
				this.deleteAccount(args.getLong(0), callbackContext);
				return true;
			} else if (action.equals("startService")) {
				this.startService(args, callbackContext);
				return true;
			} else if (action.equals("disconnectService")) {
				this.disconnectService();
				return true;
			} else if (action.equals("connectService")) {
				this.connectService();
				return true;
			} else if (action.equals("getMails")) {
				this.getMails(callbackContext);
				return true;
			} else if (action.equals("sendMail")) {
				this.sendMail(args, callbackContext);
				return true;
			} else if (action.equals("getContactData")) {
				this.getContactData(args.getString(0), callbackContext);
				return true;
			} else if (action.equals("setContactData")) {
				this.setContactData(args.getJSONObject(0), callbackContext);
				return true;
			}
		} catch (Exception e) {
			callbackContext.error("Error: " + e.getMessage());
			return false;
		}
		callbackContext.error("Invalid Action");
		return false;
	}

	private void disconnectService() {
		MailService instance = MailService.getInstance();
		if (instance != null) {
			instance.setActivity(null);
		}
	}

	private void connectService() {
		while (!MailService.isStarted()) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
		MailService.getInstance().setActivity((CordovaActivity) this.cordova.getActivity());
	}

	private void getContactData(String id, CallbackContext callbackContext) {
		MailSQLiteHelper helper = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		SQLiteDatabase db = helper.getReadableDatabase();
		MailContact contact = new MailContact();
		contact.load(db, id);
		db.close();
		callbackContext.success(contact.getJSON());
	}

	private void setContactData(JSONObject json, CallbackContext callbackContext) {
		MailSQLiteHelper helper = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		SQLiteDatabase db = helper.getWritableDatabase();
		MailContact contact = new MailContact();
		try {
			contact.load(db, json.getString("contact_id"));
			contact.setJSON(json);
			contact.save(helper);
			db.close();
			callbackContext.success();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendMail(JSONArray args, CallbackContext callbackContext) {
		MailSQLiteHelper db = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		List<MailAccount> accounts = db.getAccounts();
		for (MailAccount acc : accounts) {
			if (acc.canSend()) {
				MailContact contact = new MailContact();
				try {
					// Log.v(TAG, "trying to load contact " +
					// args.getString(0));
					contact.load(db.getReadableDatabase(), args.getString(0));
					String preferredEmail = null;
					if (contact.getPreferred_email() != null && contact.getPreferred_email().length() > 0) {
						preferredEmail = contact.getPreferred_email();
					} else if (contact.getLast_email() != null && contact.getLast_email().length() > 0) {
						preferredEmail = contact.getLast_email();
					}
					if (preferredEmail != null) {
						if (sendMail(acc, preferredEmail, args.getString(1))) {
							callbackContext.success();
							break;
						} else {
							callbackContext.error("Sending failed.");
							break;
						}
					} else {
						callbackContext.error("no_mail");
						break;
					}
				} catch (JSONException e) {
					callbackContext.error("Invalid Arguments.");
					break;
				}

			}

		}
	}

	private void setAccounts(JSONArray args, CallbackContext callbackContext) {
		MailSQLiteHelper db = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		boolean err = false;
		for (int i = 0; i < args.length(); ++i) {
			try {
				JSONObject entry = args.getJSONObject(i);
				MailAccount acc = new MailAccount(entry);
				db.addAccount(acc);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				err = true;
			}
		}
		if (!err) {
			callbackContext.success();
		} else {
			callbackContext.error("an error occured.");
		}
	}

	private void getAccounts(CallbackContext callbackContext) {
		final CordovaInterface cordovaInterface = this.cordova;
		final MailSQLiteHelper db = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
//		AccountManager accMgr = AccountManager.get(this.cordova.getActivity());
//		String authTokenType = "com.google";
//		Account[] tokenAccounts = accMgr.getAccountsByType(authTokenType);
		List<MailAccount> accounts = db.getAccounts();

		// put Token Accounts that are already in the db but expired into
		// expiredAccounts,
		// MailAccounts whose Token counterpart does not exist anymore into
		// delete,
		// and new Token Accounts into newAccounts
//		List<Account> newAccounts = new LinkedList<Account>();
//		List<Account> expiredAccounts = new LinkedList<Account>();
//		List<MailAccount> delete = new LinkedList<MailAccount>();
//		for (MailAccount mailAccount : accounts) {
//			Account found = null;
//			for (Account tokenAccount : tokenAccounts) {
//				if (mailAccount.getUsername().equalsIgnoreCase(tokenAccount.name)) {
//					found = tokenAccount;
//					break;
//				}
//			}
//			if (found != null && mailAccount.getToken_state() == MailAccount.TOKEN_INVALID) {
//				expiredAccounts.add(found);
//			}
//			if (found == null && mailAccount.getToken_state() != MailAccount.TOKEN_NONE) {
//				delete.add(mailAccount);
//				accounts.remove(mailAccount);
//			}
//		}
//		for (Account tokenAccount : tokenAccounts) {
//			if (!expiredAccounts.contains(tokenAccount)) {
//				newAccounts.add(tokenAccount);
//			}
//		}
//
//		// invalidate expired Tokens
//		for (Account account : expiredAccounts) {
//			MailAccount acc = db.getAccount(account.name);
//			if (acc != null) {
//				accMgr.invalidateAuthToken(authTokenType, acc.getPassword());
//				newAccounts.add(account);
//			}
//		}
//		Log.v(TAG, "newAccounts: ");
//		for (Account account : newAccounts) {
//			Log.v(TAG, account.name);
//		}
//		AccountManagerFuture<Bundle> amf;
//		for (Account account : newAccounts) {
//			MailAccount acc = db.getAccount(account.name);
//			if (acc == null) {
//				Log.v(TAG, "creating new account for " + account.name);
//				acc = new MailAccount();
//
//				acc.setImap_address("imap.gmail.com");
//				acc.setImap_auth(MailAccount.AUTH_PASSWORD);
//				acc.setImap_port(993);
//				acc.setImap_ssl(true);
//
//				acc.setSmtp_address("smtp.gmail.com");
//				acc.setSmtp_auth(MailAccount.AUTH_PASSWORD);
//				acc.setSmtp_port(465);
//				acc.setSmtp_ssl(true);
//
//				acc.setUsername(account.name);
//				acc.setToken_state(MailAccount.TOKEN_INVALID);
//				acc.save(db);
//			}
//			accounts.add(0, acc);
//			String token = null;
//			try {
//				token = GoogleAuthUtil.getToken(this.cordova.getActivity(), account.name, "https://www.googleapis.com/auth/userinfo.profile");
//				if (token != null) {
//					acc.setPassword(token);
//					acc.save(db);
//				}
//
//			} catch (GooglePlayServicesAvailabilityException playEx) {
//				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(playEx.getConnectionStatusCode(), this.cordova.getActivity(), 0);
//				// Use the dialog to present to the user.
//			} catch (UserRecoverableAuthException recoverableException) {
//				Intent recoveryIntent = recoverableException.getIntent();
//				recoveryIntent.putExtra("testtest", "testtest");
//				// Use the intent in a custom dialog or just
//				// startActivityForResult.
//				this.cordova.startActivityForResult(this, recoveryIntent, RESULT_AUTH);
//			} catch (GoogleAuthException authEx) {
//				// This is likely unrecoverable.
//				Log.e(TAG, "Unrecoverable authentication exception: " + authEx.getMessage(), authEx);
//			} catch (IOException ioEx) {
//				Log.i(TAG, "transient error encountered: " + ioEx.getMessage());
//			}
			// amf = accMgr.getAuthToken(account, authTokenType, null,
			// this.cordova.getActivity(), new AccountManagerCallback<Bundle>()
			// {
			//
			// @Override
			// public void run(AccountManagerFuture<Bundle> arg0) {
			//
			// try {
			// Bundle result;
			// Intent i;
			// String token;
			// String name;
			// Log.v(TAG, "result:");
			// result = arg0.getResult();
			// for(String s: result.keySet()) {
			// Log.v(TAG, s);
			// }
			// if (result.containsKey(AccountManager.KEY_INTENT)) {
			// i = (Intent) result.get(AccountManager.KEY_INTENT);
			// if (i.toString().contains("GrantCredentialsPermissionActivity"))
			// {
			// // Will have to wait for the user to accept
			// // the request therefore this will have to
			// // run in a foreground application
			// cordovaInterface.getActivity().startActivity(i);
			// } else {
			// cordovaInterface.getActivity().startActivity(i);
			// }
			//
			// } else {
			// token = (String) result.get(AccountManager.KEY_AUTHTOKEN);
			// name = (String) result.get(AccountManager.KEY_ACCOUNT_NAME);
			// MailAccount acc = db.getAccount(name);
			// if (acc != null) {
			// acc.setPassword(token);
			// acc.setToken_state(MailAccount.TOKEN_VALID);
			// acc.save(db);
			// }
			// }
			// } catch (OperationCanceledException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (AuthenticatorException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			//
			// }
			// }, handler);
//		}
		JSONArray result = new JSONArray();
		Log.v(TAG, "accounts: ");
		for (MailAccount acc : accounts) {
			result.put(acc.getJSON());
			Log.v(TAG, acc.getJSON().toString());
		}
		callbackContext.success(result);
	}

	private void deleteAccount(long id, CallbackContext callbackContext) {
		MailSQLiteHelper db = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		db.deleteAccount(id);
		callbackContext.success();
	}

	private void startService(JSONArray args, CallbackContext callbackContext) {
		// Log.v(TAG, "starting service");
		HashMap<String, String> emails = new HashMap<String, String>();
		for (int i = 0; i < args.length(); ++i) {
			try {
				JSONObject obj = args.getJSONObject(i);
				emails.put(obj.getString("email").toLowerCase(), obj.getString("id"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		// if (broadcast == null) {
		// broadcast = new MailBroadcast();
		// }
		// IntentFilter intentFilter = new IntentFilter(BROADCAST);
		// this.cordova.getActivity().getApplicationContext().registerReceiver(broadcast,
		// intentFilter);
		// Intent intent = new Intent(BROADCAST);
		// intent.putExtra("emails", emails);
		// this.cordova.getActivity().getApplicationContext().sendBroadcast(intent);

		Intent intent = new Intent(this.cordova.getActivity().getApplicationContext(), MailService.class);
		intent.putExtra("emails", emails);
		intent.putExtra("user_started", true);
		this.cordova.getActivity().getApplicationContext().startService(intent);

		Intent alarm = new Intent(this.cordova.getActivity(), MailBroadcast.class);
		AlarmManager alarmManager = (AlarmManager) this.cordova.getActivity().getSystemService(Context.ALARM_SERVICE);
		alarm.putExtra("emails", emails);
		alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				AlarmManager.INTERVAL_FIFTEEN_MINUTES, PendingIntent.getBroadcast(this.cordova.getActivity(), 1, alarm, PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void getMails(CallbackContext callbackContext) {
		MailSQLiteHelper db = new MailSQLiteHelper(this.cordova.getActivity().getApplicationContext());
		List<Mail> mails = db.getMails();
		Log.i(TAG, "got " + mails.size() + " emails from cache.");
		JSONArray result = new JSONArray();
		for (Mail mail : mails) {
			result.put(mail.getJSON());
		}
		callbackContext.success(result);
	}

	public static List<Message>[] fetchEmails(Context context, MailAccount acc, Store store) {
		long d = System.currentTimeMillis();

		MailSQLiteHelper db = new MailSQLiteHelper(context);
		LinkedList<Message> cumMessages = new LinkedList<Message>();
		LinkedList<Message> sentMessages = new LinkedList<Message>();
		try {

			long d2 = System.currentTimeMillis();
			// Log.v(TAG, "connecting took " + (d2 - d) + "ms");
			if (store == null) {
				store = getMailStore(acc);
			}
			Folder[] f = ((IMAPFolder) store.getDefaultFolder()).list();
			HashMap<String, Long> lastUIDS = db.getFolders(acc.getId());
			d = System.currentTimeMillis();
			// Log.v(TAG, "loading folder info took " + (d - d2) + "ms");
			for (Folder fd : f) {
				IMAPFolder ifd = (IMAPFolder) fd;
				if ((ifd.getType() & 0x1) == Folder.HOLDS_MESSAGES) {
					if (!ifd.getFullName().equalsIgnoreCase("drafts") && !ifd.getFullName().equalsIgnoreCase("junk")
							&& !ifd.getFullName().equalsIgnoreCase("deleted"))
						d = System.currentTimeMillis();
					ifd.open(Folder.READ_ONLY);
					Long lastUID = lastUIDS.get(ifd.getFullName());
					Message[] messages = ifd
							.getMessagesByUID(Math.max(lastUID != null ? lastUID : 0, ifd.getUIDNext() - MESSAGE_MAX_OLD_UID), ifd.getUIDNext());
					Message lastMessage = null;
					for (Message message : messages) {
						lastMessage = message;
						if (ifd.getFullName().equalsIgnoreCase("sent")) {
							sentMessages.add(message);
						} else {
							cumMessages.add(message);
						}

					}
					if (lastMessage != null) {
						lastUIDS.put(ifd.getFullName(), ifd.getUID(lastMessage));
					}
					d2 = System.currentTimeMillis();
					// Log.v(TAG, "loading messages for folder " +
					// ifd.getFullName() + " took " + (d2 - d) + "ms");
				}
			}
			db.setFolders(acc.getId(), lastUIDS);
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return new List[] { cumMessages, sentMessages };
	}

	public static Mail validateMessage(Message message, HashMap<String, String> emails, boolean fromMe) {
		try {
			// Log.v(TAG, "validate message");
			if (message.getFrom() != null) {
				// Log.v(TAG, "step1");
				try {
					boolean valid = false;
					String from = ((InternetAddress) message.getFrom()[0]).getAddress();
					String partnerID = "";
					String partnerEmail = "";
					if (fromMe) {
						Address[] recipients = message.getRecipients(Message.RecipientType.TO);
						for (Address address : recipients) {
							String to = ((InternetAddress) address).getAddress();
							// Log.v(TAG, "to: " + to);
							if (emails.containsKey(to.toLowerCase())) {
								valid = true;
								partnerID = emails.get(to.toLowerCase());
								partnerEmail = to.toLowerCase();
							}
						}
					} else {
						// Log.v(TAG, "emails: " + emails + " from: " + from);
						valid = emails.containsKey(from.toLowerCase());
						partnerID = emails.get(from.toLowerCase());
						partnerEmail = from.toLowerCase();
					}
					if (valid) {
						long time = System.currentTimeMillis();
						long messageTime = message.getReceivedDate().getTime();
						if (time - (MESSAGE_MAX_AGE_SECONDS * 1000) < messageTime) {
							Log.v(TAG, "valid email: " + partnerEmail);
							Mail mail = new Mail();
							mail.setPartner(partnerID);
							mail.setPartnerEmail(partnerEmail);
							String messageID = message.getHeader("Message-ID") != null ? message.getHeader("Message-ID")[0] : null;
							if (messageID != null && messageID.length() > 0) {
								mail.setId(messageID);
							} else {
								mail.setId(from + message.getReceivedDate().toLocaleString());
							}

							mail.setMessage_id(messageID);
							mail.setTimestamp_received(message.getReceivedDate().getTime() / 1000);
							mail.setContent(message);
							return mail;
						} else {
							Log.i(TAG, "rejecting message: too old.");
						}
					} else {
						Log.i(TAG, "rejecting message: unknown email.");
					}
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				Log.i(TAG, "rejecting message: sender unknown.");
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean sendMail(final MailAccount acc, String recipient, String content) {
		if (acc.canSend()) {
			Properties props = new Properties();
			props.put("mail.smtp.host", acc.getSmtp_address());
			if (acc.getSmtp_auth() != MailAccount.AUTH_NONE) {
				props.put("mail.smtp.auth", "true");
			} else {
				props.put("mail.smtp.auth", "false");
			}
			if (acc.isSmtp_starttls()) {
				props.put("mail.smtp.starttls.enable", "true");
			}
			props.put("mail.smtp.socketFactory.port", acc.getSmtp_port() + "");
			if (acc.isSmtp_ssl()) {
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			}
			props.put("mail.smtp.port", acc.getSmtp_port() + "");

			Session session = Session.getInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(acc.getUsername(), acc.getPassword());
				}
			});

			try {

				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(acc.getUsername()));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
				// message.setSubject("Testing Subject");
				message.setText(content);

				Transport.send(message);
				try {
					Store store = getMailStore(acc);
					Folder folder;
					if (acc.getSentFolder() != null && acc.getSentFolder().length() > 0) {
						folder = store.getFolder(acc.getSentFolder());
					} else {
						folder = store.getFolder("SENT");
					}
					folder.open(Folder.READ_WRITE);
					message.setFlag(Flag.SEEN, true);
					folder.appendMessages(new Message[] { message });
					store.close();
				} catch (MessagingException e) {
					Log.e(TAG, "Moving Email to SENT Folder failed.");
				}
				return true;
			} catch (MessagingException e) {
				Log.e(TAG, "Email sending failed: " + e.getMessage());
			}
		} else {
			Log.e(TAG, "Email sending failed: Account incomplete");
		}
		return false;
	}

	public static Store getMailStore(MailAccount acc) throws MessagingException {
		Store store;
		Properties props = System.getProperties();
		String protocol = acc.isImap_ssl() ? "imap" : "imaps";
		props.setProperty("mail.store.protocol", protocol);

		props.setProperty("mail." + protocol + ".connectiontimeout", "50000");
		props.setProperty("mail." + protocol + ".timeout", "50000");

		props.setProperty("mail." + protocol + ".port", acc.getImap_port() + "");
		if (acc.isImap_ssl()) {
			props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.imap.socketFactory.fallback", "false");
			props.setProperty("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.imaps.socketFactory.fallback", "false");
		}
		if (acc.isImap_starttls()) {
			props.setProperty("mail.imap.starttls.enable", "true");
		}
		Session session = Session.getInstance(props, null);
		session.setDebug(true);
		session.setDebugOut(null);
		store = session.getStore(protocol);
		store.connect(acc.getImap_address(), acc.getUsername(), acc.getPassword());

		return store;
	}

	public static String getSentFolder(Store store) {
		try {
			Stack<IMAPFolder> folders = new Stack<IMAPFolder>();
			Folder[] f = ((IMAPFolder) store.getDefaultFolder()).list();
			for (Folder fd : f) {
				folders.push((IMAPFolder) fd);
			}
			IMAPFolder ifd;
			while (!folders.empty()) {
				ifd = folders.pop();
				String[] attrib;

				attrib = ifd.getAttributes();
				for (String s : attrib) {
					if (s.equalsIgnoreCase("\\Sent")) {
						return ifd.getFullName();
					}
				}

			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "result intent: ");
		for(String s: intent.getExtras().keySet()) {
			Log.v(TAG, s);
		}
	}
}
