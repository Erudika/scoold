/*
 * Unobtrusive javascript for scoold.com
 * author: Alexander Bogdanovski
 * (CC) BY-SA
 */
/*global window: false, jQuery: false, $: false, lang: false */
$(function () {
	"use strict";
	var ajaxpath = window.location.pathname,
		infobox = $("div.infostrip"),
		hideMsgBoxAfter = 10 * 1000, //10 sec
		mapCanvas = $("div#map-canvas"),
		rusuremsg = lang.areyousure,
		highlightfn = function(element) {$(element).addClass("error");clearLoading();},
		unhighlightfn = function(element) {$(element).removeClass("error");},
		errorplacefn = function(error, element) {error.insertBefore(element);},
		errorplacefn2 = function(error, element) {error.insertAfter(element);},
		reqmsg = lang['signup.form.error.required'],
		emailmsg = lang['signup.form.error.email'],
		digitsmsg = lang.invalidyear,
		maxlenmsg = lang.maxlength,
		minlenmsg = lang.minlength,
		tagsmsg = lang["tags.toomany"],
		secdata = {stoken: (typeof stoken !== "undefined" ? stoken : null), 
			pepper: (typeof pepper !== "undefined" ? pepper : null)};

	/**************************
	 *    Google Maps API
	 **************************/
	
	function initMap(elem){
		var geocoder = new google.maps.Geocoder(),
			myLatlng = new google.maps.LatLng(42.6975, 23.3241),
			marker = new google.maps.Marker({}),
			locbox = $("input.locationbox"),
			locality = "",
			sublocality = "",
			country = "",
			mapElem = elem || mapCanvas.get(0),
			mapZoom = 3,
			mapMarker = new google.maps.Marker({visible: false}),
			map = new google.maps.Map(mapElem, {
					zoom: mapZoom,
					center: myLatlng,
					mapTypeId: google.maps.MapTypeId.ROADMAP,
					mapTypeControl: false,
					streetViewControl: false
			});
			
		if (locbox.length && $.trim(locbox.val()) !== "") {
			geocoder.geocode({address: locbox.val()}, function(results, status){
				if(status === google.maps.GeocoderStatus.OK && results.length && results.length > 0){
					var latlng = results[0].geometry.location;
					mapMarker = new google.maps.Marker({position: latlng, visible: true});
					mapMarker.setMap(map);		
					map.setCenter(latlng);
					map.setZoom(7);
				}
			});
		}
		
		google.maps.event.addListener(map, 'click', function(event) {
			map.setCenter(event.latLng);
			marker.setPosition(event.latLng);
			marker.setMap(map);
			
			geocoder.geocode({location: event.latLng}, function(results, status){
				if (status !== google.maps.GeocoderStatus.OK) {
					locbox.val("");
				} else {
					if(results.length && results.length > 0){
						var h = 0,
							country = "",
							locality = "",
							sublocality = "";
							
						for (h = 0; h < results.length; h++) {
							var arr = results[h].address_components, i;
							for (i = 0; i < arr.length; i++) {
								var type = $.trim(arr[i].types[0]);
								var name = arr[i].long_name;
								if(type === "country"){
									country = name || "";
								}
								if(type === "locality"){
									locality = name || "";
								}
								if(type === "sublocality"){
									sublocality = name || "";
								}
							}
						}
					}
					var found = "";
					
					if(sublocality !== "" && country !== ""){
						found = sublocality + ", " + country;
					}else if(locality !== "" && country !== ""){
						found = locality +  ", " + country;
					}else{
						found = country;
					}
					
					locbox.val(found);
				}
			});
		});
	}
	
	if(mapCanvas.length){
		if(mapCanvas.is(":visible")){
			initMap();
		}
	}
	
	$(".map-div-toggle").click(function(){
		if (mapCanvas.is(":visible")) {
			mapCanvas.html("").hide(0, initMap);
		} else {
			mapCanvas.show(0, initMap);
		}
		return false;
	});

	/****************************************************
     *            FACEBOOK API FUNCTIONS
     ****************************************************/
	
	var fbauthurl = "facebook_auth";
	var fbattachurl = "attach_facebook";

	if($("#fb-login").length){
		FB.Event.subscribe('auth.login', function(response) {
			if(response.status === 'connected'){
				window.location = fbauthurl;
			}
		});
	}
	
	function fbAuthAction(actionurl){
		FB.getLoginStatus(function(response) {
			if (response.status === 'connected') {
				window.location = actionurl;
			}else{
				FB.login(function(response) {
					if (response.authResponse) {
						window.location = actionurl;
					}
				}, {scope: 'email'});
			}
		});
	}

	$("#fb-login-btn").click(function(){
		fbAuthAction(fbauthurl);
		return false;
	});

	$("#fb-attach-btn").click(function(){
		fbAuthAction(fbattachurl);
		return false;
	});

	var fbPicture = $("#fb-picture"),
		fbName = $("#fb-name"),
		fbNames = $(".fb-name");
		
	if(fbPicture.length || fbName.length){
		FB.getLoginStatus(function(response) {
			if (response.status === 'connected') {
				FB.api({
					method: 'fql.query',
					query: 'SELECT uid, name, pic_small, email FROM user WHERE uid=' + response.authResponse.userID
				}, function(response) {
					var user = response[0];
					fbPicture.html('<img src="' + user.pic_small + '" alt="'+user.name+'"/>');
					fbName.text(user.name + " #" + user.uid);
					$('input.fb-name-box').val(user.name);
					$('input.fb-email-box').val(user.email);
				});
			}
		});
	}
	
	if (fbNames.length) {
		FB.api({
			method: 'fql.query',
			query: 'SELECT name, pic_small, url FROM profile WHERE id=' + fbNames.text()
		}, function(response) {
			var user = response[0];
			fbNames.text(user.name);
		});
	}

	/****************************************************
     *					MISC FUNCTIONS
     ****************************************************/

	function clearLoading(){
		$(".loading").removeClass("loading");
		$("img.ajaxwait").hide();
	}

	function clearForm(form) {
		$(":input", form).each(function() {
			var type = this.type;
			var tag = this.tagName.toLowerCase(); // normalize case
			if (type === "text" || type === "password" || type === "hidden" ||
				tag === "textarea"){
				this.value = "";
			}else if (type === "checkbox"){
				this.checked = false;
			}else if (tag === "select"){
				this.selectedIndex = 0;
			}
		});
	}
	
	function readCookie(name) {
		var nameEQ = name + "=";
		var ca = document.cookie.split(';'), i;
		for(i = 0; i < ca.length; i++) {
			var c = ca[i];
            while (c.charAt(0) === ' '){c = c.substring(1,c.length);}
            if (c.indexOf(nameEQ) === 0){return c.substring(nameEQ.length,c.length);}
		}
		return null;
	}
	
	function createCookie(name, value) {
		var expires = "";
		var date = new Date();
		date.setTime(date.getTime()+(sessiontimeout * 1000));
		expires = ";expires="+date.toGMTString();
		document.cookie = name+"="+value+expires+";path=/";
	}
	
	function deleteCookie(name) {
		if (readCookie(name)){
			document.cookie = name + "=;path=/;expires=Thu, 01-Jan-1970 00:00:01 GMT";
		} 
	}

		
	function highlight(elem, hlclass){
		$('.'+hlclass).removeClass(hlclass);
		$(elem).addClass(hlclass);
	}
	
	function crossfadeToggle(elem1, elem2){
		if($(elem1).hasClass("hide") || $(elem1).css("display") === "none"){
            $(elem2).animate({opacity: "hide"}, 200, function(){
				$(this).addClass("hide");
                $(elem1).animate({opacity: "show"}, 200).removeClass("hide");
			});
		}else{
            $(elem1).animate({opacity: "hide"}, 200, function(){
				$(this).addClass("hide");
                $(elem2).animate({opacity: "show"}, 200).removeClass("hide");
			});
		}
	}

	function crossfade(elem1, elem2){
		$(elem1).animate({opacity: "hide"}, 200, function(){
			$(this).addClass("hide");
			$(elem2).animate({opacity: "show"}, 200).removeClass("hide");
		});
	}

	function submitForm(form, method, callbackfn){
		if (method === "GET" || method === "POST") {
			$.ajax({
				type: method,
				url: form.action,
				data: $(form).serialize(),
				success: function(data, status, xhr){
					clearLoading();
					callbackfn(data, status, xhr, form);
				}
			});
		}
	}
	
	function submitFormBind(formname, callbackfn){
		return $(document).on("submit", formname,  function(){
			submitForm(this, "POST", callbackfn);
			return false;
		});
	}

	function autocompleteBind(elem, params){
		var that = $(elem);
		that.attr("autocomplete", "off");
		that.autocomplete(ajaxpath, {
			minChars: 3,
			width: that.attr("width"),
			matchContains: true,
			highlight: false,
            extraParams: params,
			autoFill: true,
			formatItem: function(row) {
				return row[0] + "<br/>" + row[1];
			}
		});
		that.result(function(event, data, formatted) {
			$(this).next("input:hidden").val(data[2]);
		});
		//clear hidden fields on keypress except enter
		that.keypress(function(e){
			if(e.which !== 13 || e.keyCode !== 13){
				$(this).next("input:hidden").val("");
			}
		});
	}

	function autocompleteTagBind(elem, params){
		var that = $(elem);
		that.attr("autocomplete", "off");
		that.autocomplete(ajaxpath, {
			minChars: 2,
			width: that.attr("width"),
			matchContains: true,
			multiple: true,
			highlight: false,
            extraParams: params,
			autoFill: true,
			scroll: true,
			formatItem: function(row) {
				return row[0];
			}
		});
	}
	
	function autocompleteLabelBind(elem, params){
		if(typeof allLabels !== "undefined"){
			var that = $(elem);
			that.attr("autocomplete", "off");
			that.autocomplete(allLabels, {
				minChars: 2,
				width: that.attr("width"),
				matchContains: true,
				multiple: true,
				highlight: false,
				extraParams: params,
				autoFill: true,
				scroll: true
			});
			that.result(function(event, data, formatted) {
				$(this).val(data[0]+", ");
			});
		}
	}
	
	
	function autocompleteUserBind(elem, params){
		autocompleteBind(elem, params);
		$(elem).keypress(function(e){
			if(e.which === 13 || e.keyCode === 13){
				$(this).closest("form").submit();
			}
		});
	}

	function autocompleteContactBind(elem, params){
		if(typeof contacts !== "undefined"){
			var that = $(elem);
			that.attr("autocomplete", "off");
			that.autocomplete(contacts, {
				minChars: 3,
				width: that.attr("width"),
				matchContains: true,
				multiple: true,
				highlight: false,
				extraParams: params,
//				autoFill: true,
				scroll: true,
				formatItem: function(row) {
					return row.fullname;
				}
			});
			that.result(function(event, data, formatted) {
				var that = $(this),
					hidden = that.nextAll("input:hidden"),
					newHidden = hidden.clone();
				newHidden.val(data.id);
				hidden.after(newHidden);
				that.val(data.fullname+", ");
			});

			var clear = function(e){
				$(e).nextAll("input:hidden").not(":first").remove();
				$(e).val("");
			};

			//clear hidden fields on keypress except enter
			that.keyup(function(e){
				if(e.keyCode === 8 || e.keyCode === 46){
					clear(this);
				}
			}).bind('copy', function(e) {
				clear(this);
			}).bind('paste', function(e) {
				clear(this);
			}).bind('cut', function(e) {
				clear(this);
			});
		}
	}
	
	function showMsgBox(msg, clazz, hideafter){
		infobox.removeClass("infoBox errorBox successBox");
		infobox.find(".ico").hide();
		infobox.find("."+clazz+"Icon").show();
		infobox.addClass(clazz).children(".infostrip-msg").text(msg);
		infobox.show();
		
		if(hideafter && hideafter > 0){
			setTimeout(function(){
				infobox.hide();
			}, hideafter);
		}
	}
	
	function showInfoBox(msg){
		showMsgBox(msg, "infoBox", hideMsgBoxAfter);
	}
	function showErrorBox(msg){
		showMsgBox(msg, "errorBox", 0);
	}
	function showSuccessBox(msg){
		showMsgBox(msg, "successBox", hideMsgBoxAfter);
	}

	function hideMsgBoxes(){
		infobox.hide();
	}

	function areYouSure(func, msg, returns){
		if(confirm(msg)){
			func();
			if(returns){
				return true;
			}
		}
		return false;
	}	

	/****************************************************
     *					GLOBAL BINDINGS
     ****************************************************/

	$(document).on("click", ".rusure",  function(){
		return areYouSure($.noop, rusuremsg, true);
	});
	
	$(document).on("click", ".editlink",  function(){
		var that = $(this);
		var viewbox = that.parent().nextAll(".viewbox:first");
		var editbox = that.parent().nextAll(".editbox:first");
		if(!viewbox.length){
			viewbox = that.nextAll(".viewbox:first");
		}
		if(!editbox.length){
			editbox = that.nextAll(".editbox:first");
		}
		
		if(!viewbox.length){
			viewbox = that.closest(".viewbox");
		}
		if(!editbox.length){
			editbox = viewbox.nextAll(".editbox:first");
		}

		crossfadeToggle(viewbox.get(0), editbox.get(0));
		return false;
	});
	
	$(document).on("click", ".canceledit",  function(){
		var editbox = $(this).closest(".editbox").get(0);
		var viewbox = $(editbox).siblings(".viewbox").get(0);	
		crossfadeToggle(viewbox, editbox);
		return false;
	});

	//target=_blank is not valid XHTML
	$(document).on("click", "a.extlink",  function(){
		$(this).attr("target", "_blank");
		return true;
	});

	$(document).on("click", "a.votelink",  function(){
		var up = false;
		up = $(this).hasClass("upvote");
		var votes = $(this).closest("div.votebox").find(".votecount");
		var newvotes = parseInt(votes.text(), 10);
		if (!isNaN(newvotes)) {
			$.get(this.href, function(data){
				if(data === true ){
					if(up){
						newvotes++;
					}else{
						newvotes--;
					}
				}
				votes.text(newvotes);
			}, "json");
		}
		return false;
	});

	//close msg boxes 
	$(document).on("click", ".infostrip, .messagebox",  function(event){
		var that = $(this);
		that.hide();
		if(that.hasClass("introBox")){
			createCookie("intro", "0");
		}
		return (event.target.nodeName === "A");
	});
	
	$("body").ajaxSuccess(function() {
		clearLoading();
	});
	
	var color1 = $("body").css("color");
	var color2 = "#AAAAAA";
	$(".hintbox").focus(function(){
		var t = $(this);
		if(t.data("value") === ""){t.data("value", $(this).val());}
		if(t.val() === t.data("value")){t.val("");}
		t.css("color", color1);
	}).blur(function(){
		var t = $(this);
		if($.trim(t.val()) === ""){t.val(t.data("value"));
		t.css("color", color2);}
	}).css("color", color2).data("value", $(this).val());

	$("a#search-icon").click(function(){
		$(this).closest("form").submit();
		return false;
	});

	$(document).on("click", "a.next-div-toggle",  function(e){
		var that = $(this);
		var hdiv = that.nextAll("div:first");
		if(!hdiv.length){
			hdiv = that.parent().nextAll("div:first");
		}
		if(!hdiv.length){
			hdiv = that.closest("div").find("div:first");
		}
		hdiv.slideToggle("fast").find("input[type=text]:first, textarea:first").focus();
		return false;
	});

	$(document).on("click", "a.next-span-toggle",  function(){
		$(this).nextAll("span:first").toggle();
		return false;
	});
	
	
	function postAsk(elem, fn, callback){
		return areYouSure(function(){
			$.post(elem.attr("href"), secdata, function(data){
				callback(data);
			});
			fn();
		}, rusuremsg, false);
	}
		
	$(document).on("click", ".post-refresh-ask",  function(){
		postAsk($(this), function(){}, function(){
			window.location.reload(true);
		});
	});
	
	$(document).on("click", ".post-refresh",  function(){
		$.post($(this).attr("href"), secdata, function(){
			window.location.reload(true);
		});
	});
	
	$(".set-sys-param").click(function(){
		var form = $("form#sys-param-form"), title = $(this).attr("title");
		form.find("input[name=name]").val(title.substring(0, title.indexOf(":")));
		form.find("input[name=value]").val(title.substring(title.indexOf(":")+1, title.length));
		return false;
	});
	
	// TODO: ^^^^^^^^^^^ USE the new postAsk code on THESE  \/ \/ \/
	
	/****************************************************
     *                    REPORTS
     ****************************************************/

	$("a.close-report").click(function(){
		var dis = $(this);
		dis.closest(".reportbox").find(".report-solution-box").toggle();
		return false;
	});

	$("a.delete-report").click(function(){
		var dis = $(this);
		return areYouSure(function(){
			$.post(dis.attr("href"), secdata);
			dis.closest(".reportbox").fadeOut(function(){dis.remove();});
		 }, rusuremsg, false);
	});

	submitFormBind("form.report-solution-form", function(data, status, xhr, form){
		var dis = $(form);
		var parent = dis.closest(".reportbox");
		$(".report-solution", parent).show().children("span").text(dis.find("textarea").val());
		$("a.close-report", parent).hide();
		$("div:hidden:first", parent).show();
		clearForm(form);
	});

	/****************************************************
     *                    SETTINGS
     ****************************************************/
	 
	 $(".delopenid").click(function(){
		 var that = $(this);
		 return areYouSure(function(){
			$.post(that.attr("href"), secdata);
			that.closest("tr").fadeOut(function(){
				that.remove();
			}).siblings("tr").find(".delopenid").hide();
		 }, rusuremsg, false);
	 });

	/****************************************************
     *                    MESSAGES
     ****************************************************/

	$(".delete-message").click(function(){
		$.post(this.href, secdata);
		$(this).closest("div").fadeOut();
		return false;
	});
	
	submitFormBind("form.delete-all-messages-form", function(data, status, xhr, form){
		$("#all-messages").empty();
		$(form).add(".reputationbox").hide();
	});
	
	$("#sendmessage-close").click(function(){
		$(this).closest("div.newmessage").slideToggle("fast");
		return false;
	});

	submitFormBind("form.new-message-form", function(data, status, xhr, form){
		var dis = $(form);
		dis.closest("div").slideToggle("fast");
		showSuccessBox(lang["messages.sent"]);
		dis.find("input[type='hidden']").not(":first").remove();
		clearForm(form);
	});

	/****************************************************
     *                    PROFILE 
     ****************************************************/

	submitFormBind("form#about-edit-form", function(data, status, xhr, form){
		var dis = $(form);
		dis.closest("div.editbox").siblings("div.viewbox").html(data);
		dis.find("input.canceledit").click();
	});
	
	$(document).on("click", "a.addfriend",  function(){
		$.post(this.href, secdata);
		showSuccessBox(lang["profile.contacts.added"]);
		$(this).fadeOut();
		return false;
	});

	//delete friend link
	$(document).on("click", "a.delfriend",  function(){
		var that = $(this);
		return areYouSure(function(){
			that.fadeOut();
			$.post(that.attr("href"), secdata);
		}, rusuremsg, false);
	});

	/****************************************************
     *                    EDITABLES
     ****************************************************/
		
	var editable_settings = {
			submit: lang.save,
			tooltip: lang.clickedit,
			placeholder: lang['profile.status.txt'],
			cssclass: "clickedit"
		};

	function editableBind(elem, param, opts){
		if(opts){
			$.extend(true, editable_settings, opts);
		}

		var params = {};		
		$.extend(params, secdata);
		var $elem = $(elem);			
		$elem.editable(function(value, settings) {
			var $text = $elem.data("value");
			params[param] = value;
				$.post(ajaxpath, params);
				$text = $elem.text(value).text();
				$elem.data("value", $text);
			return $text;}, editable_settings
		).data("value", $elem.text());
	}

	editableBind("#fullname.editable", "fullname");
	editableBind("#mystatus.editable", "status");
	editableBind("#schoolname.editable", "name");
	editableBind("#classname.editable", "identifier");
	editableBind("#groupname.editable", "name");
	editableBind("#groupdesc.editable", "description");
	editableBind("#questiontitle.editable", "title");
	
	var editable_settings3 = {};
	$.extend(true, editable_settings3, editable_settings, {type: "textarea"});
	$(".drawer-description.editable").editable(function(value, settings){
		var $that = $(this);
		$.post(ajaxpath+"?update-description=true", $.extend({description: value, mid: this.id}, secdata));
		return $that.text(value).text();}, editable_settings3
	);

	/****************************************************
     *               CONTACT DETAILS
     ****************************************************/

	function getDetailValue(typeRaw){
		var val = "";
		if (typeRaw === "WEBSITE") {
			val = "http://";
		}else if(typeRaw === "FACEBOOK"){
			val = "http://facebook.com/";
		} 
		return val;
	}
	
	$(".add-contact").click(function(){
		var txtbox = $(this).prev("input");
		var val = txtbox.val();
		var type = $("select#detail-type option:selected").text();
		var typeRaw = $("select#detail-type").val();
		if($.trim(val) !== ""){
			var clonable = txtbox.closest("tr").nextAll(".detailbox:hidden");
			var box = clonable.clone();
			val = val.replace(/[;,]/gi, "");
			box.find(".contact-type").text(type+":");
			box.find(".contact-value").text(val);
			clonable.after(box.show());
			txtbox.val(getDetailValue(this.value));
			box.find("input[type='hidden']").val(typeRaw + "," + val);
		}
		return false;
	});

	$(document).on("click", ".remove-contact",  function(){
		$(this).closest("tr").remove();
		return false;
	});

	$("select#detail-type").change(function(){
		$("#detail-value").val(getDetailValue(this.value));
	}).change();
    
	/****************************************************
     *                    AUTOCOMPLETE
     ****************************************************/
    
	autocompleteBind("input.locationbox", {find: "locations"});
	autocompleteContactBind("input.contactnamebox", {find: "contacts"});
	autocompleteLabelBind("input.addlabelbox", {find: "labels"});
	autocompleteTagBind("input.tagbox", {find: "tags"});
	autocompleteUserBind("input.personbox", {find: "people"});
	autocompleteBind("input.schoolnamebox", {find: "schools"}); 
	autocompleteBind("input.classnamebox", {find: "classes"});
	
	
	/****************************************************
     *                   SCHOOLS 
     ****************************************************/
	
	submitFormBind("form#school-about-edit-form", function(data, status, xhr, form){
		var dis = $(form);
		dis.closest("div.editbox").siblings("div.viewbox").html(data);
		dis.find("input.canceledit").click();
	});

	submitFormBind("form.school-edit-form", function(data, status, xhr, form){
		$("div#schools-edit").html(data);
	});

	/****************************************************
     *                   CLASSES 
	 ****************************************************/

	$("#add-more-classmates").click(function(){
		var that = $(this);
		var clone = that.prev("div").clone();
		clone.find("input").val("");
		that.before(clone);
		clone.find("input[name=fullname]").focus();
		return false;
	});
	
	/****************************************************
     *                   GROUPS 
	 ****************************************************/
		
	$(".remove-groupmember-btn").click(function(){
		$.post(this.href, secdata);
		var that = $(this);
		that.closest("div.media").fadeOut("slow", function(){
			that.remove();
		});
		return false;
	});
	
	$(".add-groupmember-btn").click(function(){
		var that = $(this);
		that.closest("form").submit();
	});

	/****************************************************
     *						 CHAT
	 ****************************************************/

	var chatlog = $("#chat-log");
	var chatmessage = $("input#chat-message");
	var timeout = 1000;
	
	function chatPoll(){
		setTimeout(function(){
			$.get(ajaxpath, {receivechat: true}, function(data){
				chatlog.html(data);
				timeout = 30 * 1000;
				chatPoll();
			})
		}, timeout);
	}
	
	if(chatlog.length){
		chatPoll();
	}
	
	submitFormBind("form#chat-form", function(data, status, xhr, form){
		chatlog.html(data);
		chatmessage.val("");
	});
	
	$("a#chat-send-msg").click(function(){
		$(this).closest("form").submit();
		chatmessage.val("");
		return false;
	});

	/****************************************************
     *                    MODAL DIALOGS
     ****************************************************/

	$("div.report-dialog").jqm({
		trigger: ".trigger-report",
		onShow: function(hash){
			var div = hash.w;
			var trigr = $(hash.t);
			if(typeof trigr.data("loadedForm") === "undefined"){
				$.get(trigr.attr("href"), function(data) {
					clearLoading();
					div.html(data);
					trigr.data("loadedForm", data);
				});
			}else{
				div.html(trigr.data("loadedForm"));
			}
			div.on("click", ".jqmClose", function(){
				hash.w.jqmHide();
				return false;
			});
			div.show();
		}
	});

	$(document).on("click", ".trigger-report",  function(){
		$("div.report-dialog").jqmShow(this);
		return false;
	});

	submitFormBind("form.create-report-form", function(data, status, xhr, form){
		$("div.report-dialog").jqmHide();
		clearForm(form);
	});


	$("div#embedly-services").jqm({
		trigger: ".trigger-embedly-services",
		onShow: function(hash){
			var div = hash.w.children("div:first");
			if(typeof hash.w.data("loadedServices") === "undefined"){
				var html = "";
				$.getJSON('http://api.embed.ly/1/services?callback=?', function(data) {
					clearLoading();
					$.each(data, function(key, service) {
						html += '<span class="smallText">' + service.displayname + '</span> &nbsp; ';
					});
					div.html(html);
					hash.w.data("loadedServices", html);
				});
			}else if(div.children().length === 0){
				div.html(hash.w.data("loadedServices"));
//				hash.w.data("loaded", true);
			}
			hash.w.width("900px").css("margin-left", "-470px").show();
		}
	});

	$(document).on("click", ".trigger-embedly-services",  function(){
		$("div#embedly-services").jqmShow(this);
		return false;
	});

	/****************************************************
     *                    COMMENTS
     ****************************************************/

	submitFormBind("form.new-comment-form", function(data, status, xhr, form){
		var textbox = $("textarea[name='comment']", form);
		var that = $(form);
		that.closest("div.newcommentform").hide();
		that.closest("div.postbox").find("div.comments").prepend(data);
		textbox.val("");
	});

	$(document).on("click", "a.delete-comment",  function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest("div.commentbox").fadeOut("slow", function(){that.remove();});
			$.post(that.attr("href"), secdata);
		}, rusuremsg, false);
	});

	$(document).on("click", ".more-comments-btn",  function(){
		$(this).nextAll("div:first").show().end().remove();
		return false;
	});

	$(document).on("click", "a.show-comment",  function(){
		$(this).siblings("div:hidden").show().end().prev("span").andSelf().remove();
		return false;
	});

	/****************************************************
     *                    TRANSLATIONS
     ****************************************************/
	
	var validateTrans = function(form){
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {value: {required: true, notEmpty: true}},
			messages: {
				value: {required: reqmsg, notEmpty: reqmsg}
			}
		});
		return form.valid();
	};
	
	$(document).on("click", "a.delete-translation",  function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest("div.translationbox").fadeOut("slow", function(){
				that.remove();
			});
			$.post(that.attr("href"), secdata);
		}, rusuremsg, false);
	});

	$(document).on("click", "input#addcont-translation-btn",  function(){
		return validateTrans($(this).closest("form"));
	});
	$(document).on("click", "input#add-translation-btn",  function(){
		var that = $(this);
		var form = that.closest("form");
		var isValid = validateTrans(form);
		var textbox = $("textarea[name='value']", form);
		var father = that.closest("div.newtranslationform");
		if($.trim(textbox.val()) !== "" && isValid){
			father.hide();
			submitForm(form, "POST", function(data, status, xhr, form){
				if($.trim(data) !== ""){
					$("#translations").prepend(data);
				}
				clearForm(form);
			});
		}
		return false;
	});
	
	$("form.new-translation-form").find("textarea").focus();

	/****************************************************
     *                  PAGINATION
     ****************************************************/
	
	function loadMoreHandler(dis, callback){
		var that = $(dis),
			macroCode = that.siblings(".page-macro-code").text(),
			contentDiv = that.parent("div").prev("div"),
			href = that.attr("href"),
			cleanHref = href.substring(0, href.lastIndexOf("page=") + 5);
			
		that.addClass("loading");
		$.get(dis.href, {pageMacroCode: macroCode}, function(data){
			clearLoading();
			var trimmed = $.trim(data);
			if(trimmed !== ""){
				var spanBlock = trimmed.substring(trimmed.lastIndexOf("<span")),
					nextPageSpan = $(spanBlock),
					nextPage = parseInt(nextPageSpan.attr("class"), 10),
					parsedData = $(trimmed);
					
				if(nextPageSpan && !isNaN(nextPage)){
					that.attr("href", cleanHref + nextPage);
					nextPageSpan.remove();					
					if(parsedData.length === 1){
						that.hide();
					}else{
						contentDiv.append(trimmed);
						callback(contentDiv);
					}
				}
			}
		});

		return false;
	}

	$(document).on("click", "a.more-link",  function(){
		return loadMoreHandler(this, function(){});
	});

	/****************************************************
     *                       PHOTOS
     ****************************************************/
	
	if ($("#gallery").length > 0) {
		var anext = $("a.next"),
			aprev = $("a.prev");
			
		$(window).keyup(function(e){
			if(e.keyCode === 37 || e.which === 37){
				window.location = aprev.attr("href");
			}else if(e.keyCode === 39 || e.which === 39){
				window.location = anext.attr("href");
			}
		});
	}

	$(document).on("click", "a.remove-label",  function(){
		var box = $(this).closest(".labelbox");
		$.post(this.href, secdata, function(){
			clearLoading();
			box.fadeOut(function(){
				box.remove();
			});
		});
		return false;
	});

	$(document).on("click", ".image-delete",  function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest(".thumb-wrap").fadeOut(function(){
				that.remove();
			});
			$.post(that.attr("href"), secdata);
		}, rusuremsg, false);
	});

	submitFormBind("form#add-label-form", function(data, status, xhr, form){
		var id = $("form#add-label-form input[name=id]").val(),
			labelBox = $("form#add-label-form input[name=addlabel]"),
			labelBoxClass = "button-tiny",
			label = labelBox.val();
		
		labelBox.val("");	//clear box
		if($.trim(label) !== "" && $.trim(data) === "true"){
			var labelsCont = $("#labels");
			var box;
			if(label.indexOf(",") >= 0){
				var labels = label.split(","), i;
				
				for (i = 0; i<labels.length; i++) {
					var ltrim = labels[i];
					ltrim = $.trim(ltrim);
					if(ltrim !== ""){
						box = labelsCont.children(":hidden:first").clone();
						box.find("a:first").attr("href", function(){
							return this.href + ltrim;
						}).text(ltrim);
						box.find("a:last").attr("href", function(){
							return this.href + ltrim + "&id=" + id;
						});
						labelsCont.append(box.addClass(labelBoxClass).show());
					}
				}
			}else{
				var trimed = $.trim(label);
				box = labelsCont.children(":hidden:first").clone();
				box.find("a:first").attr("href", function(){
					return this.href + trimed;
				}).text(trimed);
				box.find("a:last").attr("href", function(){
					return this.href + trimed + "&id=" + id;
				});
				labelsCont.append(box.addClass(labelBoxClass).show());
			}
		}
	});

	/****************************************************
     *                     DRAWER
     ****************************************************/

	// oembed plugin init bind
	var oembedBox = $(".oembed-box"),
		oembedContainer = $("div#oembed-container"),
		pform = oembedContainer.closest("form"),
		errorbox = $(".embed-error", pform),
		oembedPreview = oembedContainer.closest(".oembed-preview");
		
	oembedBox.each(function(i){
		var that = $(this);
		$.oembed.fetchData(this.href, function(data){
			that.replaceWith(data.html);
		});
	});
	
	function clearAndCloseOembedPreview(){
		oembedContainer.html("");
		oembedContainer.removeData("oembed-data");
		pform.find("input[type='text']").val("");
		
		oembedPreview.hide();
	}

	function onEmbedClick(that, type){
		var url = $.trim($(that).prev("input").val());
		oembedPreview.show();
		$("img.ajaxwait", oembedPreview).show();
		$.oembed.fetchData(url, function(data){
			var thumb = null, bool = true;
			
			if (data.type !== "error" && !data.error_code) {
				thumb = $.oembed.getThumb(data, type);
				data.filter = null;
				if(type && type === "photo" && data){
					data.filter = "photo";
					bool = data.url.match(/(jpg|png|gif|jpeg)$/i) !== null;
				}
			}else{
				thumb = null;
				bool = false;
			}
			
			if(thumb && thumb.length > 0 && bool){
				oembedContainer.html(thumb);
				oembedContainer.data("oembed-data", data);
				errorbox.text("");
			}else{
				errorbox.text(lang["profile.drawer.embedly.notanimage"]);
				clearAndCloseOembedPreview();
			}
			
			$("img.ajaxwait", oembedPreview).hide();
		});
	}
	
	$(".addvideo").click(function(){
		onEmbedClick(this);
		return false;
	}).prev("input").focus();

	$(".addimage").click(function(){
		onEmbedClick(this, "photo");
		return false;
	}).prev("input").focus();

	$(".cancel-embed-btn").click(function(){
		clearAndCloseOembedPreview();
		return false;
	});

	$("form#embed-form").keypress(function(e){
		if(e.which === 13){
			$(".addvideo, .addimage").click();
		}
	}).submit(function(){
		var data = oembedContainer.data("oembed-data"),
			phorm = $(this);

		if(data && data !== ""){
			var params = {
				url: data.url,
				link: this.url.value,
				thumburl: data.thumbnail_url,
				title: data.title,
				provider: data.provider_name,
				description: data.description,
				height: data.height,
				width: data.width,
				type: data.type
			};

			var callbackfn = null;
			if (data.filter && data.filter === "photo") {
				callbackfn = function(html){
					var photos = $("div#photos");
					if(photos.find(".thumb-wrap").length === 0){
						photos.html(html);
					}else{
						photos.find(".thumb-wrap:first").before(html);
					}
				};
			} else {
				callbackfn = function(html){
					clearLoading();
					var drawer = $("div#drawer");
					if(drawer.children().length === 0){
						drawer.siblings().hide();
					}
					drawer.prepend(html);
				};
				
				if(data.type === "photo"){
					$(".embed-error", phorm).text(lang["profile.drawer.embedly.photosaved"]);
				}
			}

			clearAndCloseOembedPreview();
			$.post(this.action, $.extend(params, secdata), callbackfn);
		}

		return false;
	});

	$(document).on("click", "a.delvideo",  function(){
		var that = $(this);
		return areYouSure(function(){
			var parent = that.closest("div.drawerbox");
			parent.fadeOut("slow", function(){parent.remove();});
			$.post(that.attr("href"), secdata);
		}, rusuremsg, false);
	});

	/****************************************************
     *                     QUESTIONS
     ****************************************************/

	$("input.close", "form#ask-question-form").click(function(){
		$(this).closest("div").hide();
		return false;
	});

	$(".close-answer-form", "form#answer-question-form").click(function(){
		crossfadeToggle($(this).closest("form").parent("div").get(0), $(".open-answer-form").closest("div").get(0));
		return false;
	});

	$(".open-answer-form").click(function(){
		crossfadeToggle($(".close-answer-form").closest("form").parent("div").get(0), $(this).closest("div").get(0));
		return false;
	});

	$(document).on("click", "a.approve-answer, a.approve-translation", function(){
		var on = "green";
		var that = $(this);
		$(".approve-answer, .approve-translation").not(that).removeClass(on).text("3");
		
		if(that.hasClass(on)){
			that.removeClass(on);
			that.text("3");
		}else{
			that.addClass(on);
			that.text("2");
		}

		$.post(this.href, secdata);
		return false;
	});

	function markdownToHTML(text, last, converter) {
		// if there's no change to input, cancel conversion
		if (text && text !== last) {
			last = text;
		}
		// Do the conversion
		text = converter.makeHtml(text);
		return text;
	}

	function initPostEditor(index, elem){
		var that = $(elem).addClass("markedUp"),
			lastText,
			preview = that.nextAll("div.edit-preview"),
			converter = new Showdown.converter();

		that.markItUp(miu_set_markdown);

		// First, try registering for keyup events
		// (There's no harm in calling onInput() repeatedly)
		that.keyup(function(e){
			preview.html(markdownToHTML(that.val(), lastText, converter));
		});

		// In case we can't capture paste events, poll for them
		var pollingFallback = window.setInterval(function(){
			if(that.html() !== lastText){
				preview.html(markdownToHTML(that.val(), lastText, converter));
			}
		}, 1000);

		// Try registering for paste events
		that.bind("paste", function() {
			// It worked! Cancel paste polling.
			if (pollingFallback !== undefined) {
				window.clearInterval(pollingFallback);
				pollingFallback = undefined;
			}
			preview.html(markdownToHTML(that.val(), lastText, converter));
		});

		// Try registering for input events (the best solution)
		that.bind("input", function(){
			// It worked! Cancel paste polling.
			if (pollingFallback !== undefined) {
				window.clearInterval(pollingFallback);
				pollingFallback = undefined;
			}
			preview.html(markdownToHTML(that.val(), lastText, converter));
		});

		// do an initial conversion to avoid a hiccup
		preview.html(markdownToHTML(that.val(), lastText, converter));
	}

	var inputPane = $("textarea.edit-post");
	if(inputPane.length > 0){
		inputPane.each(initPostEditor);

		window.onbeforeunload = function() {
			var txtbox = $("textarea.unload-confirm");
			if(txtbox.length && txtbox.val() !== ""){
				return lang["posts.unloadconfirm"];
			}
		};

		$(document).on("click", "a.more-link",  function(){
			return loadMoreHandler(this, function(updatedContainer){
				updatedContainer.find("textarea.edit-post").not(".markedUp").each(initPostEditor);
			});
		});
	}

	var dmp = new diff_match_patch();

	function diffToHtml(diffs, oldText, newText) {
		var html = [];
		var intag = false;
		var appendMe = "";
		var done = false;
		
		function reconstruct(diffArray, what){
			var out = "", i;
			if(what === 0){
				return out;
			} 
			for (i = 0; i < diffArray.length; i++) {
				var o = diffArray[i][0];
				var t = diffArray[i][1];
				
				if(o === what || o === 0){
					out = out.concat(t);
				}				
			}
			return out;
		}

		function diffMarkup(text, op){
			var t1,t2 = "";
			switch (op) {
				case 1:t1 = '<span class="diff-ins">';t2 = '</span>';break;
				case -1:t1 = '<span class="diff-del">';t2 = '</span>';break;
				case 0:t1 = "";t2 = "";break;
			}

			var trimed = $.trim(text);
			if(trimed !== ""){
				text = t1.concat(text,t2);
			}
			return text;
		}

		//main loop over diff fragments
		var x;
		for (x = 0; x < diffs.length; x++) {
			html[x] = diffMarkup(diffs[x][1], diffs[x][0]);
			if(done){break;}
		}
		
		if($.trim(appendMe) !== ""){
			return appendMe;
		}else{
			return html.join('');
		}
	}

	$(".newrev").html(function(index, html){
		var newText = html;
		var dis = $(this);
		var oldText = dis.next(".oldrev").html();
		var ds = newText;

		if(newText && oldText){
//			var dd = diff_linesToHtml(oldText, newText);
//			var d = dmp.diff_main(dd[0], dd[1], false);

			var d = dmp.diff_main(oldText, newText);
			dmp.diff_cleanupSemantic(d);
			ds = diffToHtml(d, oldText, newText);
		}
		
		return ds;
	});
	

	var questionFilterForm = $("form#filter-questions-form");
	questionFilterForm.find("select").change(function(){
		questionFilterForm.submit();
	}).end().find(":submit").hide();


	/****************************************************
     *                      OPENID
     ****************************************************/
	function setInputSelection(input, startPos, endPos) {
        if (typeof input.selectionStart != "undefined") {
            input.selectionStart = startPos;
            input.selectionEnd = endPos;
        } else if (document.selection && document.selection.createRange) {
            // IE branch
            input.focus();
            input.select();
            var range = document.selection.createRange();
            range.collapse(true);
            range.moveEnd("character", endPos);
            range.moveStart("character", startPos);
            range.select();
        }
    }
	
	var openid_identifier = $("#openid_identifier");
    
	if(openid_identifier.length > 0){
		var cookie_name = 'openid_url',
		cookie_expires = 30,	// 30 days.
		openid_form = openid_identifier.parents("form"),
		openid_url = readCookie(cookie_name);

		if(openid_form.attr("id") !== "add-openid-form"){
			if($.trim(openid_url) !== "") {
				openid_identifier.val(openid_url);
				openid_identifier.show();
			}
		}
		openid_form.submit(function(){
			var value2 = openid_identifier.val();
			createCookie(cookie_name, value2, cookie_expires);
			return true;
		});		
		$("a.openidbtn-small, a.openidbtn-large").click(function(){
			var that = $(this);
			var url = that.attr("href");
			openid_identifier.val(url);			
			if (url.indexOf("(") >= 0 && url.indexOf(")") >= 0) {
				openid_identifier.show().focus();
				setInputSelection(openid_identifier.get(0), url.indexOf("("), url.indexOf(")") + 1);
			} else {
				openid_identifier.hide();
				openid_form.submit();
			}
			return false;
		});
	}
	
	/************************************************************************
     *                           JS VALIDATION
     ************************************************************************/

	/********* SIGNUP FORM ************/
	$(document).on("click", "input#signup-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			onsubmit: true,
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				fullname: {required: true, minlength: 4},
                email: {required: true, email: true}
			},
			messages: {
				fullname: {
					required: reqmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
				},
				email: {
					required: reqmsg, email: emailmsg
				}
			}			
		});
		
        return form.valid();
	});

	/********* CREATE SCHOOL FORM ************/	
	$(document).on("click", "input#createschool",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				name: "required", type: "required", location: "required", address: "required"
			},
			messages: {
				name: reqmsg, type: reqmsg, location: reqmsg, address: reqmsg
			}
		});
        return form.valid();
	});

	/********* CHANGE EMAIL FORM ************/
	$(document).on("click", "input#change-email-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				email: {required: true, email: true}
			},
			messages: {
				email: {
					required: reqmsg, email: emailmsg
				}
			}
		});
        return form.valid();
	});

	/********* CREATE CLASS FORM ************/
	$(document).on("click", "input#createclass",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				gradyear: {required: true, digits: true},
				schoolid: "required",
				identifier: {required: true, minlength: 4}
			},
			messages: {
				gradyear: {
					required: reqmsg,
					digits: digitsmsg
				},
				schoolid: reqmsg,
				identifier: {
					required: reqmsg,
					minlength: jQuery.format(minlenmsg)
				}
			}
		});
		return form.valid();
	});

	/********* ADD CLASSMATES FORM ************/
	$(document).on("click", "input#addclassmates-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
                fullname: {required: true, minlength: 3, maxlength: 255},
				email: {
					required: false, email: true
				}
			},
			messages: {
				email: {
					email: emailmsg
				},
				fullname: {
					required: reqmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
				}
			}
		});
        return form.valid();
	});
	/********* ASK QUESTION FORM ************/
	var maxTags = 6, maxTagsS = "6";
	$.validator.addMethod("tags", function(value, elem){
		return this.optional(elem) || value.split(",").length < maxTags;
	});
	$(document).on("click", "input#ask-btn, input.post-edit-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				title: {required: true, minlength: 10, maxlength: 255},
				body: {required: true, minlength: 10, maxlength: 20000},
				tags: {required: true, tags: true},
				parentid: "required"
			},
			messages: {
				title: {required: reqmsg,
					minlength: jQuery.format(minlenmsg),
					maxlength: jQuery.format(maxlenmsg)
				},
				body: {required: reqmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
				},
				tags: {required: reqmsg,
					tags: $.validator.format(tagsmsg, maxTagsS)
				},
				parentid: reqmsg
			}
		});
		return form.valid();
	});

	/********* ANSWER QUESTION FORM ************/

	$(document).on("click", "input#answer-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				body: {required: true, minlength: 15, maxlength: 20000}
			},
			messages: {
				body: {
					required: reqmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
				}
			}
		});
		window.onbeforeunload = null;
		return form.valid();
	});

	/********* NEW MESSAGE FORM ************/

	$(document).on("click", "input#sendmessage-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn2,
			rules: {
				body: {required: true},
				to: {required: true}
			},
			messages: {body: reqmsg, to: reqmsg}
		});
		return form.valid();
	});

	/********* SETTINGS FORMS ************/

	$(document).on("click", "input.import-media-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				username: {required: true, maxlength: 150}
			},
			messages: {
				username: {required: reqmsg,
					maxlength: jQuery.format(maxlenmsg)
				}
			}
		});
		return form.valid();
	});

	var maxFavTags = 50, maxFavTagsS = "50";
	$.validator.addMethod("tags2", function(value, elem){
		return this.optional(elem) || value.split(",").length < maxFavTags;
	});
	$(document).on("click", "input#add-favtag-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				favtags: {tags2: true}
			},
			messages: {
				favtags: {
					tags2: $.validator.format(tagsmsg, maxFavTagsS)
				}
			}
		});
		return form.valid();
	});

	/********* REPORT SOLUTION FORM ************/
	$(document).on("click", "input.report-solution-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
                solution: {required: true, minlength:5, maxlength: 255}
			},
			messages: {
				solution: {
					required: reqmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
				}
			}
		});
        return form.valid();
	});
	/********* NEW COMMENT FORM ************/
	$(document).on("click", "input.new-comment-btn",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				comment: {required: true, maxlength: 500}
			},
			messages: {
				comment: {
					required: reqmsg,
					maxlength: jQuery.format(maxlenmsg)
				}
			}
		});
		return form.valid();
	});

	/********* NEW TRANSLATION FORM ************/
	$.validator.addMethod("notEmpty", function(value, elem){
		var datval = $(elem).data("value");
		return this.optional(elem) || (datval !== "" && datval !== value);
	});	
	
	/********* CREATE GROUP FORM ************/
	$(document).on("click", "input#creategroup",  function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				name: {required: true, minlength: 4}
			},
			messages: {
				name: {
					required: reqmsg,
					minlength: jQuery.format(minlenmsg)
				}
			}
		});
		return form.valid();
	});

});//end of scoold script

