package homecontrolserver;

public class Login
{
	private String userID;
	private String password;
	private String version;
	
	Login(String userID, String password, String version)
	{
		this.userID = userID;
		this.password = password;
		this.version = version;
	}
	
	//getters
	public String getUserID() { return userID; }
	public String getPassword() { return password; }
	public String getVersion() { return version; }
}
