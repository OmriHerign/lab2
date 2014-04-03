************************* Lab 2 Computer Networks *************************
Name: Omri Hering  ID: 026533067
Name: Gal Ringle   ID: 300922424



Usage:
*******
1. Compile the java file using compile.bat
2. Run the run.bat file 
3. Surf to 127.0.0.1:8080 
4. Login using any valid mail address.
5. Use the menu to add/remove remainder task or poll.
6. Logout as from the main page. 

Implementation
*****************
We decided to implement our project using XML Files as data base.
Polls Remainders and Tasks have a separated XML file that update every time that a data change occur.
each of them has a unique ID so we can identify them when needed and they attached to a user.
We web server from lab 1 is the base of this lab so http message and responses attached to the user interface
handels as before by the relevant classes.
We used ConcurrentHashMap in java to load database to memory.

Classes
*******
** HTTPConnectionHandler.java
This class handle a single connection:
It is also implements runnable because each connection is running in a different thread
Class action:
Gets the inputStream from the connection and parse it
Parse the inputStream into an HTTPRequest
Decide what to do by the HTTP REQUEST type (GET,POST,HEAD,TRACE,OPTIONS) as requested
Generate a proper HTTP RESPONSE to the client and write it to the stream
On each bad request it generate 404


** HTTPMessage.java
This is our main Object which reflects the basic fields and action of HTTP REQUEST AND RESPONSE.
It hold all the common variables of request and response like:
GetBody(), SetBody()
Get and Set Headers
write() to stream at the end.
HTTPRequest and HTTPResponse will extends it for more functionality!


** HTTPRequest.java
This class is in charge of parsing a HTTP request connection
It parses the following:
Request method
Request uri
Request query parameters
Request headers
Request Body (if it is a post)
Request Cookies
It uses a regular expression to validate the first REQUEST line,
and then keep parsing all the headers and store them in the appropriate variables


** HTTPResponse.java
This class is in charge of creating a HTTP Response
It support the following response codes:  200, 301, 400, 404, 500, 501
This class knows to generate a response from a GENERIC POST request (from any <form> type)
This class knows to generate a "A
HTTPStreamParsingUtil.java
HTTPUtils.java
PollDatabase.java
Handles the poll database it parse the poll database XML file and also write to the
XML file when the server closed. it also uses singleton design pattern to make sure that 
this object created only once for data integrity.


PollDatabase.java
Handles the poll database it parse the poll database XML file into ConcurrentHashMap and also write to the
XML file when data change occur. it also uses singleton design pattern to make sure that 
this object created only once for data integrity.
PollInfo.java


PollResponse.java
This class responsible for creating all reminders html pages with the most updated 
data, after every change in the task options a new update page is created.

ReminderDatabase.java
Handles the reminder database it parse the reminder database XML file  into ConcurrentHashMap and also write to the
XML file when data change occur. it also uses singleton design pattern to make sure that 
this object created only once for data integrity.

ReminderInfo.java
This is a reminder object it handles all fields of the reminder that been saved from
database.

ReminderResponse.java
This class responsible for creating all reminders html pages 
(reminders.html,reminder_editor.html,submit_reminder.html) 
with the most updated data, after every change in the task options 
a new update page is created.

SMTPClient.java
This class implements the SMTP protocol 
it parse the relevant parameters from config.ini file starts the relevant streams
responsible for mail sending and the AUTH LOGIN.

SMTPSender.java
This class represent a thread the run at the background and responsible for sending 
the email of remainders and tasks the their due date has arrived or not completed
it sends the mail via SMTPClient.java class.

TaskDatabase.java
Handles the task database it parse the tasks database XML file into ConcurrentHashMap and also write to the
XML file when data change occur. it also uses singleton design pattern to make sure that 
this object created only once for data integrity.

TaskInfo.java
This is a task object it handles all fields of the task that been saved from
database.

TaskResponse.java
This class responsible for creating all tasks html (tasks.html,task_editor.html,submit_task.html)  
pages with the most updated data, after every change in the task options a new update page is created. 

WebServer.java
This is our main file, from this file we start the Server.
It starts the connection and responsible for the threads, the database loading 
and the configuration file parsing.