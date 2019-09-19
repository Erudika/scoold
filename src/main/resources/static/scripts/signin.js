/* global FB_APP_ID, gapi, FB, GOOGLE_CLIENT_ID, GITHUB_APP_ID, LINKEDIN_APP_ID, APPID, ENDPOINT, TWITTER_APP_ID, MICROSOFT_APP_ID, CONTEXT_PATH, OAUTH2_APP_ID, OAUTH2_SCOPE, OAUTH2_ENDPOINT, SLACK_APP_ID, OAUTH2_SECOND_APP_ID, OAUTH2_SECOND_ENDPOINT, OAUTH2_SECOND_SCOPE, OAUTH2_THIRD_APP_ID, OAUTH2_THIRD_ENDPOINT, OAUTH2_THIRD_SCOPE */
/************************
 * Facebook integration *
 ************************/
if (FB_APP_ID && FB_APP_ID.trim() !== "") {
	(function(d, s, id) {
		var js, fjs = d.getElementsByTagName(s)[0];
		if (d.getElementById(id)) return;
		js = d.createElement(s); js.id = id;
		js.src = "https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v3.0&appId=" + FB_APP_ID;
		fjs.parentNode.insertBefore(js, fjs);
	}(document, 'script', 'facebook-jssdk'));

	$('#fb-login-btn').on('click', function () {
		FB.login(function(response) {
			if (response.authResponse) {
				window.location = CONTEXT_PATH + "/signin?provider=facebook&access_token=" + response.authResponse.accessToken;
			} else {
				window.location = CONTEXT_PATH + "/signin?code=3&error=true";
			}
		}, {scope: 'public_profile,email'});
		return false;
	});
}
/**********************
 * Google integration *
 **********************/
if (GOOGLE_CLIENT_ID && GOOGLE_CLIENT_ID.trim() !== "") {
	(function(d, s, id) {
		var js, gjs = d.getElementsByTagName(s)[0];
		if (d.getElementById(id)) return;
		js = d.createElement(s); js.id = id;
		js.src = "https://apis.google.com/js/api:client.js";
		js.addEventListener('load', function (e) { gpLogin(); }, false);
		gjs.parentNode.insertBefore(js, gjs);
	}(document, 'script', 'google-jssdk'));

	function gpLogin() {
		if ($('#gp-login-btn').length) {
			gapi.load('auth2', function(){
				gapi.auth2.init({
					client_id: GOOGLE_CLIENT_ID,
					scope: 'openid email'
				}).attachClickHandler($('#gp-login-btn').get(0), {}, function(googleUser) {
					window.location = CONTEXT_PATH + "/signin?provider=google&access_token=" +
							googleUser.getAuthResponse(true).access_token;
				}, function(error) {
					window.location = CONTEXT_PATH + "/signin?code=3&error=true";
				});
			});
		}
	}
}
/**********************
 * GitHub integration *
 **********************/
if (GITHUB_APP_ID && GITHUB_APP_ID.trim() !== "") {
	$('#gh-login-btn').on('click', function () {
		window.location = "https://github.com/login/oauth/authorize?" +
				"response_type=code&client_id=" + GITHUB_APP_ID +
				"&scope=user%3Aemail&state=" + APPID +
				"&redirect_uri=" + ENDPOINT + "/github_auth";
		return false;
	});
}
/************************
 * LinkedIn integration *
 ************************/
if (LINKEDIN_APP_ID && LINKEDIN_APP_ID.trim() !== "") {
	$('#in-login-btn').on('click', function () {
		window.location = "https://www.linkedin.com/oauth/v2/authorization?" +
				"response_type=code&client_id=" + LINKEDIN_APP_ID +
				"&scope=r_liteprofile%20r_emailaddress&state=" + APPID +
				"&redirect_uri=" + ENDPOINT + "/linkedin_auth";
		return false;
	});
}
/***********************
 * Twitter integration *
 ***********************/
if (TWITTER_APP_ID && TWITTER_APP_ID.trim() !== "") {
	$('#tw-login-btn').on('click', function () {
		window.location = ENDPOINT + "/twitter_auth?state=" + APPID;
		return false;
	});
}
/*************************
 * Microsoft integration *
 *************************/
if (MICROSOFT_APP_ID && MICROSOFT_APP_ID.trim() !== "") {
	$('#ms-login-btn').on('click', function () {
		window.location = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?" +
                "response_type=code&client_id=" + MICROSOFT_APP_ID +
                "&scope=https%3A%2F%2Fgraph.microsoft.com%2Fuser.read&state=" + APPID +
                "&redirect_uri=" + ENDPOINT + "/microsoft_auth";
		return false;
	});
}
/*************************
 * Slack integration     *
 *************************/
if (SLACK_APP_ID && SLACK_APP_ID.trim() !== "") {
	$('#sl-login-btn').on('click', function () {
		window.location = "https://slack.com/oauth/authorize?" +
                "response_type=code&client_id=" + SLACK_APP_ID +
                "&scope=identity.basic%20identity.email%20identity.team%20identity.avatar&state=" + APPID +
                "&redirect_uri=" + ENDPOINT + "/slack_auth";
		return false;
	});
}
/*************************
 * OAuth 2.0 integration *
 *************************/
if (OAUTH2_APP_ID && OAUTH2_APP_ID.trim() !== "") {
	$('#oa2-login-btn').on('click', function () {
		window.location = OAUTH2_ENDPOINT + "?" +
                "response_type=code&client_id=" + OAUTH2_APP_ID +
                "&scope=" + OAUTH2_SCOPE + "&state=" + APPID +
                "&redirect_uri=" + ENDPOINT + "/oauth2_auth";
		return false;
	});
}
/********************************
 * Second OAuth 2.0 integration *
 ********************************/
if (OAUTH2_SECOND_APP_ID && OAUTH2_SECOND_APP_ID.trim() !== "") {
	$('#oa2second-login-btn').on('click', function () {
		window.location = OAUTH2_SECOND_ENDPOINT + "?" +
                "response_type=code&client_id=" + OAUTH2_SECOND_APP_ID +
                "&scope=" + OAUTH2_SECOND_SCOPE + "&state=" + APPID +
                "&redirect_uri=" + ENDPOINT + "/oauth2second_auth";
		return false;
	});
}
/*******************************
 * Third OAuth 2.0 integration *
 *******************************/
if (OAUTH2_THIRD_APP_ID && OAUTH2_THIRD_APP_ID.trim() !== "") {
	$('#oa2third-login-btn').on('click', function () {
		window.location = OAUTH2_THIRD_ENDPOINT + "?" +
                "response_type=code&client_id=" + OAUTH2_THIRD_APP_ID +
                "&scope=" + OAUTH2_THIRD_SCOPE + "&state=" + APPID +
                "&redirect_uri=" + ENDPOINT + "/oauth2third_auth";
		return false;
	});
}
/********************
 * LDAP integration *
 ********************/
$('#ldap-login-form').on('submit', function () {
	var username = $("#username").val();
	var password = $("#password").val();
	if (username && password) {
		$(this).find("input[name='access_token']").val(username + ":" + password);
	}
});
/*******************
 *  Password Auth  *
 *******************/
$('#password-login-form').on('submit', function () {
	var email = $("#email").val();
	var passw = $("#passw").val();
	if (email && passw) {
		$(this).find("input[name='access_token']").val(email + "::" + passw);
	}
});