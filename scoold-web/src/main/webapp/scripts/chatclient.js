(function($) {

	var defaults = {
		serverUrl: '',
		logContSel: '#chat-log',
		messageContSel: '#chat-message',
		msgFormSel: '#chat-form',
		usersContSel: '#chat-members',
		chatMsgClass: 'chat-msg',
		chatSystemMsgClass: 'chat-system-msg',
		chatTimeMsgClass: 'chat-time mrs',
		chatNickMsgClass: 'chat-nick mrs bold',
		chatTextMsgClass: 'chat-text',
		profileLinkPrefix: 'profile/',
		userJoinText: 'joined the room',
		userLeaveText: 'left the room',
		connectionErrorText: "Failed to connect!",
		pollingErrorText: "Disconnected. Let's try again...",
		reconnectErrorText: "OK, let's try to reconnect..."
	};
	
	$.fn.nodechat = function (nickname, channelname, userid, settings) {
		
		$.extend(this, {			
			pollingErrors: 0,
			lastMessageId: 0,
			
			init: function() {
				if(!channelname || $.trim(channelname) === ""){return this;}
				if(!nickname || $.trim(nickname) === ""){return this;}
				if(!userid || isNaN(userid)){return this;}
				if(channelname.charAt(0) !== '/'){channelname = "/" + channelname;}

				this.serverUrl += channelname;
				
				return this.join();
			},			
			
			request: function(url, options) {
				var chatnode = this;
				var fullurl = chatnode.serverUrl + url;
				
				return $.jsonp($.extend({
					url: fullurl,
					callbackParameter: "callback"
				}, options));
			},

			poll: function() {
				if (this.pollingErrors > 2) {
					$(this).triggerHandler("reconnect-error");
					this.pollingErrors = 0;
					this.join();
					return this;
				}
				var chatnode = this;
				
				chatnode.request("/recv", {
					data: {
						since: chatnode.lastMessageId,
						userid: userid
					},
					success: function(data) {
						if (data) {
							chatnode.handlePoll(data);
						} else {
							chatnode.handlePollError();
						}
					},
					error: function(){
						chatnode.handlePollError();
					}
				});
				
				return this;
			},

			handlePoll: function(data) {
				var chatnode = this;
				chatnode.pollingErrors = 0;
				
				if (data && data.messages) {
					$.each(data.messages, function(i, message) {
						chatnode.lastMessageId = Math.max(chatnode.lastMessageId, message.id);
						$(chatnode).triggerHandler(message.type, message);
					});
				}
				return chatnode.poll();
			},

			handlePollError: function() {
				this.pollingErrors++;
				var chatnode = this;

				$(chatnode).triggerHandler("polling-error");
				
				setTimeout(function(){
					chatnode.poll();
				}, 5*1000);


				return this;
			},

			join: function() {
				var chatnode = this;

				return chatnode.request("/join", {
					data: {
						nick: nickname,
						userid: userid
					},
					success: function(data) {
						if (!data) {
							$(chatnode).triggerHandler("connection-error");
						}

						chatnode.since = data.since;
						chatnode.poll();

						chatnode.messageCont.focus();
					},
					error: function(){
						$(chatnode).triggerHandler("connection-error");
					}
				});
				
			},

			part: function() {
				var chatnode = this;

				return chatnode.request("/part", {
					data: {userid: userid}
				});
			},

			send: function(msg) {
				var chatnode = this;
				// TODO: use POST
				return chatnode.request("/send", {
					data: {				
						userid: userid,
						text: msg
					}
				});
			},

			who: function() {
				var chatnode = this;
				
				return chatnode.request("/who", {
					success: function(data) {
						var users = $(chatnode.usersContSel);
						$.each(data.nicks, function(i, nick) {
							users.append(nick);
						});
					}
				});
			}	
			
		});		
				
		$.extend(this, defaults, settings);
		
		var chatnode = this;

		this.logCont = $(this.logContSel);
		this.messageCont = $(this.messageContSel);
		if(this.messageCont.length){
			this.messageCont.attr("autocomplete", "off");
		}
		
		// notify the chat server that we're leaving if we close the window
		$(window).unload(function() {
			chatnode.part();
		});

		function formatTime(timestamp) {
			var date = new Date(timestamp),
				hours = date.getHours(),
				minutes = date.getMinutes(),
				seconds = date.getSeconds();

			if (hours < 10) {hours = "0" + hours;}
			if (minutes < 10) {minutes = "0" + minutes;}
			if (seconds < 10) {seconds = "0" + seconds;}
			return "[" + hours + ":" + minutes + ":" + seconds + "]";
		}

		function getChatRow(timestamp, nick, msg, issystem){
			var row = $("<div></div>").addClass(chatnode.chatMsgClass);
			if(issystem){row.addClass(chatnode.chatSystemMsgClass);}

			if(timestamp && timestamp !== null){
				$("<span></span>").addClass(chatnode.chatTimeMsgClass).text(formatTime(timestamp)).appendTo(row);
			}
			if(nick && nick !== null){
				$("<span></span>").addClass(chatnode.chatNickMsgClass).text(nick+":").appendTo(row);
			}
			if(msg && msg !== null){
				$("<span></span>").addClass(chatnode.chatTextMsgClass).text(msg).appendTo(row);
			}

			return row;
		}

		function getUserLink(name, id){
			$.get(contextpath+"/profile/"+id+"/?getsmallpersonbox=true", function(data){
				$(chatnode.usersContSel).append(data);
			});
//			return $("<a/>", {
//				href: chatnode.profileLinkPrefix + id,
//				title: name,
//				text: name,
//				"class": "user-"+id
//			});
		}
		
		// handle sending a message
		$(this.msgFormSel).submit(function() {
			chatnode.send(chatnode.messageCont.val());
			chatnode.messageCont.val("").focus();
			return false;
		});

		// Bind custom channel events to actions
		// new message posted to channel
		chatnode.bind("msg", function(event, message) {
			var row = getChatRow(message.timestamp, message.nick, message.text, false);
			row.appendTo(chatnode.logCont);
		})
		// user joined the channel
		.bind("join", function(event, message) {
//			var row = getChatRow(message.timestamp, message.nick, chatnode.userJoinText, true);
//			row.appendTo(chatnode.logCont);
			getUserLink(message.nick, message.userid);
//			$(chatnode.usersContSel).append(getUserLink(message.nick, message.userid));
		})
		// another user left the channel
		.bind("part", function(event, message) {
//			var row = getChatRow(message.timestamp, message.nick, chatnode.userLeaveText, true);
//			row.appendTo(chatnode.logCont);
			$(chatnode.usersContSel).find(".user-"+message.userid).remove();
		})

		// Auto scroll list to bottom
		.bind("join part msg", function() {
			// auto scroll if we're within 50 pixels of the bottom
			if (chatnode.logCont.scrollTop() + 100 >=
				chatnode.logCont[0].scrollHeight - chatnode.logCont.height()) {
				
				window.setTimeout(function() {
					chatnode.logCont.scrollTop(chatnode.logCont[0].scrollHeight);
				}, 10);
			}
		})
		// display error message when connenction failed
		.bind("connection-error", function(){
			var row = getChatRow(null, "System", chatnode.connectionErrorText, true);
			row.appendTo(chatnode.logCont);
		})
		// display error msg when polling failed
		.bind("polling-error", function(){
			var row = getChatRow(null, "System", chatnode.pollingErrorText, true);
			row.appendTo(chatnode.logCont);
		})

		// display error msg when polling failed
		.bind("reconnect-error", function(){
			var row = getChatRow(null, "System", chatnode.reconnectErrorText, true);
			row.appendTo(chatnode.logCont);
		});
				
		chatnode.init();

		return this;
	};

})(jQuery);
