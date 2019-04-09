/*
 * Unobtrusive javascript for scoold.com
 * author: Alexander Bogdanovski
 * (CC) BY-SA
 */
/*global window: false, jQuery: false, $: false, CSRF_COOKIE, google, hljs, RTL_ENABLED, CONTEXT_PATH: false */
"use strict";
$(function () {
	var mapCanvas = $("div#map-canvas");
	var locationbox = $("input.locationbox");
	var rusuremsg = "Are you sure about that?"; // john cena :)

	// Materialize CSS init

	function initMaterialize() {
		$(".sidenav").sidenav();
		$('select').formSelect();
		$('textarea').characterCounter();
		$('input.autocomplete').autocomplete();
		$('.collapsible').collapsible();
		$('.tooltipped').tooltip();
		$('.modal').modal();
		$('.chips').chips();
		$('.dropdown-trigger').dropdown({
			constrainWidth: false,
			coverTrigger: false,
			alignment: RTL_ENABLED ? 'left' : 'right'
		});
	}

	initMaterialize();

	/***************************************************************************
	 * Google Maps API
	 **************************************************************************/

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
// map.setCenter(event.latLng);
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

	/***************************************************************************
	 * MISC FUNCTIONS
	 **************************************************************************/

	function clearLoading() {
		$(".ajaxwait").hide();
	}

	function startLoading() {
		$(".ajaxwait").removeClass("hide");
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

	function submitForm(form, method, callbackfn, errorfn) {
		if (method === "GET" || method === "POST") {
			$.ajax({
				type: method,
				url: form.action,
				data: $(form).serialize(),
				success: function(data, status, xhr) {
					clearLoading();
					callbackfn(data, status, xhr, form);
				},
				error: function (xhr, statusText, error) {
					clearLoading();
					var cb = errorfn || $.noop;
					cb(xhr, statusText, error);
				}
			});
		}
	}

	function submitFormBind(formname, callbackfn, errorfn) {
		return $(document).on("submit", formname,  function() {
			submitForm(this, "POST", callbackfn, errorfn);
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

	function getCookie(cookie) {
		var name = cookie + "=";
		var ca = document.cookie.split(';');
		for (var i = 0; i < ca.length; i++) {
			var c = (ca[i] || "").trim();
			if (c.indexOf(name) === 0) {
				return c.substring(name.length, c.length);
			}
		}
		return "";
	}

	/***************************************************************************
	 * GLOBAL BINDINGS
	 **************************************************************************/

	$.ajaxSetup({
		beforeSend: function(xhr, settings) {
			if (settings.type !== "GET") {
				xhr.setRequestHeader("X-CSRF-TOKEN", getCookie(CSRF_COOKIE));
			}
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
		var dis = $(this);
		var up = dis.hasClass("upvote");
		var votes = dis.closest("div.votebox").find(".votecount");
		var newvotes = parseInt(votes.text(), 10) || 0;
		if (!dis.data("disabled")) {
			dis.data("disabled", true);
			$.get(this.href, function(data) {
				if (data === true) {
					if (up) {
						newvotes++;
					} else {
						newvotes--;
					}
				}
				votes.text(newvotes).removeClass("hide");
				dis.removeData("disabled");
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
		return false;
	});

	$(document).on("click", ".permalink",  function() {
		if ($(this).attr("href") === $(this).text()) {
			return true;
		} else {
			$(this).text($(this).attr("href"));
			return false;
		}
	});

	$(document).on("click", ".click2hide",  function() {
		$(this).hide();
		return false;
	});

	$(document).on("click", ".toggle-drawer",  function() {
		$('#search-drawer').toggleClass('hide');
		$('#search-box').focus();
		return false;
	});

	$(".signout").click(function () {
		$.post($(this).attr("href"), function (data) {
			window.location = CONTEXT_PATH + "/signin?code=5&success=true";
		});
		return false;
	});

	/***************************************************************************
	 * ADMIN
	 **************************************************************************/

    submitFormBind("form#create-space-form", function (data, status, xhr, form) {
		clearForm(form);
		$('.spaces-wrapper div:eq(0)').after(data);
		$('#name_text').removeClass("invalid");
	}, function () {
		$('#name_text').addClass("invalid");
	});

	$(document).on("click", "a.delete-space", function () {
		var elem = $(this);
		return areYouSure(function () {
			elem.closest(".spacebox").fadeOut("fast", function () { elem.remove(); });
			$.post(elem.attr("href"));
		}, rusuremsg, false);
	});

	/***************************************************************************
	 * REPORTS
	 **************************************************************************/

	$(document).on("click", "a.delete-report", function() {
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

	/***************************************************************************
	 * PROFILE
	 **************************************************************************/

	$("#use-gravatar-switch").change(function () {
		var dis = $(this);
		var currentPic = $("#picture_url");
		var newPic = $("#picture_url").next("input[type=hidden]");
		var newPicValue = newPic.val();
		$("img.profile-pic:first").attr("src", newPicValue);
		$(".profilepage img.profile-pic").attr("src", newPicValue);
		$.post(dis.closest("form").attr("action"), {picture: newPicValue});
		// swap
		newPic.val(currentPic.val());
		currentPic.val(newPicValue);
	});

	$("#picture_url").on('focusout paste', function () {
		var dis = $(this);
		setTimeout(function () {
			console.log(dis.val());
			$("img.profile-pic:first").attr("src", dis.val());
			$.post(dis.closest("form").attr("action"), {picture: dis.val()});
		}, 200);
	});

	$("img.profile-pic, #edit-picture-link").on("mouseenter touchstart", function () {
		$("#edit-picture-link").show();
	});
	$("img.profile-pic").on("mouseleave", function () {
		$("#edit-picture-link").hide();
	});

	function fixBrokenImages() {
		$('img.profile-pic').on('error', function (e) {
			this.src = 'https://www.gravatar.com/avatar/?size=350&d=mm';
		});
	}

	fixBrokenImages();

	$(document).on("event:page", function() {
		fixBrokenImages();
	});

	$("a.make-mod-btn").on('click', function () {
		var dis = $(this);
		$.post(dis.attr("href"));
		dis.siblings(".make-mod-btn.hide").removeClass("hide");
		$(".moderator-icon").toggleClass("hide");
		dis.addClass("hide");
		return false;
	});

	/***************************************************************************
	 * AUTOCOMPLETE
	 **************************************************************************/

	var autocomplete = $('.chips-autocomplete');
	var autocompleteValue = $('input[name=tags]').val() || "";
	var autocompleteCache = {};
	var autocompleteDelayTimer;
	var autocompleteInitData = autocompleteValue.split(",").filter(function (t) {
		return t && t.trim().length > 0;
	}).map(function (t) {
		return {tag: t};
	});

	autocomplete.chips({
		limit: 5,
		placeholder: autocomplete.attr('title') || "Tags",
		data: autocompleteInitData || [],
		autocompleteOptions: {
			data: {},
			minLength: 2
		},
		onChipAdd: function (c) {
			$(c).find('i.close').text("");
			autocomplete.next('input[type=hidden]').val(this.chipsData.map(function (c) {
				return c.tag;
			}).join(','));
		},
		onChipDelete: function () {
			autocomplete.next('input[type=hidden]').val(this.chipsData.map(function (c) {
				return c.tag;
			}).join(','));
		}
	}).find('i.close').text("");

	if (autocomplete.length) {
		var autocompleteChipsInstance = M.Chips.getInstance(autocomplete.get(0));
		function autocompleteFetchData(value, callback) {
			var val = value.toLowerCase();
			if (val && val.trim().length > 0) {
				$.get(CONTEXT_PATH + "/tags/" + val, function (data) {
					var tags = {};
					if (data.length === 0) {
						data.push({tag: val});
					}
					data.map(function (t) {
						tags[t.tag] = null;
					});
					callback(val, tags);
				});
			}
		}
		function autocompleteFetchCallback(value, list) {
			if (!autocompleteCache.hasOwnProperty(value)) {
				autocompleteCache[value] = list;
			}
			if (autocompleteChipsInstance && autocompleteChipsInstance.hasAutocomplete) {
				autocompleteChipsInstance.autocomplete.updateData(list);
				autocompleteChipsInstance.autocomplete.open();
			}
		}

		autocomplete.on('input', function () {
			var value = $(this).find('input:first').val().toUpperCase();

			if (autocompleteCache.hasOwnProperty(value) && autocompleteCache[value]) {
				autocompleteFetchCallback(value, autocompleteCache[value]);
			} else {
				clearTimeout(autocompleteDelayTimer);
				autocompleteDelayTimer = setTimeout(function () {
					autocompleteFetchData(value, autocompleteFetchCallback);
				}, 400);
			}
			return true;
		});
	}

	/***************************************************************************
	 * MODAL DIALOGS
	 **************************************************************************/

	 $('#main-modal').modal({
		onOpenStart: function (modal, trigger) {
			var div = $(modal);
			var trigr = $(trigger);
			if (typeof trigr.data("loadedForm") === "undefined") {
				$.get(trigr.attr("data-fetch"), function(data) {
					clearLoading();
					div.html(data);
					trigr.data("loadedForm", data);
					div.find('select').formSelect();
					$('textarea').characterCounter();
				});
			} else {
				div.html(trigr.data("loadedForm"));
				div.find('select').formSelect();
			}
			div.on("click", ".modal-close", function() {
				$("#main-modal").modal("close");
				return false;
			});
		}
	});


	/***************************************************************************
	 * COMMENTS
	 **************************************************************************/

	var commentInput = $("input[name='comment']", "form.new-comment-form");
	commentInput.characterCounter();
	submitFormBind("form.new-comment-form", function(data, status, xhr, form) {
		var textbox = commentInput;
		var that = $(form);
		var commentsbox = that.closest("div.postbox").find("div.comments");
		var pageContentDiv = commentsbox.find(".page-content");
		if (pageContentDiv.length) {
			pageContentDiv.append(data);
		} else {
			commentsbox.append(data);
		}
		that.closest("div.newcommentform").addClass("hide");
		textbox.val("");
	});

	$(document).on("click", "a.delete-comment",  function() {
		var that = $(this);
		return areYouSure(function() {
			that.closest("div.commentbox").fadeOut("fast", function() {that.remove();});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$(document).on("click", "a.show-comment",  function() {
		$(this).siblings("div:hidden").show().end().prev("span").addBack().remove();
		return false;
	});

	/***************************************************************************
	 * TRANSLATIONS
	 **************************************************************************/

	$(document).on("click", "a.delete-translation",  function() {
		var that = $(this);
		return areYouSure(function() {
			that.closest("div.translationbox").fadeOut("fast", function() {
				that.remove();
			});
			$.post(that.attr("href"));
		}, rusuremsg, false);
	});

	$("form.new-translation-form").find("textarea").focus();

	/***************************************************************************
	 * PAGINATION
	 **************************************************************************/

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
						contentDiv.append(trimmed).find(".dropdown-trigger").dropdown();
					}
					$(document).trigger("event:page");
					initMaterialize(); // fix for tooltips after ajax load
				}
			}
		});

		return false;
	});

	/***************************************************************************
	 * QUESTIONS
	 **************************************************************************/

	if (window.location.hash !== "" && window.location.hash.match(/^#post-.*/)) {
		$(window.location.hash).addClass("selected-post");
	}

	function initPostEditor($elem) {
		var UploadButton = function (context) {
		  var ui = $.summernote.ui;

		  var button = ui.button({
		    contents: '<i class="fa fa-upload"/>',
		    tooltip: 'File Attachment',
		    container: 'body',
		    click: function () {
				var fileInput = document.createElement('input');
				fileInput.setAttribute('type', 'file');
				fileInput.setAttribute('multiple', '');
				fileInput.setAttribute('accept', 'image/*,.pdf,.txt');
				fileInput.onchange = function (evt) {
					var files = fileInput.files;
	        		if (files === null || files[0] === null) {
	        			return;
	        		}
	        		
	        		var formData = new FormData();
	        		$.each(files, function(index, f) {
		        		formData.append('file', f);
	        		});
	        		
	        		$.ajax({
	        		    method: "POST",
	        		    url: 'http://localhost:3000/upload',
	        		    data: formData,
	        		    dataType: 'json',
	        	        processData: false,
	        	        contentType: false,
	        	        cache: false,
	        		    crossDomain : true
	        		})
	        			.done(function(data) {
	        				$.each(data, function(index, f) {
	        					var insertText;
		        				var extention = f.extention.toLowerCase().replace(/\./g,'');
		        				if (['bmp', 'jpg', 'jpge', 'gif', 'png'].indexOf(extention) > -1) {
		        					insertText = '<img src="#url" title="#filename">';
			                	}
			                	else {
			                		insertText = '<a href="#url" title="#filename" target="_blank">#url</a>';
			                	}

		        				$elem.summernote('pasteHTML',
		                				insertText.replace(/#url/g, f.path).replace(/#filename/g, f.name + f.extention));
	        				});

		                	fileInput.value = "";
	        			});
				}
			  	fileInput.click();
			}
		  });

		  return button.render();   // return button as jquery object
		}
	
		
		$elem.summernote({
			lang: $('#hidLocaleCode').val() // default: 'en-US'
			, height: 300
			, toolbar: [
				['style', ['style']],
			    ['font', ['bold', 'italic', 'underline', 'clear']],
			    ['fontname', ['fontname']],
			    ['fontsize', ['fontsize']],
			    ['color', ['color']],
			    ['para', ['ul', 'ol', 'paragraph']],
			    ['height', ['height']],
			    ['table', ['table']],
			    ['insert', ['link', 'upload', 'hr']],
			    ['view', ['fullscreen', 'codeview']],
			    ['help', ['help']]
			]
			, buttons: {
				upload: UploadButton
			}
		});
	}

	$(document).on("event:show", ".editbox", function () {
		var $el = $(this).find("textarea.edit-post:visible");
		if ($el.length) {
			initPostEditor($el);
		}
		$("#title_text").on("keyup", function () {
			$('#post-title').text($(this).val());
		});
	});


	// save draft in localstorage
	var askForm = $("form#ask-question-form, form#write-feedback-form");
	if (askForm.length) {
		var title = askForm.find("input[name=title]");
		var tags = askForm.find("input[name=tags]");
		var $editor = askForm.find("textarea[name=body]");
		initPostEditor($editor);
		var body = $('<textarea />').val($editor.summernote('code')).val();

		try {
			if (!title.val()) title.val(localStorage.getItem("ask-form-title") || "");
			if ($editor.summernote('isEmpty')) $editor.summernote('code', localStorage.getItem("ask-form-body") || "");
			if (!tags.val()) tags.val(localStorage.getItem("ask-form-tags") || "");

			var saveDraftInterval1 = setInterval(function () {
				var saved = false;
				if (localStorage.getItem("ask-form-title") !== title.val()) {
					localStorage.setItem("ask-form-title", title.val());
					saved = true;
				}
				if (localStorage.getItem("ask-form-body") !== body) {
					localStorage.setItem("ask-form-body", body);
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

			var titleTimeout = null;
			var similarContainer = title.next(".similar-posts");
			var similarTitle = similarContainer.find(".similar-posts-title").first().remove();
			title.on("input", function () {
				if (titleTimeout) {
					clearTimeout(titleTimeout);
				}
				if (title.val() && title.val().trim().length > 0) {
					titleTimeout = setTimeout(function () {
						$.get(CONTEXT_PATH + "/questions/similar/" + title.val(), function (data) {
							if (data && data.trim().length > 0) {
								similarContainer.html(data).prepend(similarTitle);
							} else {
								similarContainer.html("");
							}
						});
					}, 1000);
				} else {
					similarContainer.html("");
				}
			});

			askForm.on("submit", function () {
				clearInterval(saveDraftInterval1);
				if (tags.val() && tags.val().length > 0) {
					localStorage.removeItem("ask-form-title");
					localStorage.removeItem("ask-form-body");
					localStorage.removeItem("ask-form-tags");
				}
			});
		} catch (exception) {}
	}

	var answerForm = $("form#answer-question-form");
	if (answerForm.length) {
		var $answer = answerForm.find("textarea[name=body]");
		initPostEditor($answer);
		var answerBody = $('<textarea />').val($answer.summernote('code')).val();
		try {
			if ($answer.summernote('isEmpty')) $answer.summernote('code', localStorage.getItem("answer-form-body") || "");
			var saveDraftInterval2 = setInterval(function () {
				if (localStorage.getItem("answer-form-body") !== answerBody) {
					localStorage.setItem("answer-form-body", answerBody);
					answerForm.find(".save-icon").show().delay(2000).fadeOut();
				}
			}, 3000);

			submitFormBind("#answer-question-form", function (data, status, xhr, form) {
				clearInterval(saveDraftInterval1);
				var allPosts = answerForm.closest(".row").find(".postbox");
				if (allPosts.length > 1) {
					allPosts.last().after(data);
				} else {
					$(".answers-head").removeClass("hide").after(data);
				}
				answerForm.hide();
				$answer.summernote('reset');
				localStorage.removeItem("answer-form-body");
			}, function (xhr, status, error) {
				clearInterval(saveDraftInterval1);
				localStorage.removeItem("answer-form-body");
				window.location.reload(true);
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

	$(document).on("click", ".delete-post",  function() {
		var dis = $(this);
		var postBox = dis.closest(".postbox");
		return areYouSure(function () {
			startLoading();
			$.post(dis.attr("href"), function () {
				clearLoading();
				if (!postBox.hasClass("replybox")) {
					window.location = CONTEXT_PATH + "/questions?success=true&code=16&deleted=" + postBox.attr("id");
				}
			});
			if (postBox.hasClass("replybox")) {
				postBox.fadeOut("fast", function() {postBox.remove();});
			}
		}, rusuremsg, false);
	});

	$("#follow-thread-check").click(function () {
		var dis = $(this);
		$.post(dis.closest("form").attr("action"), {emailme: dis.is(":checked")});
	});

	if (typeof hljs !== "undefined") {
		$("pre code").each(function (i, block) {
			hljs.highlightBlock(block);
		});
	}

	// small fix for custom checkbox rendering of GFM task lists
	$(".task-list-item>input[type=checkbox]").addClass("filled-in").after("<label style='height:15px'></label>");

	/***************************************************************************
	 * REVISIONS
	 **************************************************************************/

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

		// main loop over diff fragments
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

	function diffInit(elem) {
		$(elem).html(function(index, html) {
			var dis = $(this);
			var undiffed = dis.data("undiffedHTML");
			var newText = undiffed || html;
			var oldText = dis.next(".oldrev").html();
			var ds = newText;
			if (!undiffed) {
				dis.data("undiffedHTML", html);
			}
			if (newText && oldText) {
				var d = dmp.diff_main(oldText, newText);
				dmp.diff_cleanupSemantic(d);
				ds = diffToHtml(d, oldText, newText);
			}
			return ds;
		});
	}

	diffInit(".newrev");

	$(document).on("event:page", function() {
		diffInit(".newrev");
	});
});
