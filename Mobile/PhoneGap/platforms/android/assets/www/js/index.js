/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
var app = {
	emails : new Array(),
	contacts : new Array(),
	// Application Constructor
	initialize : function() {
		this.bindEvents();
	},
	// Bind Event Listeners
	//
	// Bind any events that are required on startup. Common events are:
	// 'load', 'deviceready', 'offline', and 'online'.
	bindEvents : function() {
		document.addEventListener('deviceready', this.onDeviceReady, false);
	},
	// deviceready Event Handler
	//
	// The scope of 'this' is the event. In order to call the 'receivedEvent'
	// function, we must explicity call 'app.receivedEvent(...);'
	onDeviceReady : function() {
		// load contacts
		var options = new ContactFindOptions();
		options.multiple = true;
		var fields = [ "displayName", "photos", "emails", "id" ];
		navigator.contacts.find(fields, app.onContactSuccess,
				app.onContactError, options);

		// new messages handler
		mail.onNewMessages = app.onNewMessages;

		// accountpage
		$(document).on('tap', '#button_add_account', app.btnAddAccount);
		$(document).on('tap', '#button_save_accounts', app.btnSaveAccounts);
		$(document).on('tap', '.btn_account_delete', app.btnDeleteAccount);

		// contactlist
		$(document).on('tap', '#table_contact_list tr,#table_open_chats tr',
				app.btnOpenChat);

		// chatpage
		$(document).on('tap', '.chat_page [data-role=header]', function(e) {
			var id = $(this).parents('.chat_page').attr("data-id");
			app.openContactPage(id);
		});
		$(document).on('tap', '.chat_page button', app.btnSendMessage);
		$(document).on('tap', '.contact_page .save', app.btnSaveContact);

		app.populateAccountPage();
		mail.getMails(app.onGetEmailsSuccess, function(e) {
			alert(e)
		});
		document.addEventListener("pause", app.onPause, false);
		document.addEventListener("resume", app.onResume, false);

	},
	openContactPage : function(id) {
		if ($('#account_page_' + id).length == 0) {
			mail
					.getContactData(
							id,
							function(data) {
								$content = $(page_contact_template);
								var contact;
								$.each(app.contacts, function(i, v) {
									if (v.id == id) {
										contact = v;
										return false;
									}
								});
								var $fieldset = $content
										.find('#contact_preferred_email fieldset');
								$
										.each(
												contact.emails,
												function(i, v) {
													var checked = "";
													if (v.value == data.preferred_email) {
														checked = 'checked="checked"';
													}
													$fieldset
															.append('<input type="radio" id="preferred_email_'
																	+ i
																	+ '" name="preferred_email" value="'
																	+ v.value
																			.toLowerCase()
																	+ '" '
																	+ checked
																	+ '/><label for="preferred_email_'
																	+ i
																	+ '">'
																	+ v.value
																			.toLowerCase()
																	+ '</label>');
												});
								var checked = "";
								if (data.preferred_email == null) {
									checked = 'checked="checked"';
								}
								$fieldset
										.append('<input type="radio" id="preferred_email_default" name="preferred_email" value="" '
												+ checked
												+ '/><label for="preferred_email_default">Decide Dynamically ('
												+ data.last_email + ')</label>');

								$content.find('[data-role=header] *').html(
										contact.displayName);
								$content.attr("data-id", id);
								$content.attr("id", 'account_page_' + id)
								$content.attr("data-url", 'account_page_' + id);

								// $('[data-role=page]:last').after($content)
								$content.appendTo($.mobile.pageContainer);
								$(
										'#account_page_'
												+ id
												+ ' [data-role=collapsible-set]')
										.collapsibleset().trigger('create');
								$('#account_page_' + id + ' [type=radio]')
										.checkboxradio("refresh");
								$.mobile.pageContainer.pagecontainer("change",
										$('#account_page_' + id), {
											transition : "fade"
										})
							});

		} else {
			$.mobile.pageContainer.pagecontainer("change", $('#account_page_'
					+ id), {
				transition : "fade"
			})
		}
	},
	onContactError : function(contactError) {
		alert(contactError);
	},
	onContactSuccess : function(contacts) {
		app.contacts = contacts;
		var $table = $('#table_contact_list').find('tbody:last');
		var open_chats = localStorage.getItem("open_chats");
		var $table_open_chats = $('#table_open_chats').find('tbody:last');
		var emails = new Array();
		for ( var i = 0; i < contacts.length; i++) {
			if (contacts[i].emails && contacts[i].emails.length > 0
					&& contacts[i].displayName != null) {
				if (contacts[i].photos && contacts[i].photos.length > 0) {
					if (contacts[i].photos[0].type === "url") {
						app.returnValidPhoto(contacts[i].photos[0].value, [
								contacts[i].displayName, contacts[i].id ],
								function(url, v) {
									var code = '<tr data-id="' + v[1]
											+ '"><td><img src="' + url
											+ '"></td><td><h4>' + v[0]
											+ "</h4></td></tr>"
									$table.append(code);
									if (app.isOpenChat(v[1])) {
										$table_open_chats.append(code);
									}
								});

						// photo = '<img
						// src="'+contacts[i].photos[0].value+'"/>';
					} else {
						var code = '<tr data-id="' + contacts[i].id
								+ '"><td><img src="data:image/gif;base64,'
								+ contacts[i].photos[0].value
								+ '"></td><td><h4>' + contacts[i].displayName
								+ "</h4></td></tr>";
						$table.append(code);
						if (app.isOpenChat(contacts[i].id)) {
							$table_open_chats.append(code);
						}
					}

				}
				for ( var j = 0; j < contacts[i].emails.length; j++) {
					var obj = new Object();
					obj["email"] = contacts[i].emails[j].value;
					obj["id"] = contacts[i].id;
					emails.push(obj);
				}
			}

		}
		mail.startService(emails);
	},
	onGetEmailsSuccess : function(emails) {
		app.emails = emails;
	},
	onPageBeforeChange : function(e, data) {
	},
	getMessageHTML : function(email) {
		var date = new Date();
		var received = new Date(email.timestamp_received * 1000);
		var cl = "partner";
		if (email.own) {
			cl = "own";
		}
		timeString = received.toLocaleTimeString();
		if (date.getDate() != received.getDate()
				|| date.getMonth() != received.getMonth()
				|| date.getYear() != received.getYear()) {
			timeString = received.toLocaleDateString()+" "+timeString;
		} else {
//			timeString = received.toLocaleTimeString();
		}
		return '<div class="chat_message ' + cl + '">' + email.content
				+ '<div class="chat_info ' + cl + '">' + timeString
				+ '</div></div>';
	},
	populateAccountPage : function() {
		mail.getAccounts(function(val) {
			// $('#page_accounts').find('[data-role=collapsible]').hide();
			$('#page_accounts').find('[data-role=collapsible-set]').html("");
			$.each(val,
					function(i, v) {
						var $content = $("<div>" + page_account_template
								+ "</div>");
						$('#page_accounts').find('[data-role=collapsible-set]')
								.append(page_account_template);
						var $content = $('[data-role=collapsible]:last');
						$content.find("[name=username]").val(v["username"]);
						$content.find("[name=password]").val(v["password"]);
						$content.find("[name=imap_address]").val(
								v["imap_address"]);
						$content.find("[name=imap_port]").val(v["imap_port"]);
						$content.find("[name=smtp_address]").val(
								v["smtp_address"]);
						$content.find("[name=smtp_port]").val(v["smtp_port"]);
						if (v["imap_ssl"]) {
							$content.find("[name=imap_ssl]").attr("checked",
									"checked");
						}
						if (v["imap_auth"]) {
							$content.find("[name=imap_auth]").attr("checked",
									"checked");
						}
						if (v["smtp_ssl"]) {
							$content.find("[name=smtp_ssl]").attr("checked",
									"checked");
						}
						if (v["smtp_auth"]) {
							$content.find("[name=smtp_auth]").attr("checked",
									"checked");
						}
						if (v["smtp_starttls"]) {
							$content.find("[name=smtp_starttls]").attr(
									"checked", "checked");
						}
						if (v["imap_starttls"]) {
							$content.find("[name=imap_starttls]").attr(
									"checked", "checked");
						}
						$content.find("[name=id]").val(v["id"]);
						$content.attr("data-id", v["id"]);
						$content.find('.coll_header').html(v["username"]);

					});
			$('#page_accounts [data-role=collapsible-set]').collapsibleset()
					.trigger('create');
			$('#page_accounts').find('[type=checkbox]')
					.checkboxradio("refresh");
			// $('#page_accounts').find('[data-role=collapsible]').show();
		}, function(e) {
			alert(e);
		});
	},
	returnValidPhoto : function(url, vars, callback) {
		var img = new Image();
		img.onload = function() {
			// Image is ok
			callback(url, vars);
		};
		img.onerror = function(err) {
			// Returning a default image for users without photo
			callback("icon.png", vars);
		}
		img.src = url;
	},
	onNewMessages : function(emails) {
		$.each(emails, function(i, v) {
			app.emails.push(v);
			if ($('#chat_page_' + v.partner).length != 0) {
				$('#chat_page_' + v.partner).find('[data-role=content]')
						.append(app.getMessageHTML(v));
			}
		});
	},
	btnAddAccount : function(e) {
		$('#page_accounts').find('[data-role=content]').find(
				'[data-role=collapsible-set]').append(page_account_template);
		$('#page_accounts [data-role=collapsible-set]').collapsibleset()
				.trigger('create');
		$('#page_accounts').find('[type=checkbox]').checkboxradio("refresh");
		$('#page_accounts [data-role=collapsible-set]').children().last().attr(
				"data-id", "-1").children().first().click();
	},
	btnSaveAccounts : function(e) {
		var arr = new Array();
		$.each($('#page_accounts [data-role=collapsible-set] .coll_content'),
				function(i, v) {
					arr[i] = new Object();
					arr[i]["username"] = $(v).find('[name=username]').val();
					arr[i]["id"] = parseInt($(v).find('[name=id]').val());
					arr[i]["password"] = $(v).find('[name=password]').val();
					arr[i]["imap_address"] = $(v).find('[name=imap_address]')
							.val();
					arr[i]["imap_port"] = parseInt($(v)
							.find('[name=imap_port]').val());
					arr[i]["imap_ssl"] = $(v).find('[name=imap_ssl]').is(
							':checked');
					arr[i]["imap_starttls"] = $(v).find('[name=imap_ssl]').is(
							':checked');
					arr[i]["imap_auth"] = $(v).find('[name=imap_auth]').is(
							':checked');
					arr[i]["smtp_address"] = $(v).find('[name=smtp_address]')
							.val();
					arr[i]["smtp_port"] = parseInt($(v)
							.find('[name=smtp_port]').val());
					arr[i]["smtp_ssl"] = $(v).find('[name=smtp_ssl]').is(
							':checked');
					arr[i]["smtp_starttls"] = $(v).find('[name=smtp_ssl]').is(
							':checked');
					arr[i]["smtp_auth"] = $(v).find('[name=smtp_auth]').is(
							':checked');
				});
		mail.setAccounts(arr, function() {
			alert("Saved.");
		}, function(e) {
			alert("Saving failed: " + e);
		});
	},
	btnDeleteAccount : function(e) {
		$content = $(this).parents('[data-role=collapsible]');
		if (confirm("Do you really want to delete '"
				+ $content.find('.coll_header').attr("data-name") + "'?")) {
			mail.deleteAccount(parseInt($content.attr("data-id")), function() {
//				app.populateAccountPage();
				$content.remove();
			}, function(e) {
				alert("Delete failed: " + e);
			});
			$content.remove();
		}
	},
	btnOpenChat : function(e) {
		var id = $(this).attr("data-id");
		app.addOpenChat(id);
		if ($('#chat_page_' + id).length == 0) {
			$content = $(page_chat_template);
			var contact;
			$.each(app.contacts, function(i, v) {
				if (v.id == id) {
					contact = v;
					return false;
				}
			});
			var $chat_body = $content.find('[data-role=content]');
			var $bottom = $content.find('[data-role=bottom]');
			var date = new Date();
			$.each(app.emails, function(i, v) {
				if (v.partner == id) {
//					$chat_body.append(app.getMessageHTML(v));
					$bottom.prepend(app.getMessageHTML(v));
				}
			});
			$content.find('[data-role=header] *').html(contact.displayName);
			$content.attr("data-id", id);
			$content.attr("id", 'chat_page_' + id)
			$content.attr("data-url", 'chat_page_' + id);

			// $('[data-role=page]:last').after($content)
			$content.appendTo($.mobile.pageContainer);
		}
		$.mobile.pageContainer.pagecontainer("change", $('#chat_page_' + id), {
			transition : "fade"
		});
	},
	btnSendMessage : function(e) {
		var contact_id = $(this).parents('.chat_page').attr("data-id");
		var content = $(this).parents('[data-role=footer]').find("textarea")
				.val();
		var timeString = (new Date()).toLocaleTimeString();
		// var messageid = (new Date()).getUTCMilliseconds()+""+Math.random();
		$content = $('<div class="chat_message own">' + content
				+ '<div class="chat_info own">' + timeString
				+ '<span></span></div></div>');
		$(this).parents('[data-role=page]').find("[data-role=content]").append(
				$content);
		mail.sendMail(contact_id, content, function() {
			$content.find('.chat_info span').html('&#x2713;');
		}, function(e) {
			if (e == "no_email") {
				alert("You need to specify the recipient's email address.");
				app.openContactPage(contact_id);
			} else {
				alert("Sending failed: " + e);
				$content.find('.chat_info span').html('&#x2717;');
			}

		});
		$(this).parents('[data-role=footer]').find("textarea").val("");
	},
	btnSaveContact : function(e) {
		var contact_id = $(this).parents('.contact_page').attr("data-id");
		var contact = new Object();
		contact["contact_id"] = contact_id;
		contact["preferred_email"] = $(this).parents('.contact_page').find(
				'[name=preferred_email]:checked').attr("value");
		mail.setContactData(contact, function() {
			alert("saved.");
		});
	},
	onPause : function() {
		mail.disconnectService(function() {
		}, function() {
		})
	},
	onResume : function() {
		mail.connectService(function() {
		}, function() {
		});
	},
	isOpenChat : function(id) {
		var open_chats = JSON.parse(localStorage.getItem("open_chats"));
		if (open_chats != null) {
			var found = false;
			$.each(open_chats, function(i, v) {
				if (v == id) {
					found = true;
					return false;
				}
			});
			return found;
		}
		return false;
	},
	addOpenChat : function(id) {
		var open_chats = JSON.parse(localStorage.getItem("open_chats"));
		if (open_chats == null) {
			open_chats = new Array();
		}
		if (!app.isOpenChat(id)) {
			var $table_open_chats = $('#table_open_chats').find('tbody:last');
			for ( var i = 0; i < app.contacts.length; i++) {
				if (app.contacts[i].id == id) {
					if (app.contacts[i].photos[0].type === "url") {
						app.returnValidPhoto(app.contacts[i].photos[0].value,
								[ app.contacts[i].displayName,
										app.contacts[i].id ], function(url, v) {
									var code = '<tr data-id="' + v[1]
											+ '"><td><img src="' + url
											+ '"></td><td><h4>' + v[0]
											+ "</h4></td></tr>"
									$table_open_chats.append(code);
								});
					} else {
						var code = '<tr data-id="' + app.contacts[i].id
								+ '"><td><img src="data:image/gif;base64,'
								+ app.contacts[i].photos[0].value
								+ '"></td><td><h4>'
								+ app.contacts[i].displayName
								+ "</h4></td></tr>";
						$table_open_chats.append(code);
					}
				}
			}
		}
		open_chats.push(id);
		localStorage.setItem("open_chats", JSON.stringify(open_chats));
	}
};

