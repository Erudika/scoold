(function($) {

	var defaults = {
		serverUrl: '',
		logContSel: '#chat-log',
		messageContSel: '#chat-message',
		msgFormSel: '#chat-form',
		usersContSel: '#chat-members',
		chatMsgClass: 'chat-msg',
		chatSystemMsgClass: 'chat-system-msg',
		chatTimeMsgClass: 'chat-time smallText',
		chatNickMsgClass: 'chat-nick',
		chatTextMsgClass: 'chat-text',
		userJoinText: 'joined the room',
		userLeaveText: 'left the room',
		connectionErrorText: "Failed to connect!",
		pollingErrorText: "Disconnected. Let's try again...",
		reconnectErrorText: "OK, let's try to reconnect...",
		tryReconnectAfter: 20, // sec
		autoExpireAfter: 5 * 60 // sec
	};
	
	$.fn.nodechat = function (nickname, channelname, userid, settings) {
		
		var $chatnode = $(this);
		
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
					for (i = 0; i < data.messages.length; i++) {
						var message = data.messages[i];
						chatnode.lastMessageId = Math.max(chatnode.lastMessageId, message.id);
						$chatnode.triggerHandler(message.type, message);
					}
				}
				return chatnode.poll();
			},

			handlePollError: function() {
				this.pollingErrors++;
				var chatnode = this;

				$chatnode.triggerHandler("polling-error");
				
				setTimeout(function(){
					chatnode.poll();
				}, chatnode.tryReconnectAfter * 1000);

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
							$chatnode.triggerHandler("connection-error");
						}

						chatnode.poll();
						chatnode.messageCont.focus();
					},
					error: function(){
						$chatnode.triggerHandler("connection-error");
					}
				});
			},

			send: function(msg) {
				var chatnode = this;
				// TODO: could use POST but jsonp lib doesn't support it!
				return chatnode.request("/send", {
					data: {				
						userid: userid,
						nick: nickname,
						text: msg
					}
				});
			}			
		});	
		
		$.extend(this, defaults, settings);
		
		var chatnode = this;
		var $membersbox = $(this.usersContSel);
		
		this.logCont = $(this.logContSel);
		this.messageCont = $(this.messageContSel);
		if(this.messageCont.length){
			this.messageCont.attr("autocomplete", "off");
		}
		
		function formatTime(time){
			var now = new Date();
			var diff = now.getTime() - time.getTime();
 
			var timeDiff = getTimeDiffDescription(diff, 'd', 86400000);
			if (!timeDiff) {
				timeDiff = getTimeDiffDescription(diff, 'h', 3600000);
				if (!timeDiff) {
					timeDiff = getTimeDiffDescription(diff, 'm', 60000);
					if (!timeDiff) {
						timeDiff = getTimeDiffDescription(diff, 's', 1000);
						if (!timeDiff) {
							timeDiff = '';
						}
					}
				}
			}
 
			return timeDiff;
		}
 
		function getTimeDiffDescription(diff, unit, timeDivisor){
			var unitAmount = (diff / timeDivisor).toFixed(0);
			if (unitAmount > 0) {
				return unitAmount + unit + " ago";
			} else if (unitAmount < 0) {
				return 'in ' + Math.abs(unitAmount) + unit;
			} else {
				return null;
			}
 
		}

		function getChatRow(timestamp, uid, nick, msg, issystem){
			var row = $("<div></div>").addClass(chatnode.chatMsgClass);
			if(issystem){row.addClass(chatnode.chatSystemMsgClass);}

			if(nick && nick !== null){
				if (uid && uid !== null) {
					nick = "<a class=\"extlink\" href=\""+contextpath+"/profile/"+uid+"\">"+nick+"</a>"
				}
				$("<span></span>").addClass(chatnode.chatNickMsgClass).html(nick).appendTo(row);
			}
			if(timestamp && timestamp !== null){
				var date = new Date(timestamp);
				$("<span></span>").addClass(chatnode.chatTimeMsgClass).attr("title", date).
					html("&nbsp;"+formatTime(date)+":&nbsp;").data("timestamp", date).appendTo(row);
			}
			if(msg && msg !== null){
				$("<span></span>").addClass(chatnode.chatTextMsgClass).text(msg).appendTo(row);
			}

			return row;
		}
		
		// handle sending a message
		$(this.msgFormSel).submit(function() {
			chatnode.send(chatnode.messageCont.val());
			chatnode.messageCont.val("").focus();
			return false;
		});
		
		setInterval(function(){
			$(chatnode.logContSel).find(".chat-time").each(function(i, elem){
				$(elem).html("&nbsp;"+formatTime($(elem).data("timestamp")) + ":&nbsp;");
			});
		}, 30*1000);


		// Bind custom channel events to actions
		// new message posted to channel
		chatnode.bind("msg", function(event, message) {
			var row = getChatRow(message.timestamp, message.userid, message.nick, message.text, false);
			row.appendTo(chatnode.logCont);
		})
		// Auto scroll list to bottom
		.bind("join msg", function(event, message) {
			// auto scroll if we're within 50 pixels of the bottom
			if(!$membersbox.data("user-"+message.userid)){
				$.get(contextpath+"/profile/"+message.userid+"/?getsmallpersonbox=true", function(data){
					$membersbox.append(data);
				});
				// prevent duplicates
				$membersbox.data("user-"+message.userid, true);
				// auto-expire after 5 min
				setTimeout(function(){
					$("#user-"+message.userid, $membersbox).remove();
					$membersbox.removeData("user-"+message.userid);						
				},chatnode.autoExpireAfter * 1000);
			}
			if (chatnode.logCont.scrollTop() + 100 >=
				chatnode.logCont[0].scrollHeight - chatnode.logCont.height()) {
				
				window.setTimeout(function() {
					chatnode.logCont.scrollTop(chatnode.logCont[0].scrollHeight);
				}, 10);
			}
		})
		// display error message when connenction failed
		.bind("connection-error", function(){
			var row = getChatRow(null, null, "System", chatnode.connectionErrorText, true);
			row.appendTo(chatnode.logCont);
		})
		// display error msg when polling failed
		.bind("polling-error", function(){
			var row = getChatRow(null, null, "System", chatnode.pollingErrorText, true);
			row.appendTo(chatnode.logCont);
		})

		// display error msg when polling failed
		.bind("reconnect-error", function(){
			var row = getChatRow(null, null, "System", chatnode.reconnectErrorText, true);
			row.appendTo(chatnode.logCont);
		});
				
		chatnode.init();
		return this;
	};

})(jQuery);
