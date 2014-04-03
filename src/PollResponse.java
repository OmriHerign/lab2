import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

/**
 * 
 * @author pringles
 *
 */
public class PollResponse {

	public PollResponse() {
		// Empty
	}
	
	public static String buildPollsPage (String loggedUsername, ArrayList<PollInfo> polls) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Polls</h1>");	
		sb.append("<a href=\"poll_editor.html\">New Poll</a><br>");
		sb.append("<a href=\"main.html\">Back to homepage</a><br><br><br>");
		sb.append("Current logged Username: <b>" + loggedUsername + "</b><br><br>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th>Poll Title</th>");
		sb.append("<th>Poll Creation Date</th>");
		sb.append("<th>Poll Recepients replies</th>");
		sb.append("<th>Poll Status</th>");
		sb.append("<th>Delete</th>");
		sb.append("</tr>");	
		
		if (polls != null && polls.size() > 0) {
			for (PollInfo pollInfo : polls) {
				sb.append("<tr>");
				sb.append("<td>" + pollInfo.getPollTitle() + "</td>");
				sb.append("<td>" + pollInfo.getPollCreationDate() + "</td>");
				sb.append("<td>");

				for (SimpleEntry<String,String> entry : pollInfo.getPollRecipientToAnswers()) {
					sb.append(entry.getKey() + " - " + entry.getValue() + "<br>");
				}
				
				sb.append("</td>");
				sb.append("<td>" + pollInfo.getPollStatus() + "</td>");
				sb.append("<td><a href=\"/poll_delete?id=" + pollInfo.getPollId() + "\">delete</a></td>");
				sb.append("</tr>");
			}
		}
		
		sb.append("</table></center>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @return
	 */
	public static String buildPollSubmitERRORHTML (String loggedUsername) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Submit Poll</h1>");
		sb.append("<h1><font color=\"red\"> Error: Failure of creating This Poll </font></h1>");
		sb.append("<br><a href=\"polls.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @return
	 */
	public static String buildPollThanksForReplying () {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Poll - Thanks for replying!</h1>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @return
	 */
	public static String buildPollErrorPage (String loggedUsername, String errorSubject, String exceptionMessage) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Poll Error</h1>");
		sb.append("<h1><font color=\"red\">" + errorSubject + "</font></h1>");
		sb.append(exceptionMessage);
		sb.append("<br><a href=\"polls.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	public static String createANewPollHTML () {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Poll Editor</h1>");
		sb.append("<form action=\"submit_poll.html\" method=\"post\">");
		sb.append("<input type=\"hidden\" name=\"poll_action\" value=\"create\" />");
		sb.append("Poll Subject: <br><input type=\"text\" name=\"poll_subject\" /><br><br>");
		sb.append("Poll Body: <br><textarea name=\"poll_body\" rows=\"4\" cols=\"50\"></textarea><br>");
		sb.append("Poll Recepients: <br><textarea name=\"poll_recepients\" value=\"delimited by CRLF\" rows=\"4\" cols=\"50\"></textarea><br>");
		sb.append("Poll Answers: <br><textarea name=\"poll_answers\" value=\"delimited by CRLF\" rows=\"4\" cols=\"50\"></textarea><br>");
		sb.append("Notify on every recipient reply? <input type=\"checkbox\" name=\"notify\" value=\"yes\"><br>");
		sb.append("<input type=\"submit\" value=\"Send Poll\" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<a href=\"polls.html\">Cancel</a>");
		sb.append("</form></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
}
