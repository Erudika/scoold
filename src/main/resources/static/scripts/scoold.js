/*
 * Unobtrusive javascript for scoold.com
 * author: Alexander Bogdanovski
 * (CC) BY-SA
 */
/*global window: false, jQuery: false, $: false, lang, google: false */
$(function () {
	"use strict";
	var ajaxpath = window.location.pathname,
		ipdbkey = "da8cac8c9dd7287636b06a0421c0efa05872f65f70d3902e927cf66f530b9fd6",
		ipdburl = "http://api.ipinfodb.com/v3/ip-city/?key="+ipdbkey+"&format=json&callback=?&ip=",
		hideMsgBoxAfter = 10 * 1000, //10 sec
		mapCanvas = $("div#map-canvas"),
		locationbox = $("input.locationbox"),
		rusuremsg = lang.areyousure,
		highlightfn = function(element) {$(element).addClass("error");clearLoading();},
		unhighlightfn = function(element) {$(element).removeClass("error");},
		errorplacefn = function(error, element) {error.insertBefore(element);},
		reqmsg = lang['signup.form.error.required'];

	/**************************
	 *    Google Maps API
	 **************************/

	if (locationbox.length && !mapCanvas.length) {
		new google.maps.places.SearchBox(locationbox.get(0));
	}

	function initMap(elem) {
		var geocoder = new google.maps.Geocoder(),
			marker = new google.maps.Marker({}),
			locbox = locationbox,
			latlngbox = $("input.latlngbox:first"),
			mapElem = elem || mapCanvas.get(0),
			mapZoom = 3,
			mapMarker = new google.maps.Marker({visible: false});

		var myLatlng = new google.maps.LatLng(42.6975, 23.3241);
		if (latlngbox.length && latlngbox.val().length > 5) {
			var ll = latlngbox.val().split(",");
			myLatlng = new google.maps.LatLng(ll[0], ll[1]);
			mapZoom = 10;
		}
		var map = new google.maps.Map(mapElem, {
					zoom: mapZoom,
					center: myLatlng,
					mapTypeId: google.maps.MapTypeId.ROADMAP,
					mapTypeControl: false,
					streetViewControl: false
			});

		marker.setPosition(myLatlng);
		marker.setMap(map);

		if (locbox.length && $.trim(locbox.val()) !== "") {
			geocoder.geocode({address: locbox.val()}, function(results, status) {
				if (status === google.maps.GeocoderStatus.OK && results.length && results.length > 0) {
					var latlng = results[0].geometry.location;
					mapMarker = new google.maps.Marker({position: latlng, visible: true});
					mapMarker.setMap(map);
					map.setCenter(latlng);
					map.setZoom(7);
				}
			});
		}

		google.maps.event.addListener(map, 'click', function(event) {
//			map.setCenter(event.latLng);
			marker.setPosition(event.latLng);
			marker.setMap(map);
			latlngbox.val(event.latLng.lat() + "," + event.latLng.lng());

			geocoder.geocode({location: event.latLng}, function(results, status) {
				if (status !== google.maps.GeocoderStatus.OK) {
					locbox.val("");
				} else {
					if (results.length && results.length > 0) {
						var h = 0,
							country = "",
							locality = "",
							sublocality = "";

						for (h = 0; h < results.length; h++) {
							var arr = results[h].address_components, i;
							for (i = 0; i < arr.length; i++) {
								var type = $.trim(arr[i].types[0]);
								var name = arr[i].long_name;
								if (type === "country") {
									country = name || "";
								}
								if (type === "locality") {
									locality = name || "";
								}
								if (type === "sublocality") {
									sublocality = name || "";
								}
							}
						}
					}
					var found = "";

					if (sublocality !== "" && country !== "") {
						found = sublocality + ", " + country;
					} else if (locality !== "" && country !== "") {
						found = locality +  ", " + country;
					} else {
						found = country;
					}

					locbox.val(found);
				}
			});
		});
	}

	if (mapCanvas.length && mapCanvas.is(":visible")) {
		initMap();
	}

	try {
		if (typeof userip !== "undefined" && userip !== "" && userip !== "127.0.0.1") {
			$.getJSON(ipdburl + userip, function(data) {
				if (data && data.statusCode === 'OK' && $.trim(data.cityName).length > 1) {
					var found = data.cityName + ", " + data.countryName;
					found = found.toLowerCase();
					localStorage.setItem("scoold-country", data.countryCode);
				}
			});
		}
	} catch(err) {}


	/****************************************************
     *					MISC FUNCTIONS
     ****************************************************/

	function clearLoading() {
		$(".loading").removeClass("loading");
		$("img.ajaxwait").hide();
	}

	function clearForm(form) {
		$(":input", form).each(function() {
			var type = this.type;
			var tag = this.tagName.toLowerCase(); // normalize case
			if (type === "text" || type === "password" || type === "hidden" ||
				tag === "textarea") {
				this.value = "";
			} else if (type === "checkbox") {
				this.checked = false;
			} else if (tag === "select") {
				this.selectedIndex = 0;
			}
		});
	}

	function crossfadeToggle(elem1, elem2) {
		if ($(elem1).hasClass("hide") || $(elem1).css("display") === "none") {
			$(elem2).trigger("event:hide");
            $(elem2).animate({opacity: "hide"}, 200, function() {
				$(this).addClass("hide");
                $(elem1).animate({opacity: "show"}, 200).removeClass("hide");
				$(elem1).trigger("event:show");
			});
		} else {
			$(elem1).trigger("event:hide");
            $(elem1).animate({opacity: "hide"}, 200, function() {
				$(this).addClass("hide");
                $(elem2).animate({opacity: "show"}, 200).removeClass("hide");
				$(elem2).trigger("event:show");
			});
		}
	}

	function submitForm(form, method, callbackfn) {
		if (method === "GET" || method === "POST") {
			$.ajax({
				type: method,
				url: form.action,
				data: $(form).serialize(),
				success: function(data, status, xhr) {
					clearLoading();
					callbackfn(data, status, xhr, form);
				}
			});
		}
	}

	function submitFormBind(formname, callbackfn) {
		return $(document).on("submit", formname,  function() {
			submitForm(this, "POST", callbackfn);
			return false;
		});
	}

	function areYouSure(func, msg, returns) {
		if (confirm(msg)) {
			func();
			if (returns) {
				return true;
			}
		}
		return false;
	}

	/****************************************************
     *					GLOBAL BINDINGS
     ****************************************************/

	$.ajaxSetup({
		beforeSend: function(xhr, settings) {
				xhr.setRequestHeader("X-CSRF-TOKEN", csrftoken);
		}
	});

	$(document).on("click", ".rusure",  function() {
		return areYouSure($.noop, rusuremsg, true);
	});

	$(document).on("click", "a.next-div-toggle",  function(e) {
		var that = $(this);
		var hdiv = that.nextAll("div:first");
		if (!hdiv.length) {
			hdiv = that.parent().nextAll("div:first");
		}
		if (!hdiv.length) {
			hdiv = that.closest("div").find("div:first");
		}
		hdiv.toggleClass("hide").find("input[type=text]:first, textarea:first").focus();
		return false;
	});

	$(document).on("click", "a.next-span-toggle",  function() {
		$(this).nextAll("span:first").toggle();
		return false;
	});

	$(document).on("click", ".editlink",  function() {
		var that = $(this);
		var parent = that.closest(".row");
		var viewbox = parent.find(".viewbox:first");
		var editbox = parent.find(".editbox:first");
		if (!viewbox.length) {
			viewbox = that.nextAll(".viewbox:first");
		}
		if (!viewbox.length) {
			viewbox = that.closest(".viewbox");
		}
		if (!viewbox.length) {
			viewbox = parent.closest(".row").nextAll(".viewbox:first");
		}
		if (!viewbox.length) {
			viewbox = parent.closest(".postbox").find(".viewbox:first");
		}
		if (!editbox.length) {
			editbox = that.nextAll(".editbox:first");
		}
		if (!editbox.length) {
			editbox = viewbox.nextAll(".editbox:first");
		}
		crossfadeToggle(viewbox.get(0), editbox.get(0));
		return false;
	});

	$(document).on("click", ".canceledit",  function() {
		var editbox = $(this).closest(".editbox").get(0);
		var viewbox = $(editbox).siblings(".viewbox").get(0);
		crossfadeToggle(viewbox, editbox);
		return false;
	});

	//target=_blank is not valid XHTML
	$(document).on("click", "a.extlink",  function() {
		$(this).attr("target", "_blank");
		return true;
	});

	$(document).on("click", "a.votelink",  function() {
		var up = false;
		up = $(this).hasClass("upvote");
		var votes = $(this).closest("div.votebox").find(".votecount");
		var newvotes = parseInt(votes.text(), 10);
		if (!isNaN(newvotes)) {
			$.get(this.href, function(data) {
				if (data === true ) {
					if (up) {
						newvotes++;
					} else {
						newvotes--;
					}
				}
				votes.text(newvotes);
			}, "json");
		}
		return false;
	});

	$("body").ajaxSuccess(function() {
		clearLoading();
	});

	$(document).on("click", ".post-refresh-ask",  function() {
		var elem = $(this);
		return areYouSure(function() {
			$.post(elem.attr("href"), function(data) {
				window.location = elem.attr("href");
			});
		}, rusuremsg, false);
	});

	$(document).on("click", ".post-refresh",  function() {
		$.post($(this).attr("href"), function() {
			window.location.reload(true);
		});
	});

	$(".set-sys-param").click(function() {
		var form = $("form#sys-param-form"), title = $(this).attr("title");
		form.find("input[name=name]").val(title.substring(0, title.indexOf(":")));
		form.find("input[name=value]").val(title.substring(title.indexOf(":")+1, title.length));
		return false;
	});

	/****************************************************
     *                    REPORTS
     ****************************************************/

	$("a.delete-report").click(function() {
		var dis = $(this);
		$.post(dis.attr("href"));
		dis.closest(".reportbox").fadeOut("fast", function() {dis.remove();});
		return false;
	});

	submitFormBind("form.create-report-form", function(data, status, xhr, form) {
		$("#main-modal").modal("close");
		clearForm(form);
	});

	submitFormBind("form.close-report-form", function(data, status, xhr, form) {
		var parent = $(form).closest(".reportbox");
		parent.find(".report-closed-icon").removeClass("hide");
		parent.find(".report-close-btn").click().hide();
	});

	/****************************************************
     *                    PROFILE
     ****************************************************/

	$("#use-gravatar-switch").change(function () {
		var dis = $(this);
		var newPic = dis.is(":checked") ? $("#picture_url").next("input[type=hidden]").val() : $("#picture_url").val();
		$("img.profile-pic:first").attr("src", newPic);
		$.post(dis.closest("form").attr("action"), {picture: newPic});
	});

	$("#picture_url").on('focusout', function () {
		var dis = $(this);
		$("img.profile-pic:first").attr("src", dis.val());
		$.post(dis.closest("form").attr("action"), {picture: dis.val()});
	});

	$("img.profile-pic, #edit-picture-link").on("mouseenter touchstart", function () {
		$("#edit-picture-link").show();
	});
	$("img.profile-pic").on("mouseleave", function () {
		$("#edit-picture-link").hide();
	});

	/****************************************************
     *                    AUTOCOMPLETE
     ****************************************************/
	var tagsAutocomplete = $('input.tagbox').materialize_autocomplete({
		multiple: {
			enable: true
		},
		appender: {
			el: '.ac-tags',
			tagTemplate: '<div class="chip" data-id="<%= item.id %>" data-text="<%= item.text %>">\n\
							<%= item.text %><i class="fa fa-close close"></i></div>'
		},
		dropdown: {
			el: '#tags-dropdown'
		},
		hidden: {
			enabled: true,
			el: '.ac-hidden'
		},
		getData: function (value, callback) {
			var val = value.toLowerCase();
			$.get("/ajax/" + val, function (data) {
				var tags = data.map(function (t) {
					return {id: t.tag, text: t.tag};
				});
				tags.push({id: val, text: val});
				callback(value, tags);
			});
		}
	});

	function displayTags(tags) {
		var tagsVal = tags.val().split(",");
		for (var i = 0; i < tagsVal.length; i++) {
			if (tagsVal[i].length > 0) {
				tagsAutocomplete.append({id: tagsVal[i], text: tagsVal[i]});
			}
		}
	}

	var tagsInput = $("input[name=tags].ac-hidden");
	if (tagsInput.length) {
		displayTags(tagsInput);
	}

//	autocompleteBind("input.locationbox", {find: "locations"});
//	autocompleteContactBind("input.contactnamebox", {find: "contacts"});
//	autocompleteTagBind("input.tagbox", {find: "tags"});
//	autocompleteUserBind("input.personbox", {find: "people"});
//	autocompleteBind("input.typebox", {find: "classes"});

	/****************************************************
     *                    MODAL DIALOGS
     ****************************************************/

	 $('#main-modal').modal({
		ready: function (modal, trigger) {
			var div = $(modal);
			var trigr = $(trigger);
			if (typeof trigr.data("loadedForm") === "undefined") {
				$.get(trigr.attr("data-fetch"), function(data) {
					clearLoading();
					div.html(data);
					trigr.data("loadedForm", data);
					div.find('select').material_select();
				});
			} else {
				div.html(trigr.data("loadedForm"));
				div.find('select').material_select();
			}
			div.on("click", ".modal-close", function() {
				$("#main-modal").modal("close");
				return false;
			});
		},
		complete: function () {}
	});


	/****************************************************
     *                    COMMENTS
     ****************************************************/

	submitFormBind("form.new-comment-form", function(data, status, xhr, form) {
		var textbox = $("textarea[name='comment']", form);
		var that = $(form);
		that.closest("div.newcommentform").hide();
		that.closest("div.postbox").find("div.comments").prepend(data);
		textbox.val("");
	});

	$(document).on("click", "a.delete-comment",  function() {
		var that = $(this);
		return areYouSure(function() {
			that.closest("div.commentbox").fadeOut("fast", function() {that.remove();});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$(document).on("click", ".more-comments-btn",  function() {
		$(this).nextAll("div:first").show().end().remove();
		return false;
	});

	$(document).on("click", "a.show-comment",  function() {
		$(this).siblings("div:hidden").show().end().prev("span").addBack().remove();
		return false;
	});

	/****************************************************
     *                    TRANSLATIONS
     ****************************************************/

	var validateTrans = function(form) {
		form.validate({
			highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn,
			rules: {value: {required: true, notEmpty: true}},
			messages: {
				value: {required: reqmsg, notEmpty: reqmsg}
			}
		});
		return form.valid();
	};

	$(document).on("click", "a.delete-translation",  function() {
		var that = $(this);
		return areYouSure(function() {
			that.closest("div.translationbox").fadeOut("fast", function() {
				that.remove();
			});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$(document).on("click", "#addcont-translation-btn",  function() {
		return validateTrans($(this).closest("form"));
	});
	$(document).on("click", "#add-translation-btn",  function() {
		var that = $(this);
		var form = that.closest("form");
		var isValid = true; //validateTrans(form);
		var textbox = $("textarea[name='value']", form);
		var father = that.closest("div.newtranslationform");
		if ($.trim(textbox.val()) !== "" && isValid) {
			father.hide();
			submitForm(form, "POST", function(data, status, xhr, form) {
				if ($.trim(data) !== "") {
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

	function loadMoreHandler(dis, callback) {
		var that = $(dis),
			macroCode = that.siblings(".page-macro-code").text(),
			contentDiv = that.parent("div").prev("div"),
			href = that.attr("href"),
			cleanHref = href.substring(0, href.lastIndexOf("page=") + 5);

		that.addClass("loading");
		$.get(dis.href, {pageMacroCode: macroCode}, function(data) {
			clearLoading();
			var trimmed = $.trim(data);
			if (trimmed !== "") {
				var spanBlock = trimmed.substring(trimmed.lastIndexOf("<span")),
					nextPageSpan = $(spanBlock),
					nextPage = parseInt(nextPageSpan.attr("class"), 10),
					parsedData = $(trimmed);

				if (nextPageSpan && !isNaN(nextPage)) {
					that.attr("href", cleanHref + nextPage);
					nextPageSpan.remove();
					if (parsedData.length === 1) {
						that.hide();
					} else {
						contentDiv.append(trimmed);
						callback(contentDiv);
					}
				}
			}
		});

		return false;
	}

	$(document).on("click", "a.more-link",  function() {
		return loadMoreHandler(this, function() {});
	});

	/****************************************************
     *                     QUESTIONS
     ****************************************************/

	if (window.location.hash !== "" && window.location.hash.match(/^#post-.*/)) {
		$(window.location.hash).addClass("selected-post");
	}

	function initPostEditor(elem) {
		return new SimpleMDE({
			element: elem,
			showIcons: ["code", "table"],
			spellChecker: false
		});
	}

	$(".editbox").on("event:show", function () {
		var el = $(this).find("textarea.edit-post:visible");
		if (el.length) {
			initPostEditor(el.get(0));
		}
	});

	var newAnswerForm = $("#answer-question-form");
	if (newAnswerForm.length) {
		initPostEditor(newAnswerForm.find("textarea.edit-post:visible").get(0));
	}

	var askForm = $("form#ask-question-form");
	if (askForm.length) {

		var title = askForm.find("input[name=title]");
		var tags = askForm.find("input[name=tags]");
		var body = initPostEditor(askForm.find("textarea[name=body]").get(0));

		try {
			if (!title.val()) title.val(localStorage.getItem("ask-form-title"));
			if (!body.value()) body.value(localStorage.getItem("ask-form-body"));
			if (!tags.val()) tags.val(localStorage.getItem("ask-form-tags"));
			displayTags(tagsInput);
			setInterval(function () {
				var saved = false;
				if (localStorage.getItem("ask-form-title") !== title.val()) {
					localStorage.setItem("ask-form-title", title.val());
					saved = true;
				}
				if (localStorage.getItem("ask-form-body") !== body.value()) {
					localStorage.setItem("ask-form-body", body.value());
					saved = true;
				}
				if (localStorage.getItem("ask-form-tags") !== tags.val()) {
					localStorage.setItem("ask-form-tags", tags.val());
					saved = true;
				}
				if (saved) {
					askForm.find(".save-icon").show().delay(2000).fadeOut();
				}
			}, 2000);

			askForm.on("submit", function () {
				localStorage.removeItem("ask-form-title");
				localStorage.removeItem("ask-form-body");
				localStorage.removeItem("ask-form-tags");
			});
		} catch (exception) {}
	}

	$(".close-answer-form", "form#answer-question-form").click(function() {
		crossfadeToggle($(this).closest("form").parent("div").get(0), $(".open-answer-form").closest("div").get(0));
		return false;
	});

	$(".open-answer-form").click(function() {
		crossfadeToggle($(".close-answer-form").closest("form").parent("div").get(0), $(this).closest("div").get(0));
		return false;
	});

	$(document).on("click", "a.approve-answer, a.approve-translation", function() {
		var on = "green-text";
		var that = $(this);
		$(".approve-answer, .approve-translation").not(that).removeClass(on);

		if (that.hasClass(on)) {
			that.removeClass(on);
		} else {
			that.addClass(on);
		}

		$.post(this.href);
		return false;
	});


	var dmp = new diff_match_patch();

	function diffToHtml(diffs, oldText, newText) {
		var html = [];
		var intag = false;
		var appendMe = "";
		var done = false;

		function reconstruct(diffArray, what) {
			var out = "", i;
			if (what === 0) {
				return out;
			}
			for (i = 0; i < diffArray.length; i++) {
				var o = diffArray[i][0];
				var t = diffArray[i][1];

				if (o === what || o === 0) {
					out = out.concat(t);
				}
			}
			return out;
		}

		function diffMarkup(text, op) {
			var t1,t2 = "";
			switch (op) {
				case 1:t1 = '<span class="diff-ins">';t2 = '</span>';break;
				case -1:t1 = '<span class="diff-del">';t2 = '</span>';break;
				case 0:t1 = "";t2 = "";break;
			}

			var trimed = $.trim(text);
			if (trimed !== "") {
				text = t1.concat(text,t2);
			}
			return text;
		}

		//main loop over diff fragments
		var x;
		for (x = 0; x < diffs.length; x++) {
			html[x] = diffMarkup(diffs[x][1], diffs[x][0]);
			if (done) {break;}
		}

		if ($.trim(appendMe) !== "") {
			return appendMe;
		} else {
			return html.join('');
		}
	}

	$(".newrev").html(function(index, html) {
		var newText = html;
		var dis = $(this);
		var oldText = dis.next(".oldrev").html();
		var ds = newText;

		if (newText && oldText) {
//			var dd = diff_linesToHtml(oldText, newText);
//			var d = dmp.diff_main(dd[0], dd[1], false);

			var d = dmp.diff_main(oldText, newText);
			dmp.diff_cleanupSemantic(d);
			ds = diffToHtml(d, oldText, newText);
		}

		return ds;
	});


	var questionFilterForm = $("form#filter-questions-form");
	questionFilterForm.find("select").change(function() {
		questionFilterForm.submit();
	}).end().find(":submit").hide();


	/************************************************************************
     *                           JS VALIDATION
     ************************************************************************/
	var valobj = {highlight: highlightfn, unhighlight: unhighlightfn, errorPlacement: errorplacefn};

	$(document).on("click touchend", "input[type=submit], button[type=submit]",  function() {
		var form = $(this).closest("form"),
			vj = form.find("input[name=vj]").val(),
			result = true;
		if (vj && vj !== "") {
			$.extend(valobj, JSON.parse(vj.replace(/'/g, "\"")));
			form.validate(valobj);
			result = form.valid();
		}
		return result;
	});

});//end of scoold script
