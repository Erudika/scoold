var router = require("./node-router"),
	url = require("url"),
	qs = require("querystring"),
	sys = require("sys"),
	fs = require("fs");

var basePath = "/chat";
var msgBufferLength = 20; //lines in memory
var longPollPeriod = 60 * 1000; //ms
var maxMsgSize = 500;	// symbols
var flushCallbacksInterval = 5 * 1000;

function Server() {
	this.httpServer = router.getServer();
	this.channels = [];
}

function createServer() {
	var server = new Server();
	
	function handle(req, res, match, handler){
		var query = qs.parse(url.parse(req.url).query);
		if(/^[a-zA-Z0-9]+$/.test(match)){
			var chan = server.channels[match];
			if(!chan){
				chan = server.channels[match] = {
					name: match, 
					messages: [], 
					sysmessages: [], 
					callbacks: [],
					lastid: 0, 
					server: server
				};
				setInterval(function() {
					server.flushCallbacks(chan);
				}, flushCallbacksInterval);
			}
			
			return handler(chan, req, res, query); //"hello "+ match;
		}else{
			return res.simpleText(400, "Bad request");
		}
	}
	
	server.get(new RegExp(basePath + "/(.+[^/])/join"), function(req, res, match){
		handle(req, res, match, server.join);
	});
	
	server.get(new RegExp(basePath + "/(.+[^/])/send"), function(req, res, match){
		handle(req, res, match, server.send);
	});
	
	server.get(new RegExp(basePath + "/(.+[^/])/recv"), function(req, res, match){
		handle(req, res, match, server.recv);
	});
	
	return server;
}

extend(Server.prototype, {
	listen: function(port, host) {this.httpServer.listen(port, host);},
	get: function(path, handler) {this.httpServer.get(path, handler);},
	
	join: function(channel, request, response, query) {
		var nick = query.nick;
		var userid = query.userid;

		if (!nick || !userid) {
			response.simpleJsonp(400, {error: "missing parameter: nick or id."}, query.callback);
			return;
		}
		
		channel.sysmessages.push({
			id: ++channel.lastid,
			userid: userid, 
			nick: nick, 
			type: "join",
			timestamp: (new Date()).getTime()
		});
		
		channel.server.channels[channel.name] = channel;
		response.simpleJsonp(200, {status: "ok"}, query.callback);
	},

	recv: function(channel, request, response, query) {
		var since = parseInt(query.since, 10);

		if (isNaN(since)) {
			response.simpleJsonp(400, {error: "Missing parameter 'since'."}, query.callback);
			return;
		}
		
		channel.server.getMessages(channel, since, function(messages) {
			response.simpleJsonp(200, {messages: messages}, query.callback);
		});
	},

	send: function(channel, request, response, query) {
		var text = query.text;
		var userid = query.userid;
		var nick = query.nick;

		if (!text || !text.length) {
			response.simpleJsonp(400, {error: "Message cannot be empty."}, query.callback);
			return;
		}

		var id = channel.server.appendMessage(channel, nick, userid, "msg", text);
		response.simpleJsonp(200, {id: id}, query.callback);
	},
	
	appendMessage: function(channel, nick, userid, type, text) {
		var id = ++channel.lastid;
		
		var message = {
			id: id,
			nick: nick,
			userid: userid,
			type: type,
			text: shorten(text, maxMsgSize),
			timestamp: (new Date()).getTime()
		};
		channel.messages.push(message);
		
		while (channel.callbacks.length > 0) {
			channel.callbacks.shift().callback([message]);
		}
		
		while (channel.messages.length > msgBufferLength) {
			channel.messages.shift();
		}
		
		channel.server.channels[channel.name] = channel;
		
		return id;
	},
	
	getMessages: function(channel, since, callback) {
		var matching = [];
		var sysmatching = [];
		
		for (var i = 0; i < channel.messages.length; i++) {
			if (channel.messages[i].id > since) {
				matching = channel.messages.slice(i);
				break;
			}
		}
		for (var j = 0; j < channel.sysmessages.length; j++) {
			if (channel.sysmessages[j].id > since) {
				sysmatching = channel.sysmessages.slice(j);
				break;
			}
		}
				
		var all = matching.concat(sysmatching);
		
		if (all.length) {
			// any new messages since last poll? yes -> return
			callback(all);
		} else {
			// no new messages -> keep connection open till timeout
			channel.callbacks.push({
				timestamp: new Date(),
				callback: callback
			});
		}
		channel.sysmessages = [];
	},
	
	flushCallbacks: function(channel) {
		var now = new Date();
		var callbacks = channel.callbacks;
		while (callbacks.length && (now - callbacks[0].timestamp) > longPollPeriod * 0.75) {
			callbacks.shift().callback([]);
		}
	}
});

var slice = [].slice;
Function.prototype.partial = function() {
	var fn = this,
		args = slice.call(arguments);
	
	return function() {
		return fn.apply(this, args.concat(slice.call(arguments)));
	};
};

function extend(obj, props) {
	for (var prop in props) {
		obj[prop] = props[prop];
	}
}
function shorten(txt, len){
	if(txt && txt.length > len){
		return txt.substr(0, len);
	}
	return txt;
}

// start server
createServer().listen(8001);