var page_account_template = '<div data-content-theme="b" data-role="collapsible" data-theme="b">'
		+ '<h4 class="coll_header" data-name="New Account">New Account</h4>'
		+ '<div class="coll_content">'
		+ '<input type="hidden" name="id" value="-1"  />'
		+ '<div class="ui-field-contain">'
		+ '<label for="username">Username:</label>'
		+ '<input type="text" name="username"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<label for="password">Password:</label>'
		+ '<input type="password" name="password"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<label for="imap_address">IMAP Address:</label>'
		+ '<input type="text" name="imap_address"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<label for="imap_port">IMAP Port:</label>'
		+ '<input type="text" name="imap_port"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<fieldset data-role="controlgroup">'
		+ '<legend>IMAP Security:</legend>'
		+ '<input type="checkbox" name="imap_ssl" id="imap_ssl" />'
		+ '<label for="imap_ssl">SSL/TLS</label>'
		+ '<input type="checkbox" name="imap_starttls" id="imap_starttls" />'
		+ '<label for="imap_starttls">STARTTLS</label>'
		+ '<input type="checkbox" name="imap_auth" id="imap_auth" />'
		+ '<label for="imap_auth">Login</label>'
		+ '</fieldset>'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<label for="smtp_address">SMTP Address:</label>'
		+ '<input type="text" name="smtp_address"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<label for="smtp_port">SMTP Port:</label>'
		+ '<input type="text" name="smtp_port"  />'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<fieldset data-role="controlgroup">'
		+ '<legend>SMTP Security:</legend>'
		+ '<input type="checkbox" name="smtp_ssl" id="smtp_ssl" />'
		+ '<label for="smtp_ssl">SSL/TLS</label>'
		+ '<input type="checkbox" name="smtp_starttls" id="smtp_starttls" />'
		+ '<label for="smtp_starttls">STARTTLS</label>'
		+ '<input type="checkbox" name="smtp_auth" id="smtp_auth" />'
		+ '<label for="smtp_auth">Login</label>'
		+ '</fieldset>'
		+ '</div>'
		+ '<div class="ui-field-contain">'
		+ '<div class="ui-grid-b">'
		+ '<div class="ui-block-a"></div>'
		+ '<div class="ui-block-b"></div>'
		+ '<div class="ui-block-c"><button class="btn_account_delete">Delete</button></div>'
		+ '</div>'
		+ '</div>'
		+ '</div>' 
		+ '</div>';
