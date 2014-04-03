import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
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

/**
 * 
 * @author pringles
 *
 */
public class PollDatabase {

	// Singleton to database xml
	private static PollDatabase instance = null;

	public static PollDatabase getInstance() throws Exception {
		
	    if (instance == null) {
			synchronized (PollDatabase.class) {  
	            if (instance == null) { 
	            	
	            	String rootFolder = HTTPUtils.LoadDatabaseXmlNameFromConfig("root");
	            	instance = new PollDatabase(rootFolder);
	            }
	        }
	    }
	    
	    return instance;
	}
	
	// Hold the tasks.xml serialized
	private ConcurrentHashMap<String, ArrayList<PollInfo>> _pollsDb = new ConcurrentHashMap<String, ArrayList<PollInfo>>();
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap<String, ArrayList<PollInfo>> getPollsDb() {
		return _pollsDb;
	}
	
	/**
	 * 
	 * @param rootFolder
	 */
	private PollDatabase(String rootFolder) {
		
		String taskXmlPath = null;
		try {
			taskXmlPath = HTTPUtils.LoadDatabaseXmlNameFromConfig("pollFilePath");
			String xmlFilename = rootFolder + "/" + taskXmlPath;
			
			if (serializeDatabaseXml(xmlFilename)) {
				System.out.println("Polls database xml was parsed and loaded into memory!");
			} else {
				System.err.println("ERROR: Could not load polls.xml");
			}
			
			
		} catch (IOException ex) {
			System.err.println("ERROR: " + ex.getMessage());
		}
	}
	
	public void addPollInfo(String loggedUsername, PollInfo newPollInfo) throws Exception {
		
		ArrayList<PollInfo> polls = _pollsDb.get(loggedUsername);
		
		if (polls == null) {
			// meaning its the first poll for that user
			polls = new ArrayList<>();
			_pollsDb.put(loggedUsername, polls);
			polls = _pollsDb.get(loggedUsername);
		}

		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		newPollInfo.setPollCreationDate(dateFormat.format(cal.getTime()));
		polls.add(newPollInfo);
		
		// Update database on disc
		PollDatabase.saveDataBaseToXML(this._pollsDb);
	}
	
	public void deletePollId(String loggedUsername, String pollId) throws Exception {
		
		ArrayList<PollInfo> polls = _pollsDb.get(loggedUsername);
		if (polls == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any poll for the logged-in username: " + loggedUsername);
		}

		int pollIndexToDelete = -1;
		for (int i = 0; i < polls.size(); i++) {
			
			PollInfo currentPoll = polls.get(i);
			if (currentPoll.getPollId().equals(pollId)) {
				pollIndexToDelete = i;
				break;
			}
		}
		
		if (pollIndexToDelete == -1) {
			// Could not find id to delete, return error
			throw new Exception("the poll could not be deleted! id is not exists.");
		}
		
		// Remove the reminder
		polls.remove(pollIndexToDelete);
		System.out.println("Poll had been deleted successfully!");
		
		// Update database on disc
		PollDatabase.saveDataBaseToXML(this._pollsDb);
	}
	
	public void markPollInfoAsCompleted(String pollId, String pollEmail, String pollAnswer) throws Exception {
		
		ArrayList<PollInfo> polls = null;
		String pollInitiatior = null;
		boolean pollFound = false;
		
		for (Entry<String, ArrayList<PollInfo>> pollss : _pollsDb.entrySet()) {
			polls = _pollsDb.get(pollss.getKey());
		    for (PollInfo pollInfo : polls) {
				if (pollInfo.getPollId().equals(pollId)) {
					pollInitiatior = pollss.getKey();
					pollFound = true;
					break;
				}
			}
		    
		    if (pollFound) {
		    	// we init and use again for pollId
		    	pollFound = false;
		    	break;
		    }
		}
		
		if (polls == null) {
			
			// Could not find loggedUsername
			throw new Exception("Could not find any poll for the given pollEmail: " + pollEmail);
		}
		
		for (int i = 0; i < polls.size(); i++) {
			PollInfo currentPoll = polls.get(i);
			if (currentPoll.getPollId().equals(pollId)) {
				if (currentPoll.getPollStatus().equals("In Progress...")) {
					
					ArrayList<SimpleEntry<String,String>> recipAnswers = currentPoll.getPollRecipientToAnswers();
					for (SimpleEntry<String, String> entry : recipAnswers) {
						if (entry.getKey().equals(pollEmail)) {
							
							// check if the user already marked it:
							if (entry.getValue().equals("None")) {
							
								// update his answer
								entry.setValue(pollAnswer);
								pollFound = true;
								
								// Update database on disc
								PollDatabase.saveDataBaseToXML(this._pollsDb);
								
								// Check if we need to notify on every reply
								if (currentPoll.getPollNotify()) {
									// Send a mail to the poll initiator about the replay
									NotifyOnReply(pollInitiatior, currentPoll,entry);
								}
								
								break;
							} else {
								// recipient already applied,  can't update
								throw new Exception("Cannot reply twice. Poll has been closed!");
							}
						}
					}
					
					// Count the current recipients replies
					int recipientReplied = CountRecipientsReplies(recipAnswers);

					// Check if poll has completed
					if (recipientReplied == recipAnswers.size()) {
						
						// Send a summerized mail and close the poll
						PollCompleted(pollInitiatior, currentPoll, recipAnswers);
					}
				} else {
					throw new Exception("Could mark the given poll because it's already completed!");
				}
			}
		}
		
		if (!pollFound) {
			throw new Exception("The given pollId was not found in database, could not mark as replied!");
		}
	}

