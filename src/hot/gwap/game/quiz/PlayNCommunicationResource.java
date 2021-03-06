/*
 * This file is part of gwap, an open platform for games with a purpose
 *
 * Copyright (C) 2013
 * Project play4science
 * Lehr- und Forschungseinheit für Programmier- und Modellierungssprachen
 * Ludwig-Maximilians-Universität München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package gwap.game.quiz;

import gwap.game.quiz.tools.FacesContextBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Status;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.log.Log;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.web.AbstractResource;
import org.json.simple.JSONObject;

/**
 * This servlet provides a new Quiz Game for the PlayN-HTML5-Interface by
 * returning a HttpServletRespone in JSON-Format
 * 
 * @author Jonas Hölzler
 * 
 */
@Startup
@Scope(ScopeType.APPLICATION)
@Name("playNCommunicationResource")
@BypassInterceptors
public class PlayNCommunicationResource extends AbstractResource {
	@Logger
	private Log logger;

	private HttpServletRequest request;

	private HttpServletResponse response;

	private ExpressionFactory elFactory;

	private ELContext elc;

	private HttpSession ses;

	private QuizSessionBean quizSessionBean;

	@Override
	public String getResourcePath() {
		return "/quiz";
	}

	@Override
	public void getResource(final HttpServletRequest request,
			final HttpServletResponse response) throws ServletException,
			IOException {
		new ContextualHttpServletRequest(request) {
			@Override
			public void process() throws IOException {
				doWork(request, response);
			}
		}.run();
	}

	private void doWork(HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		// HttpSession session = request.getSession();
		this.request = request;
		this.response = response;

		ses = request.getSession();
		SessionTracker.instance().add(ses);

		// }

		// give it ten trys to find a valid game configuration
		for (int ii = 0; ii < 10; ++ii) {
			JSONObject jsonObject = createJSONObjectForNewGame();
			if (jsonObject != null) {

				jsonObject.put("SID", ses.getId());
				InputStream instream = null;
				OutputStream outstream = null;

				try {
					instream = request.getInputStream();
					response.setContentType("text/plain");
					outstream = response.getOutputStream();
					BufferedWriter out = new BufferedWriter(
							new OutputStreamWriter(outstream));
					jsonObject.writeJSONString(out);
					out.flush();
					outstream.flush();
					outstream.close();

					instream.close();
					logger.info("Successfully initialized game");
					break;
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				logger.info("Couldn't initialize a game in try no." + ii);
			}

		}

	}

	/**
	 * Sets up a new Quiz Game Session
	 * 
	 * @return gameArray Array for JSON
	 */
	private JSONObject createJSONObjectForNewGame() {

		try {
			/*
			 * setting up dummy JSF FacesContext
			 */
			if (Transaction.instance().getStatus() == Status.STATUS_NO_TRANSACTION) {
				Transaction.instance().begin();
			}
			// Conversation.instance().begin();
			FacesContext facesContext = new FacesContextBuilder()
					.getFacesContext(request, response, request.getSession());
			this.elc = facesContext.getELContext();

			this.elFactory = facesContext.getApplication()
					.getExpressionFactory();

			ValueExpression mexp = elFactory.createValueExpression(elc,
					"#{quizSession}", QuizSessionBean.class);
			this.quizSessionBean = (QuizSessionBean) mexp.getValue(elc);
			ses.setAttribute("quizSession", quizSessionBean);

			JSONObject jsonResult = quizSessionBean.getJSONResult();

			facesContext.release();
			Transaction.instance().commit();

			return jsonResult;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