var page_chat_template = '<div data-role="page" class="chat_page" data-theme="b" data-dom-cache="true">'
		+ '<div data-role="header" data-position="fixed" data-tap-toggle="false" data-theme="b">'
		+ '<h1></h1>'
		+ '</div>'
		+ '<div data-role="content">'
		+ '<div data-role="bottom"></div>'
		+ '</div>'
		+ '<div data-role="footer" data-position="fixed">'
		+ '<textarea></textarea>'
		+ '<button>Send</button>'
		+ '</div>'
		+ '</div>';
var page_contact_template = '<div data-role="page" class="contact_page" data-theme="b" data-dom-cache="true">'
		+ '<div data-role="header" data-position="fixed" data-tap-toggle="false" data-theme="b">'
		+ '<h1></h1>'
		+ '</div>'
		+ '<div data-role="content">'
		+ '<div data-role="collapsible-set" data-theme="b" data-content-theme="b">'
		+ '<div data-content-theme="b" data-role="collapsible" data-theme="b" id="contact_preferred_email">'
		+ '<h4 class="coll_header">Preferred Email</h4>'
		+ '<div class="coll_content">'
		+ '<div data-role="fieldcontain">'
		+ '<fieldset data-role="controlgroup">'
		+ '</fieldset>'
		+ '</div>'
		+ '</div>'
		+ '</div>'
		+ '</div>'
		+ '<button class="save">Save</button>'
		+ '</div>' + '</div>';