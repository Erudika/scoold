/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.util;

import com.scoold.core.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.math.NumberUtils;

/**
 *
 * @author alexb
 */
public class ContactsServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {

		Long uid = NumberUtils.toLong(request.getParameter("id"), 0);
		if(uid.longValue() == 0 || request.getRemoteUser() == null) return;
		Map<String, String> contacts = readContactKeyNameMap(uid);

		response.setContentType("text/javascript;charset=UTF-8");
		//response.setHeader("pragma", "");
		//print out a lang array: {String key: String value,...}
        PrintWriter out = response.getWriter();
		StringBuilder sb = new StringBuilder("var contacts = [");
			try {
				for (Entry<String, String> entry : contacts.entrySet()) {
					sb.append("{id:");
					sb.append(entry.getKey());
					sb.append(", name:\"");
//					sb.append(StringEscapeUtils.escapeJavaScript(entry.getValue()));
					sb.append(entry.getValue());
					sb.append("\"},");
				}
				if(sb.charAt(sb.length() - 1) == ','){
					sb.deleteCharAt(sb.length() - 1);
				}
				sb.append("];");
				out.print(sb.toString());
			} finally {
				out.close();
			}
    }

	private Map<String, String> readContactKeyNameMap(Long userid) {
		Map<String, String> map = new HashMap<String, String>();

		ArrayList<User> contacts = User.getUserDao().
				readAllContactsForUser(userid, null, null);
		
		for (User user : contacts) {
			map.put(user.getUuid(), user.getFullname());
		}

		return map;
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
