var EventEmitter = require("events").EventEmitter,
	sys = require("sys"),
	Session = require("./session").Session;

var msgBufferLength = 50; //lines
var longPollPeriod = 60000; //ms
var maxMsgSize = 500;

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
	this.callbacks = [];
	this.sessions = {};
	
	var channel = this;
	setInterval(function() {
		channel.flushCallbacks();
		channel.expireOldSessions();
	}, 1000);
}
sys.inherits(Channel, EventEmitter);

extend(Channel.prototype, {
	appendMessage: function(nick, userid, type, text) {
		var shortTxt = shorten(text, maxMsgSize);
		var id = ++this.nextMessageId,
			message = {
				id: id,
				nick: nick,
				userid: userid,
				type: type,
				text: shortTxt,
				timestamp: (new Date()).getTime()
			};
		this.messages.push(message);
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
		var matching = [],
			length = this.messages.length;
		for (var i = 0; i < length; i++) {
			if (this.messages[i].id > since) {
				matching = this.messages.slice(i);
				break;
			}
		}
		
		if (matching.length) {
			callback(matching);
		} else {
			this.callbacks.push({
				timestamp: new Date(),
				callback: callback
			});
		}
	},
	
	flushCallbacks: function() {
		var now = new Date();
		while (this.callbacks.length && now - this.callbacks[0].timestamp > this.sessionTimeout * 0.75) {
			this.callbacks.shift().callback([]);
		}
	},
	
	createSession: function(nick, id) {
		var session = new Session(nick, id);
		if (!session) {
			return null;
		}
			
		if(this.sessions[id]){
			return this.sessions[id];
		}
		
		this.sessions[session.userid] = session;
		session.since = this.appendMessage(nick, id, "join");
		
		return session;
	},
	
	destroySession: function(id) {
		if (!id || !this.sessions[id]) {
			return false;
		}
		var nick = this.sessions[id].nick;
		var userid = this.sessions[id].userid;
		
		var eventId = this.appendMessage(nick, userid, "part");
		delete this.sessions[id];
		return eventId;
	},
	
	expireOldSessions: function() {
		var now = new Date();
		for (var session in this.sessions) {
			if (now - this.sessions[session].timestamp > this.sessionTimeout) {
				this.destroySession(session);
			}
		}
	}
});

exports.Channel = Channel;

function shorten(txt, len){
	if(txt && txt.length > len){
		return txt.substr(0, len) + "...";
	}
	return txt;
}

function extend(obj, props) {
	for (var prop in props) {
		obj[prop] = props[prop];
	}
}
