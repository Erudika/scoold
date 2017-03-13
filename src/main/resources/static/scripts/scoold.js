/*
 * Unobtrusive javascript for scoold.com
 * author: Alexander Bogdanovski
 * (CC) BY-SA
 */
/*global window: false, jQuery: false, $: false, lang, google: false */
"use strict";
$(function () {
	var mapCanvas = $("div#map-canvas");
	var locationbox = $("input.locationbox");
	var rusuremsg = lang.areyousure;

	/**************************
	 *    Google Maps API
	 **************************/

	if (locationbox.length && !mapCanvas.length) {
		var searchLocation = new google.maps.places.SearchBox(locationbox.get(0));
		searchLocation.addListener('places_changed', function () {
			var lat = searchLocation.getPlaces()[0].geometry.location.lat();
			var lng = searchLocation.getPlaces()[0].geometry.location.lng();
			if (lat && lng) {
				locationbox.siblings("input[name=latlng]").val(lat + "," + lng);
			}
			if (searchLocation.getPlaces()[0].formatted_address) {
				locationbox.siblings("input[name=address]").val(searchLocation.getPlaces()[0].formatted_address);
			}
		});
		// prevent form submit on enter pressed
		locationbox.keypress(function (event) {
			if (event.keyCode === 10 || event.keyCode === 13) event.preventDefault();
		});
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

	$(document).on("click", "a.votelink",  function() {
		var up = $(this).hasClass("upvote");
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
				votes.text(newvotes).removeClass("hide");
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

	$("a.make-mod-btn").on('click', function () {
		var dis = $(this);
		$.post(dis.attr("href"));
		dis.siblings(".make-mod-btn.hide").removeClass("hide");
		dis.addClass("hide");
	});

	/****************************************************
     *                    AUTOCOMPLETE
     ****************************************************/
	var tagsAutocomplete = $('input.tagbox').materialize_autocomplete({
		multiple: {
			enable: true,
			maxSize: 5
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
			$.get("/tags/" + val, function (data) {
				var tags = data.map(function (t) {
					return {id: t.tag, text: t.tag};
				});
				tags.push({id: val, text: val});
				callback(value, tags);
			});
		}
	});

	function displayTags(tags) {
		if (tags && tags.val() && tags.val().length > 1) {
			var tagsVal = tags.val().split(",");
			for (var i = 0; i < tagsVal.length; i++) {
				if (tagsVal[i].length > 0) {
					tagsAutocomplete.append({id: tagsVal[i], text: tagsVal[i]});
				}
			}
		}
	}

	var tagsInput = $("input[name=tags].ac-hidden");
	if (tagsInput.length) {
		displayTags(tagsInput);
	}

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

	$(document).on("click", "a.delete-translation",  function() {
		var that = $(this);
		return areYouSure(function() {
			that.closest("div.translationbox").fadeOut("fast", function() {
				that.remove();
			});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$(document).on("click", "#add-translation-btn",  function() {
		var that = $(this);
		var form = that.closest("form");
		var isValid = true;
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

	$(document).on("click", "a.more-link",  function() {
		var that = $(this),
			macroCode = that.siblings(".page-macro-code").text(),
			contentDiv = that.parent("div").prev("div"),
			href = that.attr("href"),
			cleanHref = href.substring(0, href.lastIndexOf("=") + 1);

		that.addClass("loading");
		$.get(this.href, {pageMacroCode: macroCode}, function(data) {
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
					}
				}
			}
		});

		return false;
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
			autoDownloadFontAwesome: false,
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

	// save draft in localstorage
	var askForm = $("form#ask-question-form, form#write-feedback-form");
	if (askForm.length) {
		var title = askForm.find("input[name=title]");
		var tags = askForm.find("input[name=tags]");
		var body = initPostEditor(askForm.find("textarea[name=body]").get(0));

		try {
			if (!title.val()) title.val(localStorage.getItem("ask-form-title") || "");
			if (!body.value()) body.value(localStorage.getItem("ask-form-body") || "");
			if (!tags.val()) tags.val(localStorage.getItem("ask-form-tags") || "");
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
			}, 3000);

			askForm.on("submit", function () {
				localStorage.removeItem("ask-form-title");
				localStorage.removeItem("ask-form-body");
				localStorage.removeItem("ask-form-tags");
			});
		} catch (exception) {}
	}

	var answerForm = $("form#answer-question-form");
	if (answerForm.length) {
		var answerBody = initPostEditor(answerForm.find("textarea[name=body]").get(0));
		try {
			if (!answerBody.value()) answerBody.value(localStorage.getItem("answer-form-body") || "");
			setInterval(function () {
				if (localStorage.getItem("answer-form-body") !== answerBody.value()) {
					localStorage.setItem("answer-form-body", answerBody.value());
					answerForm.find(".save-icon").show().delay(2000).fadeOut();
				}
			}, 3000);

			answerForm.on("submit", function () {
				localStorage.removeItem("answer-form-body");
			});
		} catch (exception) {}
	}

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

	/****************************************************
     *                   REVISIONS
     ****************************************************/

	var dmp = new diff_match_patch();

	function diffToHtml(diffs, oldText, newText) {
		var html = [];
		var appendMe = "";
		var done = false;

		function diffMarkup(text, op) {
			var t1,t2 = "";
			switch (op) {
				case 1:  t1 = '<span class="diff-ins">';t2 = '</span>'; break;
				case -1: t1 = '<span class="diff-del">';t2 = '</span>'; break;
				case 0:  t1 = "";t2 = ""; break;
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
			console.log(ds);
		}

		return ds;
	});
});
