function Session(nick, id) {
	if (!nick || !id || isNaN(id)) {
		return;
	}
	
	this.nick = nick;
	this.userid = id; //Math.floor(Math.random() * 1e10).toString();
	this.timestamp = new Date();
}

Session.prototype.poke = function() {
	this.timestamp = new Date();
};

exports.Session = Session;
