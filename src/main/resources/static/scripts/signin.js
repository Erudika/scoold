/* global FB_APP_ID, gapi, FB, GOOGLE_CLIENT_ID */

(function(d, s, id) {
	var js, fjs = d.getElementsByTagName(s)[0];
	if (d.getElementById(id)) return;
	js = d.createElement(s); js.id = id;
	js.src = "//connect.facebook.net/en_US/sdk.js#xfbml=1&version=v2.8&appId=" + FB_APP_ID;
	fjs.parentNode.insertBefore(js, fjs);
}(document, 'script', 'facebook-jssdk'));

gapi.load('auth2', function(){
	auth2 = gapi.auth2.init({
		client_id: GOOGLE_CLIENT_ID,
		scope: 'https://www.googleapis.com/auth/plus.me'
	});

	auth2.attachClickHandler(document.getElementById('g-login-btn'), {}, function(googleUser) {
		window.location = "/signin?provider=google&access_token=" + googleUser.getAuthResponse(true).access_token;
	}, function(error) {
		window.location = "/signin?code=3&error=true";
	});
});

function fbLogin() {
	FB.login(function(response) {
		if (response.authResponse) {
			window.location = "/signin?provider=facebook&access_token=" + response.authResponse.accessToken;
		} else {
			window.location = "/signin?code=3&error=true";
		}
	}, {scope: 'public_profile,email'});
	return false;
}

if( document.body.attachEvent) {
    document.getElementById('fb-login-btn').attachEvent("onclick", fbLogin);
} else {
    document.getElementById('fb-login-btn').addEventListener("click", fbLogin);
}
