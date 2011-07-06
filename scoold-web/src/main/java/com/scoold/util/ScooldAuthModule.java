package com.scoold.util;

import com.scoold.core.User;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.pages.BasePage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.aikesommer.authenticator.AuthenticationRequest;
import name.aikesommer.authenticator.AuthenticationRequest.ManageAction;
import name.aikesommer.authenticator.AuthenticationRequest.Status;
import name.aikesommer.authenticator.PluggableAuthenticator;
import name.aikesommer.authenticator.RequestHandler;
import name.aikesommer.authenticator.SimplePrincipal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.openid4java.OpenIDException;
import org.openid4java.consumer.ConsumerManager;
import org.openid4java.consumer.VerificationResult;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.Identifier;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageExtension;
import org.openid4java.message.ParameterList;
import org.openid4java.message.ax.AxMessage;
import org.openid4java.message.ax.FetchRequest;
import org.openid4java.message.ax.FetchResponse;
import org.openid4java.message.sreg.SRegMessage;
import org.openid4java.message.sreg.SRegRequest;
import org.openid4java.message.sreg.SRegResponse;

/**
 *
 * @author alexb
 */
public class ScooldAuthModule extends PluggableAuthenticator { //ServletAuthModule

	// development mode => bypasses authentication if 'true'
	private static boolean DEVEL_MODE = false;

	public static final String OPENID_ACTION = "openid_auth";
	public static final String FB_CONNECT_ACTION = "fbconnect_auth";
	public static final String ATTACH_OPENID_ACTION = "attach_openid";	
	public static final String ATTACH_FACEBOOK_ACTION = "attach_facebook";	
	public static final String OPENID_IDENTIFIER = "openid_identifier";
	public static final String IDENTIFIER = "identifier";
	public static final String FB_USER_CLIENT = "facebook.user.client";
	public static final String SIGNUP_SUCCESS = "signup-success";
	public static final String NEW_USER = "new-user";
	public static final String THIS_IS_ME = "this-is-me";
	public static final String OPENID = "openid";
	public static final String FACEBOOK = "facebook";
	//the keys for FB connect - used throughout the app
	public static final String FB_APP_ID = BasePage.FB_APP_ID;
	public static final String FB_API_KEY = BasePage.FB_API_KEY;
	private static final String FB_SECRET = "955b8c8bca778a1476d620ab36b762ef";
	public static final String CONSUMER_MANAGER = "consumer-manager";
	public static final String DISCOVERY_INFO = "discovery-info";
	
	private static final String SIGNIN_PAGE = "/signin";
	private static final String SIGNUP_PAGE = "/signup";
	private static final String SETTINGS_PAGE = "/settings";
	
	private static final String HOME = "/";

	private static RequestHandler reqHandler = new RequestHandler();
	private static final Logger logger = Logger.getLogger(ScooldAuthModule.class.getName());
	private static final ConsumerManager consman = new ConsumerManager();

	public Status tryAuthenticate(AuthenticationManager manager, AuthenticationRequest request) {
		// authentication finishes here if successful...
		SimplePrincipal sp = SimplePrincipal.getPrincipal(request.getHttpServletRequest());
		if(manager.matchesRequest(request) && sp != null){
			manager.register(request, sp);
			manager.restoreRequest(request);
			return Status.Success;
		}
		
		return authAction(manager, request, null, null);
	}

	public Status authenticate(AuthenticationManager manager, AuthenticationRequest request) {
		manager.saveRequest(request);

		if(DEVEL_MODE){
			//override using facebook uid or openid
			tryCreatingPrincipal("517966023", manager, request);
			return Status.Success;
		}
		
		manager.forward(request, SIGNIN_PAGE);
		return Status.Continue;
	}

	public ManageAction manage(AuthenticationManager manager, AuthenticationRequest request) {
		return ManageAction.None;
	}
	
	private static Long getFacebookId(HttpServletRequest request){
		Cookie c = getCookie("fbs_"+FB_APP_ID, request.getCookies());
		Long id = null;
		if(isValidSignature(c)){
			id = NumberUtils.toLong(getFBCookieAttribute(c, "uid"));
		}

		return id;
	}

	private static String getFBCookieAttribute(Cookie c, String attributeName) {
		if(c == null) return null;

		String cookieValue = c.getValue();
		int startIndex = cookieValue.indexOf(attributeName);
		if (startIndex == -1) {
			return null;
		}
		int endIndex = cookieValue.indexOf("&", startIndex + 1);
		if (endIndex == -1) {
			endIndex = cookieValue.length();
		}
		return cookieValue.substring(startIndex	+ attributeName.length() + 1, endIndex);
	}

