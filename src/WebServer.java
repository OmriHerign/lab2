import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the main class:
 * - Parse the config.ini file with all the relevant fields
 * - BONUS: Create a ThreadPool and BlockingQueue for the incoming connections
 * - Creates a ServerSocket
 * - Handles incoming connections
 * 
 * @author Omri Hering 026533067 & Gal Ringel 300922424
 *
 */
public final class WebServer {

	/* Global Variables */
	private static int _maxThreads;
	private static int _serverPort;
	private static ServerSocket _serverSocket;
	private static Properties _properties;
	public static String _rootFolder;
	public static String _defaultPage;
	public static String workingDir;
	public static boolean _ativateLogger;
	
	/**
	 * 
	 * @param argv
	 */
	public static void main(String argv[]) {		
		
		try {
			// Parse config ini file
			parseConfigIni();
			
			LoadDatabases(_rootFolder);

		} catch (NumberFormatException ex) {
			String exceptionString = "ERROR: Problem Initiazlie parameters. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			return;
		} catch (FileNotFoundException ex) {
			String exceptionString = "ERROR: config.ini file missing! Please place it in the \"src\\\" folder of the project. " +
					"\nException details: " + ex.getMessage();
			System.err.println(exceptionString);
			return;
		} catch (IOException ex) {
			String exceptionString = "ERROR: Problem parsing config.ini. Exception details: " + ex.getMessage(); 
			System.err.println(exceptionString);
			return;
		} catch (Exception ex) {
			String exceptionString = "ERROR: General Exception was cought. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			return;
		}
		try {
			_serverSocket = new ServerSocket(_serverPort, _maxThreads);	
			// Logs
			String msg = "Server is up and running on port: " + _serverPort;
			System.err.println(msg);

		} catch (IOException ex) {
			String exceptionString = "ERROR: Can't listen to Socket. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			return;
		} catch (Exception ex) {
			String exceptionString = "ERROR: Can't listen to Socket. Exception details: " + ex.getMessage();
			System.err.println(exceptionString);
			return;
		}
		
		while (true) {

			try {
				
				// Accept incoming connection
				Socket connectionSocket = _serverSocket.accept();
				System.out.println("A new connection accepted succesfully!");
				
				// Create a new connectionHandler
				HTTPConnectionHandler currentClient = new HTTPConnectionHandler(connectionSocket, _defaultPage, _rootFolder);
				
				Thread t = new Thread(currentClient);
				t.start();
				
			} catch (Exception ex) {
				
				String exceptionString = "ERROR: Could not accept a new connection. waiting for a new one...\n" + 
						"Exception Details: " + ex.getMessage();
				System.err.println(exceptionString);
			}
		}
	}

	private static void LoadDatabases(String rootFolder) {
		
		try {
			ReminderDatabase.getInstance();
			TaskDatabase.getInstance();
			PollDatabase.getInstance();
			ActiveUsers.getInstance();
			
			// run the SMTP Sender
			SMTPSender sender = new SMTPSender();
			ExecutorService exec = Executors.newCachedThreadPool();
			exec.submit(sender);
			
		} catch (Exception ex) {
			// in this kind of error, we close the server.
			System.err.println("ERROR: Could not parse one of the databases, close the server! Exception: " + ex.getMessage());
			System.exit(69);
		}
		
	}

	/**
	 * 
	 * @throws Exception
	 */
	private static void parseConfigIni() throws Exception {

		_properties = new Properties();
		_properties.load(new FileInputStream("config.ini"));

		_maxThreads = Integer.parseInt(_properties.getProperty("maxThreads"));
		if (_maxThreads < 1 || _maxThreads > 10) {
			throw new Exception("maxThread must be between 1-10");
		}
		
		_serverPort = Integer.parseInt(_properties.getProperty("port"));
		_rootFolder = (_properties.getProperty("root"));
		_defaultPage = (_properties.getProperty("defaultPage"));
	}
}