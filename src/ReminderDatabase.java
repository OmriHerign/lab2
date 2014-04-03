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
import org.xml.sax.SAXException;

/**
 * 
 * @author 
 *
 */
public class ReminderDatabase {

	// Singleton to database xml
	private static ReminderDatabase instance = null;

	public static ReminderDatabase getInstance() throws Exception {
		
	    if (instance == null) {
			synchronized (ReminderDatabase.class) {  
	            if (instance == null) { 
	            	
	            	String rootFolder = HTTPUtils.LoadDatabaseXmlNameFromConfig("root");
	            	instance = new ReminderDatabase(rootFolder);
	            }
	        }
	    }
	    
	    return instance;
	}
	
	// Hold the reminders.xml serialized
	private ConcurrentHashMap<String, ArrayList<ReminderInfo>> _remindersDb = new ConcurrentHashMap<String, ArrayList<ReminderInfo>>();
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, ArrayList<ReminderInfo>> getRemindersDb() {
		return _remindersDb;
	}
	
	/**
	 * 
	 * @param newReminderInfo
	 */
	public void updateReminderInfo(String loggedUsername, ReminderInfo newReminderInfo) throws Exception {
		
		ArrayList<ReminderInfo> reminders = _remindersDb.get(loggedUsername);
		if (reminders == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any reminder for the logged-in username: " + loggedUsername);
		}

		for (int i=0; i < reminders.size(); i++) {
			
			ReminderInfo currentReminder = reminders.get(i);
			if (currentReminder.getReminderId().equals(newReminderInfo.getReminderId())) {
				currentReminder.setReminderTitle(newReminderInfo.getReminderTitle());
				currentReminder.setReminderBody(newReminderInfo.getReminderBody());
				currentReminder.setReminderSendingDate(newReminderInfo.getReminderSendingDate());
				break;
			}
		}
		
		// Update database on disc
		ReminderDatabase.saveDataBaseToXML(this._remindersDb);
	}
	
	/**
	 * 
	 * @param newReminderInfo
	 */
	public void addReminderInfo(String loggedUsername, ReminderInfo newReminderInfo) throws Exception {
		
		ArrayList<ReminderInfo> reminders = _remindersDb.get(loggedUsername);
		
		if (reminders == null) {
			// meaning its the first reminder for that user
			reminders = new ArrayList<>();
			_remindersDb.put(loggedUsername, reminders);
			reminders = _remindersDb.get(loggedUsername);
		}

		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		newReminderInfo.setReminderCreationDate(dateFormat.format(cal.getTime()));
		reminders.add(newReminderInfo);
		
		// Update database on disc
		ReminderDatabase.saveDataBaseToXML(this._remindersDb);
	}
	
	/**
	 * 
	 * @param loggedUsername
	 * @param reminderId
	 * @throws Exception
	 */
	public void deleteReminderId(String loggedUsername, String reminderId) throws Exception {
		
		ArrayList<ReminderInfo> reminders = _remindersDb.get(loggedUsername);
		if (reminders == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any reminder for the logged-in username: " + loggedUsername);
		}

		int reminderIndexToDelete = -1;
		for (int i = 0; i < reminders.size(); i++) {
			
			ReminderInfo currentReminder = reminders.get(i);
			if (currentReminder.getReminderId().equals(reminderId)) {
				reminderIndexToDelete = i;
				break;
			}
		}
		
		if (reminderIndexToDelete == -1) {
			// Could not find id to delete, return error
			throw new Exception("the reminder could not be deleted!");
		}
		
		// Remove the reminder
		reminders.remove(reminderIndexToDelete);
		System.out.println("Reminder had been deleted successfully!");
		
		// Update database on disc
		ReminderDatabase.saveDataBaseToXML(this._remindersDb);
	}
	