	private static Cookie getCookie(String name, Cookie[] cookies){
		if(cookies == null || cookies.length == 0) return null;
		Cookie c = null;
		for (int i = 0; i < cookies.length; i++) {
			c = cookies[i];
			if (c.getName().equals(name)) {
				break;
			}
		}
		return c;
	}

	private static boolean isValidSignature(Cookie fbCookie) {
		String signature = "";
		String sig = null;
		if (fbCookie != null) {
			ArrayList<String> list = new ArrayList<String>();
			for (String param : fbCookie.getValue().split("&")) {
				if(!param.startsWith("sig=")){
					list.add(param);
				}else{
					sig = param.substring(param.indexOf("=") + 1);
				}
			}

			Collections.sort(list);

			for (String param : list) {
				signature = signature.concat(param);
			}
			signature = AbstractDAOUtils.urlDecode(signature).concat(FB_SECRET);
		}

		return StringUtils.equals(AbstractDAOUtils.MD5(signature), sig);
	}
	
	private static void attachIdentifier(String openidURL, HttpServletRequest request){
		ScooldPrincipal<User> userPricipal = (ScooldPrincipal<User>) 
				SimplePrincipal.getPrincipal(request);
		if (userPricipal != null) {
			User authUser = userPricipal.getUser();
			//initCoreObjects(remoteUser);
			if (authUser != null) {
				authUser.attachIdentifier(openidURL);
			}
		}
	}

	private static void tryCreatingPrincipal(String identifier,
			AuthenticationManager manager,
			AuthenticationRequest request) {

		String reqUrl = reqHandler.getPathForRequest(request);
		User authUser = User.getUser(identifier);
		
		if (authUser == null) {			
			// new user! send to signup page			
			request.getSessionMap().put(IDENTIFIER, identifier);
			// "THIS IS ME" button logic
			if(reqUrl != null){
				Map<String, String> paramMap = getParamMap(reqUrl);
				String thisisme = paramMap.get("thisisme");

				if(!StringUtils.isBlank(thisisme)){
					try {
						thisisme = URLDecoder.decode(thisisme, "UTF-8");
					} catch (UnsupportedEncodingException ex) {}
					request.getSessionMap().put(THIS_IS_ME, thisisme);
				}
			}
			manager.forward(request, SIGNUP_PAGE);
		} else {
			//clean up a bit
			request.getSessionMap().remove(NEW_USER);
			request.getSessionMap().remove(IDENTIFIER);
			request.getSessionMap().remove(DISCOVERY_INFO);

			//is this account active??? 
			if (authUser.getActive()) {
				//update lastseen
				authUser.setLastseen(System.currentTimeMillis());
				authUser.setNewmessages(authUser.countNewMessages());
				authUser.update();

				ScooldPrincipal<User> principal = new ScooldPrincipal<User>
						(identifier, authUser, authUser.getGroups());

				SimplePrincipal.setPrincipal(request.getHttpServletRequest(), principal);

				if(manager.hasRequest(request)){
					//FINALLY: success. send back to request
					manager.redirectToRequest(request);
				}else{
					manager.forward(request, HOME);
				}
			} else {
				//account is not active - return to signin page
				manager.forward(request, SIGNIN_PAGE+ "?code=3&error=true");
			}			
		}
	}

	private static String buildAuthRequestUrl(String suppliedIdentifier,
			HttpServletRequest httpReq, ConsumerManager consmanager) {

		if (suppliedIdentifier == null) {
			return null;
		}

		String authRequestURL = null;
		try {
			// configure the return_to URL where your application will receive
			// the authentication responses from the OpenID provider
			//String returnToUrl = "http://localhost:8080"+BasePage.HOMEPAGE;
			String returnToUrl = httpReq.getRequestURL().toString() + "?return=true";

			// perform discovery on the user-supplied identifier
			List discoveries = consmanager.discover(suppliedIdentifier);

			// attempt to associate with the OpenID provider
			// and retrieve one service endpoint for authentication
			DiscoveryInformation discovered = consmanager.associate(discoveries);
			if (discovered == null) return null;

			// store the discovery information in the user's session
			httpReq.getSession().setAttribute(DISCOVERY_INFO, discovered);

			// Attribute Exchange extension
			FetchRequest fetch = FetchRequest.createFetchRequest();
			fetch.addAttribute("FullName", "http://axschema.org/namePerson", true);
			fetch.addAttribute("Email", "http://axschema.org/contact/email", true);
			fetch.addAttribute("Language", "http://axschema.org/pref/language", true);

			// Simple Registration extension
			SRegRequest sregReq = SRegRequest.createFetchRequest();
			sregReq.addAttribute("fullname", true);
			sregReq.addAttribute("email", true);
			sregReq.addAttribute("language", true);

			// obtain a AuthRequest message to be sent to the OpenID provider
			AuthRequest authReq = consmanager.authenticate(
					discovered, returnToUrl, httpReq.getRequestURL().toString());

			String port = "";
			if(httpReq.getServerName().contains("localhost")){
				port = ":" + httpReq.getServerPort();
			}
			authReq.setRealm("http://" + httpReq.getServerName() + port);

			if (!fetch.getAttributes().isEmpty()) {
				authReq.addExtension(fetch);
			}
			if (!sregReq.getAttributes().isEmpty()) {
				authReq.addExtension(sregReq);
			}

			if (!discovered.isVersion2()) {
				// Option 1: GET HTTP-redirect to the OpenID Provider endpoint
				// The only method supported in OpenID 1.x
				// redirect-URL usually limited ~2048 bytes
				authRequestURL = authReq.getDestinationUrl(true);
			} else {
				// Option 2: HTML FORM Redirection (Allows payloads >2048 bytes)
				//build auth request query and redirect to OP endpoint
				authRequestURL = rebuildQuery(authReq.getParameterMap(),
						authReq.getOPEndpoint());
			}

		} catch (OpenIDException e) {
			logger.severe(e.toString());
		}
		
		if(authRequestURL == null) return SIGNIN_PAGE + "?code=3&error=true";

		return authRequestURL;
	}

