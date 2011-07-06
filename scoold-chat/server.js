var router = require("./node-router"),
	url = require("url"),
	qs = require("querystring"),
	sys = require("sys"),
	fs = require("fs"),
	Channel = require("./channel").Channel;

var servers = [];
var basepath = "/chat";

function Server() {
	this.httpServer = router.getServer();
	this.channels = [];
	this.basePath = basepath;
}

extend(Server.prototype, {
	listen: function(port, host) {
		this.httpServer.listen(port, host);
	},
	
	get: function(path, handler) {
		this.httpServer.get(path, handler);
	},
	
	// server static web files
	serveFiles: function(localDir, webDir) {
		var server = this;
		fs.readdirSync(localDir).forEach(function(file) {
			var local = localDir + "/" + file,
				web = webDir + "/" + file;

			if (fs.statSync(local).isDirectory()) {
				server.serveFiles(local, web);
			} else {
				server.get(web, router.staticHandler(local));
			}
		});
	},

	// Handlers
	
	who: {path: "/who", handler: function(channel, request, response, query) {
		var nicks = [];
		var userids = [];
		for (var id in channel.sessions) {
			nicks.push(channel.sessions[id].nick);
			userids.push(channel.sessions[id].userid);
		}
		sys.puts(nicks);
		response.simpleJsonp(200, {nicks: nicks, userids: userids}, query.callback);
	}},

	join: {path: "/join", handler: function(channel, request, response, query) {
		var nick = query.nick;
		var userid = query.userid;

		if (!nick || !userid) {
			response.simpleJsonp(400, {error: "bad nick or id."}, query.callback);
			return;
		}
		var session = channel.createSession(nick, userid);

		if (!session) {
			response.simpleJsonp(400, {error: "nick in use."}, query.callback);
			return;
		}
		response.simpleJsonp(200, {id: session.since, nick: nick, userid: userid, since: session.since}, query.callback);
	}},

	part: {path: "/part", handler: function(channel, request, response, query) {
		var userid = query.userid;
		var	session = channel.sessions[userid];

		if (!session) {
			response.simpleJsonp(400, {error: "No such session id."}, query.callback);
			return;
		}

		var eventId = channel.destroySession(userid);
		response.simpleJsonp(200, {id: eventId, userid: userid}, query.callback);
	}},

	recv: {path: "/recv", handler: function(channel, request, response, query) {
		var since = parseInt(query.since, 10);
		var	session = channel.sessions[query.userid];

		if (!session) {
			response.simpleJsonp(400, {error: "No such session id."}, query.callback);
			return;
		}

		if (isNaN(since)) {
			response.simpleJsonp(400, {error: "Must supply since parameter."}, query.callback);
			return;
		}

		session.poke();
		channel.query(since, function(messages) {
			session.poke();
			response.simpleJsonp(200, {messages: messages}, query.callback);
		});
	}},

	send: {path: "/send", handler: function(channel, request, response, query) {
		var text = query.text,
			session = channel.sessions[query.userid];

		if (!session) {
			response.simpleJsonp(400, {error: "No such session id."}, query.callback);
			return;
		}

		if (!text || !text.length) {
			response.simpleJsonp(400, {error: "Must supply text parameter."}, query.callback);
			return;
		}

		session.poke();
		var id = channel.appendMessage(session.nick, session.userid, "msg", text);
		response.simpleJsonp(200, {id: id}, query.callback);
	}}
});


function createServer() {
	var server = new Server();
	
	function handle(req, res, match, handler){
		var query = qs.parse(url.parse(req.url).query);
		if(/^[a-zA-Z0-9]+$/.test(match)){
			var chan = server.channels[match];
			if(!chan){
				chan = new Channel({basePath: match});
				server.channels[match] = chan;
			}
			return handler.handler(chan, req, res, query); //"hello "+ match;
		}else{
			return res.simpleText(400, "Bad request");
		}
	}
	
	server.get(new RegExp(server.basePath + "/(.+[^/])" + server.who.path),
		function(req, res, match){handle(req, res, match, server.who)} );
	server.get(new RegExp(server.basePath + "/(.+[^/])" + server.join.path),
		function(req, res, match){handle(req, res, match, server.join);});
	server.get(new RegExp(server.basePath + "/(.+[^/])" + server.part.path),
		function(req, res, match){handle(req, res, match, server.part);});
	server.get(new RegExp(server.basePath + "/(.+[^/])" + server.send.path),
		function(req, res, match){handle(req, res, match, server.send);});
	server.get(new RegExp(server.basePath + "/(.+[^/])" + server.recv.path),
		function(req, res, match){handle(req, res, match, server.recv);});
	
	servers.push(server);
	
	return server;
}

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

// start server
createServer().listen(8001);
