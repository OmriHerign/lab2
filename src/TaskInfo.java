import java.util.ArrayList;

/**
 * 
 * @author pringles
 *
 */
public class TaskInfo {

	private String _taskId;
	private String _taskTitle;
	private String _taskCreationDate;
	private String _taskDueDate;
	private String _taskBody;
	private String _taskRecipient;
	private String _taskStatus;
	private boolean _taskIsSent;
	
	public TaskInfo() {
		//Empty
	}
	
	public TaskInfo(String taskId, String taskTitle, String taskCreationDate, 
			String taskDueDate, String taskBody, String taskStatus, String taskRecipient) {
		_taskId = taskId;
		_taskTitle = taskTitle;
		_taskCreationDate = taskCreationDate;
		_taskDueDate = taskDueDate;
		_taskBody = taskBody;
		_taskStatus = taskStatus;
		_taskRecipient = taskRecipient;
	}
	
	public void setTaskId(String taskId) {
		this._taskId = taskId;
	}

	public void setTaskBody(String taskBody) {
		this._taskBody = taskBody;
	}
	
	public void setTaskRecipient(String taskRecipient) {
		this._taskRecipient = taskRecipient;
	}
	
	public void setTaskTitle(String taskTitle) {
		this._taskTitle = taskTitle;
	}

	public void setTaskCreationDate(String taskCreationDate) {
		this._taskCreationDate = taskCreationDate;
	}

	public void setTaskDueDate(String taskDueDate) {
		this._taskDueDate = taskDueDate;
	}
	
	public void setTaskIsSent(boolean result) {
		this._taskIsSent = result;
	}
	
	public void setTaskStatus(String taskStatus) {
		this._taskStatus = taskStatus;
	}

	public String getTaskId() {
		return _taskId;
	}

	public String getTaskTitle() {
		return _taskTitle;
	}
	
	public String getTaskRecipient() {
		return _taskRecipient;
	}

	public String getTaskCreationDate() {
		return _taskCreationDate;
	}

	public String getTaskDueDate() {
		return _taskDueDate;
	}
	
	public String getTaskBody() {
		return _taskBody;
	}
	
	public boolean getTaskIsSent() {
		return _taskIsSent;
	}
	
	public String getTaskStatus() {
		return _taskStatus;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("TaskId = " + _taskId + "\r\n");
		sb.append("TaskTitle = " + _taskTitle + "\r\n");
		sb.append("Task Body = " + _taskBody + "\r\n");
		sb.append("Task Recepient = " + _taskRecipient + "\r\n");
		sb.append("TaskCreationDate = " + _taskCreationDate + "\r\n");
		sb.append("TaskDueDate = " + _taskDueDate + "\r\n");
		sb.append("TaskStatus = " + _taskStatus + "\r\n");
		sb.append("TaskIsSent = " + _taskIsSent + "\r\n");
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
