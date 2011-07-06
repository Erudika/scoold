(function($) {

	$.oembed = {

		// Plugin defaults
		filter: "default",
		maxWidth: 500,
		maxHeight: 400,
//		oembedServiceUrl: "http://oohembed.com/oohembed/",
		oembedServiceUrl: "http://api.embed.ly/v1/api/oembed",
		callbackParam: "callback",
		userAgent: "Mozilla/5.0 (compatible; Scoold/0.1; +http://erudika.com)",

		getThumb: function(data, filterParam) {
			var embedElem = null;
			var filter = filterParam ? filterParam : $.oembed.filter;

			if(data){
				var desc = (data.description) ? "<p>"+data.description+"</p>" : "";
				if(filter === "default" || data.type === filter){
					if(data.thumbnail_url){
						embedElem = $("<img src='"+data.thumbnail_url+"' alt='loading...'/>"+desc);
					}else if(data.url){
						var h = (data.height * 30) / 100;
						var w = (data.width * 30) / 100;
						embedElem = $("<img height='"+h+"' width='"+w+"' src='"+data.url+"' alt='loading...'/>"+desc);
					}
				}
			}

			return embedElem;
		},

		fetchData: function(url, callback) {
			if(!url || url === "" || url.indexOf("http") == -1){
				return;
			}
			
			$.ajax({
				beforeSend: function(xhr) {xhr.setRequestHeader("User-Agent", $.oembed.userAgent)},
				dataType: "jsonp",
				url: $.oembed.getRequestUrl(url),
				success: function(data, status, xhr){
					if(data){
						var code = data.html;
						switch (data.type) {
							case "photo":
								var alt = data.title ? data.title : '';
								alt += data.author_name ? ' - ' + data.author_name : '';
								alt += data.provider_name ? ' - ' +data.provider_name : '';
								code = '<div><a href="' + url + '" class="extlink"><img src="' +
									data.url + '" alt="' + alt + '"/></a></div>';
								if (data.html){
									code += "<div>" + data.html + "</div>";
								}
								break;
//							case "video":break;
//							case "rich":break;
							default:
//								var title = (data.title != null) ? data.title : url;
								if (code){
									code = "<div>" + code + "</div>";
								}
								break;
						}
						data.html = code;
						callback(data);
					}

				}
			});

		},

		getRequestUrl: function(externalUrl) {
			var url = $.oembed.oembedServiceUrl;
			if (url.indexOf("?") <= 0)  url = url + "?";

			url += "maxwidth=" + $.oembed.maxWidth +
						"&maxheight=" + $.oembed.maxHeight +
						"&format=json" +
						"&url=" + escape(externalUrl) +
						"&" + $.oembed.callbackParam + "=?";
			return url;
		}

	};
})(jQuery);