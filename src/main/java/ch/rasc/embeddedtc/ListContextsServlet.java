/**
 * Copyright 2012-2015 Ralph Schaer <ralphschaer@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.rasc.embeddedtc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;

public class ListContextsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Context parentContext;

	public ListContextsServlet(Context parentContext) {
		this.parentContext = parentContext;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		StringBuilder sb = new StringBuilder(300);
		sb.append("<!DOCTYPE html>");
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<meta charset=\"utf-8\">");
		sb.append("<title>Error 404 - Not Found</title>");
		sb.append("</head>");
		sb.append(
				"<body style=\"font-family: Consolas,Monaco,Lucida Console,Liberation Mono,DejaVu Sans Mono,Bitstream Vera Sans Mono,Courier New, monospace;\">");
		sb.append("<h2>Error 404 - Not Found.</h2>");

		sb.append("No context on this server matched or handled this request.");
		sb.append("<br>Contexts known to this server are: ");

		Host host = (Host) this.parentContext.getParent();

		sb.append("<ul>");
		Container[] contexts = host.findChildren();
		for (Container container : contexts) {
			Context context = (Context) container;
			if (context != null) {
				if (!context.getPath().equals("") && context.getState().isAvailable()) {

					sb.append("<li>");
					sb.append("<a href=\"");
					sb.append(context.getPath());
					sb.append("\">");
					sb.append(context.getPath());
					sb.append("</a>");
					sb.append(" --> ");
					sb.append(context.getDocBase());
					sb.append("</li>");
				}
			}
		}
		sb.append("</ul>");
		sb.append("</body>");
		sb.append("</html>");

		resp.setStatus(404);
		resp.getOutputStream().write(sb.toString().getBytes());
		resp.flushBuffer();
	}
}