	private void NotifyOnReply(String pollInitiatior, PollInfo currentPoll,
			SimpleEntry<String, String> entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body>");
		sb.append("<h1><b>Your poll got a reply!</b></h1><br>");
		sb.append("<b>Poll Subject:</b> " + currentPoll.getPollTitle() + "<br>");
		sb.append("<b>Poll Answer:</b><br>");
		sb.append("Poll Recipient: " + entry.getKey() + "<br>");
		sb.append("Poll Recipient Answer: " + entry.getValue() + "<br>");
		sb.append("</body></html>");
		
		SMTPClient smtpClient = new SMTPClient();
		smtpClient.SendMail(pollInitiatior, pollInitiatior, "Your poll got a reply", sb.toString());
	}

	/**
	 * 
	 * @param recipAnswers
	 * @return
	 */
	private int CountRecipientsReplies( ArrayList<SimpleEntry<String, String>> recipAnswers) {
		int recipientReplied = 0;
		
		for (SimpleEntry<String, String> entry : recipAnswers) {
			if (!entry.getValue().equals("None")) {
				recipientReplied++;
			}
		}
		
		return recipientReplied;
	}

	private void PollCompleted(String pollInitiatior, PollInfo currentPoll,
			ArrayList<SimpleEntry<String, String>> recipAnswers) {

		
		// We can mark task as "Completed" only if all recipients replied
		currentPoll.setPollStatus("Completed");
		
		// Update database on disc
		PollDatabase.saveDataBaseToXML(this._pollsDb);
		
		// meaning we need to update the poll creator and close the poll
		StringBuilder sb = new StringBuilder();
		
		sb.append("<html><body>");
		sb.append("<h1><b>Poll Is Completed!</b></h1><br>");
		sb.append("<b>Poll Subject:</b> " + currentPoll.getPollTitle() + "<br>");
		sb.append("<b>Poll Answers:</b><br>");
		for (SimpleEntry<String, String> entry : recipAnswers) {
			sb.append("Poll Recipient: " + entry.getKey() + "<br>");
			sb.append("Poll Recipient Answer: " + entry.getValue() + "<br>");
		}
		sb.append("</body></html>");
		
		
		SMTPClient smtpClient = new SMTPClient();
		smtpClient.SendMail(pollInitiatior, pollInitiatior, "Poll Completed!", sb.toString());
	}
		
	/**
	 * 
	 * @param databaseXML
	 * @return
	 */
	private boolean serializeDatabaseXml(String databaseXML) {
		try {
			
			File fXmlFile = new File(databaseXML);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			
			Element rootElement = doc.getDocumentElement();
			NodeList pollNodes = rootElement.getElementsByTagName("poll");
			
			// Iterates poll elements
			for (int i = 0; i < pollNodes.getLength(); i++) {
				
				// Single poll node
				Node pollNode = pollNodes.item(i);
				
				// Gets the email address found in "id" element
				NamedNodeMap pollAttributes = pollNode.getAttributes();
				Attr attribute = (Attr)pollAttributes.item(0);
				String pollEmailCreator = attribute.getValue();
				
				// Continue iterating the xml
				if (pollNode.hasChildNodes()) {
					
					// Get all poll childs (pollInfo)
					Element pollElement = (Element)pollNode;
					NodeList pollInfoNodes = pollElement.getElementsByTagName("pollInfo");
					ArrayList<PollInfo> pollInfoList = new ArrayList<PollInfo>();
					
					// Iterates all pollInfo elements
					for (int j = 0; j < pollInfoNodes.getLength(); j++) {
						
						// Gets a single pollInfo
						Node pollInfoNode = pollInfoNodes.item(j);
						
						// Gets pollInfo attribute "id"
						NamedNodeMap nodeAttributes = pollInfoNode.getAttributes();
						attribute = (Attr)nodeAttributes.item(0);
						
						if (pollInfoNode.hasChildNodes()) {
							
							NodeList pollChildNodes = pollInfoNode.getChildNodes();
							PollInfo pollInfo = new PollInfo();
							ArrayList<String> pollRecepients = new ArrayList<>();
							ArrayList<String> pollAnswers = new ArrayList<>();
							
							// Iterates taskInfo child elements
							for (int k = 0; k < pollChildNodes.getLength(); k++) {
								pollInfo.setPollId(attribute.getValue());
								Node pollChild = pollChildNodes.item(k);
								
								// We skip #text element (unknown shit of DOM parsing)
								if (!(pollChild.getNodeType() == Node.TEXT_NODE)) {
									
									String pollChildName = pollChild.getNodeName();
									String pollChildValue = pollChild.getTextContent();
									switch (pollChildName) {
										case "pollTitle": {
											pollInfo.setPollTitle(pollChildValue);
											break;
											}
										case "pollCreationDate": {
											pollInfo.setPollCreationDate(pollChildValue);
											break;
											}
										case "pollBody": {
											pollInfo.setPollBody(pollChildValue);
											break;
										}
										case "pollRecipient": {
											pollRecepients.add(pollChildValue);
											break;
										}
										case "pollRecepientAnswer": {
											pollAnswers.add(pollChildValue);
											break;
										}
										case "pollIsSent": {
											pollInfo.setPollIsSent(Boolean.parseBoolean(pollChildValue));
											break;
										}
										case "pollStatus": {
											pollInfo.setPollStatus(pollChildValue);
											break;
										}
										case "pollNotify": {
											pollInfo.setPollNotify(Boolean.parseBoolean(pollChildValue));
											break;
										}
									}
								}	
							}
							
							//pollInfo.setPollAnswers(pollAnswers);
							//pollInfo.setPollRecepients(pollRecepients);
							for (int k = 0; k < pollRecepients.size(); k++) {
								String recep = pollRecepients.get(k);
								String answer = pollAnswers.get(k);
								pollInfo.getPollRecipientToAnswers().add(new SimpleEntry<>(recep,answer));
							}
							pollInfoList.add(pollInfo);
						}
					}
					// add the result to the dictionary
					this._pollsDb.put(pollEmailCreator, pollInfoList);
				}
			}
			return true;
		} catch (Exception ex) {
			System.err.println("ERROR: Could not serialize xml. Exception: " + ex.getMessage());
			return false;
		}
	}
	
