package com.dgrodt.phonegapMail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Store;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.mail.event.FolderEvent;
import javax.mail.event.FolderListener;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;

import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.ProtocolCommand;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.IMAPStore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class MailService extends Service {
	private final static String TAG = "MailService";
	private boolean started = false;
	private static List<IMAPStore> stores = new LinkedList<IMAPStore>();
	private static List<IMAPIdler> idlers = new LinkedList<IMAPIdler>();
	private static Map<Long, MessageCountListener> listeners = Collections.synchronizedMap(new HashMap<Long, MessageCountListener>());;
	private static boolean run = true;
	private static HashMap<String, String> emails;
	private final static long FORCE_RECONNECT_AFTER = 60 * 10 * 1000;
	private final static long MIN_RUNTIME = 60 * 5 * 1000;
	private static long lastRun;
	private static List<Mail> newCachedMails = new LinkedList<Mail>();

	private static MailService thisInstance;
	private static boolean isStarted = false;
	private static CordovaActivity cordovaActivity;

	@Override
	public void onCreate() {
		// Log.v(TAG, TAG + " created");
		lastRun = 0;
		thisInstance = this;
	}

	public static MailService getInstance() {
		return thisInstance;
	}

	public static boolean isStarted() {
		return isStarted;
	}

	public void setActivity(CordovaActivity c) {
		if (cordovaActivity == null && c != null) {
			cordovaActivity = c;
			notifyActivity(newCachedMails);
			newCachedMails.clear();
		} else {
			cordovaActivity = c;
		}
	}

	@Override
	public void onDestroy() {
		isStarted = false;
		thisInstance = null;
		for (IMAPStore store : stores) {
			try {
				if (store.isConnected()) {
					store.close();
				}
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Log.v(TAG, ">>>>2");
		stores.clear();
		for (IMAPIdler idler : idlers) {
			idler.end();
		}
		// Log.v(TAG, ">>>>3");
		idlers.clear();
	}

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		// Log.v(TAG, TAG + " started");
		final long time = System.currentTimeMillis();
		if (time - MIN_RUNTIME > lastRun) {

			new Thread(new Runnable() {

				@Override
				public void run() {
					PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
					PowerManager.WakeLock wakeLock = pm.newWakeLock(pm.PARTIAL_WAKE_LOCK, TAG);
					wakeLock.acquire();
					started = true;
					if (intent != null) {
						HashMap<String, String> emails = (HashMap<String, String>) intent.getSerializableExtra("emails");
						if (intent.getBooleanExtra("user_started", false)) {
							newCachedMails.clear();
						}
						if (emails != null) {
							MailService.emails = emails;
							connect(getApplicationContext());
							lastRun = time;
						}
					}
					// Log.v(TAG, TAG + " ended");
					wakeLock.release();
				}

			}).start();
			while (!started) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		isStarted = true;
		return Service.START_REDELIVER_INTENT;
	}

	private synchronized void connect(Context context) {
		MailSQLiteHelper db = new MailSQLiteHelper(context);
		int newMessagesCount = 0;
		List<Mail> newMessages = new LinkedList<Mail>();
		db.clearOldMails();

		// Log.v(TAG, ">>>>2");
		stores.clear();
		for (IMAPIdler idler : idlers) {
			idler.end();
		}
		// Log.v(TAG, ">>>>3");
		idlers.clear();
		for (IMAPStore store : stores) {
			try {
				if (store.isConnected()) {
					store.close();
				}
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Log.v(TAG, ">>>>1");
		for (MailAccount acc : db.getAccounts()) {
			if (acc.getToken_state() != 2) {
				IMAPStore store = createStore(acc);
				if (store != null) {
					// Log.v(TAG, ">>>>4");
					/*
					 * A new connection was created; fetch new mails
					 */
					try {
						long d2 = System.currentTimeMillis();
						Stack<IMAPFolder> folders = new Stack<IMAPFolder>();
						Folder[] f = ((IMAPFolder) store.getDefaultFolder()).list();
						for (Folder fd : f) {
							folders.push((IMAPFolder) fd);
						}
						HashMap<String, Long> lastUIDS = db.getFolders(acc.getId());
						long d = System.currentTimeMillis();
						// Log.v(TAG, "loading folder info took " + (d - d2) +
						// "ms");
						IMAPFolder ifd;
						while (!folders.empty()) {
							ifd = folders.pop();
							if ((ifd.getType() & Folder.HOLDS_MESSAGES) != 0) {
								boolean isSentFolder = false;
								boolean useFolder = true;
								String[] attrib = ifd.getAttributes();
								for (String s : attrib) {
									if (s.equalsIgnoreCase("\\Sent")) {
										isSentFolder = true;
										if (acc.getSentFolder() == null || acc.getSentFolder().length() == 0) {
											acc.setSentFolder(ifd.getFullName());
											acc.save(db);
										}
										break;
									} else if ((s.equalsIgnoreCase("\\All") && !ifd.getFullName().equalsIgnoreCase("INBOX")) || s.equalsIgnoreCase("\\Drafts")
											|| s.equalsIgnoreCase("\\Trash")) {
										useFolder = false;
										break;
									}
								}
								if (useFolder) {
									d = System.currentTimeMillis();
									ifd.open(Folder.READ_WRITE);
									ifd.setSubscribed(true);
									if (store.hasCapability("IDLE")) {
										spawnIdleThread(acc, ifd);
									}
									ifd.addMessageCountListener(getMessageListener(context, acc.getId()));
									Long lastUID = lastUIDS.get(ifd.getFullName());
									if (lastUID == null) {
										lastUID = new Long(0);
									}
									Message[] messages = ifd.getMessagesByUID(Math.max(lastUID + 1, ifd.getUIDNext() - MailUtils.MESSAGE_MAX_OLD_UID),
											ifd.getUIDNext());
									for (Message message : messages) {
										long tmp = ifd.getUID(message);
										if (tmp > lastUID.longValue()) {
											lastUID = tmp;
										}
										Mail mail = handleMessage(message, db, isSentFolder);
										if (mail != null) {
											++newMessagesCount;
											newMessages.add(mail);
										}

									}
									db.setFolder(acc.getId(), ifd.getFullName(), lastUID);
									lastUIDS.put(ifd.getFullName(), lastUID);

								}

							} else if ((ifd.getType() & Folder.HOLDS_FOLDERS) != 0) {
								f = ifd.list();
								for (Folder fd : f) {
									folders.push((IMAPFolder) fd);
								}
							}
						}
						// db.setFolders(acc.getId(), lastUIDS);
					} catch (NoSuchProviderException e) {
						e.printStackTrace();
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
			}
		}
		db.close();
		notifyActivity(newMessages);
		Log.i(TAG, "Received " + newMessagesCount + " new messages.");
	}

	private synchronized IMAPStore createStore(MailAccount acc) {
		IMAPStore store = null;
		try {
			store = (IMAPStore) MailUtils.getMailStore(acc);
			stores.add(store);
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return store;
	}

	private synchronized MessageCountListener getMessageListener(Context context, long accountID) {
		MessageCountListener listener = listeners.get(accountID);
		if (listener == null) {
			listener = new MessageListener(context, accountID);
			listeners.put(accountID, listener);
		}
		return listener;
	}

	private synchronized void spawnIdleThread(MailAccount acc, IMAPFolder folder) {
		IMAPIdler thread = new IMAPIdler(folder);
		thread.start();
		idlers.add(thread);
	}

	private Mail handleMessage(Message message, MailSQLiteHelper helper, boolean isSent) {
		Mail mail;
		mail = MailUtils.validateMessage(message, emails, isSent);
		if (mail != null) {
			mail.save(helper);
			return mail;
		} else {
			Log.e(TAG, "could not save mail");
		}
		return null;
	}

	public class IMAPIdler extends Thread {
		private IMAPFolder folder;
		private long lastAction;
		private final AtomicReference<Thread> currentThread = new AtomicReference<Thread>();

		public IMAPIdler(IMAPFolder folder) {
			this.folder = folder;
		}

		@Override
		public void run() {
			currentThread.set(Thread.currentThread());
			// Log.v(TAG, "starting idler for " + folder.getFullName());
			while (!Thread.currentThread().isInterrupted() && folder.isOpen()) {
				try {
					lastAction = System.currentTimeMillis();
					folder.idle();
					// Log.v(TAG, "looping " + folder.getFullName());
				} catch (MessagingException e) {
					Log.e(TAG, "IDLE Exception: " + e);
					break;
				}
			}
			// Log.v(TAG, "ending idler for " + folder.getFullName());

		}

		@Override
		public void destroy() {
			// Log.v(TAG, "destroying idler for " + folder.getFullName());
		}

		/**
		 * @return the lastAction
		 */
		public long getLastAction() {
			return lastAction;
		}

		public void end() {
			try {
				currentThread.get().interrupt();
			} catch (Exception e) {

			}
		}
	}

	public class MessageListener implements MessageCountListener {

		private Context context;
		private long accountID;

		public MessageListener(Context context, long accountID) {
			this.context = context;
			this.accountID = accountID;
		}

		@Override
		public void messagesAdded(final MessageCountEvent arg0) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					Message[] messages = arg0.getMessages();
					MailSQLiteHelper helper = new MailSQLiteHelper(context);
					List<Mail> newMessages = new LinkedList<Mail>();
					for (Message message : messages) {
						try {
							boolean isSentFolder = false;
							String[] attrib;
							IMAPFolder folder = ((IMAPFolder) message.getFolder());
							attrib = folder.getAttributes();
							for (String s : attrib) {
								if (s.equalsIgnoreCase("\\Sent")) {
									isSentFolder = true;
									break;
								}
							}
							Mail mail = handleMessage(message, helper, isSentFolder);
							if (mail != null) {
								newMessages.add(mail);
							}
							long oldUID = helper.getFolder(accountID, folder.getFullName());
							long newUID = folder.getUID(message);
							if (newUID > oldUID) {
								helper.setFolder(accountID, folder.getFullName(), newUID);
							}

						} catch (MessagingException e) {
							Log.e(TAG, "could not retrieve mail: " + e.getMessage());
						}
					}
					notifyActivity(newMessages);
				}
			}).start();

		}

		@Override
		public void messagesRemoved(MessageCountEvent arg0) {
		}

	}

	private void notifyActivity(List<Mail> mails) {
		CordovaActivity tmp = cordovaActivity;
		if (tmp != null && mails != null && mails.size() > 0) {
			JSONArray arr = new JSONArray();
			for (Mail mail : mails) {
				arr.put(mail.getJSON());
			}
			tmp.sendJavascript("mail.onNewMessages(" + arr.toString() + ")");
		} else if (tmp == null && mails != null && mails.size() > 0) {
			newCachedMails.addAll(mails);
			showNotification();
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private void showNotification() {
		Intent intent = new Intent("com.dgrodt.phonegapMail.MAIN");
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

		// build notification
		// the addAction re-use the same intent to keep the example short
		Notification n = new Notification.Builder(this).setContentTitle("New Messages").setContentText("You have " + newCachedMails.size() + " new Messages.")
				.setSmallIcon(this.getApplicationInfo().icon).setContentIntent(pIntent).setAutoCancel(true).build();

		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		notificationManager.notify(0, n);
	}
}
