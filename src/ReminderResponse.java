import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author pringles
 * 
 */
public class ReminderResponse {
	
	/**
	 * 
	 * @param rootFolder
	 * @param loggedUsername
	 */
	public ReminderResponse() {

		// Empty
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @param reminderInfo
	 * @return
	 */
	public static String editAReminderHTML (ReminderInfo reminderInfo) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminder Editor</h1>");
		sb.append("<form action=\"submit_reminder.html\" method=\"post\">");
		sb.append("<input type=\"hidden\" name=\"reminder_id\" value=\"" + reminderInfo.getReminderId() + "\" />");
		sb.append("Reminder Subject: <br><input type=\"text\" name=\"reminder_subject\" value=\"" + reminderInfo.getReminderTitle() + "\" /><br><br>");
		sb.append("Reminder Body: <br><textarea name=\"reminder_body\" rows=\"4\" cols=\"50\">");
		sb.append(reminderInfo.getReminderBody());
		sb.append("</textarea><br><br>");
		sb.append("Reminder Date: <br><input type=\"text\" name=\"reminder_date\" value=\"" +
				reminderInfo.getReminderCreationDate() + "\" /><br><br>");
		sb.append("Reminder Time: <br><input type=\"text\" name=\"reminder_time\" value=\"" +
				reminderInfo.getReminderSendingDate() +"\" /><br><br>");
		sb.append("<input type=\"submit\" value=\"Save\" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<a href=\"reminders.html\">Cancel</a>");
		sb.append("</form></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	public static String buildReminderErrorPage (String loggedUsername, String errorSubject, String exceptionMessage) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminder Error</h1>");
		sb.append("<h1><font color=\"red\">" + errorSubject + "</font></h1>");
		sb.append(exceptionMessage);
		sb.append("<br><a href=\"reminders.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public static String createANewReminderHTML () {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminder Editor</h1>");
		sb.append("<form action=\"submit_reminder.html\" method=\"post\">");
		sb.append("<input type=\"hidden\" name=\"reminder_action\" value=\"create\" />");
		sb.append("Reminder Subject: <br><input type=\"text\" name=\"reminder_subject\" /><br><br>");
		sb.append("Reminder Body: <br><textarea name=\"reminder_body\" rows=\"4\" cols=\"50\">");
		sb.append("</textarea><br><br>");
		sb.append("Reminder Date: <br><input type=\"text\" name=\"reminder_date\" value=\"" +
				"dd/MM/yyyy" + "\" /><br><br>");
		sb.append("Reminder Time: <br><input type=\"text\" name=\"reminder_time\" value=\"" +
				"hh:mm:ss" +"\" /><br><br>");
		sb.append("<input type=\"submit\" value=\"Save\" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<a href=\"reminders.html\">Cancel</a>");
		sb.append("</form></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @return
	 */
	public static String buildReminderSubmitERRORHTML (String loggedUsername) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Submit Reminder</h1>");
		sb.append("<h1><font color=\"red\"> Error: Failure of creating This Remainder </font></h1>");
		sb.append("<a href=\"reminders.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @return
	 */
	public static String BuildReminderHTML (String loggedUsername, ArrayList<ReminderInfo> reminders) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminders</h1>");	
		sb.append("<a href=\"reminder_editor.html\">New Reminder</a><br>");
		sb.append("<a href=\"main.html\">Back to homepage</a><br><br><br>");
		sb.append("Current logged Username: <b>" + loggedUsername + "</b><br><br>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		//sb.append("<th>ID</th>");
		sb.append("<th>Reminder Title</th>");
		sb.append("<th>Reminder Creation Date</th>");
		sb.append("<th>Reminder Remind Date</th>");
		sb.append("<th>Edit</th>");
		sb.append("<th>Delete</th>");
		sb.append("</tr>");	
		if (reminders != null && reminders.size() > 0) {
			for (ReminderInfo reminderInfo : reminders) {
				sb.append("<tr>");
				//sb.append("<td>" + reminderInfo.getReminderId() + "</td>");
				sb.append("<td>" + reminderInfo.getReminderTitle() + "</td>");
				sb.append("<td>" + reminderInfo.getReminderCreationDate() + "</td>");
				sb.append("<td>" + reminderInfo.getReminderSendingDate() + "</td>");
				sb.append("<td><a href=\"/reminder_editor.html?id=" + reminderInfo.getReminderId() + "\">edit</a></td>");
				sb.append("<td><a href=\"/reminder_delete?id=" + reminderInfo.getReminderId() + "\">delete</a></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table></center>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
}
