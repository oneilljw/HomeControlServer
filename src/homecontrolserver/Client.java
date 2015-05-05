package homecontrolserver;

import homecontrolclient.Login;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Client extends Thread 
{
	private int id;
	private String version;
	private ClientState state; //connected, started, logged in, dbSelected
	private Heartbeat heartbeat;
	private int year; 	//What year data is the client connected to
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    
    private GarageDoorDB garageDoorDB;
    
    private Queue<String> changeQ; //Keeps list of change JSONs that client polls
    
    private ClientManager clientMgr;
    private Login loginUser;
   
    private Calendar timestamp;
    private long timeLastActive;
    private String lastcommand;

    /**
     * Constructs a handler thread for a given socket and mark
     * initializes the stream fields, displays the first two
     * welcoming messages.
     * @throws IOException 
     */
    public Client(Socket socket, int id) throws IOException
    {
    	this.id = id;
    	version = "N/A";
    	state = ClientState.Connected;
    	heartbeat = Heartbeat.Not_Started;
    	year = -1;
        this.socket = socket;
        
        //Initialize the client manager interface
        clientMgr = ClientManager.getInstance();
        loginUser = null;
	    timestamp = Calendar.getInstance();
	    timeLastActive = System.currentTimeMillis();
        
	    changeQ = new LinkedList<String>();
        
	    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        
        garageDoorDB = GarageDoorDB.getInstance();
            
        //tell the client that they have successfully connected to the server
        output.println("LOGINConnected to the Home Control Server, Please Login");
    }
    
    int getClientID() { return id; }
    ClientState getClientState() { return state; }
    Heartbeat getClientHeartbeat() { return heartbeat; }
    long getTimeLastActiveInMillis() { return timeLastActive; }
//  int getClientUserID() { return clientUser == null ? -1 : clientUser.getID(); }
    
    void setClientState (ClientState cs) { state = cs; }
    void setClientHeartbeat(Heartbeat hb) { heartbeat = hb; }

    /**
     * The run method of this thread.
     */
    public void run()
    {
    	state = ClientState.Running;	//Client has started
    	heartbeat = Heartbeat.Active;
    	clientMgr.clientStateChanged();	//tell client 
    	String command = "";
    	lastcommand = "";
    	
        try 
        {
            // Repeatedly get commands from the client and process them.
            while (state != ClientState.Ended)
            {	
            	command = input.readLine();	//Blocks until the client sends a message to the socket
            	lastcommand = command;
            	
            	//note the time the last command was received from the client.This is the 
                //clients heart beat. When connected, the client should be asking for changes
                //no less than once a second. However the heart beat rate is determined by the client
                timeLastActive = System.currentTimeMillis();
            	
                if (command.startsWith("LOGIN_REQUEST"))
                { 
                    String response = loginRequest(command.substring(13));
                    output.println(response);
                }
                else if(command.startsWith("GET<garage_door_status>"))
                {
                	clientMgr.addLogMessage(command);
                	String response = garageDoorDB.getGarageDoorStatus();
                	output.println(response);
                	clientMgr.addLogMessage(response);               	
                }
                else if(command.startsWith("GET<changes>"))
                {   
                	if(changeQ.peek() == null)
                		output.println("NO_CHANGES");
                	else
                	{
                		//bundle the change q into a list of strings and send it
                		//to the client
                		Gson gson = new Gson();
                		List<String> qContents = new ArrayList<String>();
                		Type listOfChanges = new TypeToken<ArrayList<String>>(){}.getType();
                		
                		while(!changeQ.isEmpty())
	                		qContents.add(changeQ.remove());
                		
                		String response = gson.toJson(qContents, listOfChanges);
                		output.println(response);
                		clientMgr.addLogMessage("GET<changes> Response: " + response);
                	}
                }
                else if(command.startsWith("POST<garage_door_status"))
                {
                	clientMgr.addLogMessage(command);
                	String response = garageDoorDB.update(command.substring(23));
                	output.println(response);
                	clientMgr.addLogMessage(response);
                	clientMgr.dataChanged(this, response);
                }
                else if (command.startsWith("LOGOUT")) 
                {
                	String response = "GOODBYE";
                	output.println(response);
                	state = ClientState.Ended;
                	clientMgr.clientLoggedOut(this);  
                }
                else
                	output.println("UNRECOGNIZED_COMMAND" + command);
            } 
        } 
       	catch (IOException e) 
        {
       		String logMssg = String.format("Client %d died, I/O exception %s, last command: %s",
       				id, e.getMessage(), lastcommand);
       		clientMgr.addLogMessage(logMssg);
       		clientMgr.clientDied(this); 
        }
        catch (NullPointerException npe)
        {
        	String logMssg = String.format("Client %d died, NullPointerException %s, last command: %s",
        			id, npe.getMessage(), lastcommand);
        	clientMgr.addLogMessage(logMssg);
        	clientMgr.clientDied(this);
        }
    }
    
    void closeClientSocket()
    {
    	try {
        	socket.close();
        } 
    	catch (IOException e) {
    		String logMssg = String.format("Client %d: Close Socket IOException: %s", id, e.getMessage());
        	clientMgr.addLogMessage(logMssg);
        }
    }
    
    String loginRequest(String loginjson)
    {
    	Gson gson = new Gson();
    	Login lo = gson.fromJson(loginjson, Login.class);
    	
//    	float lo_version = Float.parseFloat(lo.getVersion());
  	
    	String value = "INVALID";
    	
    	if(lo != null && lo.getUserID().equals("john") && lo.getPassword().equals("erin1992"))//user found, password matches
    	{
    		//Create user json and attach to VALID response
    		state = ClientState.Logged_In;	//Client logged in
    		
    		loginUser = lo;
    		version = lo.getVersion();
    		
    		value = "VALID";
    		
    		clientMgr.clientLoginAttempt(true, String.format("Client %d, %s %s login request sucessful",
    															id, loginUser.getUserID(), loginUser.getUserID()));	
    	}
    	
    	else 	//found the user but pw is incorrect
    	{
    		clientMgr.clientLoginAttempt(false, String.format("Client %d login request failed with v%s:"
    				+ " Incorrect password", id, lo.getVersion()));
    		value += "Incorrect password";
    	}
    	
    	return value;
    }
    
    String[] getClientTableRow()
    {
    	String[] row = new String[9];
    	row[0] = Long.toString(id);
    	
    	if(loginUser != null)	//if server user is known, user their name
    	{
    		row[1] = loginUser.getUserID();
    		row[2] = loginUser.getUserID();
    		row[3] = "Active";	
    	}
    	else
    	{
    		row[1] = "Anonymous";
    		row[2] = "Anonymous";
    		row[3] = "U";
    	}
    	
    	row[4] = state.toString();
    	row[5] = heartbeat.toString().substring(0,1);	//get string of 1st character
    	row[6] = state == ClientState.DB_Selected ? Integer.toString(year) : "None";
    	row[7] = version;
    	row[8] = new SimpleDateFormat("MM/dd H:mm:ss").format(timestamp.getTime());
    		
    	return row;
    }
    
    void addChange(String change)
    {
    	changeQ.add(change);
    }
    
    String getClientName()
    { 
    	return "John";
    }
}

