package com.scoold.util;

import com.scoold.core.Language;
import com.scoold.core.User;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import name.aikesommer.authenticator.AuthenticationRequest;
import name.aikesommer.authenticator.AuthenticationRequest.ManageAction;
import name.aikesommer.authenticator.AuthenticationRequest.Status;
import name.aikesommer.authenticator.PluggableAuthenticator;
import name.aikesommer.authenticator.RequestHandler;
import name.aikesommer.authenticator.SimplePrincipal;
import org.apache.click.Context;
import org.apache.click.util.ClickUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
	private static boolean DEVEL_MODE = !BooleanUtils.
			toBoolean(System.getProperty("com.scoold.production"));

	public static final boolean USE_SESSIONS = false;
	public static final String OPENID_ACTION = "openid_auth";
	public static final String FB_CONNECT_ACTION = "facebook_auth";
	public static final String FB_COOKIE_PREFIX = "fbsr_";
	public static final String ATTACH_OPENID_ACTION = "attach_openid";	
	public static final String ATTACH_FACEBOOK_ACTION = "attach_facebook";	
	public static final String OPENID_IDENTIFIER = "openid_identifier";
	public static final String IDENTIFIER = "identifier";
	public static final String AUTH_USER = "auth-user";
	public static final String RETURNTO = "return-to";
	public static final String SIGNUP_SUCCESS = "signup-success";
	public static final String NEW_USER_NAME = "new-user-name";
	public static final String NEW_USER_EMAIL = "new-user-email";
	public static final String THIS_IS_ME = "this-is-me";
	public static final String OPENID = "openid";
	public static final String FACEBOOK = "facebook";
	public static final String FB_APP_ID = System.getProperty("com.scoold.fbappid");
	public static final String FB_API_KEY = System.getProperty("com.scoold.fbapikey");
	private static final String FB_SECRET = System.getProperty("com.scoold.fbsecret");
	public static final String CONSUMER_MANAGER = "consumer-manager";
