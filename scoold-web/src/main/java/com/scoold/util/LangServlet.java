/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.Language;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.pages.BasePage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.click.Context;
import org.apache.commons.lang.StringEscapeUtils;

/**
 *
 * @author alexb
 */
public class LangServlet extends HttpServlet {

	// the lang keys for all strings used in scoold.js
	private static final String[] jsLang = {
		"success",
		"areyousure",
		"messages.sent",
		"messages.to",
		"profile.contacts.added",
		"alumnus", "teacher", "student",
		"profile.status.txt",
		"profile.status.update",
		"profile.status.txt",
		"class.chat.userin",
		"class.chat.userout",
		"class.chat.connection.error",
		"class.chat.polling.error",
		"class.chat.reconnect.error",
		"more",
		"posts.unloadconfirm",
		"signup.form.error.required",
		"signup.form.error.email",
		"maxlength",
		"minlength",
		"tags.toomany",
		"close", "save", "clickedit",
		"profile.drawer.embedly.notanimage",
		"profile.drawer.embedly.photosaved",
		"epicfail",
		"invalidyear"
	};


    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/javascript;charset=UTF-8");
		//response.setHeader("pragma", "");
		String l = AbstractDAOUtils.getStateParam(Context.LOCALE, request, response, BasePage.USE_SESSIONS);
		Locale loc = (l == null) ? request.getLocale() : new Locale(l);
		Map<String, String> lang = Language.readLanguage(loc);
        PrintWriter out = response.getWriter();
		StringBuilder sb = new StringBuilder("var lang = {");
		if(lang != null){
			try {
				//print out a lang array: {String key: String value,...}
				for (String entry : jsLang) {
					sb.append("\"");
					sb.append(entry);
					sb.append("\":\"");
					sb.append(StringEscapeUtils.escapeJavaScript(lang.get(entry)));
					sb.append("\",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append("};");
				out.print(sb.toString());
			} finally {
				out.close();
			}
		}
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
