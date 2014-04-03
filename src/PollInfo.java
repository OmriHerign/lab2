import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

/**
 * 
 * @author pringles
 *
 */
public class PollInfo {

	private String _pollId;
	private String _pollTitle;
	private String _pollCreationDate;
	private String _pollBody;
	private ArrayList<SimpleEntry<String,String>> _pollRecipientToAnswers;
	private boolean _pollIsSent;
	private String _pollStatus;
	private boolean _notify;
	
	public PollInfo() {
		_pollRecipientToAnswers = new ArrayList<SimpleEntry<String,String>>();
	}
	
	public void setPollStatus(String pollStatus) {
		this._pollStatus = pollStatus;
	}
	
	public void setPollId(String pollId) {
		this._pollId = pollId;
	}

	public void setPollBody(String pollBody) {
		this._pollBody = pollBody;
	}
	
	public void setPollTitle(String pollTitle) {
		this._pollTitle = pollTitle;
	}

	public void setPollCreationDate(String pollCreationDate) {
		this._pollCreationDate = pollCreationDate;
	}
	
	public void setPollIsSent(boolean result) {
		this._pollIsSent = result;
	}
	
	public void setPollNotify(boolean notify) {
		this._notify = notify;
	}

	public boolean getPollNotify() {
		return _notify;
	}

	public String getPollId() {
		return _pollId;
	}
	
	public String getPollStatus() {
		return _pollStatus;
	}

	public String getPollTitle() {
		return _pollTitle;
	}
	
	public ArrayList<SimpleEntry<String,String>> getPollRecipientToAnswers() {
		return _pollRecipientToAnswers;
	}

	public String getPollCreationDate() {
		return _pollCreationDate;
	}

	public String getPollBody() {
		return _pollBody;
	}
	
	public boolean getPollIsSent() {
		return _pollIsSent;
	}
	

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("Poll Id = " + _pollId + "\r\n");
		sb.append("Poll Title = " + _pollTitle + "\r\n");
		sb.append("Poll Body = " + _pollBody + "\r\n");
		sb.append("Poll Recepients = " + _pollRecipientToAnswers.toString() + "\r\n");
		sb.append("Poll Answers = " + _pollRecipientToAnswers.toString() + "\r\n");
		sb.append("Poll CreationDate = " + _pollCreationDate + "\r\n");
		sb.append("Poll Status = " + _pollStatus + "\r\n");
		sb.append("Poll Is Sent = " + _pollIsSent + "\r\n");
		return sb.toString();
	}	
	
	/**
	 * 
	 * @param taskId
	 * @param tasks
	 * @return null if not found
	 */
	public static TaskInfo getSpecificTaskInfo (String taskId, ArrayList<TaskInfo> tasks) {
		for (TaskInfo taskInfo : tasks) {
			if (taskInfo.getTaskId().equals(taskId)) {
				return taskInfo;
			}
		}
		
		return null;
	}
}