//	public static final String DISCOVERY_INFO = "discovery-info";
	
	private static final String SIGNIN_PAGE = "/signin";
	private static final String SIGNUP_PAGE = "/signup";
	private static final String SETTINGS_PAGE = "/settings";
	
	private static final String HOME = "/";

	private static RequestHandler reqHandler = new RequestHandler();
	private static final Logger logger = Logger.getLogger(ScooldAuthModule.class.getName());
	private static final ConsumerManager consman = new ConsumerManager();
	private static final ObjectMapper mapper = new ObjectMapper();

	public Status tryAuthenticate(AuthenticationManager manager, AuthenticationRequest request) {
		// authentication finishes here if successful...
		SimplePrincipal sp = getPrincipal(request.getHttpServletRequest(), request.getHttpServletResponse());
		if(matchesRequest(manager, request) && sp != null){
			manager.register(request, sp);
			restoreRequest(manager, request);
			return Status.Success;
		}
		
		return authAction(manager, request, null, null);
	}

	public Status authenticate(AuthenticationManager manager, AuthenticationRequest request) {
		saveRequest(manager, request);

		if(ClickUtils.isAjaxRequest(request.getHttpServletRequest())){
			return expiredResponse(request);
		}
		
		if(DEVEL_MODE && request.isMandatory()){
			//override using facebook uid or openid
			tryCreatingPrincipal("517966023", manager, request);
			return Status.Success;
		}

		forward(manager, request, null, null, SIGNIN_PAGE);
		
		return Status.Continue;
	}

	public ManageAction manage(AuthenticationManager manager, AuthenticationRequest request) {
		return ManageAction.None;
	}
	
	private void saveRequest(AuthenticationManager manager, AuthenticationRequest request){
		if (USE_SESSIONS) {
			manager.saveRequest(request);
		} else {
			String uri = request.getRequestPath();
			String qs = request.getHttpServletRequest().getQueryString();
			if(!StringUtils.isBlank(qs)){
				uri += "?" + qs;
			}
			AbstractDAOUtils.setStateParam(RETURNTO, uri, request.getHttpServletRequest(), 
					request.getHttpServletResponse(), USE_SESSIONS);
		}
	}
	
	private static void restoreRequest(AuthenticationManager manager, AuthenticationRequest request){
		if(USE_SESSIONS){
			manager.restoreRequest(request);
		}else{
			AbstractDAOUtils.removeStateParam(RETURNTO, request.getHttpServletRequest(), 
					request.getHttpServletResponse(), USE_SESSIONS);
		}
	}
	
	private boolean matchesRequest(AuthenticationManager manager, AuthenticationRequest request){
		if (USE_SESSIONS) {
			return manager.matchesRequest(request);
		} else {
//			String originalPath = AbstractDAOUtils.getStateParam(REQUEST_PATH_NOTE);
//			String path = request.getRequestPath();
//			String originalContext = (String) session(request).get(REQUEST_CONTEXT_NOTE);
//			String context = request.getOriginalContext().getContextPath();
//
//			if (originalPath != null && originalContext != null) {
//				return path.equals(originalPath) && context.equals(originalContext);
//			}
//			String returnto = AbstractDAOUtils.getStateParam(RETURNTO, request.getHttpServletRequest(), 
//					request.getHttpServletResponse(), BasePage.USE_SESSIONS);
			
			return true;
		}
	}
		
	private static void redirectToRequest(AuthenticationManager manager, AuthenticationRequest request){
		if (USE_SESSIONS) {
			manager.redirectToRequest(request);
		} else {
			String returnto = AbstractDAOUtils.getStateParam(RETURNTO, request.getHttpServletRequest(), 
					request.getHttpServletResponse(), USE_SESSIONS);
			restoreRequest(manager, request);
			String returnurl = StringUtils.isBlank(returnto) ? HOME : returnto;
			forward(manager, request, null, null, returnurl);
		}
	}
	
	private SimplePrincipal getPrincipal(HttpServletRequest req, HttpServletResponse res){
		if (USE_SESSIONS) {
			return SimplePrincipal.getPrincipal(req);
		} else {
			String authToken = AbstractDAOUtils.getStateParam(AUTH_USER, req, res, USE_SESSIONS);
			if (!StringUtils.isBlank(authToken) && StringUtils.contains(authToken, AbstractDAOFactory.SEPARATOR)) {
				String[] tuparts = authToken.split(AbstractDAOFactory.SEPARATOR);
				
				Long uid = NumberUtils.toLong(tuparts[0], 0L);
				String savedKey = tuparts[1];
				User u = null;
				
				if(uid.longValue() != 0L){
					u = User.getUser(uid);
				}
				
				if(u != null && u.getAuthstamp() != null){
					String[] groups = StringUtils.split(u.getGroups(), ',');
					Long authstamp = u.getAuthstamp();
					long now = System.currentTimeMillis() ;
					long expires = authstamp + (AbstractDAOFactory.SESSION_TIMEOUT_SEC * 1000);

					String authKey = AbstractDAOUtils.MD5(authstamp.toString().
							concat(AbstractDAOFactory.SEPARATOR).concat(uid.toString()));

					if (now <= expires && authKey.equals(savedKey)) {
						return new SimplePrincipal(uid.toString(), groups);
					}
				}
				clearAuthCookie(req, res);
			}
			return null;
		}
	}
	
	private static void setPrincipal(SimplePrincipal sp, Long timestamp, 
			HttpServletRequest req, HttpServletResponse res){
		if (USE_SESSIONS) {
			SimplePrincipal.setPrincipal(req, sp);
		} else {
			String userid = sp.getName();
			
			if(sp != null && !StringUtils.isBlank(userid) && !sp.getGroups().isEmpty()){
				String authKey = AbstractDAOUtils.MD5(timestamp.toString().
						concat(AbstractDAOFactory.SEPARATOR).concat(userid));
				String auth = userid.concat(AbstractDAOFactory.SEPARATOR).concat(authKey);
								
				AbstractDAOUtils.setStateParam(AUTH_USER, auth, 
						req, res, USE_SESSIONS, true);					
			}
		}
	}
	
	private static Long getFacebookId(HttpServletRequest request){
		String cookie = ClickUtils.getCookieValue(request, FB_COOKIE_PREFIX+FB_APP_ID);
		if(StringUtils.isBlank(cookie)) return null;
		
		Long id = null;
		
		try {
			String[] parts = cookie.split("\\.");
			byte[] sig = Base64.decodeBase64(parts[0]);
			byte[] json = Base64.decodeBase64(parts[1]);
			byte[] plaintext = parts[1].getBytes();	// careful, we compute against the base64 encoded version
			
			JsonNode root = mapper.readTree(new String(json));
			JsonNode alg = root.get("algorithm");
			if(alg != null){
				// "HMAC-SHA256" doesn't work, but "HMACSHA256" does.
				String algorithm = alg.getTextValue().replace("-", "");

				SecretKey secret = new SecretKeySpec(FB_SECRET.getBytes(), algorithm);

				Mac mac = Mac.getInstance(algorithm);
				mac.init(secret);
				byte[] digested = mac.doFinal(plaintext);

				if(Arrays.equals(sig, digested)){
					id = NumberUtils.toLong(root.get("user_id").getTextValue());
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to decode cookie", ex);
		}

		return id;
	}
	
	private static void attachIdentifier(String openidURL, HttpServletRequest request){
		if (request.getRemoteUser() != null) {
			User authUser = User.getUser(NumberUtils.toLong(request.getRemoteUser(),0));
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
		
		HttpServletRequest req = request.getHttpServletRequest();
		HttpServletResponse res = request.getHttpServletResponse();
		
		if (authUser == null) {	
			// new user! send to signup page			
			AbstractDAOUtils.setStateParam(IDENTIFIER, identifier, req, res, USE_SESSIONS);
			// "THIS IS ME" button logic
			if(reqUrl != null){
				Map<String, String> paramMap = getParamMap(reqUrl);
				String thisisme = paramMap.get("thisisme");
				
				if(!StringUtils.isBlank(thisisme)){
					try {
						thisisme = URLDecoder.decode(thisisme, "UTF-8");
					} catch (UnsupportedEncodingException ex) {}
					AbstractDAOUtils.setStateParam(THIS_IS_ME, thisisme, req, res, USE_SESSIONS);
				}
			}
			forward(manager, request, null, null, SIGNUP_PAGE);
		} else {
			//clean up a bit
			AbstractDAOUtils.removeStateParam(NEW_USER_NAME, req, res, USE_SESSIONS);
			AbstractDAOUtils.removeStateParam(NEW_USER_EMAIL, req, res, USE_SESSIONS);
			AbstractDAOUtils.removeStateParam(IDENTIFIER, req, res, USE_SESSIONS);
//			AbstractDAOUtils.removeStateParam(DISCOVERY_INFO, req, res, BasePage.USE_SESSIONS);

			//is this account active??? 
			if (authUser.getActive()) {
				//update lastseen
				authUser.setLastseen(System.currentTimeMillis());
				authUser.setAuthstamp(authUser.getLastseen());
				authUser.setIdentifier(identifier);
				authUser.update();
			
				setPrincipal(new SimplePrincipal(authUser.getId().toString(), 
						StringUtils.split(authUser.getGroups(), ',')), 
						authUser.getAuthstamp(), req, res);
				
				//FINALLY: success. send back to request
				redirectToRequest(manager, request);
			} else {
				//account is not active - return to signin page
				forward(manager, request, null, null, SIGNIN_PAGE+ "?code=3&error=true");
			}			
		}
	}

	private static String buildAuthRequestUrl(String suppliedIdentifier,
			HttpServletRequest httpReq, HttpServletResponse httpRes, ConsumerManager consmanager) {

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
//			AbstractDAOUtils.setStateParam(DISCOVERY_INFO, discovered);

			// Attribute Exchange extension
			FetchRequest fetch = FetchRequest.createFetchRequest();
			
			fetch.addAttribute("firstname", "http://axschema.org/namePerson/first", true);
			fetch.addAttribute("lastname", "http://axschema.org/namePerson/last", true);
			fetch.addAttribute("email", "http://axschema.org/contact/email", true);

			// Simple Registration extension
			SRegRequest sregReq = SRegRequest.createFetchRequest();
			sregReq.addAttribute("fullname", true);
			sregReq.addAttribute("email", true);

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

		} catch (Exception e) {
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
			HttpServletResponse httpRes, ConsumerManager consmanager) {
		try {
			// extract the parameters from the authentication response
			// (which comes in as a HTTP request from the OpenID provider)
			ParameterList response = new ParameterList(httpReq.getParameterMap());

			// retrieve the previously stored discovery information
//			DiscoveryInformation discovered = (DiscoveryInformation)
//					AbstractDAOUtils.setStateParam(DISCOVERY_INFO);
//			if (discovered == null) {
//				return null;
//			}
			
			// extract the receiving URL from the HTTP request
			StringBuffer receivingURL = httpReq.getRequestURL();
			String queryString = httpReq.getQueryString();

			if (queryString != null && queryString.length() > 0) {
				receivingURL.append("?").append(httpReq.getQueryString());
			}
			// verify the response; ConsumerManager needs to be the same
			// (static) instance used to place the authentication request
			VerificationResult verification =
					consmanager.verify(receivingURL.toString(), response, null);

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

						String fname = fetchResp.getAttributeValue("firstname");
						String lname = fetchResp.getAttributeValue("lastname");
						axFullName = fname + " " + lname;
						axEmail = fetchResp.getAttributeValue("email");
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

				String newUserName = "";
				String newUserEmail = "";
				
				if (StringUtils.isBlank(sregFullName)) {
					newUserName = StringUtils.trimToEmpty(axFullName);
				} else {
					newUserName = StringUtils.trimToEmpty(sregFullName);
				}

				if (StringUtils.isBlank(sregEmail)) {
					newUserEmail = axEmail;
				} else {
					newUserEmail = sregEmail;
				}
				
				if(!StringUtils.isBlank(newUserName)){
					AbstractDAOUtils.setStateParam(NEW_USER_NAME, newUserName, httpReq, 
							httpRes, USE_SESSIONS);
				}
				
				if(!StringUtils.isBlank(newUserEmail)){
					AbstractDAOUtils.setStateParam(NEW_USER_EMAIL, newUserEmail, httpReq, 
							httpRes, USE_SESSIONS);
				}

				return verified; // success
			}
		} catch (Exception e) {
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
		String forwardToFailure = isAttachReq ? SETTINGS_PAGE + "?code=6&error=true" :
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
				String oidpRequestURL = buildAuthRequestUrl(openidFromUser, 
						request, response, consman);

				if (oidpRequestURL != null) {
					//save and redirect to oidp
					//manager.saveRequest(request);
					forward(manager, authreq, request, response, oidpRequestURL);
					return Status.Continue;
				} else {										
					forward(manager, authreq, request, response, forwardToFailure);
					return Status.Failure;
				}
			} else if ("true".equals(request.getParameter("return"))) {
				// Coming back from oidp? then process return
				Identifier identifier = verifyResponse(request, response, consman);
				if (identifier == null) {
					//error! send back to signin page					
					forward(manager, authreq, request, response, forwardToFailure);
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
				String openidURL = AbstractDAOUtils.getStateParam(IDENTIFIER, 
						request, response, USE_SESSIONS);
				tryCreatingPrincipal(openidURL, manager, authreq);
				return Status.Continue;
			}
			//identifier is null or something else... -> signin page			
			forward(manager, authreq, request, response, forwardToFailure);
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
			forward(manager, authreq, request, response, forwardToFailure);
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
	
	private Status expiredResponse(AuthenticationRequest request){
		String l = AbstractDAOUtils.getStateParam(Context.LOCALE, request.getHttpServletRequest(), 
				request.getHttpServletResponse(), USE_SESSIONS);
		Locale loc = (l == null) ? request.getHttpServletRequest().getLocale() : new Locale(l);
		Map<String, String> lang = Language.readLanguage(loc);
		try {
			request.getHttpServletResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED, 
					lang.get("sessiontimeout"));
		} catch (Exception ex) { }
		return Status.None;
	}
	
	public static void clearSession(HttpServletRequest req, HttpServletResponse res, 
			boolean useSessions){
		req.getSession().invalidate();
		clearAuthCookie(req, res);
	}
	
	public static void clearAuthCookie(HttpServletRequest req, HttpServletResponse res){
		Cookie c = ClickUtils.getCookie(req, ScooldAuthModule.AUTH_USER);
		if(c != null){
			AbstractDAOUtils.setRawCookie(ScooldAuthModule.AUTH_USER, "", req, res, true, true);
			AbstractDAOUtils.removeStateParam(ScooldAuthModule.IDENTIFIER, req, res, false);
			AbstractDAOUtils.removeStateParam(ScooldAuthModule.NEW_USER_NAME, req, res, false);
			AbstractDAOUtils.removeStateParam(ScooldAuthModule.NEW_USER_EMAIL, req, res, false);
		}
	}
}
