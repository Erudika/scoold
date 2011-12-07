// load scripts
function loadScripts(scriptslink, minsuffix){
	head.js(
		{jquery:		"http://ajax.aspnetcdn.com/ajax/jQuery/jquery-1.7.1.min.js"},
		{validate:		"http://ajax.aspnetcdn.com/ajax/jquery.validate/1.9/jquery.validate.min.js"},
		{modal:			scriptslink+"/jquery.modal"+minsuffix+".js"},
		{jeditable:		scriptslink+"/jquery.jeditable"+minsuffix+".js"},
		{autocomplete:	scriptslink+"/jquery.autocomplete"+minsuffix+".js"},
		{gallery:		scriptslink+"/jquery.galleriffic"+minsuffix+".js"},
		{history:		scriptslink+"/jquery.history"+minsuffix+".js"},
		{oembed:		scriptslink+"/jquery.oembed"+minsuffix+".js"},
		{markitup:		scriptslink+"/jquery.markitup"+minsuffix+".js"},
		{mathjax:		"http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS_HTML"},
		{chatclient:	scriptslink+"/chatclient"+minsuffix+".js"},
		{miuicons:		scriptslink+"/miu.set.markdown"+minsuffix+".js"},
		{showdown:		scriptslink+"/showdown"+minsuffix+".js"},
		{diff:			scriptslink+"/diff_match_patch"+minsuffix+".js"},
		
		{scoold:		scriptslink+"/scoold"+minsuffix+".js"}
	);
}