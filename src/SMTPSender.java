import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * @author pringles
 *
 */
public class SMTPSender implements Runnable {

	ConcurrentHashMap<String, ArrayList<ReminderInfo>> _remindersDatabase;
	ConcurrentHashMap<String, ArrayList<TaskInfo>> _tasksDatabse;
	ConcurrentHashMap<String, ArrayList<PollInfo>> _pollDatabase;
	
	// Thread sleep time
	private final int sleepTime = 60000;
	
	public SMTPSender () {
		
	}
	
	@Override
	public void run() {
		
		while (true) {
			
			try {
				
				// Gets the current state of the reminders and tasks
				_remindersDatabase = ReminderDatabase.getInstance().getRemindersDb();
				_tasksDatabse = TaskDatabase.getInstance().getTasksDB();
				_pollDatabase = PollDatabase.getInstance().getPollsDb();
				Calendar cal = Calendar.getInstance();
				
				// Check which reminders needs to be sent and send them!
				//sendReminders(cal);
				
				// Check which task are DUE and send a mails
				//sendTasks(cal);
				
				ReminderDatabase.saveDataBaseToXML(this._remindersDatabase);
				TaskDatabase.saveDataBaseToXML(this._tasksDatabse);
				PollDatabase.saveDataBaseToXML(this._pollDatabase);
				
			} catch (Exception ex) {
				System.err.println("ERROR: could not get reminderesQueue! Exception: " + ex.getMessage());
			}
			
			try {
				System.out.println("SMTPSender is going to sleep for: " + sleepTime + " sec!");
				Thread.sleep(sleepTime);
			} catch (InterruptedException ex) {
				System.err.println("ERROR: could not sleep! Exception: " + ex.getMessage());
			}
		}
		
	}

	/**
	 * 
	 * @param cal
	 */
	private void sendTasks(Calendar cal) {
		
		// Iterate all the tasks and check who we need to send
		for (Map.Entry<String, ArrayList<TaskInfo>> entry : _tasksDatabse.entrySet()) {
			
			// Gets the an entry
		    String taskKey = entry.getKey();
		    ArrayList<TaskInfo> taskValue = entry.getValue();
		    
		    for (TaskInfo taskInfo : taskValue) {
		    	
		        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); 
		        Date sendingDate;
		        try {
		            sendingDate = df.parse(taskInfo.getTaskDueDate());
		            
		            if (cal.getTime().after(sendingDate)) {
		            	// Meaning time had passed and we need to send
		            	// We just verify that this mail was not already sent and that the task is not marked as "Completed"
		            	if (!taskInfo.getTaskIsSent() && (!taskInfo.getTaskStatus().equals("Completed") || !taskInfo.getTaskStatus().equals("Due"))) {
		            		
		            		SMTPClient smtpClient = new SMTPClient();
		            		StringBuilder sb = new StringBuilder();
		            		sb.append("<html><body><br>");
		            		sb.append(taskInfo.getTaskBody() + "<br><br>");
		            		sb.append("<b>Task time is DUE!<br> The recipient did not finish on time!<b>");
		            		sb.append("</body></html>");
		            		boolean resultCreator = smtpClient.SendMail(taskKey, taskKey, "Task: " + taskInfo.getTaskTitle() , sb.toString());
		            		
		            		String recepient = taskInfo.getTaskRecipient();
		            		StringBuilder sb1 = new StringBuilder();
		            		sb1.append("<html><body><br>");
		            		sb1.append(taskInfo.getTaskBody() + "<br><br>");
		            		sb1.append("<b>Task time is DUE! (PASSED) Next time be quicker!<b>");
		            		sb1.append("</body></html>");
		            		boolean resultRecipient = smtpClient.SendMail(recepient, recepient, "Task: " + taskInfo.getTaskTitle() , sb1.toString());
		            		
		            		if (!resultCreator || !resultRecipient) {
		            			System.out.println("ERROR: Could not send task right now. will try later");
		            		} else {
		            			// Set the mail has sent successfully
		            			taskInfo.setTaskIsSent(true);
		            			taskInfo.setTaskStatus("Due");
		            		}
		            	}
		    		}
		        } catch (ParseException e) {
		            // continue to next ReminderInfo
		        	continue;
		        }
			}
		}
	}

	/**
	 * 
	 * @param cal
	 */
	private void sendReminders(Calendar cal) {
		
		// Iterate all the reminders and check who we need to send
		for (Map.Entry<String, ArrayList<ReminderInfo>> entry : _remindersDatabase.entrySet()) {
			
			// Gets the an entry
		    String entryKey = entry.getKey();
		    ArrayList<ReminderInfo> entryValue = entry.getValue();
		    
		    for (ReminderInfo reminderInfo : entryValue) {
		    	
		        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); 
		        Date sendingDate;
		        try {
		            sendingDate = df.parse(reminderInfo.getReminderSendingDate());
		            
		            if (cal.getTime().after(sendingDate)) {
		            	// Meaning time had passed and we need to send
		            	// We just verify that this mail was not already sent
		            	if (!reminderInfo.getReminderIsSent()) {
		            		
		            		SMTPClient smtpClient = new SMTPClient();
		            		boolean isSent = smtpClient.SendMail(entryKey, entryKey, reminderInfo.getReminderTitle(),
		            				reminderInfo.getReminderBody());
		            		
		            		if (!isSent) {
		            			System.out.println("ERROR: Could not send reminder right now. will try later");
		            			//continue;
		            		} else {
		            			// Set the mail has sent successfully
		            			reminderInfo.setReminderIsSent(true);
		            		}
		            	}
		    		}
		        } catch (ParseException e) {
		            // continue to next ReminderInfo
		        	continue;
		        }
			}
		}
	}
}
