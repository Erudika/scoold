var EventEmitter = require("events").EventEmitter,
	sys = require("sys"),
	Session = require("./session").Session;

var msgBufferLength = 10; //lines in memory
var longPollPeriod = 60 * 1000; //ms
var maxMsgSize = 500;	// symbols

function Channel(options) {
	EventEmitter.call(this);
	
	if (!options || !options.basePath) {
		return;
	}
	
	this.basePath = options.basePath;
	this.messageBacklog = parseInt(options.messageBacklog) || msgBufferLength;
	this.sessionTimeout = parseInt(options.sessionTimeout) || longPollPeriod;
	
	this.nextMessageId = 0;
	this.messages = [];
	this.systemMessages = [];
	this.callbacks = [];
//	this.sessions = {};
	
	var channel = this;
	setInterval(function() {
		channel.flushCallbacks();
//		channel.expireOldSessions();
	}, 5000);
}
sys.inherits(Channel, EventEmitter);

extend(Channel.prototype, {
	appendMessage: function(nick, userid, type, text) {
		var id = ++this.nextMessageId;
		
		var message = {
			id: id,
			nick: nick,
			userid: userid,
			type: type,
			text: shorten(text, maxMsgSize),
			timestamp: (new Date()).getTime()
		};
		if (type === "msg") {
			this.messages.push(message);
		}else{
			this.systemMessages.push(message);
		}
		this.emit(type, message);
		
		while (this.callbacks.length > 0) {
			this.callbacks.shift().callback([message]);
		}
		
		while (this.messages.length > this.messageBacklog) {
			this.messages.shift();
		}
		
		return id;
	},
	
	query: function(since, callback) {
		var matching = [];
		var systemMatching = [];
		
		for (var i = 0; i < this.messages.length; i++) {
			if (this.messages[i].id > since) {
				matching = this.messages.slice(i);
				break;
			}
		}
		for (var j = 0; j < this.systemMessages.length; j++) {
			if (this.systemMessages[j].id > since) {
				systemMatching = this.systemMessages.slice(j);
				break;
			}
		}
		
		var all = matching.concat(systemMatching);
		
		if (all.length) {
			// any new messages since last poll? yes -> return
			callback(all);
		} else {
			// no new messages -> keep connection open till timeout
			this.callbacks.push({
				timestamp: new Date(),
				callback: callback
			});
		}
		this.systemMessages = [];
	},
	
	flushCallbacks: function() {
		var now = new Date();
		while (this.callbacks.length && now - this.callbacks[0].timestamp > this.sessionTimeout * 0.75) {
			this.callbacks.shift().callback([]);
		}
	}
});

exports.Channel = Channel;

function shorten(txt, len){
	if(txt && txt.length > len){
		return txt.substr(0, len);
	}
	return txt;
}

function extend(obj, props) {
	for (var prop in props) {
		obj[prop] = props[prop];
	}
}