	public static void saveDataBaseToXML(ConcurrentHashMap<String ,ArrayList<PollInfo>> hashimap) {
		  try {
			
			// Builds document.  
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// root elements
			Document doc = docBuilder.newDocument();
			
			// Creates polls_db
			Element polls_db = doc.createElement("polls_db");
			doc.appendChild(polls_db);
	 
			// For each user.
			for (String string : hashimap.keySet()){
				
				//Creates task
				Element poll = doc.createElement("poll");
				polls_db.appendChild(poll);
				
				// Creates attribute for poll.
				Attr pollAttr = doc.createAttribute("id");
				pollAttr.setValue(string);
				poll.setAttributeNode(pollAttr);
				
				// For each user poll.
				for (PollInfo pol : hashimap.get(string)){
					
					// Creates poll info.
					Element pollInfo = doc.createElement("pollInfo");
					poll.appendChild(pollInfo);
		 
					// Attribute for poll info.
					Attr attr = doc.createAttribute("id");
					attr.setValue(pol.getPollId());
					pollInfo.setAttributeNode(attr);
					
					// pollTitle and its value
					Element pollTitle = doc.createElement("pollTitle");
					pollTitle.appendChild(doc.createTextNode(pol.getPollTitle()));
					pollInfo.appendChild(pollTitle);
			 
					// pollBody and its value
					Element pollBody = doc.createElement("pollBody");
					pollBody.appendChild(doc.createTextNode(pol.getPollBody()));
					pollInfo.appendChild(pollBody);
					
					// pollCreationDate and its value.
					Element pollCreationDate = doc.createElement("pollCreationDate");
					pollCreationDate.appendChild(doc.createTextNode(pol.getPollCreationDate()));
					pollInfo.appendChild(pollCreationDate);
			 
					for (SimpleEntry<String,String> entry : pol.getPollRecipientToAnswers()) {
						// pollRecipient and its value.
						Element pollRecipient = doc.createElement("pollRecipient");
						pollRecipient.appendChild(doc.createTextNode(entry.getKey()));
						pollInfo.appendChild(pollRecipient);
					
						// pollRecepientAnswer and its value.
						Element pollRecepientAnswer = doc.createElement("pollRecepientAnswer");
						pollRecepientAnswer.appendChild(doc.createTextNode(entry.getValue()));
						pollInfo.appendChild(pollRecepientAnswer);
					}
					
					// pollStatus and its value.
					Element pollStatus = doc.createElement("pollStatus");
					pollStatus.appendChild(doc.createTextNode(String.valueOf(pol.getPollStatus())));
					pollInfo.appendChild(pollStatus);
					
					// pollIsSent and its value.
					Element pollIsSent = doc.createElement("pollIsSent");
					pollIsSent.appendChild(doc.createTextNode(String.valueOf(pol.getPollIsSent())));
					pollInfo.appendChild(pollIsSent);
					
					Element pollNotify = doc.createElement("pollNotify");
					pollNotify.appendChild(doc.createTextNode(String.valueOf(pol.getPollNotify())));
					pollInfo.appendChild(pollNotify);	
				}
			}
			
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File("wwwroot/polls.xml"));
	 
			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);
	 
			transformer.transform(source, result);
	 
			System.out.println("File: polls.xml saved!");
	 
		  } catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		  } catch (TransformerException tfe) {
			tfe.printStackTrace();
		  }		
	}
}
