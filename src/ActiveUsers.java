import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;


public class ActiveUsers {

	// Singleton to database xml
	private static ActiveUsers instance = null;

	public static ActiveUsers getInstance() throws Exception {
		
	    if (instance == null) {
			synchronized (ActiveUsers.class) {  
	            if (instance == null) { 
	            	
	            	instance = new ActiveUsers();
	            }
	        }
	    }
	    
	    return instance;
	}
	
	// Hold the reminders.xml serialized
	private ConcurrentHashMap<String, String> _activeUsers = new ConcurrentHashMap<String, String>();
	
	/**
	 * 
	 * @param loggedUsername
	 */
	public void addUserLoggedUsername(String loggedUsername) {
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
		_activeUsers.put(loggedUsername, timeStamp);
	}
	
	public ConcurrentHashMap<String, String> getAllLoggedusers() {
		return _activeUsers;
	}
	
	public void removeLoggedUsername(String loggedUsername) {
		_activeUsers.remove(loggedUsername);
	}
}
