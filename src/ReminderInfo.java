import java.util.ArrayList;

/**
 * Holds a single reminderInfo
 * @author pringles
 *
 */
public class ReminderInfo {
	
	public ReminderInfo() {
		//Empty
	}
	
	public void setReminderId(String reminderId) {
		this._reminderId = reminderId;
	}

	public void setReminderBody(String reminderBody) {
		this._reminderBody = reminderBody;
	}
	
	public void setReminderTitle(String reminderTitle) {
		this._reminderTitle = reminderTitle;
	}

	public void setReminderCreationDate(String reminderCreationDate) {
		this._reminderCreationDate = reminderCreationDate;
	}

	public void setReminderSendingDate(String reminderSendingDate) {
		this._reminderSendingDate = reminderSendingDate;
	}
	
	public void setReminderIsSent(boolean result) {
		this._reminderIsSent = result;
	}

	public String getReminderId() {
		return _reminderId;
	}

	public String getReminderTitle() {
		return _reminderTitle;
	}

	public String getReminderCreationDate() {
		return _reminderCreationDate;
	}

	public String getReminderSendingDate() {
		return _reminderSendingDate;
	}
	
	public String getReminderBody() {
		return _reminderBody;
	}
	
	public boolean getReminderIsSent() {
		return _reminderIsSent;
	}

	private String _reminderId;
	private String _reminderTitle;
	private String _reminderCreationDate;
	private String _reminderSendingDate;
	private String _reminderBody;
	private boolean _reminderIsSent;
	
	public ReminderInfo(String reminderId, String reminderTitle,
			String reminderCreationDate, String reminderSendingDate, String reminderBody) {
		_reminderId = reminderId;
		_reminderTitle = reminderTitle;
		_reminderCreationDate = reminderCreationDate;
		_reminderSendingDate = reminderSendingDate;
		_reminderBody = reminderBody;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("ReminderId = " + _reminderId + "\r\n");
		sb.append("ReminderTitle = " + _reminderTitle + "\r\n");
		sb.append("ReminderCreationDate = " + _reminderCreationDate + "\r\n");
		sb.append("ReminderSendingDate = " + _reminderSendingDate + "\r\n");
		sb.append("ReminderIsSent = " + _reminderIsSent + "\r\n");
		return sb.toString();
	}	
	
	/**
	 * 
	 * @param reminderId
	 * @param reminders
	 * @return null if not found
	 */
	public static ReminderInfo getSpecificReminderInfo (String reminderId, ArrayList<ReminderInfo> reminders) {
		for (ReminderInfo reminderInfo : reminders) {
			if (reminderInfo.getReminderId().equals(reminderId)) {
				return reminderInfo;
			}
		}
		
		return null;
	}
}