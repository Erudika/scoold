/*
 * jQuery Galleriffic plugin
 *
 * Copyright (c) 2008 Trent Foley (http://trentacular.com)
 * Licensed under the MIT License:
 *   http://www.opensource.org/licenses/mit-license.php
 *
 * Modified for scoold.com by Alexander Bogdanovski
 */
(function($) {

	var ver = 'galleriffic-1.1-modified';

	$.galleriffic = {
		go2: function(gallery, hash) {
			hash = gallery.getHashFromString(hash);
			gallery.go2(hash);
		}
	};

//	function getHashFromString(hash) {
//		if (typeof hash == 'number'){return hash;}
//		if (!hash){return -1;}
//		hash = hash.replace(/^.*#/, '');
//
//		if (isNaN(hash)) {return -1;}
//
//		return (+hash);
//	}

	var defaults = {
		delay:                     34000,
		imageDataInit:			   {},
		galleryUri:				   '',
		totalCount:				   0,
		label:					   '',
		labelBoxClass:					   '',
		preloadAhead:              1, // Set to -1 to preload all images
		commentProfileLinkSel:	   '',
		commentBoxSel:		       '.commentbox',
		commentTimestampSel:	   '',
		commentTextSel:			   '',
		commentFormSel:			   '',
		commentsContainerSel:	   '',
		reportLinkSel:			   '',
		deleteCommentSel:		   '',
		imageContainerSel:         '',
		controlsContainerSel:      '',
		captionContainerSel:       '',
		labelsContainerSel:		   '',
		addLabelFormSel:		   '',
		loadingContainerSel:       '',
		slideshowToggleSel:		   '.ss-controls a',
		pageNavigationSel:		   '',
		titleSel:				   '.image-title',
		captionSel:				   '.image-caption',
		originalLinkSel:		   '.image-original',
		voteboxSel:				   '',
		upvoteSel:				   '',
		downvoteSel:			   '',
		voteLinkSel:			   '',
		votecountSel:			   '',
		prevLinkSel:			   'a.prev',
		nextLinkSel:			   'a.next',
		prevPageSel:			   'a.prevpage',
		nextPageSel:			   'a.nextpage',
		playLinkText:              'Play',
		pauseLinkText:             'Pause',
		enableHistory:             false,
		autoStart:                 false
	};

	$.fn.galleriffic = function(settings) {
		//  Extend Gallery Object
		$.extend(this, {

			ver: function() {
				return ver;
			},

			getHashFromString: function(hash){
				if (typeof hash == 'number'){return hash;}
				if (!hash){return -1;}
				hash = hash.replace(/^.*#/, '');

				if (isNaN(hash)) {return -1;}

				return (+hash);
			},

			clickHandler: function(e, link) {
				this.pause();

				if (!this.enableHistory) {
					var hash = getHashFromString(link.href);
					if (hash >= 0) {
						this.go2(hash);
					}
					e.preventDefault();
				}
			},

			currentPosition: 0,
			orderedData: [],

			getNextPosition: function(){
				if(this.currentPosition == this.totalCount - 1){
					return 0;
				}else{
					return this.currentPosition + 1;
				}
			},

			getPrevPosition: function(){
				if(this.currentPosition == 0){
					return this.totalCount - 1;
				}else{
					return this.currentPosition - 1;
				}
			},

			getData: function(index, oldIndex, go, nextPrevAll) {
				var gallery = this;
				var label = this.label;

				var params = {mid:index, nextprevall: nextPrevAll, getimagedataobject:true};
				if($.trim(this.label) !== ""){
					$.extend(params, {label: label});
				}

				if(nextPrevAll == 1){
					this.currentPosition = this.getNextPosition();
				}else if(nextPrevAll == -1){
					this.currentPosition = this.getPrevPosition();
				}

				var adjacentIndex = null;
				
				if(nextPrevAll == 1){
					adjacentIndex = this.orderedData[this.getNextPosition()];
				}else if(nextPrevAll == -1){
					adjacentIndex = this.orderedData[this.getPrevPosition()];
				}
				// check if there are any more photos to preload
				if (typeof adjacentIndex == "undefined" || adjacentIndex === null) {
					$.getJSON(gallery.galleryUri, params,
						function(jsondata){
							for (i = 0; i < jsondata.media.length; i++) {
								var media = jsondata.media[i];
								gallery.data[media.id] = media;
							}

							if(typeof gallery.orderedData[gallery.currentPosition] == "undefined"){
								gallery.orderedData[gallery.currentPosition] = jsondata.media[0].id;
							}

							gallery.orderedData[gallery.currentPosition] = index;
							if(nextPrevAll == 1){
								if(oldIndex != null){gallery.prevIndex = oldIndex;}
								gallery.nextIndex = jsondata.media[1].id;
								gallery.orderedData[gallery.getNextPosition()] = gallery.nextIndex;
								gallery.preload(gallery.nextIndex);
							}else if(nextPrevAll == -1){
								gallery.prevIndex = jsondata.media[1].id;
								if(oldIndex != null){gallery.nextIndex = oldIndex;}
								gallery.orderedData[gallery.getPrevPosition()] = gallery.prevIndex;
								gallery.preload(gallery.prevIndex);
							}else{
								gallery.prevIndex = jsondata.media[1].id;
								gallery.nextIndex = jsondata.media[2].id;
								gallery.orderedData[gallery.getPrevPosition()] = gallery.prevIndex;
								gallery.orderedData[gallery.getNextPosition()] = gallery.nextIndex;
								gallery.preload(gallery.prevIndex);
								gallery.preload(gallery.nextIndex);
							}

							gallery.updateNavLinks();

							if(window.location.hash !== ""){
								//resume = go to the next page
								if(go === true){
									gallery.go2(index);
								}
							}
						});
				}else{
					if(nextPrevAll == 1){
						if(oldIndex != null){gallery.prevIndex = oldIndex;}
						gallery.nextIndex = gallery.orderedData[gallery.getNextPosition()];
					}else if(nextPrevAll == -1){
						gallery.prevIndex = gallery.orderedData[gallery.getPrevPosition()];
						if(oldIndex != null){gallery.nextIndex = oldIndex;}
					}else{
						gallery.prevIndex = gallery.orderedData[gallery.getPrevPosition()];
						gallery.nextIndex = gallery.orderedData[gallery.getNextPosition()];
					}

					gallery.updateNavLinks();
					
					if(window.location.hash !== ""){
						//resume = go to the next page
						if(go === true){
							gallery.go2(index);
						}
					}
				}
				return this;
			},

			getComments: function(imageData){
				var gallery = this;

				if(this.$commentForm){
					this.$commentForm.find("input[name=parentuuid]").val(imageData.uuid);
				}
				if(this.$commentsContainer){
					var commentsCont = this.$commentsContainer;
					var comboxParent = $(this.commentBoxSel).parent("div");

					var comment, div;
					commentsCont.find(gallery.commentBoxSel+":visible").remove(); // clear container

					for (i = 0; i < imageData.comments.length; i++) {
						comment = imageData.comments[i];

						div = commentsCont.find(gallery.commentBoxSel+":first").clone();
						div.find(gallery.commentProfileLinkSel).attr("href", function(){
							return this.href + comment.userid;
						}).text(comment.author);
						if (comment.candelete === true) {
							div.find(gallery.reportLinkSel).remove();
							div.find(gallery.deleteCommentSel).attr("href", function(){
								var newhref = $.trim(this.href);
								var params = "deletecomment="+comment.id+"&parentuuid="+
									imageData.uuid;
								if(newhref.indexOf("?") != -1){
									newhref = newhref +"&"+ params;
								}else{
									newhref = newhref + "?" + params;
								}
								return newhref;
							});
						} else {
							div.find(gallery.deleteCommentSel).remove();
							div.find(gallery.reportLinkSel).attr("href", function(){
								var newhref = $.trim(this.href);
								var params = "getreportform=true&parentid="+comment.id+
									"&grandparentuuid="+imageData.uuid+"&parentuuid="
									+comment.uuid+"&classname=Comment";
								if(newhref.indexOf("?") != -1){
									newhref = newhref +"&"+ params;
								}else{
									newhref = newhref + "?" + params;
								}
								return newhref;
							});
						}

						div.find(gallery.commentTimestampSel).text(comment.timestamp);
						div.find(gallery.commentTextSel).text(comment.comment);
						div.find(gallery.votecountSel).text(comment.votes);
						div.find(gallery.voteLinkSel).attr("href", function(){
							return this.href + comment.uuid;
						}).show();

						comboxParent.append(div.show());
					}

					var moreLink = commentsCont.find("div:last > a");

					if(imageData.comments.length == imageData.commentcount){
						moreLink.hide();
					}else{
						// pageless mode!
						moreLink.attr("href",
							window.location.pathname + "?parentuuid="+imageData.uuid+
							"&mid="+imageData.id+"&page="+imageData.pagenum).show();
					}
				}
				return this;
			},

			getVotes: function(imageData){
				if(this.$captionContainer){
					//load votes
					var vbox = this.$captionContainer.find(this.voteboxSel);

					if(vbox.length){
						var newUpvoteHref = vbox.find(this.upvoteSel).attr("href");
						var newDownvoteHref = vbox.find(this.downvoteSel).attr("href");
						newUpvoteHref = newUpvoteHref.substring(0, 
							newUpvoteHref.lastIndexOf("/") + 1) + imageData.uuid;
						newDownvoteHref = newDownvoteHref.substring(0, 
							newDownvoteHref.lastIndexOf("/") + 1) + imageData.uuid;

						vbox.find(this.votecountSel).text(imageData.votes);
						vbox.find().attr("href",newUpvoteHref);
						vbox.find().attr("href",newDownvoteHref);
					}
				}
			},

			getLabels: function(imageData){
				if(this.$labelsContainer){
					var labelsCont = this.$labelsContainer;
					labelsCont.children(":visible").remove(); // clear container

					for (i = 0; i < imageData.labels.length; i++) {
						var label = imageData.labels[i];
						if(label !== ""){
							box = labelsCont.children(":hidden:first").clone();
							box.find("a:first").attr("href", function(){
								return this.href + label;
							}).text(label);
							box.find("a:last").attr("href", function(){
								return this.href + label + "&uuid=" + imageData.uuid;
							});
							labelsCont.append(box.addClass(this.labelBoxClass).show());
						}
					}
					// update labels form mid
					$(this.addLabelFormSel+" input[name=mid]").val(this.currentIndex);
				}

				return this;
			},

			getImageInfo: function(imageData){
				if(this.$captionContainer && this.$imageContainer){
					var title = imageData.title;
					var caption = imageData.description;
					var originalUrl = imageData.originalurl;

					var currentImage = this.$imageContainer.find('img').get(0);

					currentImage.alt = imageData.title;
					currentImage.src = imageData.url;
					currentImage.height = imageData.height;
					currentImage.width = imageData.width;

					this.$captionContainer.find(this.titleSel).text(title);
					this.$captionContainer.find(this.captionSel).text(caption);
					var origlink = this.$captionContainer.find(this.originalLinkSel)
						.children('a');

					if(!originalUrl || $.trim(originalUrl) === ""){
						$(this.originalLinkSel).hide();
					}else if(origlink.length){
						origlink.attr("href", originalUrl);
					}

				}
				return this;
			},

			updateNavLinks: function(){
				// Update Controls
				if (this.$controlsContainer && this.$imageContainer) {
					var gallery = this;

					this.$controlsContainer.find(this.prevLinkSel)
						.attr('href', function(){
							return this.href.substring(0, this.href.lastIndexOf("#") + 1)
								+ gallery.prevIndex;
						})
						.end()
						.find(this.nextLinkSel)
						.attr('href', function(){
							return this.href.substring(0, this.href.lastIndexOf("#") + 1)
								+ gallery.nextIndex;
						});

					// click photo to go to next
					this.$imageContainer.find('img')
						.parent('a')
						.attr('href', function(){
							return this.href.substring(0, this.href.lastIndexOf("#") + 1)
								+ gallery.nextIndex;
						})
						.click(function(e) {
							gallery.clickHandler(e, this);
						}
					);
				}
				return this;
			},

			showPhoto: function(imageData){
				var currentImage = this.$imageContainer.find('img:first');
				
				if (imageData.image){ 
					this.getImageInfo(imageData);

					//show the image
					if (this.$loadingContainer.is(":visible")) {
						this.$loadingContainer.hide();
					}
					currentImage.show();
				} else {

					// Show loading container
					if (gallery.$loadingContainer) {
						currentImage.hide();
						gallery.$loadingContainer.show();
					}

					var image = new Image();

					// Wire up mainImage onload event
					image.onload = function() {
						imageData.image = this;
						gallery.showPhoto(imageData);
					};

					image.src = imageData.url;
					image.alt = imageData.title;
				}

				return this;
			},

			isPreloadComplete: false,

			preload: function(index){
				var gallery = this;
				var imageData = this.data[index];
				// Preload if no data found, otherwise continue
				if (!imageData){
					this.isPreloadComplete = true;
				}else if (!imageData.image){
					// Preload the image
					var image = new Image();
					image.onload = function() {
						imageData.image = this;
					};

					image.alt = imageData.title;
					image.src = imageData.url;
				}
				return this;
			},

			preloadInit: function(index) {
				if (this.preloadAhead === 0) {return this;}
				this.preloadStartIndex = index;
				this.isPreloadComplete = false;

				var preloadThese = [this.prevIndex, this.nextIndex];

				for(var indx in preloadThese){
					var imageData = this.data[preloadThese[indx]];
					if (!imageData){
						this.isPreloadComplete = true;
						break;
					}
					// Preload if no data found, otherwise continue
					if (!imageData.image){
						// Preload the image
						var image = new Image();
						image.onload = function() {
							imageData.image = this;
						};

						image.alt = imageData.title;
						image.src = imageData.url;
					}
				}

				return this;
			},

			pause: function() {
				if (this.interval){this.toggleSlideshow();}
				return this;
			},

			play: function() {
				if (!this.interval){this.toggleSlideshow();}
				return this;
			},

			toggleSlideshow: function() {
				if (this.interval) {
					clearInterval(this.interval);
					this.interval = 0;
					
					if (this.$slideshowToggle) {
						this.$slideshowToggle.removeClass('pause').addClass('play')
							.attr('title', this.playLinkText)
							.attr('href', '#play')
							.find("span:contains(6)").text("4");
					}
				} else {
					var gallery = this;
					this.interval = setInterval(function() {
						gallery.ssAdvance();
					}, this.delay);
					
					if (this.$slideshowToggle) {
						this.$slideshowToggle.removeClass('play').addClass('pause')
							.attr('title', this.pauseLinkText)
							.attr('href', '#pause')
							.find("span:contains(4)").text("6");
					}
				}

				return this;
			},

			ssAdvance: function() {
				// Seems to be working on both FF and Safari
				if (this.enableHistory){
					// At the moment, historyLoad only accepts string arguments
					$.history.load(String(this.nextIndex));
				}else{
					this.go2(this.nextIndex);
				}

				return this;
			},

			next: function() {
				this.pause();
				go2(this.nextIndex);
			},

			previous: function() {
				this.pause();
				go2(this.prevIndex);
			},

			go2: function(index) {
				var oldCurrentIndex = this.currentIndex;
				this.currentIndex = index;

				var gallery = this;
				var imageData = this.data[this.currentIndex];
				if (!imageData){
					return gallery.getData(this.currentIndex, oldCurrentIndex, true, 0);
				}else if(index === this.nextIndex){
					//end of page near so get next page of data
					gallery.getData(this.nextIndex, oldCurrentIndex, false, 1);
				}else if(index === this.prevIndex){
					gallery.getData(this.prevIndex, oldCurrentIndex, false, -1);
				}
				
				this.getComments(imageData);
				this.getVotes(imageData);
				this.getLabels(imageData); 
				this.showPhoto(imageData);

				return this;
			}
			
		});

		// Now initialize the gallery
		$.extend(this, defaults, settings);

		if (this.interval)	{clearInterval(this.interval);}
		this.interval = 0;
		this.currentIndex = null;
		var gallery = this;
		this.data = {};
		
		if(this.imageDataInit && this.imageDataInit.length == 3){
			this.currentIndex = this.imageDataInit[0].id;
			this.prevIndex = this.imageDataInit[1].id;
			this.nextIndex = this.imageDataInit[2].id;
			this.data[this.currentIndex] = this.imageDataInit[0];
			this.data[this.prevIndex] = this.imageDataInit[1];
			this.data[this.nextIndex] = this.imageDataInit[2];
		}

		// Verify the history plugin is available
		if (this.enableHistory && !$.history.init){this.enableHistory = false;}
		
		// Select containers
		if (this.imageContainerSel) {this.$imageContainer = $(this.imageContainerSel);}
		if (this.captionContainerSel) {this.$captionContainer = $(this.captionContainerSel);}
		if (this.labelsContainerSel) {this.$labelsContainer = $(this.labelsContainerSel);}
		if (this.loadingContainerSel) {this.$loadingContainer = $(this.loadingContainerSel);}
		if (this.slideshowToggleSel) {this.$slideshowToggle = $(this.slideshowToggleSel);}
		if (this.commentFormSel){this.$commentForm = $(this.commentFormSel);}
		if (this.commentsContainerSel) {this.$commentsContainer = $(this.commentsContainerSel);}
		
		if (this.maxPagesToShow < 3) {this.maxPagesToShow = 3;}

		// Hide the loadingContainer
		if (this.$loadingContainer) {this.$loadingContainer.hide();}

		// Setup controls
		if (this.controlsContainerSel) {
			this.$controlsContainer = $(this.controlsContainerSel);
			
			//hidden if no javascript
			this.$slideshowToggle.show();

			this.$slideshowToggle.click(function(e) {
				gallery.toggleSlideshow();
				e.preventDefault();
				return false;
			});

			this.$controlsContainer.children("a").click(function(e) {
				gallery.clickHandler(e, this);
			});
		}

		if (this.autoStart) {
			setTimeout(function() {gallery.play();}, this.delay);
		}

		// Kickoff Image Preloader after 1 second
		setTimeout(function() {gallery.preloadInit(gallery.currentIndex);}, 1000);

		return this;
	};
})(jQuery);
