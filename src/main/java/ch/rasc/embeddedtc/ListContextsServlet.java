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
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		StringBuilder sb = new StringBuilder(300);
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<title>Error 404 - Not Found</title>");
		sb.append("</head>");
		sb.append("<body>");
		sb.append("<h2>Error 404 - Not Found.</h2>");

		sb.append("No context on this server matched or handled this request.");
		sb.append("<br>Contexts known to this server are: ");

		Host host = (Host) parentContext.getParent();

		sb.append("<ul>");
		Container[] contexts = host.findChildren();
		for (int i = 0; i < contexts.length; i++) {
			Context context = (Context) contexts[i];
			if (context != null) {
				if (!context.getPath().equals("") && context.getState().isAvailable()) {

					sb.append("<li>");
					sb.append("<a href=\"");
					sb.append(context.getPath());
					sb.append("\">");
					sb.append(context.getPath());
					sb.append("</a>");
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
