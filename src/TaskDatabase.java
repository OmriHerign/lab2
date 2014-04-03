import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class TaskDatabase {

	// Singleton to database xml
	private static TaskDatabase instance = null;

	public static TaskDatabase getInstance() throws Exception {
		
	    if (instance == null) {
			synchronized (TaskDatabase.class) {  
	            if (instance == null) { 
	            	
	            	String rootFolder = HTTPUtils.LoadDatabaseXmlNameFromConfig("root");
	            	instance = new TaskDatabase(rootFolder);
	            }
	        }
	    }
	    
	    return instance;
	}
	
	// Hold the tasks.xml serialized
	private ConcurrentHashMap<String, ArrayList<TaskInfo>> _tasksDb = new ConcurrentHashMap<String, ArrayList<TaskInfo>>();
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, ArrayList<TaskInfo>> getTasksDB() {
		return _tasksDb;
	}
	
	/**
	 * 
	 * @param rootFolder
	 */
	private TaskDatabase(String rootFolder) {
		
		String taskXmlPath = null;
		try {
			taskXmlPath = HTTPUtils.LoadDatabaseXmlNameFromConfig("taskFilePath");
			String xmlFilename = rootFolder + "/" + taskXmlPath;
			
			if (serializeDatabaseXml(xmlFilename)) {
				System.out.println("Tasks database xml was parsed and loaded into memory!");
			} else {
				System.err.println("ERROR: Could not load task.xml");
			}
			
			
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @param taskId
	 * @throws Exception
	 */
	public void markTaskInfoAsCompleted(String loggedUsername, String taskId) throws Exception {
		
		ArrayList<TaskInfo> tasks = _tasksDb.get(loggedUsername);
		if (tasks == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any task for the logged-in username: " + loggedUsername);
		}
		
		for (int i = 0; i < tasks.size(); i++) {
			TaskInfo currentTask = tasks.get(i);
			if (currentTask.getTaskId().equals(taskId)) {
				if (currentTask.getTaskStatus().equals("In Progress...")) {
					// We can mark task as "Completed" only if its in "In Progress..." status
					currentTask.setTaskStatus("Completed");

					// Update database on disc
					TaskDatabase.saveDataBaseToXML(this._tasksDb);
					
					SMTPClient smtpClient = new SMTPClient();
					StringBuilder sb = new StringBuilder();
					sb.append("<html><body");
					sb.append(currentTask.toString() + "<br><br>");
					sb.append("Was completed succcessfully!");
					sb.append("</body></html>");
					smtpClient.SendMail(currentTask.getTaskRecipient(), loggedUsername, "Task Completed!", sb.toString());
					break;
				} else {
					throw new Exception("Could mark the given task as Completed because it's already Compledted or DueDated");
				}
			}
		}
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @param newTaskInfo
	 * @throws Exception
	 */
	public void addTaskInfo(String loggedUsername, TaskInfo newTaskInfo) throws Exception {
		
		ArrayList<TaskInfo> tasks = _tasksDb.get(loggedUsername);
		
		if (tasks == null) {
			// meaning its the first reminder for that user
			tasks = new ArrayList<>();
			_tasksDb.put(loggedUsername, tasks);
			tasks = _tasksDb.get(loggedUsername);
		}

		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		newTaskInfo.setTaskCreationDate(dateFormat.format(cal.getTime()));
		tasks.add(newTaskInfo);
		
		// Update database on disc
		TaskDatabase.saveDataBaseToXML(this._tasksDb);
	}
	
	/**
	 * Get a task id, and delete it IF AND ONLY IF it status is "In Progress..."
	 * @param loggedUsername
	 * @param taskId
	 * @return
	 * @throws Exception
	 */
	public void deleteTaskId(String loggedUsername, String taskId) throws Exception {
		
		ArrayList<TaskInfo> tasks = _tasksDb.get(loggedUsername);
		if (tasks == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any task for the logged-in username: " + loggedUsername);
		}

		int taskIndexToDelete = -1;
		for (int i = 0; i < tasks.size(); i++) {
			
			TaskInfo currentTaskInfo = tasks.get(i);
			if (currentTaskInfo.getTaskId().equals(taskId)) {
				// Meaning we found the task by the given task Id
				if (currentTaskInfo.getTaskStatus().equals("In Progress...")) {
					// This task has "In Progress..." status, we allow deletion
					taskIndexToDelete = i;
					break;
				} else {
					throw new Exception("Only \"In Progress...\" tasks can be deleted!");
				}
			}
		}
		
		if (taskIndexToDelete == -1) {
			// Could not find id to delete, return error
			throw new Exception("the task could not be deleted!");
		}
		
		// Remove the reminder
		tasks.remove(taskIndexToDelete);
		System.out.println("Task had been deleted successfully!");
		
		// Update database on disc
		TaskDatabase.saveDataBaseToXML(this._tasksDb);
	}
	
	/**
	 * Gets and xml path to a database, and serialize it into a proper class
	 * @param databaseXML
	 */
	private boolean serializeDatabaseXml(String databaseXML) {
		try {
			
			File fXmlFile = new File(databaseXML);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			Element rootElement = doc.getDocumentElement();
			NodeList taskNodes = rootElement.getElementsByTagName("task");
			
			// Iterates task elements
			for (int i = 0; i < taskNodes.getLength(); i++) {
				
				// Single task node
				Node taskNode = taskNodes.item(i);
				
				// Gets the email address found in "id" element
				NamedNodeMap taskAttributes = taskNode.getAttributes();
				Attr attribute = (Attr)taskAttributes.item(0);
				String taskEmail = attribute.getValue();
				
				// Continue iterating the xml
				if (taskNode.hasChildNodes()) {
					
					// Get all task childs (taskInfo)
					Element taskElement = (Element)taskNode;
					NodeList taskInfoNodes = taskElement.getElementsByTagName("taskInfo");
					ArrayList<TaskInfo> taskInfoList = new ArrayList<TaskInfo>();
					
					// Iterates all taskInfo elements
					for (int j = 0; j < taskInfoNodes.getLength(); j++) {
						
						// Gets a single reminderInfo
						Node taskInfoNode = taskInfoNodes.item(j);
						
						// Gets reminderInfo attribute "id"
						NamedNodeMap nodeAttributes = taskInfoNode.getAttributes();
						attribute = (Attr)nodeAttributes.item(0);
						
						if (taskInfoNode.hasChildNodes()) {
							
							NodeList taskChildNodes = taskInfoNode.getChildNodes();
							TaskInfo taskInfo = new TaskInfo();
							
							// Iterates taskInfo child elements
							for (int k = 0; k < taskChildNodes.getLength(); k++) {
								taskInfo.setTaskId(attribute.getValue());
								Node taskChild = taskChildNodes.item(k);
								
								// We skip #text element (unknown shit of DOM parsing)
								if (!(taskChild.getNodeType() == Node.TEXT_NODE)) {
									
									String taskChildName = taskChild.getNodeName();
									String taskChildValue = taskChild.getTextContent();
									switch (taskChildName) {
										case "taskTitle": {
											taskInfo.setTaskTitle(taskChildValue);
											break;
											}
										case "taskCreationDate": {
											taskInfo.setTaskCreationDate(taskChildValue);
											break;
											}
										case "taskBody": {
											taskInfo.setTaskBody(taskChildValue);
											break;
										}
										case "taskRecipient": {
											taskInfo.setTaskRecipient(taskChildValue);
											break;
										}
										case "taskDueDate": {
											taskInfo.setTaskDueDate(taskChildValue);
											break;
										}
										case "taskIsSent": {
											taskInfo.setTaskIsSent(Boolean.parseBoolean(taskChildValue));
											break;
										}
										case "taskStatus": {
											taskInfo.setTaskStatus(taskChildValue);
											break;
										}
									}
								}	
							}
							taskInfoList.add(taskInfo);
						}
					}
					// add the result to the dictionary
					this._tasksDb.put(taskEmail, taskInfoList);
				}
			}
			return true;
		} catch (Exception ex) {
			System.err.println("ERROR: Could not serialize xml. Exception: " + ex.getMessage());
			return false;
		}
	}
	
	/**
	 * 
	 * @param hashimap
	 */
	public static void saveDataBaseToXML(ConcurrentHashMap<String ,ArrayList<TaskInfo>> hashimap) {
		  try {
			
			// Builds document.  
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// root elements
			Document doc = docBuilder.newDocument();
			
			// Creates tasks_db
			Element tasks_db = doc.createElement("tasks_db");
			doc.appendChild(tasks_db);
	 
			// For each user.
			for (String string : hashimap.keySet()){
				
				//Creates task
				Element task = doc.createElement("task");
				tasks_db.appendChild(task);
				
				// Creates attribute for task.
				Attr taskAttr = doc.createAttribute("id");
				taskAttr.setValue(string);
				task.setAttributeNode(taskAttr);
				
				// For each user task.
				for (TaskInfo tas : hashimap.get(string)){
					
					// Creates task info.
					Element taskInfo = doc.createElement("taskInfo");
					task.appendChild(taskInfo);
		 
					// Attribute for task info.
					Attr attr = doc.createAttribute("id");
					attr.setValue(tas.getTaskId());
					taskInfo.setAttributeNode(attr);
					
					// taskTitle and its value
					Element taskTitle = doc.createElement("taskTitle");
					taskTitle.appendChild(doc.createTextNode(tas.getTaskTitle()));
					taskInfo.appendChild(taskTitle);
			 
					// taskBody and its value
					Element taskBody = doc.createElement("taskBody");
					taskBody.appendChild(doc.createTextNode(tas.getTaskBody()));
					taskInfo.appendChild(taskBody);
					
					// taskRecipient and its value.
					Element taskRecipient = doc.createElement("taskRecipient");
					taskRecipient.appendChild(doc.createTextNode(tas.getTaskRecipient()));
					taskInfo.appendChild(taskRecipient);
			 
					// taskCreationDate and its value.
					Element taskCreationDate = doc.createElement("taskCreationDate");
					taskCreationDate.appendChild(doc.createTextNode(tas.getTaskCreationDate()));
					taskInfo.appendChild(taskCreationDate);
					
					// taskDueDate and its value.
					Element taskDueDate = doc.createElement("taskDueDate");
					taskDueDate.appendChild(doc.createTextNode(tas.getTaskDueDate()));
					taskInfo.appendChild(taskDueDate);
				
					// taskStatus and its value.
					Element taskStatus = doc.createElement("taskStatus");
					taskStatus.appendChild(doc.createTextNode(tas.getTaskStatus()));
					taskInfo.appendChild(taskStatus);
					
					// taskIsSent and its value.
					Element taskIsSent = doc.createElement("taskIsSent");
					taskIsSent.appendChild(doc.createTextNode(String.valueOf(tas.getTaskIsSent())));
					taskInfo.appendChild(taskIsSent);	
				}
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("wwwroot/tasks.xml"));
	 
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
	 
			transformer.transform(source, result);
	 
			System.out.println("File: tasks.xml saved!");
	 
		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		  } catch (TransformerException tfe) {
			tfe.printStackTrace();
		  }		
	}

}