	/**
	 * 
	 * @param rootFolder
	 */
	private ReminderDatabase(String rootFolder) {
		
		String reminderXmlPath = null;
		try {
			reminderXmlPath = HTTPUtils.LoadDatabaseXmlNameFromConfig("reminderFilePath");
			String xmlFilename = rootFolder + "/" + reminderXmlPath;
			
			if (serializeDatabaseXml(xmlFilename)) {
				System.out.println("Reminder database xml was parsed and loaded into memory!");
			} else {
				System.err.println("ERROR: Could not load reminder.xml");
			}
			
			
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
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
			NodeList reminderNodes = rootElement.getElementsByTagName("reminder");
			
			// Iterates reminder elements
			for (int i = 0; i < reminderNodes.getLength(); i++) {
				
				// Single reminder node
				Node reminderNode = reminderNodes.item(i);
				//System.out.println("Element Name:" + reminderNode.getNodeName());
				
				// Gets the email address found in "id" element
				NamedNodeMap reminderAttributes = reminderNode.getAttributes();
				Attr attribute = (Attr)reminderAttributes.item(0);
				//System.out.println("Attribute Name: " + attribute.getName() + " Attribute Value: " + attribute.getValue());
				String reminderEmail = attribute.getValue();
				
				// Continue iterating the xml
				if (reminderNode.hasChildNodes()) {
					
					// Get all reminder childs (reminderInfo)
					Element reminderElement = (Element)reminderNode;
					NodeList reminderInfoNodes = reminderElement.getElementsByTagName("reminderInfo");
					ArrayList<ReminderInfo> reminderInfoList = new ArrayList<>();
					
					// Iterates all reminderInfo elements
					for (int j = 0; j < reminderInfoNodes.getLength(); j++) {
						
						// Gets a single reminderInfo
						Node reminderInfoNode = reminderInfoNodes.item(j);
						// Creates a reminderInfo object
						//System.out.println("Element Name:" + reminderInfoNode.getNodeName());
						
						// Gets reminderInfo attribute "id"
						NamedNodeMap nodeAttributes = reminderInfoNode.getAttributes();
						attribute = (Attr)nodeAttributes.item(0);
						//System.out.println("Attribute Name: " + attribute.getName() + ", Attribute Value: " + attribute.getValue());
						
						if (reminderInfoNode.hasChildNodes()) {
							
							NodeList reminderChildNodes = reminderInfoNode.getChildNodes();
							ReminderInfo reminderInfo = new ReminderInfo();
							
							// Iterates reminderInfo child elements
							for (int k = 0; k < reminderChildNodes.getLength(); k++) {
								reminderInfo.setReminderId(attribute.getValue());
								Node reminderChild = reminderChildNodes.item(k);
								
								// We skip #text element (unknown shit of DOM parsing)
								if (!(reminderChild.getNodeType() == Node.TEXT_NODE)) {
									
									String reminderChildName = reminderChild.getNodeName();
									String reminderChildValue = reminderChild.getTextContent();
									//System.out.println("Element Name: " + reminderChildName +", Element Value: " + reminderChildValue);
									
									switch (reminderChildName) {
										case "reminderTitle": {
											reminderInfo.setReminderTitle(reminderChildValue);
											break;
											}
										case "reminderCreationDate": {
											reminderInfo.setReminderCreationDate(reminderChildValue);
											break;
											}
										case "reminderBody": {
											reminderInfo.setReminderBody(reminderChildValue);
											break;
										}
										case "reminderSendingDate": {
											reminderInfo.setReminderSendingDate(reminderChildValue);
											break;
										}
										case "reminderIsSent": {
											reminderInfo.setReminderIsSent(Boolean.parseBoolean(reminderChildValue));
											break;
										}
									}
								}	
							}
							
							// Add a new reminderInfo to to the list
							reminderInfoList.add(reminderInfo);
						}
					}
					
					// add the result to the dictionary
					this._remindersDb.put(reminderEmail, reminderInfoList);
				}
			}
			
			return true;
		} catch (ParserConfigurationException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		} catch (SAXException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
			return false;
		}
	}
	
	/**
	 * This method saves current reminders data from memory into  XML file.
	 * @param hashimap
	 */
	public static void saveDataBaseToXML(ConcurrentHashMap<String ,ArrayList<ReminderInfo>> hashimap) {
		  try {
			
			// Builds document.  
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// root elements
			Document doc = docBuilder.newDocument();
			
			// Creates reaminder_db
			Element reminders_db = doc.createElement("reminders_db");
			doc.appendChild(reminders_db);
	 
			// For each user.
			for (String string : hashimap.keySet()){
				
				//Creates remainder
				Element reminder = doc.createElement("reminder");
				reminders_db.appendChild(reminder);
				
				// Creates attribute for remainder.
				Attr reminderattr1 = doc.createAttribute("id");
				reminderattr1.setValue(string);
				reminder.setAttributeNode(reminderattr1);
				
				// For each user remainder.
				for (ReminderInfo ri : hashimap.get(string)){
					
					// Creates reminder info.
					Element reminderInfo = doc.createElement("reminderInfo");
					reminder.appendChild(reminderInfo);
		 
					// Attribute for reminder info.
					Attr attr = doc.createAttribute("id");
					attr.setValue(ri.getReminderId());
					reminderInfo.setAttributeNode(attr);
					
					// reminderTitle and its value
					Element reminderTitle = doc.createElement("reminderTitle");
					reminderTitle.appendChild(doc.createTextNode(ri.getReminderTitle()));
					reminderInfo.appendChild(reminderTitle);
			 
					// reminderBody and its value
					Element reminderBody = doc.createElement("reminderBody");
					reminderBody.appendChild(doc.createTextNode(ri.getReminderBody()));
					reminderInfo.appendChild(reminderBody);
					
					// reminderCreationDate and its value.
					Element reminderCreationDate = doc.createElement("reminderCreationDate");
					reminderCreationDate.appendChild(doc.createTextNode(ri.getReminderCreationDate()));
					reminderInfo.appendChild(reminderCreationDate);
			 
					// reminderSendingDate and its value.
					Element reminderSendingDate = doc.createElement("reminderSendingDate");
					reminderSendingDate.appendChild(doc.createTextNode(ri.getReminderSendingDate()));
					reminderInfo.appendChild(reminderSendingDate);
					
					// reminderSendingDate and its value.
					Element reminderIsSent = doc.createElement("reminderIsSent");
					reminderIsSent.appendChild(doc.createTextNode(String.valueOf(ri.getReminderIsSent())));
					reminderInfo.appendChild(reminderIsSent);
				}
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("wwwroot/reminders.xml"));
	 
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
	 
			transformer.transform(source, result);
	 
			System.out.println("File: reminders.xml saved!");
	 
		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		  } catch (TransformerException tfe) {
			tfe.printStackTrace();
		  }		
	}
}
