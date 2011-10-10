/*
 * scoold.js - unobtrusive javascript for scoold.com
 * author: Alexander Bogdanovski
 * (cc) 2008-2009
 */
/*global window: false, jQuery: false, $: false, lang: false */
$(function(){
	var ajaxpath = window.location.pathname;
	var infobox = $("div.infostrip");
	var hideMsgBoxAfter = 10 * 1000; //10 sec

	/**************************
	 *  Google Maps API v3.3
	 **************************/
	var mapCanvas = $("div#map-canvas");
	if(mapCanvas.length){
		var geocoder = new google.maps.Geocoder();
		var myLatlng = new google.maps.LatLng(42.6975, 23.3241);
		var marker = new google.maps.Marker({});
		var locbox = $("input.locationbox");
		var locality = "";
		var sublocality = "";
		var country = "";
		
		var map = new google.maps.Map(mapCanvas.get(0), {
			zoom: 3,
			center: myLatlng,
			mapTypeId: google.maps.MapTypeId.ROADMAP,
			mapTypeControl: false,
			streetViewControl: false
		});
		
		google.maps.event.addListener(map, 'click', function(event) {
			map.setCenter(event.latLng);
			marker.setPosition(event.latLng);
			marker.setMap(map);
			
			geocoder.geocode({location: event.latLng}, function(results, status){
				if (status !== google.maps.GeocoderStatus.OK) {
					locbox.val("");
				} else {
					if(results.length && results.length > 0){
						for (var h = 0; h < results.length; h++) {
							var arr = results[h].address_components;
							for (var i = 0; i < arr.length; i++) {
								var type = $.trim(arr[i].types[0]);
								var name = arr[i].long_name;
								if(type === "country"){
									country = name;
								}else if(type === "locality"){
									locality = name;
								}else if(type === "sublocality"){
									sublocality = name;
								}
							}
							if(country !== "" && locality !== "" && sublocality !== ""){
								break;
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

	/****************************************************
     *            FACEBOOK API FUNCTIONS
     ****************************************************/

	var fbauthurl = "fbconnect_auth";
	var attachfburl = "attach_facebook"

	if($("#fb-login").length){
		FB.Event.subscribe('auth.login', function(response) {
			if(response.session){
				window.location = fbauthurl;
			}
		});
	}

	$("#fb-login-btn").click(function(){
		FB.getLoginStatus(function(response) {
			if (response.session) {
				window.location = fbauthurl;
				return false;
			}
		});
		return true;
	});

	$("#fb-attach-btn").click(function(){
		FB.getLoginStatus(function(response) {
			if (response.session) {
				window.location = attachfburl;
				return false;
			}
		});
		return true;
	});

	var fbPicture = $("#fb-picture");
	var fbName = $("#fb-name");
	if(fbPicture.length || fbName.length){
		FB.getLoginStatus(function(response) {
			if (response.session) {
				FB.api({
					method: 'fql.query',
					query: 'SELECT name, pic_small, url FROM profile WHERE id=' + response.session.uid
				}, function(response) {
					var user = response[0];
					fbPicture.html('<img src="' + user.pic_small + '" alt="'+user.name+'"/>');
					fbName.html('<a href="' + user.url + '" class="extlink">' + user.name + '</a>');
					$('input.fb-name-box').val(user.name);
				});
			}
		});
	}

	/****************************************************
     *					IMPORT PHOTOS
     ****************************************************/

//	$("#fb-getphotos").click(function(){
//		var that = $(this);
//		FB.login(function(response) {
//			if (response.session) {
//				if (response.perms) {
//					// user is logged in and granted some permissions.
//					// perms is a comma separated list of granted permissions
//					that.next(":hidden").show();
//					that.hide();
//					$.post(ajaxpath, {importphotos: "facebook",
//						authtoken: response.session.access_token}, function(){
//						//done. what now?
//						showSuccessBox(lang['success']);
//					});
//				}
//			} else {
//				// user is not logged in
//				// NOOP
//			}
//		}, {perms:'user_photos,user_photo_video_tags'});
//
//		return false;
//	});


//	$("form#flickr-import-form, form#picasa-import-form").live("submit", function(){
//		$(this).find("input[type=text]").val("");
//		$.post(this.action, $(this).serialize(), function(data){
//
//			showSuccessBox(lang['success']);
//		});
//		return false;
//	});

	// custom form submit

//	submitFormUsingGetBind("form#flickr-import-form, form#picasa-import-form", function(data){
//		//todo: show progress to user => true = success, false = fail
//	});


	/****************************************************
     *					MISC FUNCTIONS
     ****************************************************/
		
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

	function readCookie(name) {
		var nameEQ = name + "=";
		var ca = document.cookie.split(';');
		for(var i=0;i < ca.length;i++) {
			var c = ca[i];
            while (c.charAt(0) === ' '){c = c.substring(1,c.length);}
            if (c.indexOf(nameEQ) === 0){return c.substring(nameEQ.length,c.length);}
			}
		return null;
	}
	
	function eraseCookie(name) {
		createCookie(name, "", 0);
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

	function submitFormBind(formname, callbackfn){
		return $(formname).live("submit", function(){
			submitForm(this, "POST", callbackfn);
			return false;
		});
	}

	function submitFormUsingGetBind(formname, callbackfn){
		return $(formname).live("submit", function(){
			submitForm(this, "GET", callbackfn);
			return false;
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

	function autocompleteBind(elem, params){
		$(elem).attr("autocomplete", "off");
		$(elem).autocomplete(ajaxpath, {
			minChars: 3,
			width: this.width,
			matchContains: true,
			highlight: false,
            extraParams: params,	
			formatItem: function(row) {
				return row[0] + "<br/>" + row[1];
			}
		});
		$(elem).result(function(event, data, formatted) {
			$(this).next("input:hidden").val(data[2]);
		});
		//clear hidden fields on keypress except enter
		$(elem).keypress(function(e){
			if(e.which !== 13 || e.keyCode !== 13){
				$(this).next("input:hidden").val("");
			}
		});
	}

	function autocompleteTagBind(elem, params){
		$(elem).attr("autocomplete", "off");
		$(elem).autocomplete(ajaxpath, {
			minChars: 2,
			width: this.width,
			matchContains: true,
			multiple: true,
			highlight: false,
            extraParams: params,
			scroll: true,
			formatItem: function(row) {
				return row[0];
			}
		});
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
		if(typeof contacts === "undefined"){
			contacts = [];
		}
		$(elem).attr("autocomplete", "off");
		$(elem).autocomplete(contacts, {
			minChars: 3,
			width: this.width,
			matchContains: true,
			multiple: true,
			highlight: false,
            extraParams: params,
			scroll: true,
			formatItem: function(row) {
				return row.fullname;
			}
		});
		$(elem).result(function(event, data, formatted) {
			var hidden = $(this).nextAll("input:hidden");
			var newHidden = hidden.clone();
			newHidden.val(data.uuid);
			hidden.after(newHidden);
		});

		var clear = function(elem){
			$(elem).nextAll("input:hidden").not(":first").remove();
			$(elem).val("");
		};

		//clear hidden fields on keypress except enter
		$(elem).keyup(function(e){
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
	
	function showInfoBox(msg){
		showMsgBox(msg, "infoBox", hideMsgBoxAfter);
	}
	function showErrorBox(msg){
		showMsgBox(msg, "errorBox", 0);
	}
	function showSuccessBox(msg){
		showMsgBox(msg, "successBox", hideMsgBoxAfter);
	}

	function showMsgBox(msg, clazz, hideafter){
		infobox.removeClass("infoBox errorBox successBox")
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
	
	function clearLoading(){
		$(".loading").removeClass("loading");
		$("img.ajaxwait").hide();
	}

	/****************************************************
     *					GLOBAL BINDINGS
     ****************************************************/

	$(".rusure").live("click", function(){
		return areYouSure($.noop, rusuremsg, true);
	});
	
	$(".editlink").live("click", function(){
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
	
	$(".canceledit").live("click", function(){
		var editbox = $(this).closest(".editbox").get(0);
		var viewbox = $(editbox).siblings(".viewbox").get(0);	
		crossfadeToggle(viewbox, editbox);
		return false;
	});

	//target=_blank is not valid XHTML
	$("a.extlink").live("click", function(){
		$(this).attr("target", "_blank");
		return true;
	});

	$("a.votelink").live("click", function(){
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
	$(".infostrip, .messagebox").live("click", function(){
		$(this).hide();
		return false;
	});

	// show ajax indicator when submit is pressed
	$("input[type=submit]").live("click", function(){
		$(this).addClass("loading");
//		$("img.ajaxwait", $(this).parent()).show();
		return true;
	});
	
	$("body").ajaxSuccess(function() {
		clearLoading();
		console.log("DONE!");
	});
		
	$(":checked").next("label").addClass("bold");

	$("input[type=radio]").click(function(){
		$(this).next("label").addClass("bold")
		.siblings().removeClass("bold");
	});

	var color1 = $("body").css("color");
	var color2 = "#AAAAAA";
	$(".hintbox").focus(function(){
		var t = $(this);
		if(t.data("val") === ""){t.data("val", $(this).val());}
		if(t.val() === t.data("val")){t.val("");}
		t.css("color", color1);
	}).blur(function(){
		var t = $(this);
		if($.trim(t.val()) === ""){t.val(t.data("val"));
		t.css("color", color2);}
	}).css("color", color2).data("val", $(this).val());

	$("a#search-icon").click(function(){
		$(this).closest("form").submit();
		return false;
	});

	$("a.next-div-toggle").live("click", function(e){
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

	$("a.next-span-toggle").live("click", function(){
		$(this).nextAll("span:first").toggle();
		return false;
	});
	
	/****************************************************
     *                    REPORTS
     ****************************************************/

	$("a.close-report").click(function(){
		var dis = $(this);
		dis.closest(".reportbox").find(".report-solution-box").toggle();
		return false;
	});

	submitFormBind("form.report-solution-form", function(data, status, xhr, form){
		var dis = $(form);
		var parent = dis.closest(".reportbox");
		var div = dis.parent("div").hide();
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
			$.post(that.attr("href"));
			that.closest("tr").fadeOut(function(){
				that.remove();
			}).siblings("tr").find(".delopenid").hide();
		 }, rusuremsg, false);
	 });

	/****************************************************
     *                    MESSAGES
     ****************************************************/

	$(".delete-message").click(function(){
		$.post(this.href);
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
	})

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
	
	$("a.addfriend").live("click", function(){
		$.post(this.href);
		showInfoBox(lang["profile.contacts.added"]);
		$(this).fadeOut();
		return false;
	});

	//delete friend link
	$("a.delfriend").live("click", function(){
		var that = $(this);
		return areYouSure(function(){
			that.fadeOut();
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	/****************************************************
     *                    EDITABLES
     ****************************************************/
		
	var editable_settings = {
			submit: lang.save,
			tooltip: lang.clickedit,
			cssclass: "clickedit"
		};

	function editableBind(elem, param, opts){
		if(opts){
			$.extend(true, editable_settings, opts);
		}

		var params = {};
		var $elem = $(elem);			
		$elem.editable(function(value, settings) {
			var $text = $elem.data("value");
			params[param] = value;
			if(value.length >= 3){
				$.post(ajaxpath, params);
				$text = $elem.text(value).text();
				$elem.data("value", $text);
			}
			return $text;}, editable_settings
		).data("value", $elem.text());
	}

	editableBind("#fullname.editable", "fullname");
	editableBind("#mystatus.editable", "status");
	editableBind("#schoolname.editable", "name");
	editableBind("#classname.editable", "identifier");
	editableBind("#questiontitle.editable", "title");

//	var editable_settings2 = {};
//	$.extend(true, editable_settings2, editable_settings,
//		{data: {"alumnus":lang.alumnus, "teacher":lang.teacher, "student":lang.student}, type: 'select'});
//
//	$("#usertype.editable").editable(function(value, settings){
//		var $that = $(this);
//		$.post(ajaxpath, {type: value});
//		return $that.text(lang[value.toLowerCase()]).text();}, editable_settings2
//	);

	var editable_settings3 = {};
	$.extend(true, editable_settings3, editable_settings, {type: "textarea"});
	$(".drawer-description.editable").editable(function(value, settings){
		var $that = $(this);
		$.post(ajaxpath+"?update-description=true", {description: value, id: this.id});
		return $that.text(value).text();}, editable_settings3
	);

	var editablebox = $(".editable");
	var editableBorder = editablebox.css("border");
	editablebox.click(function(){
		editablebox.css("border", "3px solid white");
//		editablebox.find("input, textarea, select").addClass("nicebox");
	}).bind("mouseleave", function(){
		editablebox.css("border", editableBorder);
	});


	/****************************************************
     *               CONTACT DETAILS
     ****************************************************/

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

	$(".remove-contact").live("click", function(){
		$(this).closest("tr").remove();
		return false;
	});

	$("select#detail-type").change(function(){
		$("#detail-value").val(getDetailValue(this.value));
	}).change();

	function getDetailValue(typeRaw){
		var val = "";
		if (typeRaw === "WEBSITE") {
			val = "http://";
		}else if(typeRaw === "FACEBOOK"){
			val = "http://facebook.com/";
		} 
		return val;
	}
    
	/****************************************************
     *                    AUTOCOMPLETE
     ****************************************************/
    
	autocompleteBind("input.locationbox", {find: "locations"});
	autocompleteContactBind("input.contactnamebox", {find: "contacts"});
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
     *						 CHAT
	 ****************************************************/

	var chatbox = $("#chat");
	if (chatbox.length > 0) {
//		var chatServerHost = "http://localhost:8001/chat";
		var chatServerHost = "http://a1x.no.de:8001/chat";
		var channelname = chatbox.children("#channel").text();
		var nickname = chatbox.children("#nickname").text();
		var userid = chatbox.children("#userid").text();
		
		//node chat client init
		chatbox.nodechat(nickname, channelname, userid, {
			serverUrl: chatServerHost,
			userJoinText: lang["class.chat.userin"],
			userLeaveText: lang["class.chat.userout"],
			connectionErrorText: lang["class.chat.connection.error"],
			pollingErrorText: lang["class.chat.polling.error"],
			reconnectErrorText: lang["class.chat.reconnect.error"]
		}).find("a#chat-send-msg").click(function(){
			$(this).closest("form").submit();
			return false;
		});
	}

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
			div.find(".jqmClose").live("click", function(){
				hash.w.jqmHide();
				return false;
			});
			div.show();
		}
	});

	$(".trigger-report").live("click", function(){
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

	$(".trigger-embedly-services").live("click", function(){
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

	$("a.delete-comment").live("click", function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest("div.commentbox").fadeOut("slow", function(){that.remove();});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$(".more-comments-btn").live("click", function(){
		$(this).nextAll("div:first").show().end().remove();
		return false;
	});

	$("a.show-comment").live("click", function(){
		$(this).nextAll("div:hidden").show().end().prev("span").andSelf().remove();
		return false;
	});

	/****************************************************
     *                    TRANSLATIONS
     ****************************************************/

	$("a.delete-translation").live("click", function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest("div.translationbox").fadeOut("slow", function(){
				that.remove();
			});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$("input#addcont-translation-btn").live("click", function(){
		return validateTrans($(this).closest("form"));
	});
	$("input#add-translation-btn").live("click", function(){
		var that = $(this);
		var form = that.closest("form");
		var isValid = validateTrans(form);
		var textbox = $("textarea[name='value']", form);
		var father = that.closest("div.newtranslationform");
		if($.trim(textbox.val()) !== "" && isValid){
			father.hide();
			submitForm(form, "POST", function(data, status, xhr, form){
				if($.trim(data) !== ""){
					father.nextAll("div.translations").prepend(data);
				}
				clearForm(form);
			});
		}
		return false;
	});

	$("a.reset-translation").click(function(){
		var that = $(this);
		return areYouSure(function(){
			that.fadeOut("slow", function(){that.remove();});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	/****************************************************
     *                       PAGES
     ****************************************************/

	function loadMoreHandler(dis, callback){
		var that = $(dis);
		var contentDiv = that.parent("div").prev("div");
		var href = that.attr("href");
		var cleanHref = href.substring(0, href.lastIndexOf("page=") + 5);
//		var page = parseInt(href.substring(href.lastIndexOf("page=") + 5));
//		that.find("img:hidden").show();
		that.addClass("loading");
		$.get(dis.href, function(data){
			clearLoading();
			var trimmed = $.trim(data);
			if(trimmed !== ""){
				var spanBlock = data.substring(data.lastIndexOf("<span"));
				var nextPageSpan = $(spanBlock);
				var nextPage = parseInt(nextPageSpan.attr("class"), 10);
				if(nextPageSpan && !isNaN(nextPage)){
					that.attr("href", cleanHref + nextPage);
					nextPageSpan.remove();
					var lastDataHash = that.data("lastDataHash");
					var lastNextPage = that.data("lastNextPage");
					var dataHash = simpleHash(data);

					if(lastNextPage === nextPage || dataHash === lastDataHash){
						that.hide();
						that.removeData("lastDataHash");
						that.removeData("lastNextPage");
					}else{
						contentDiv.append(data);
						that.data("lastDataHash", dataHash);
						that.data("lastNextPage", nextPage);
						callback(contentDiv);
					}
				}
			}
		});

		return false;
	}

	$("a.more-link").live('click', function(){
		return loadMoreHandler(this, $.noop());
	});

	function simpleHash(s) {
		var i, hash = 0;
		for (i = 0; i < s.length; i++) {
			hash += (s[i].charCodeAt() * (i+1));
		}
		return Math.abs(hash);
	}

	/****************************************************
     *                       PHOTOS
     ****************************************************/
	//check if global variables exist
	if((typeof totalMediaCount !== "undefined") &&
		(typeof galleryUri !== "undefined") &&
		(typeof imageDataObject !== "undefined") &&
		(typeof galleryLabel !== "undefined")){

		// Initialize Advanced Galleriffic Gallery
		var gallery = $('#gallery').galleriffic({
			delay:                     2500,
			imageDataInit:			   imageDataObject.media,
			galleryUri:				   galleryUri,
			totalCount:				   totalMediaCount,
			label:					   galleryLabel,
			commentProfileLinkSel:	   'a.profile-link',
			commentBoxSel:			   '.commentbox',
			commentTimestampSel:	   '.comment-timestamp',
			commentTextSel:			   '.comment-text',
			commentFormSel:			   '.new-comment-form',
			commentsContainerSel:	   '.comments',
			reportLinkSel:			   'a.trigger-report',
			deleteCommentSel:		   'a.delete-comment',
			imageContainerSel:         '#slideshow',
			controlsContainerSel:      '#controls',
			captionContainerSel:       '#caption',
			labelsContainerSel:		   '#labels',
			addLabelFormSel:		   '#add-label-form',
			loadingContainerSel:       '#loading',
			slideshowToggleSel:		   '.ss-controls a',
			pageNavigationSel:		   '#pagenav',
			titleSel:				   '.image-title',
			captionSel:				   '.image-caption',
			originalLinkSel:		   '.image-original',
			voteboxSel:				   '.votebox',
			upvoteSel:				   '.upvote',
			downvoteSel:			   '.downvote',
			voteLinkSel:			   '.votelink',
			votecountSel:			   '.votecount',
			prevLinkSel:			   'a.prev',
			nextLinkSel:			   'a.next',
			prevPageSel:			   'a.prevpage',
			nextPageSel:			   'a.nextpage',
			playLinkText:              'Play Slideshow',
			pauseLinkText:             'Pause Slideshow',
			enableHistory:             true,
			autoStart:                 false
		});

		// PageLoad function
		// This function is called when:
		// 1. after calling $.historyInit();
		// 2. after calling $.historyLoad();
		// 3. after pushing "Go Back" button of a browser
		var pl = function pageload(hash) {
			// alert("pageload: " + hash);
			// hash doesn't contain the first # character.
			if(hash) {
				$.galleriffic.go2(gallery, hash);
			} else {
				$.galleriffic.go2(gallery, 0);
			}

		};
		// Initialize history plugin.
		// The callback is called at once by present location.hash.
		$.history.init(pl) //, window.location.pathname);

		// set onlick event for buttons using the jQuery 1.3 live method
		$("a[rel='history']").live('click', function() {
			var hash = this.href;
			hash = hash.replace(/^.*#/, '');
			// moves to a new page.
			// pageload is called at once.
			// hash don't contain "#", "?"
			$.history.load(hash);

			return false;
		});

		$(window).keyup(function(e){
			if(e.keyCode === 37 || e.which === 37){
				$("a.prev").click();
			}else if(e.keyCode === 39 || e.which === 39){
				$("a.next").click();
			}
		});

	}

	$("a.remove-label").live("click", function(){
		var box = $(this).closest(".labelbox");
		$.post(this.href, function(){
			clearLoading();
			box.fadeOut(function(){
				box.remove();
			});
		});
		return false;
	});

	$(".image-delete").live("click", function(){
		var that = $(this);
		return areYouSure(function(){
			that.closest(".thumb-wrap").fadeOut(function(){
				that.remove();
			});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	submitFormUsingGetBind("form#add-label-form", function(data, status, xhr, form){
		var uuid = $("form#add-label-form input[name=id]").val();
		var labelBox = $("form#add-label-form input[name=addlabel]");
		var label = labelBox.val();
		
		labelBox.val("");	//clear box
		if($.trim(label) !== "" && $.trim(data) === "true"){
			var labelsCont = $("#labels");
			var box;
			if(label.indexOf(",") >= 0){
				var labels = label.split(",");
				for (var i=0; i<labels.length; i++) {
					var ltrim = labels[i];
					ltrim = $.trim(ltrim);
					if(ltrim === ""){continue;}
					box = labelsCont.children(":hidden:first").clone();
					box.find("a:first").attr("href", function(){
						return this.href + ltrim;
					}).text(ltrim);
					box.find("a:last").attr("href", function(){
						return this.href + ltrim + "&uuid=" + uuid;
					});
					labelsCont.append(box.show());
				}
			}else{
				var trimed = $.trim(label);
				box = labelsCont.children(":hidden:first").clone();
				box.find("a:first").attr("href", function(){
					return this.href + trimed;
				}).text(trimed);
				box.find("a:last").attr("href", function(){
					return this.href + trimed + "&uuid=" + uuid;
				});
				labelsCont.append(box.show());
			}
		}
	});

	if(typeof allLabels !== "undefined"){
		$("input.addlabelbox").autocomplete(allLabels, {multiple: true, autoFill: true});
	}


	/****************************************************
     *                     DRAWER
     ****************************************************/

	// oembed plugin init bind
	$(".oembed-box").live("click", function(){
		var that = $(this);
		$.oembed.fetchData(this.href, function(data){
			that.replaceWith(data.html);
		});
		return false;
	});

	$(".addvideo").click(function(){
		onEmbedClick(this);
		return false;
	}).prev("input").focus();

	$(".addimage").click(function(){
		onEmbedClick(this, "photo");
		return false;
	}).prev("input").focus();

	function onEmbedClick(that, type){
		var container = $("div#oembed-container");
		var pform = container.closest("form");
		var errorbox = $(".embed-error", pform);
		var url = $.trim($(that).prev("input").val());

		container.closest(".oembed-preview").show();
		$.oembed.fetchData(url, function(data){
			if (data.type !== "error" && !data.error_code) {
				var thumb = $.oembed.getThumb(data, type);
				var bool = true;
				data.filter = null;
				if(type && type === "photo" && data){
					data.filter = "photo";
					bool = data.url.match(/(jpg|png|gif|jpeg)$/i) !== null;
				}

				if(thumb && thumb.length > 0 && bool){
					container.html(thumb);
					container.data("oembed-data", data);
					errorbox.text("");
				}else{
					container.removeData("oembed-data");
					pform.find("input[type='text']").val("");
					errorbox.text(lang["profile.drawer.embedly.notanimage"]);
				}
			}else{
				container.removeData("oembed-data");
				errorbox.text(lang.epicfail);
				pform.find("input[type='text']").val("");
			}
		});
	}

	$(".cancel-embed-btn").click(function(){
		$(this).closest(".oembed-preview").hide();
		return false;
	});

	$("form#embed-form").keypress(function(e){
		if(e.which === 13){
			$(".addvideo, .addimage").click();
		}
	}).submit(function(){
		var container = $("div#oembed-container");
		var data = container.data("oembed-data");
		var phorm = $(this);

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

			container.html("").data("oembed-data", "");  //clear
			container.closest(".oembed-preview").hide(); //hide buttons
			phorm.find("input[type='text']").val("");
			$.post(this.action, params, callbackfn);
		}

		return false;
	});

	$("a.delvideo").live("click", function(){
		var that = $(this);
		return areYouSure(function(){
			var parent = that.closest("div.drawerbox");
			parent.fadeOut("slow", function(){parent.remove();});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	/****************************************************
     *                     QUESTIONS
     ****************************************************/

	$("input.close", "form#ask-question-form").click(function(){
		$(this).closest("div").hide();
		return false;
	});

	$("input.close-toggle", "form#answer-question-form").click(function(){
		crossfadeToggle($(this).closest("div").get(0), $("div.close-toggle").get(0));
		return false;
	});

	$(".answerbtn", "div.close-toggle").click(function(){
		var formDiv = $(this).closest("div").prev("div");
		var thisDiv = $(this).closest("div");
		crossfadeToggle(thisDiv.get(0), formDiv.get(0));
		return false;
	});

	$("a.accept-answer, a.approve-translation").live("click", function(){
		var on = "green";
		var that = $(this);
		$("a.accept-answer, a.approve-translation").removeClass(on).text("3");
		
		if(that.hasClass(on)){
			that.removeClass(on);
			that.text("3");
		}else{
			that.addClass(on);
			that.text("2");
		}

		$.post(this.href);
		return false;
	});

	function initPostEditor(index, elem){
		var that = $(elem).addClass("markedUp");
		var lastText;
		var preview = that.nextAll("div.edit-preview");
		var converter = new Showdown.converter();

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

		$("a.more-link").die('click').live("click", function(){
			return loadMoreHandler(this, function(updatedContainer){
				updatedContainer.find("textarea.edit-post").not(".markedUp").each(initPostEditor);
			});
		});
	}


	function markdownToHTML(text, last, converter) {
		// if there's no change to input, cancel conversion
		if (text && text !== last) {
			last = text;
		}
		// Do the conversion
		text = converter.makeHtml(text);
		return text;
	}

	var dmp = new diff_match_patch();

	function diffToHtml(diffs, oldText, newText) {
		var html = [];
		var intag = false;
		var appendMe = "";
		var done = false;
		
		function reconstruct(diffArray, what){
			var out = "";
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

//			console.log(">>>> "+x+". "+op+", "+intag+" >> ", text);

			var trimed = $.trim(text);
			if(trimed !== ""){
				//CASE 1; <tag> bla bla </tag>
				if(text.indexOf("<") >= 0 && text.indexOf(">") >= 0 ){
					var theTag = $(trimed);
					//SUB CASE 1.1: <tag> bla </tag> <tag2> bla
					if(trimed.charAt(trimed.length -1) !== '>' || theTag.text() === ""){
						//text before <
						if(text.indexOf("<") < text.indexOf(">")){
							text = text.replace(/^([^>]*)</, t1.concat("$1",t2,"<"));
						}
						//text between > ... <
						text = text.replace(/>([^<>]*)</g, ">".concat(t1,"$1",t2,"<"));
						//text after >
						if(text.lastIndexOf(">") > text.lastIndexOf("<")){
							text = text.replace(/>([^<]*)$/, ">".concat(t1,"$1",t2));
						}

						if(text.lastIndexOf(">") > text.lastIndexOf("<")){
							intag = false;
						}else{
							intag = true;
						}

//						console.log("da ima problem>> ", text);
					}else{
						text = t1.concat(text, t2);
//						console.log("a tag>> ", text);
//						intag = false;
					}
//					intag = false;
				// CASE 2: <tag bla bla="blah"
				}else if((text.indexOf("<") >= 0 && text.indexOf(">") < 0) ||
					(text.indexOf(">") >= 0 && text.indexOf("<") < 0)){

//					console.log("super> "+intag);
					if (!done) {
						appendMe += " "+diffMarkup(reconstruct(diffs, -1), -1);
						appendMe += " "+diffMarkup(reconstruct(diffs, 1), 1);
						done = true;
					}
					text = "";
//					console.log("new code>> ", text);

//					text = text.replace(/(.*)</, t1.concat("$1",t2,"<"));
//					console.log("tag open>> ", text);
//					intag = true;
//				//CASE 3: bla/> bla bla
//				}else if( text.indexOf(">") >= 0 && text.indexOf("<") < 0 ){
//					if(intag !== false){
//						text = text.replace(/>(.*)/, ">".concat(t1,"$1",t2));
//					}else{
//						//SUB CASE 3.1: blabla>text (OVERLAPPING!)
//						appendMe += " "+diffMarkup(reconstruct(diffs, op), op);
//						text = "";
//					}
//					console.log("tag closed>> ", text);
//					intag = false;
				//CASE 4: bla bla bla.
				}else if(text.indexOf(">") < 0 && text.indexOf("<") < 0){
					if(intag === false){
						text = t1.concat(text,t2);
//						console.log("a string>> ", text);
					}
				}
			}
			return text;
		}

		//main loop over diff fragments
		for (var x = 0; x < diffs.length; x++) {
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
		var oldText = $(this).next(".oldrev").html();
		var ds = newText;

		if(newText && oldText){
//			var dd = diff_linesToHtml(oldText, newText);
//			console.log(dd[1]);
//			var d = dmp.diff_main(dd[0], dd[1], false);

			var d = dmp.diff_main(oldText, newText);
			dmp.diff_cleanupSemantic(d);
			ds = diffToHtml(d, oldText, newText);
		}

//		uncomment to show only changes
//		if(ds === newText){	$(this).hide();	}

		return ds;
	});

	var questionFilterForm = $("form#filter-questions-form");
	questionFilterForm.find("select").change(function(){
		questionFilterForm.submit();
	}).end().find(":submit").hide();


	/****************************************************
     *                      OPENID
     ****************************************************/
	//OPENID VARS
	var openid_input = $("#openid_identifier", $("fieldset"));
    
	if(openid_input.length > 0){
		var cookie_name2 = 'openid_url',
		openid_username = $("#openid-username"),
		openid_selectclass = "openidbtn-selected",
		cookie_expires = 6*30,	// 6 months.
		openid_form = openid_input.parents("form"),
		openid_url = readCookie(cookie_name2);

		if(openid_form.attr("id") !== "add-openid-form"){
			if(openid_url) {
				openid_input.val(openid_url);
			}
		}
		openid_form.submit(function(){
			var value2 = openid_input.val();
			createCookie(cookie_name2, value2, cookie_expires);
			return true;
		});		
		openid_username.keypress(function(e){
			var url = openid_url;
			var c = String.fromCharCode(e.which);

			if (e.which !== 8 && e.which !== 13) {
				url = url.replace('(username)', $(this).val()+c);
			}else if (e.which === 8) {
				var ns = $(this).val();
				ns = ns.substring(0, ns.length-1);
				url = url.replace('(username)', ns);
			}else if(e.which === 13){
				url = url.replace('(username)', $(this).val());
			}
			openid_input.val(url);            
		});
		openid_username.blur(function(){
			var url = openid_url;
			url = url.replace('(username)', openid_username.val());
			openid_input.val(url);
		});

		
		$("a.openid-select").click(function(){
			$("#openid-btns").slideToggle("fast");
			$(this).hide("fast");
			return false;
		});
		$("a.openidbtns-back").click(function(){
			crossfadeToggle($(this).parent()[0], $(this).parent().siblings("div")[0]);
			return false;
		});
		$("a.openidbtn-small, a.openidbtn-large").click(function(){
			highlight(this, openid_selectclass);
			var that = $(this);
			var label = that.text();
			var url = that.attr("href");
			openid_url = url;
			url = url.replace('(username)', openid_username.val());
			openid_input.val(url);
			//openid_input.val(openid_url);
			if (label !== "") {
				//prompt user fo username
				openid_username.prev("p").text(label);
				crossfadeToggle(that.parent()[0], openid_username.parent()[0]);
			} else {
				openid_username.parent().hide();
				openid_form.submit();
			}
			return false;
		});
	}

	/************************************************************************
     *                           JS VALIDATION
     ************************************************************************/

	 var highlightfn = function(element) {$(element).addClass("error");};
	 var unhighlightfn = function(element) {$(element).removeClass("error");};
	 var errorplacefn = function(error, element) {error.insertBefore(element);};
	 var errorplacefn2 = function(error, element) {error.insertAfter(element);};
	 var reqmsg = lang['signup.form.error.required'];
	 var emailmsg = lang['signup.form.error.email'];
	 var emailexistsmsg = lang['signup.form.error.emailexists'];
	 var digitsmsg = lang['signup.form.error.year'];
	 var maxlenmsg = lang.maxlength;
	 var minlenmsg = lang.minlength;
	 var tagsmsg = lang["tags.toomany"];
	 var rusuremsg = lang.areyousure;

	/********* SIGNUP FORM ************/
	$("input#signup-btn").live("click", function(){
		var form = $(this).closest("form");
		form.validate({
			onsubmit: true,
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				fullname: "required",
                email: {required: true, email: true}
			},
			messages: {
				fullname: reqmsg,
				email: {
					required: reqmsg, email: emailmsg
				}
			}			
		});
		
        return form.valid();
	});

	/********* CREATE SCHOOL FORM ************/	
	$("input#createschool").live("click", function(){
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
	$("input#change-email-btn").live("click", function(){
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
	$("input#createclass").live("click", function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				gradyear: {required: true, digits: true, minlength: 4, maxlength: 4},
				schoolid: "required",
				identifier: {required: true, minlength: 4}
			},
			messages: {
				gradyear: {
					required: reqmsg,
					digits: digitsmsg,
					maxlength: jQuery.format(maxlenmsg),
					minlength: jQuery.format(minlenmsg)
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
	$("input#addclassmates-btn").live("click", function(){
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
	var maxTags = 6;
	$.validator.addMethod("tags", function(value, elem){
		return this.optional(elem) || value.split(",").length < maxTags;
	});
	$("input#ask-btn, input.post-edit-btn").live("click", function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				title: {required: true, minlength: 10, maxlength: 255},
				body: {required: true, minlength: 15, maxlength: 20000},
				tags: {required: true, tags: true},
				parentuuid: "required"
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
					tags: $.validator.format(tagsmsg, ""+maxTags)
				},
				parentuuid: reqmsg
			}
		});
		return form.valid();
	});

	/********* ANSWER QUESTION FORM ************/

	$("input#answer-btn").live("click", function(){
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

	$("input#sendmessage-btn").live("click", function(){
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

	$("input.import-media-btn").live("click", function(){
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

	var maxFavTags = 50;
	$.validator.addMethod("tags2", function(value, elem){
		return this.optional(elem) || value.split(",").length < maxFavTags;
	});
	$("input#add-favtag-btn").live("click", function(){
		var form = $(this).closest("form");
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
				favtags: {required: true, tags2: true}
			},
			messages: {
				favtags: {
					required: reqmsg,
					tags2: $.validator.format(tagsmsg, ""+maxFavTags)
				}
			}
		});
		return form.valid();
	});

	/********* REPORT SOLUTION FORM ************/
	$("input.report-solution-btn").live("click", function(){
		var form = $(this).closest("form");
		form.validate({
            highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {
                solution: {required: true, minlength: 15, maxlength: 255}
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
	$("input.new-comment-btn").live("click", function(){
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
		var datval = $(elem).data("val");
		return this.optional(elem) || (datval !== "" && datval !== value);
	});
	
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

});//end of scoold script