	private static String rebuildQuery(Map paramMap, String baseUrl) {
		StringBuilder sb = new StringBuilder(baseUrl);
		sb.append("?");
		try {
			for (Object paramKey : paramMap.keySet()) {
				sb.append(paramKey);
				sb.append("=");
				String paramValue = (String) paramMap.get(paramKey);
				sb.append(URLEncoder.encode(paramValue, "UTF-8"));
				sb.append("&");
			}
		} catch (UnsupportedEncodingException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		sb = sb.deleteCharAt(sb.length() - 1);	//remove trailing &amp;

		return sb.toString();
	}

	private static Map<String, String> getParamMap(String url){
		Map<String, String> map = new HashMap<String, String>();
		if(StringUtils.isBlank(url)) return map;
		String tp = url.substring(url.indexOf("?") + 1);
		if (!StringUtils.isBlank(tp)) {
			String[] params = tp.split("\\&");
			for (String param : params) {
				String[] kv = param.split("\\=");
				if(kv.length > 1) map.put(kv[0], kv[1]);
			}
		}
		return map;
	}

	private static Identifier verifyResponse(HttpServletRequest httpReq, 
			ConsumerManager consmanager) {
		try {
			// extract the parameters from the authentication response
			// (which comes in as a HTTP request from the OpenID provider)
			ParameterList response = new ParameterList(httpReq.getParameterMap());

			// retrieve the previously stored discovery information
			DiscoveryInformation discovered = (DiscoveryInformation)
					httpReq.getSession().getAttribute(DISCOVERY_INFO);
			if (discovered == null) {
				return null;
			}
			// extract the receiving URL from the HTTP request
			StringBuffer receivingURL = httpReq.getRequestURL();
			String queryString = httpReq.getQueryString();

			if (queryString != null && queryString.length() > 0) {
				receivingURL.append("?").append(httpReq.getQueryString());
			}
			// verify the response; ConsumerManager needs to be the same
			// (static) instance used to place the authentication request
			VerificationResult verification =
					consmanager.verify(receivingURL.toString(), response, discovered);

			// examine the verification result and extract the verified
			// identifier
			Identifier verified = verification.getVerifiedId();
			if (verified != null) {

				AuthSuccess authSuccess = (AuthSuccess) verification.getAuthResponse();

				String axFullName = null;
				String axEmail = null;

				String sregFullName = null;
				String sregEmail = null;

				if (authSuccess.hasExtension(AxMessage.OPENID_NS_AX)) {
					MessageExtension ext = authSuccess.getExtension(AxMessage.OPENID_NS_AX);

					if (ext instanceof FetchResponse) {
						FetchResponse fetchResp = (FetchResponse) ext;

						axFullName = fetchResp.getAttributeValue("FullName");
						axEmail = fetchResp.getAttributeValue("Email");
					}
				}
				if (authSuccess.hasExtension(SRegMessage.OPENID_NS_SREG)) {
					MessageExtension ext = authSuccess.getExtension(SRegMessage.OPENID_NS_SREG);

					if (ext instanceof SRegResponse) {
						SRegResponse sregResp = (SRegResponse) ext;

						sregFullName = sregResp.getAttributeValue("fullname");
						sregEmail = sregResp.getAttributeValue("email");
					}
				}

				User newUser = new User();

				if (sregFullName != null) {
					newUser.setFullname(StringUtils.trimToEmpty(sregFullName));
				} else {
					newUser.setFullname(StringUtils.trimToEmpty(axFullName));
				}

				if (sregEmail == null) {
					newUser.setEmail(axEmail);
				} else {
					newUser.setEmail(sregEmail);
				}
				
				httpReq.getSession().setAttribute(NEW_USER, newUser);

				return verified; // success
			}
		} catch (OpenIDException e) {
			logger.log(Level.SEVERE, "Openid response verification failed: {0}", e);
		}

		return null;	//failure
	}
		
	public static Status authAction(AuthenticationManager manager, AuthenticationRequest authreq,
			HttpServletRequest request, HttpServletResponse response){
		if(request == null){
			if(authreq == null) return Status.Failure;
			else request = authreq.getHttpServletRequest();
		}
		
		if(response == null){
			if(authreq == null) return Status.Failure;
			else response = authreq.getHttpServletResponse();
		}
		
		String requestURI = request.getRequestURI();
		boolean isAttachReq = requestURI.endsWith(ATTACH_OPENID_ACTION) || 
				requestURI.endsWith(ATTACH_FACEBOOK_ACTION);
		String forwardTo = isAttachReq ? SETTINGS_PAGE + "?code=6&error=true" :
					SIGNIN_PAGE + "?code=3&error=true";
		
		boolean isOpenidReq = isAttachReq ? requestURI.endsWith(ATTACH_OPENID_ACTION) : 
				requestURI.endsWith(OPENID_ACTION);
		boolean isFaceookReq = isAttachReq ? requestURI.endsWith(ATTACH_FACEBOOK_ACTION) : 
				requestURI.endsWith(FB_CONNECT_ACTION);
		
		if (isOpenidReq) {
			//OpenID Authentication
			String openidFromUser = request.getParameter(OPENID_IDENTIFIER);

			if (openidFromUser != null) {
				// Coming from signin page? get identifier and redirect to OIDP
				//prepare oidp url for openid auth. request
				//1.discovery of OP server
				//2.request info via AX or SReg
				//3.build the openid auth request
				String oidpRequestURL = buildAuthRequestUrl(openidFromUser, request, consman);

				if (oidpRequestURL != null) {
					//save and redirect to oidp
					//manager.saveRequest(request);
					forward(manager, authreq, request, response, oidpRequestURL);
					return Status.Continue;
				} else {										
					forward(manager, authreq, request, response, forwardTo);
					return Status.Failure;
				}
			} else if ("true".equals(request.getParameter("return"))) {
				// Coming back from oidp? then process return
				Identifier identifier = verifyResponse(request, consman);
				if (identifier == null) {
					//error! send back to signin page					
					forward(manager, authreq, request, response, forwardTo);
					return Status.Failure;
				} else {
					//success!					
					if (isAttachReq) {						
						attachIdentifier(identifier.getIdentifier(), request);
						forward(manager, authreq, request, response, SETTINGS_PAGE);
					} else {
						String openidURL = identifier.getIdentifier();
						tryCreatingPrincipal(openidURL, manager, authreq);
					}					
					return Status.Continue;
				}
			} else if ("true".equals(request.getParameter(SIGNUP_SUCCESS))) {
				// Coming back from signup page? try creating principal
				String openidURL = (String) request.getSession(true).getAttribute(IDENTIFIER);
				tryCreatingPrincipal(openidURL, manager, authreq);
				return Status.Continue;
			}
			//identifier is null or something else... -> signin page			
			forward(manager, authreq, request, response, forwardTo);
			return Status.Continue;
		} else if (isFaceookReq) {
			//Facebook Connect Authentication 
			Long fbUserID = getFacebookId(request);
			if (fbUserID != null && fbUserID != 0L) {
				//success!
				if (isAttachReq) {					
					attachIdentifier(fbUserID.toString(), request);
					forward(manager, authreq, request, response, SETTINGS_PAGE);
				} else {
					tryCreatingPrincipal(fbUserID.toString(), manager, authreq);
				}
				return Status.Continue;
			}

			//user is not signed in to facebook -> signin page			
			forward(manager, authreq, request, response, forwardTo);
			return Status.Continue;
		}
		
		return Status.None;
	}
	
	private static void forward(AuthenticationManager manager, AuthenticationRequest authreq, 
			HttpServletRequest request, HttpServletResponse response, String to){
		if((manager == null && authreq == null && response == null) 
				|| StringUtils.isBlank(to)) return;
		
		if(manager != null && authreq != null){
			manager.forward(authreq, to);
		}else if(response != null){
			try {			
				String cp = request.getContextPath();
				if(!to.startsWith(cp) && !to.startsWith("http://") 
						&& !to.startsWith("https://")) to = cp + to;
				response.sendRedirect(to);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
	}
}
