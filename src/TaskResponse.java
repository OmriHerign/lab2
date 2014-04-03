import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author pringles
 *
 */
public class TaskResponse {

	public TaskResponse() {
		// Empty
	}
	
	public static String buildTasksPage (String loggedUsername, ArrayList<TaskInfo> tasks) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Tasks</h1>");	
		sb.append("<a href=\"task_editor.html\">New Task</a><br>");
		sb.append("<a href=\"main.html\">Back to homepage</a><br><br><br>");
		sb.append("Current logged Username: <b>" + loggedUsername + "</b><br><br>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th>Task Title</th>");
		sb.append("<th>Task Creation Date</th>");
		sb.append("<th>Task Due Date</th>");
		sb.append("<th>Task Status</th>");
		sb.append("<th>Delete</th>");
		sb.append("</tr>");	
		if (tasks != null && tasks.size() > 0) {
			for (TaskInfo taskInfo : tasks) {
				sb.append("<tr>");
				sb.append("<td>" + taskInfo.getTaskTitle() + "</td>");
				sb.append("<td>" + taskInfo.getTaskCreationDate() + "</td>");
				sb.append("<td>" + taskInfo.getTaskDueDate() + "</td>");
				sb.append("<td>" + taskInfo.getTaskStatus() + "</td>");
				sb.append("<td><a href=\"/task_delete?id=" + taskInfo.getTaskId() + "\">delete</a></td>");
				sb.append("</tr>");
			}
		}
		sb.append("</table></center>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
	
	public static String buildTaskErrorPage (String loggedUsername, String errorSubject, String exceptionMessage) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Task Error</h1>");
		sb.append("<h1><font color=\"red\">" + errorSubject + "</font></h1>");
		sb.append(exceptionMessage);
		sb.append("<br><a href=\"tasks.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @return
	 */
	public static String buildTaskSubmitErrorPage (String loggedUsername, String exceptionMessage) {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Submit Task</h1>");
		sb.append("<h1><font color=\"red\"> Error: Failure of Creating/Deleting This Task </font></h1>");
		sb.append("Exception Message: " + exceptionMessage);
		sb.append("<br><br><a href=\"tasks.html\">Go Back</a></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	public static String buildTaskThanksForReplying () {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Task - Thanks for replying!</h1>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
	
	public static String createANewTaskHTML () {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Task Editor</h1>");
		sb.append("<form action=\"submit_task.html\" method=\"post\">");
		sb.append("<input type=\"hidden\" name=\"task_action\" value=\"create\" />");
		sb.append("Task Subject: <br><input type=\"text\" name=\"task_subject\" /><br><br>");
		sb.append("Task Recepient: <br><input type=\"text\" name=\"task_recepient\" /><br><br>");
		sb.append("Task Body: <br><textarea name=\"task_body\" rows=\"4\" cols=\"50\">");
		sb.append("</textarea><br><br>");
		sb.append("Task Due Date: <br><input type=\"text\" name=\"task_date\" value=\"" +
				"dd/MM/yyyy" + "\" /><br><br>");
		sb.append("Task Due Time: <br><input type=\"text\" name=\"task_time\" value=\"" +
				"hh:mm:ss" +"\" /><br><br>");
		sb.append("<input type=\"submit\" value=\"Send Task\" />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		sb.append("<a href=\"tasks.html\">Cancel</a>");
		sb.append("</form></center>");
		sb.append("</body>");
		sb.append("</html>");
		
		return sb.toString();
	}
}
