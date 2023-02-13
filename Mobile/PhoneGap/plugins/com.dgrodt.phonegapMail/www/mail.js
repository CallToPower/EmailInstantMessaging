var mail = {
	setAccounts: function(accounts, callback, error) {
		cordova.exec(callback, error, "MailUtils", "setAccounts", accounts);
	},
	getAccounts: function(callback, error) {
		cordova.exec(callback, error, "MailUtils", "getAccounts", []);
	},
	deleteAccount: function(id, callback, error) {
		cordova.exec(callback, error, "MailUtils", "deleteAccount", [id]);
	},
	startService: function(emails, callback, error) {
		cordova.exec(callback, error, "MailUtils", "startService", emails);
	},
	disconnectService: function(callback, error) {
		cordova.exec(callback, error, "MailUtils", "disconnectService", []);
	},
	connectService: function(callback, error) {
		cordova.exec(callback, error, "MailUtils", "connectService", []);
	},
	getMails: function(callback, error) {
		cordova.exec(callback, error, "MailUtils", "getMails", []);
	},
	sendMail: function(contact_id, content, callback, error) {
		cordova.exec(callback, error, "MailUtils", "sendMail", [contact_id, content]);
	},
	getContactData: function(contact_id, callback) {
		cordova.exec(callback, function(){}, "MailUtils", "getContactData", [contact_id]);
	},
	setContactData: function(contact, callback) {
		cordova.exec(callback, function(){}, "MailUtils", "setContactData", [contact]);
	}
}
module.exports = mail;