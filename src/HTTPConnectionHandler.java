import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

/**
 * This class handle a single connection:
 * It is also implements runnable because each connection is running in a different thread
 * Class action:
 * - Gets the inputStream from the connection and parse it
 * - Parse the inputStream into an HTTPRequest
 * - Decide what to do by the HTTP REQUEST type (GET,POST,HEAD,TRACE,OPTIONS) as requested
 * - Generate a proper HTTP RESPONSE to the client and write it to the stream
 * - On each bad request it generate 404
 * - BONUS: if you surf to /redirect it generates 301 to the client to "GET /"
 * - BONUS: On each response we generate a unique MachineId and Set-Cookie it.
 * 			On each request we search for this cookie and prints out if we found it.
 * 			That way we can manage users if we would like.
 * @author Omri Hering 026533067 & Gal Ringel 300922424
 *
 */
public class HTTPConnectionHandler implements Runnable {
	
	/* Client connection Socket */
	private Socket _connectionSocket;
	
	/* Default root and default page settings from config.ini */
	//private String _defaultPage;
	private String _defaultRoot;

	/**
	 * Initializes all relevant members
	 * @param connectionSocket
	 * @param defaultPage
	 * @param defaultRoot
	 * @throws Exception
	 */
	public HTTPConnectionHandler(Socket connectionSocket, String defaultPage, String defaultRoot) throws Exception {
		
		this._connectionSocket = connectionSocket;
		//this._defaultPage = defaultPage;
		this._defaultRoot = defaultRoot;
	}

