import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Random;

public class HTTPUtils {

	/**
	 * Get a string to file path on server, and load it into byte[] for further use
	 * @param path - filename path on hard drive
	 * @return byte[] with the asked file
	 * @throws IOException, FileNotFoundException - if he doesn't find 
	 */
	public static byte[] getFileBytes(String path) throws IOException, FileNotFoundException {
		
		File pageFile = new File(path);
		byte[] pageContent = null;
		
		// If file exists, read it into byte[]
		if (pageFile.exists()) {
			
		    FileInputStream fis = new FileInputStream(pageFile);
		    pageContent = new byte[(int)pageFile.length()];
		    fis.read(pageContent);
		    fis.close();
		}
		
		return pageContent;
	}
	
	public static void validateFormDateFormat(String date, String format) throws Exception {
		
		try {
		    DateFormat formatter = new SimpleDateFormat(format);
		    formatter.setLenient(false);
		    formatter.parse(date);
		} catch (ParseException ex) { 
		    throw new Exception("Date was not in valid format! please use: dd/MM/yyyy HH:mm:ss");
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public static String generateUniqueId()
	{
	    return new BigInteger(130, new Random()).toString(32);
	}

	/**
	 * 
	 * @param attribute
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws Exception
	 */
	public static String LoadDatabaseXmlNameFromConfig(String value) throws FileNotFoundException, IOException{

		Properties _properties = new Properties();
		_properties.load(new FileInputStream("config.ini"));
		return (_properties.getProperty(value));
	}
}