	@Override
	/**
	 * Each connectionHandler thread main function
	 */
	public void run() {
		
		HTTPRequest req = new HTTPRequest();
		HTTPStreamParsingUtil httpInputStream = null;
		PrintStream outputStream = null;
		HTTPResponse response = null;
		
		try {
			// we always response as HTTP/1.1
			response = new HTTPResponse(HTTPMessage.HTTP_VERSION11);
			
			// Some useful booleans
			boolean isChunked = false;
			
			// Gets the inputStream of the client connection
			httpInputStream = new HTTPStreamParsingUtil(_connectionSocket.getInputStream());
			
			// Parse the current request connection
			boolean isSuccedd = req.parseRequest(httpInputStream);
			if (!isSuccedd) {
				
				// Request REGEX was failed meaning request was invalid
				// We return "400 Bad Request" (server failed to parse/understand the request)"
				response.generate400();
				
			} else {
			
				if (req.path == null) {
					throw new IOException("Got Empty Stream!");
				}
				
				// CleanURL: WE DONT SUPPORT URLS WITH "../"
				String uriBeforeCleaned = req.path.replaceAll("../", "");
				req.path = uriBeforeCleaned;
				
				// hold the request path on our server
				String pagePath = _defaultRoot;
				
				// Logs the GET header
				System.out.println(req.getRequestTitle());
				
				// Searching for our cookie
				String currentLoggedUsername = null;
				boolean isOurServerCookieExists = false;
				String cookieValue = req.getHeader("Cookie");
				if (cookieValue != null && cookieValue.contains("usermail")) {
					
					// We already did set-cookie sometime, so we know this user and we print is email
					String[] splittedCookie = cookieValue.split("=");
					if (splittedCookie.length > 1) {
						
						currentLoggedUsername = splittedCookie[1];				
						if (!currentLoggedUsername.equals("")) {
							// There is a valid cookie
							System.out.println("Welcome back! User: " + currentLoggedUsername);
							isOurServerCookieExists = true;
						}
					}
				}
				
				if (req.method.equals(HTTPRequest.Method.GET) ||
						req.method.equals(HTTPRequest.Method.HEAD)) {
					
					// Checking whether chunked is needed
					isChunked = isChunkedNeeded(req, response, isChunked);
					handleGetRequest(req, response, isChunked, pagePath,
							isOurServerCookieExists, currentLoggedUsername);
				}
				else if (req.method.equals(HTTPRequest.Method.POST)) {
					handlePostRequest(req, response, currentLoggedUsername, isOurServerCookieExists);
				} 
				else if (req.method.equals(HTTPRequest.Method.OPTIONS)) {
					handleOptionsRequest(response);
				} 
				else if (req.method.equals(HTTPRequest.Method.TRACE)) {
					handleRequestTrace(req, response);
				} else {
					// HTTPRequest.Method.OTHER, Not supported
					response.generate501();
				}
				
				// Logs the HTTP Response line
				System.out.print(response.buildResponseFirstHeader());
				
				// If it was HEAD request, we delete response body.
				if (req.method.equals(HTTPRequest.Method.HEAD)) {
					response.setBody(null);
				}
			}
			
			// Writes the response to the client connection output stream socket
			outputStream = new PrintStream(_connectionSocket.getOutputStream());
			response.write(outputStream, isChunked);
			//outputStream.flush();
			outputStream.close();
			//httpInputStream.close();
			//_connectionSocket.close();
		}
		catch (FileNotFoundException ex) {	
			closeAllResources(response, outputStream, ex);
		} 
		catch (IOException ex) {	
			closeAllResources(response, outputStream, ex);
		}
		catch (Exception ex) {
			closeAllResources(response, outputStream, ex);
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param response
	 */
	private void handleRequestTrace(HTTPRequest req, HTTPResponse response) {
		byte[] responseBytes = response.generateTRACE_HTML(req.headers, req.requestTitle).getBytes();
		response.setHeader("content-type", "text/html");
		response.generate200OK(responseBytes);
	}

	/**
	 * 
	 * @param response
	 */
	private void handleOptionsRequest(HTTPResponse response) {
		String allowHeader = response.generateAllowHeader();
		response.setStatusCode(200, HTTPResponse.STATUS_200);
		response.setHeader("allow", allowHeader);
	}

	/**
	 * 
	 * @param req
	 * @param response
	 */
	private void handlePostRequest(HTTPRequest req, HTTPResponse response,
			String loggedUsername, boolean isEmailCookieFound) {
		if (req.params.size() > 0) {
			// POST is fine and has parameters
			
			if (req.path.equals("/index_params.html")) {
				byte[] responseBytes = response.generatePOST_HTML(req.params).getBytes();
				response.setHeader("content-type", "text/html");
				response.generate200OK(responseBytes);
				
			} else if (req.path.equals("/submit_reminder.html")) {
				try {					
					String reminderId = req.params.get("reminder_id");
					String newReminderTitle = req.params.get("reminder_subject");
					String newReminderBody = req.params.get("reminder_body");
					String newReminderDueDate = req.params.get("reminder_date");
					String newReminderkDueTime = req.params.get("reminder_time");
					String isNewReminder = req.params.get("reminder_action");
					
					if (newReminderTitle.isEmpty() || newReminderBody.isEmpty() || 
							newReminderDueDate.isEmpty() || newReminderkDueTime.isEmpty()) {
						throw new Exception("Reminder form was not filled! (title or body or date was empty!)");
					}
					
					String reminderFullDueDateTime = newReminderDueDate + " " + newReminderkDueTime;
					
					// validate form date
					HTTPUtils.validateFormDateFormat(reminderFullDueDateTime, "dd/MM/yyyy HH:mm:ss");
					
					ReminderInfo newReminderInfo = new ReminderInfo();
					newReminderInfo.setReminderTitle(newReminderTitle);
					newReminderInfo.setReminderBody(newReminderBody);
					newReminderInfo.setReminderSendingDate(reminderFullDueDateTime);
					newReminderInfo.setReminderIsSent(false);
					
					if (isNewReminder != null) {
						// Meaning we need to add new reminder
						String newReminderId = HTTPUtils.generateUniqueId();
						newReminderInfo.setReminderId(newReminderId);
						ReminderDatabase.getInstance().addReminderInfo(loggedUsername, newReminderInfo);
					} else {
						// Meaning we edit an existing record reminder
						newReminderInfo.setReminderId(reminderId);
						ReminderDatabase.getInstance().updateReminderInfo(loggedUsername, newReminderInfo);
					}
					
					// Redirect back to reminders
					response.generate301("reminders.html");
					response.setHeader("content-length", null);
					
				} catch (Exception ex) {					
					String reminderErrorSubject = "Error while submitting the reminder!";
					String submitErrorHTML = ReminderResponse.buildReminderErrorPage(loggedUsername, reminderErrorSubject, "Form was empty!<br>Exception: " +ex.getMessage());
					response.setHeader("content-type", "text/html");
					response.generate200OK(submitErrorHTML.getBytes());
				}
				
			} else if (req.path.equals("/submit_task.html")) {
				try {					
					String taskId = HTTPUtils.generateUniqueId();
					String taskTitle = req.params.get("task_subject");
					String taskBody = req.params.get("task_body");
					String taskDueDate = req.params.get("task_date");
					String taskDueTime = req.params.get("task_time");
					String taskRecepient = req.params.get("task_recepient");
					
					if (taskTitle.isEmpty() || taskBody.isEmpty() || taskDueDate.isEmpty() ||
							taskDueTime.isEmpty() || taskRecepient.isEmpty()) {
						throw new Exception("Task form was not filled! (title or body or Date or Recipient was empty!)");
					}
					
					String taskFullDueDateTime = taskDueDate + " " + taskDueTime;
					
					// validate form date
					HTTPUtils.validateFormDateFormat(taskFullDueDateTime, "dd/MM/yyyy HH:mm:ss");
					
					TaskInfo newTaskInfo = new TaskInfo();
					newTaskInfo.setTaskId(taskId);
					newTaskInfo.setTaskTitle(taskTitle);
					newTaskInfo.setTaskDueDate(taskFullDueDateTime);
					newTaskInfo.setTaskStatus("In Progress...");
					newTaskInfo.setTaskIsSent(false);
					newTaskInfo.setTaskRecipient(taskRecepient);
					newTaskInfo.setTaskBody(taskBody);
					
					// adding reply link to body with the task id
					String serverName = HTTPUtils.LoadDatabaseXmlNameFromConfig("ServerName");
					String serverPort = HTTPUtils.LoadDatabaseXmlNameFromConfig("port");
					String hostname = serverName + ":" + serverPort;
					String replyLink = "http://" + hostname + "/" + "task_reply.html?id=" + taskId;
					taskBody += "\r\n <html><body>";
					taskBody += "\r\n <a href=\"" + replyLink + "\">Mark task as completed</a>";
					taskBody += "\r\n </body></html>";
					
					// Addding the new task to database
					TaskDatabase.getInstance().addTaskInfo(loggedUsername, newTaskInfo);
					
					System.out.println("New task was added successfully!");
					System.out.println("Sending a mail to the recepient address!");
					
					SMTPClient smtpClient = new SMTPClient();
					smtpClient.SendMail(loggedUsername, taskRecepient, "Task: " + newTaskInfo.getTaskTitle(), taskBody);
					
					// Redirect back to reminders
					response.generate301("tasks.html");
					response.setHeader("content-length", null);
					
				} catch (Exception ex) {					
					String taskErrorSubject = "Error while submitting the task!";
					String submitErrorHTML = TaskResponse.buildTaskErrorPage(loggedUsername, taskErrorSubject, "Form was empty!<br>Exception: " +ex.getMessage());
					response.setHeader("content-type", "text/html");
					response.generate200OK(submitErrorHTML.getBytes());
				}
			}
			else if (req.path.equals("/submit_poll.html")) {
				try {					
					
					PollInfo newPollInfo = new PollInfo();
					String pollId = HTTPUtils.generateUniqueId();
					String pollTitle = req.params.get("poll_subject");
					String pollBody = req.params.get("poll_body");
					String pollRecepient = req.params.get("poll_recepients");
					String pollAnswers = req.params.get("poll_answers");
					
					String pollNotifyOnEveryReply = "no";
					if (req.params.get("notify") != null) {
						pollNotifyOnEveryReply = req.params.get("notify");
					}
					
					if (pollTitle.isEmpty() || pollTitle.isEmpty() || pollRecepient.isEmpty() || pollAnswers.isEmpty()) {
						throw new Exception("Poll form was not filled! (title or body or Answers or Recipient was empty!)");
					}
					
					String[] allRecepients = pollRecepient.split("\r\n");
					ArrayList<String> recepientsList = new ArrayList<>(); 
					for (String recepient : allRecepients) {
						recepientsList.add(recepient);
						newPollInfo.getPollRecipientToAnswers().add(new SimpleEntry<String, String>(recepient, "None"));
					}
					
					String[] allAnswers = pollAnswers.split("\r\n");
					ArrayList<String> answersList = new ArrayList<>(); 
					for (String answer : allAnswers) {
						answersList.add(answer);
					}
					
					newPollInfo.setPollId(pollId);
					newPollInfo.setPollTitle(pollTitle);
					newPollInfo.setPollBody(pollBody);
					newPollInfo.setPollStatus("In Progress...");
					newPollInfo.setPollIsSent(false);
					
					if (pollNotifyOnEveryReply.equals("yes")) {
						newPollInfo.setPollNotify(true);
					} else {
						newPollInfo.setPollNotify(false);
					}
					
					// Addding the new poll to database
					PollDatabase.getInstance().addPollInfo(loggedUsername, newPollInfo);
					System.out.println("New poll was added successfully!");
					System.out.println("Sending mails to all recepients!");
					
					// wrap all answers with appropriate links and send mail to all recipients
					EmailRecipients(loggedUsername, newPollInfo, pollId,
							pollBody, recepientsList, answersList);
					
					// Redirect back to polls
					response.generate301("polls.html");
					response.setHeader("content-length", null);
					
				} catch (Exception ex) {					
					String pollErrorSubject = "Error while submitting the poll!";
					String submitErrorHTML = PollResponse.buildPollErrorPage(loggedUsername, pollErrorSubject, "Form was empty!<br>Exception: " +ex.getMessage());
					response.setHeader("content-type", "text/html");
					response.generate200OK(submitErrorHTML.getBytes());
				}
			}
		}
	}

	private void EmailRecipients(String loggedUsername, PollInfo newPollInfo,
			String pollId, String pollBody, ArrayList<String> recepientsList,
			ArrayList<String> answersList) throws FileNotFoundException, IOException {
		
		String serverName = HTTPUtils.LoadDatabaseXmlNameFromConfig("ServerName");
		String serverPort = HTTPUtils.LoadDatabaseXmlNameFromConfig("port");
		String hostname = serverName + ":" + serverPort;
		
		// Create SMTPClient instance in order to send mails
		SMTPClient smtpClient = new SMTPClient();
		
		for (String recipient : recepientsList) {
			
			StringBuilder sb = new StringBuilder();
			sb.append(pollBody + "<br><html><body>");
			
			for (String answer : answersList) {
				String replyLink = "http://" + hostname + "/" +
						"poll_reply.html?id=" + pollId + 
						"&email=" + recipient +
						"&answer=" + URLEncoder.encode(answer,"UTF-8");
				
				//String replyLinkEncoded = URLEncoder.encode(replyLink, "UTF-8");
				String fullLink = "<a href=\"" + replyLink + "\">" + answer + "</a><br>";
				sb.append(fullLink);
			}
			
			sb.append("<br></body></html>");
			String mailSubject = "Poll: " + newPollInfo.getPollTitle();
			smtpClient.SendMail(loggedUsername, recipient, mailSubject, sb.toString());
		}
	}

	/**
	 * 
	 * @param req
	 * @param response
	 * @param isChunked
	 * @param pagePath
	 * @return
	 * @throws Exception 
	 */
	private void handleGetRequest(HTTPRequest req, HTTPResponse response,
			boolean isChunked, String pagePath, boolean isOurServerCookieExists,
			String currentLoggedUsername) 
			throws Exception {
		
		byte[] contentBytes;
		
		ReminderDatabase reminderDatabase;
		TaskDatabase taskDatabase;
		PollDatabase pollDatabase;
		
		try {
			
			// Get instance of the databases
			reminderDatabase = ReminderDatabase.getInstance();
			taskDatabase = TaskDatabase.getInstance();
			pollDatabase = PollDatabase.getInstance();
			
		} catch (Exception ex) {
			System.out.println("ERROR: could not get an instance of one of the databases xml");
			return;
		}
		
		if (req.path.startsWith("/index.html")) {
			
			if (req.params.size() > 0) {
				// meaning we have params, we need to set cookie
				String submittedEmailAddress = req.params.get("email");
				
				// Set our server cookie in case it not exists
				if (!isOurServerCookieExists) {
					response.setCookie("usermail", submittedEmailAddress);
					response.generate301("index.html");
					response.setHeader("content-length", null);
				}
			} else {
				if (isOurServerCookieExists) {
					// cookie exists, we just redirect
					response.generate301("main.html");
					response.setHeader("content-length", null);
				} else {
					contentBytes = HTTPUtils.getFileBytes(pagePath + req.path);
					response.setHeader("content-type", "text/html");
					response.generate200OK(contentBytes);
				}	
			}
		} else if (req.path.equals("/main.html")) {
			if (isOurServerCookieExists) {
				
				// Cookie exists, we show main.html page
				ActiveUsers.getInstance().addUserLoggedUsername(currentLoggedUsername);
				
				response.setHeader("content-type", "text/html");
				response.generate200OK(response.generateMain_HTML(currentLoggedUsername).getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/logout.html")) {
			
			// we need to erase the cookie
			response.setCookie("usermail", "");
			ActiveUsers.getInstance().removeLoggedUsername(currentLoggedUsername);
			
			// redirect back to index.html
			response.generate301("index.html");
			response.setHeader("content-length", null);
			
		} else if (req.path.endsWith(".jpg") || req.path.endsWith(".jpeg") ||
				req.path.endsWith(".bmp") || req.path.endsWith(".gif") ||
				req.path.endsWith(".png") || req.path.equals("/favicon.ico")) {
			
			contentBytes = HTTPUtils.getFileBytes(pagePath + req.path);
			response.setHeader("content-type", "image");
			response.generate200OK(contentBytes);
			
		} else if (req.path.equals("/reminders.html")) {
				
			if (isOurServerCookieExists) {
				// Cookie exists, we show reminders.html page
				
				ArrayList<ReminderInfo> reminders = reminderDatabase.getRemindersDb().get(currentLoggedUsername);
				String reminderHTMLBody = ReminderResponse.BuildReminderHTML(currentLoggedUsername, reminders);
				response.setHeader("content-type", "text/html");
				response.generate200OK(reminderHTMLBody.getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
			
		} else if (req.path.equals("/reminder_editor.html")) {
			
			if (isOurServerCookieExists) {
				
				if (req.params.size() > 0) {
					
					// This case is editing an existing reminder
					String reminderId = req.params.get("id");
					
					// verify the param is "id" and not something else
					if (reminderId != null) {
						ArrayList<ReminderInfo> reminders = reminderDatabase.getRemindersDb().get(currentLoggedUsername);
						
						ReminderInfo reminderInfo = ReminderInfo.getSpecificReminderInfo(reminderId, reminders);
						if (reminderInfo != null) {
							String reminderEditorHTML = ReminderResponse.editAReminderHTML(reminderInfo);
							
							response.setHeader("content-type", "text/html");
							response.generate200OK(reminderEditorHTML.getBytes());
						}
					}
				} else {
					// No id attribute, meaning we need to add a new reminder
					
						String reminderEditorHTML = ReminderResponse.createANewReminderHTML();
						response.setHeader("content-type", "text/html");
						response.generate200OK(reminderEditorHTML.getBytes());
				}
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
			
		} else if (req.path.equals("/reminder_delete")) {
			
			// Verify id is exists
			if (req.params.size() > 0) {
				String reminderId = req.params.get("id");
				
				// verify its really "id"
				if (reminderId != null) {
					try {
						
						// Delete the given reminderId
						ReminderDatabase.getInstance().deleteReminderId(currentLoggedUsername, reminderId);
						
						// Refresh the page
						ArrayList<ReminderInfo> reminders = reminderDatabase.getRemindersDb().get(currentLoggedUsername);
						String reminderHTMLBody = ReminderResponse.BuildReminderHTML(currentLoggedUsername, reminders);
						response.setHeader("content-type", "text/html");
						response.generate200OK(reminderHTMLBody.getBytes());
						
					} catch (Exception ex) {
						// Could not delete, return error
						String submitErrorHTML = ReminderResponse.buildReminderSubmitERRORHTML(currentLoggedUsername);
						response.setHeader("content-type", "text/html");
						response.generate200OK(submitErrorHTML.getBytes());
					}
				}
			}
		} else if (req.path.equals("/tasks.html")) {			
			if (isOurServerCookieExists) {
				// Cookie exists, we show reminders.html page
				
				ArrayList<TaskInfo> tasks = taskDatabase.getTasksDB().get(currentLoggedUsername);
				String taskHTMLBody = TaskResponse.buildTasksPage(currentLoggedUsername, tasks);
				response.setHeader("content-type", "text/html");
				response.generate200OK(taskHTMLBody.getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/task_delete")) {
			
			// Verify id is exists
			if (req.params.size() > 0) {
				String taskId = req.params.get("id");
				
				// verify its really "id"
				if (taskId != null) {
					try {
						
						// Delete the given taskId
						TaskDatabase.getInstance().deleteTaskId(currentLoggedUsername, taskId);
						
						// Refresh the page
						ArrayList<TaskInfo> tasks = taskDatabase.getTasksDB().get(currentLoggedUsername);
						String taskHTMLBody = TaskResponse.buildTasksPage(currentLoggedUsername, tasks);
						response.setHeader("content-type", "text/html");
						response.generate200OK(taskHTMLBody.getBytes());
						
					} catch (Exception ex) {
						// Could not delete, return error
						String submitErrorHTML = TaskResponse.buildTaskSubmitErrorPage(currentLoggedUsername, ex.getMessage());
						response.setHeader("content-type", "text/html");
						response.generate200OK(submitErrorHTML.getBytes());
					}
				}
			}
		} else if (req.path.equals("/task_editor.html")) {
			if (isOurServerCookieExists) {

				// No id attribute, meaning we need to add a new task
				String taskEditorHTML = TaskResponse.createANewTaskHTML();
				response.setHeader("content-type", "text/html");
				response.generate200OK(taskEditorHTML.getBytes());
				
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/task_reply.html")) {
			
			// Verify id is exists
			if (req.params.size() > 0) {
				String taskId = req.params.get("id");
				
				// verify its really "id"
				if (taskId != null) {
					try {
						
						// Mark the given taskId as "Completed"
						TaskDatabase.getInstance().markTaskInfoAsCompleted(currentLoggedUsername, taskId);
						
						String pollHTMLBody = TaskResponse.buildTaskThanksForReplying();
						response.setHeader("content-type", "text/html");
						response.generate200OK(pollHTMLBody.getBytes());
						
					} catch (Exception ex) {
						// Could not delete, return error
						String submitErrorHTML = TaskResponse.buildTaskSubmitErrorPage(currentLoggedUsername, ex.getMessage());
						response.setHeader("content-type", "text/html");
						response.generate200OK(submitErrorHTML.getBytes());
					}
				}
			}
		} else if (req.path.equals("/polls.html")) {
			if (isOurServerCookieExists) {
				// Cookie exists, we show reminders.html page
				
				ArrayList<PollInfo> polls = pollDatabase.getPollsDb().get(currentLoggedUsername);
				String pollHTMLBody = PollResponse.buildPollsPage(currentLoggedUsername, polls);
				response.setHeader("content-type", "text/html");
				response.generate200OK(pollHTMLBody.getBytes());
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/poll_editor.html")) {
			if (isOurServerCookieExists) {

				// No id attribute, meaning we need to add a new poll
				String pollEditorHTML = PollResponse.createANewPollHTML();
				response.setHeader("content-type", "text/html");
				response.generate200OK(pollEditorHTML.getBytes());
				
			} else {
				// cookie does not exists we redirect to index.html
				response.generate301("index.html");
				response.setHeader("content-length", null);
			}
		} else if (req.path.equals("/poll_delete")) {
			
			// Verify id is exists
			if (req.params.size() > 0) {
				String pollId = req.params.get("id");
				
				// verify its really "id"
				if (pollId != null) {
					try {
						
						// Delete the given reminderId
						PollDatabase.getInstance().deletePollId(currentLoggedUsername, pollId);
						
						// Refresh the page
						ArrayList<PollInfo> polls = pollDatabase.getPollsDb().get(currentLoggedUsername);
						String pollHTMLBody = PollResponse.buildPollsPage(currentLoggedUsername, polls);
						response.setHeader("content-type", "text/html");
						response.generate200OK(pollHTMLBody.getBytes());
						
					} catch (Exception ex) {
						// Could not delete, return error
						String errorSubject = "Failure of Deleting the given poll Id!";
						String submitErrorHTML = PollResponse.buildPollErrorPage(currentLoggedUsername, errorSubject, ex.getMessage());
						response.setHeader("content-type", "text/html");
						response.generate200OK(submitErrorHTML.getBytes());
					}
				}
			}
		} else if (req.path.equals("/poll_reply.html")) {
			
			// Verify id is exists
			if (req.params.size() > 0) {
				String pollId = req.params.get("id");
				String pollEmail = req.params.get("email");
				String pollAnswer = req.params.get("answer");
				
				// verify its really "id"
				if ((pollId != null) && (pollEmail != null) && (pollAnswer != null)) {
					try {
						
						// Mark the given taskId as "Completed"
						PollDatabase.getInstance().markPollInfoAsCompleted(pollId, pollEmail, pollAnswer);
						
						// return thanks for replying!
						String pollHTMLBody = PollResponse.buildPollThanksForReplying();
						response.setHeader("content-type", "text/html");
						response.generate200OK(pollHTMLBody.getBytes());
						
					} catch (Exception ex) {
						// Could not delete, return error
						String errorSubject = "Failure of Replying this poll!";
						String submitErrorHTML = PollResponse.buildPollErrorPage(currentLoggedUsername, errorSubject, ex.getMessage());
						response.setHeader("content-type", "text/html");
						response.generate200OK(submitErrorHTML.getBytes());
					}
				} else {
					// id or email or answer not provided! it's not valid.
					String errorSubject = "Failure of Replying this poll!";
					String ErrorHTML = PollResponse.buildPollErrorPage(currentLoggedUsername, errorSubject, "Url parameters are bad or not found in database");
					response.setHeader("content-type", "text/html");
					response.generate200OK(ErrorHTML.getBytes());
				}
			} else {
				String errorSubject = "Failure of Replying this poll!";
				String ErrorHTML = PollResponse.buildPollErrorPage(currentLoggedUsername, errorSubject, "Url parameters are bad or not found in database");
				response.setHeader("content-type", "text/html");
				response.generate200OK(ErrorHTML.getBytes());
			}
		} else {
				// In all other pages we do:
				allOtherPages(response, isOurServerCookieExists);
			}
	}

	private void allOtherPages(HTTPResponse response,
			boolean isOurServerCookieExists) {
		if (isOurServerCookieExists) {
			// cookie exists, we just redirect
			response.generate301("main.html");
			response.setHeader("content-length", null);
		} else {
			
			// cookie does not exists we redirect to index.html
			response.generate301("index.html");
			response.setHeader("content-length", null);
		}
	}

	/**
	 * 
	 * @param req
	 * @param response
	 * @param isChunked
	 * @return
	 */
	private boolean isChunkedNeeded(HTTPRequest req, HTTPResponse response,
			boolean isChunked) {
		// Deals with transfer-encoding: chunked
		String chunkedHeader = req.headers.get("transfer-encoding");
		if (chunkedHeader != null && chunkedHeader.equals("chunked")) {
			isChunked = true;
			response.setHeader("Transfer-Encoding", "chunked");
		}
		return isChunked;
	}
	
	/**
	 * In case of an error, we log it and closes everything
	 * @param response
	 * @param outputStream
	 * @param ex
	 */
	void closeAllResources(HTTPResponse response, PrintStream outputStream, Exception ex) {
		
		// Logs the exception
		String exceptionString = "ERROR: some error occured! closing connection..." + ex.getMessage();
		System.err.println(exceptionString);
		
		// Generate 500 response and closes the stream
		response.generate500();
		outputStream.close();
		
		try {
			_connectionSocket.close();
		} catch (IOException ex1) {
			
			exceptionString = "ERROR: Could not close the connection proerply: " + ex1.getMessage();
			System.err.println(exceptionString);
		}
	}
}
