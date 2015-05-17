package homecontrolserver;

import homecontrolclient.GarageDoor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import com.google.gson.Gson;


public class GarageDoorDB implements ActionListener
{
	//Takes garage door 10 seconds to open/close. Nyquist
	private static final int STATUS_POLLING_RATE = 1000 * 5;	
	private static final boolean YUN_CONNECTED = true;
	
	private static GarageDoorDB instance = null;
	private String leftDoorOpen, rightDoorOpen;
	private Timer doorStatusTimer;
	
	private GarageDoorDB()
	{
		//initialize door status
		if(YUN_CONNECTED)
		{
			getDoorStatusFromYun(Door.LEFT);
			getDoorStatusFromYun(Door.RIGHT);
		}
		else
		{
			leftDoorOpen = "false";
			rightDoorOpen = "false";
		}
		
		//set the stoplight status
		if(leftDoorOpen.equals("true"))
			ServerUI.setStoplight(0, 2);
		else
			ServerUI.setStoplight(0, 0);
		
		if(rightDoorOpen.equals("true"))
			ServerUI.setStoplight(1, 2);
		else
			ServerUI.setStoplight(1, 0);
		
		//Create the polling timer
    	doorStatusTimer = new Timer(STATUS_POLLING_RATE, this);
    	doorStatusTimer.start();
	}
	
	public static GarageDoorDB getInstance()
	{
		if(instance == null)
			instance = new GarageDoorDB();

		return instance;
	}
	
	void getDoorStatusFromYun(Door door)
	{
		String stringURL = null;
		String response = null;
		
		if(door == Door.LEFT)
		{
			stringURL = String.format("http://arduino.local/arduino/readL");
			response = sendCommandToYun(stringURL);
			
			if(response != null && response.equals("Pin D2 value is 1"))
			{
				//left door is open
				leftDoorOpen = "true";
			}
			else if(response != null && response.equals("Pin D2 value is 0"))
			{
				//left door is closed
				leftDoorOpen = "false";
			}
			else
			{
				//ERROR OCCURRED
			}
		}
		else
		{
			stringURL = String.format("http://arduino.local/arduino/readR");
			response = sendCommandToYun(stringURL);
			
			if(response != null && response.equals("Pin D3 value is 1"))
			{
				//right door is open
				rightDoorOpen = "true";
			}
			else if(response != null && response.equals("Pin D3 value is 0"))
			{
				//right door is closed
				rightDoorOpen = "false";
			}
			else
			{
				//ERROR OCCURRED
			}
		}
	}
	
	String toggleGarageDoorUsingYun(Door door)
	{
		//form the String url request
		String stringURL;
		if(door == Door.LEFT)
			stringURL = "http://arduino.local/arduino/toggleL";	//
		else
			stringURL = "http://arduino.local/arduino/toggleR";
		
		String response = sendCommandToYun(stringURL);
		
		//if operation was successful, notify the client. It's the clients responsibility to check the status to 
		//see if the operation actually changed the door.
		if(response != null && door == Door.LEFT && response.equals("Pin D11 value is 0") ||
			door == Door.RIGHT && response.equals("Pin D12 value is 0"))
			return "UPDATED_GARAGE_DOOR";
		else
			return "UPDATE_GARAGE_DOOR_FAILED";	
	}
	
	String sendCommandToYun(String stringURL)
	{
		//send the request to arduino
		StringBuffer response = new StringBuffer(1024);
				 
		//Turn the string into a valid URL
		URL arduinoAPIUrl = null;
		try
		{
			arduinoAPIUrl = new URL(stringURL);
		} 
		catch (MalformedURLException e2)
		{
			JOptionPane.showMessageDialog(null, "Can't form Arduino API URL",
					"Arduino URL API Issue", JOptionPane.ERROR_MESSAGE);
		}
				
		//Attempt to open the URL via a network connection to the Internet
		HttpURLConnection httpconn = null;
		try
		{
//			System.out.println(String.format("Sent to Arduino: %s", arduinoAPIUrl.toString()));
			httpconn = (HttpURLConnection)arduinoAPIUrl.openConnection();
					
//			System.out.println(String.format("ArduninoReponseCode: %d: %S", 
//			    httpconn.getResponseCode(), httpconn.getResponseMessage()));
		} 
		catch (IOException e1)
		{
			JOptionPane.showMessageDialog(null, "Can't access Arduino API",
					"Ardunio access sssue", JOptionPane.ERROR_MESSAGE);
		}
				
		//It opened successfully, get the data via HTTP GET
		try
		{
			if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK)
			{   		
				BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()), 1024);
			    String strLine = null;
			    while ((strLine = input.readLine()) != null)
				    response.append(strLine);
			    			
			    input.close();
			}
		}
		catch (IOException e1)
		{
			 JOptionPane.showMessageDialog(null, e1.toString(),
						"Arduino Issue", JOptionPane.ERROR_MESSAGE);
		}
			    
		//return the response as a string
//		System.out.println(String.format("Commmand: %s, Arduino response: %s",
//							stringURL, response.toString()));
		
		return response.toString();
	}
	
	String getGarageDoorStatus()
	{ 
		return String.format("STATUS_GARAGE_DOOR{\"bLeftDoorOpen\":%s,\"bRightDoorOpen\":%s}", 
								leftDoorOpen, rightDoorOpen);
	}
	
	String update(String json)
	{
		Gson gson = new Gson();
		GarageDoor garageDoorCmmd = gson.fromJson(json, GarageDoor.class);
		
		doorStatusTimer.stop();
		String response = "UNCHANGED_GARAGE_DOOR";
		
		if(garageDoorCmmd.isLeftDoorOpen() && leftDoorOpen.equals("false"))
		{
			response = toggleGarageDoorUsingYun(Door.LEFT);
			ServerUI.setStoplight(0, 1);
		}
		else if(!garageDoorCmmd.isLeftDoorOpen() && leftDoorOpen.equals("true"))
		{
			response = toggleGarageDoorUsingYun(Door.LEFT);
			ServerUI.setStoplight(0, 1);
		}
		else if(garageDoorCmmd.isRightDoorOpen() && rightDoorOpen.equals("false"))
		{
			response = toggleGarageDoorUsingYun(Door.RIGHT);
			ServerUI.setStoplight(1, 1);
		}
		else if(!garageDoorCmmd.isRightDoorOpen() && rightDoorOpen.equals("true"))
		{
			response = toggleGarageDoorUsingYun(Door.RIGHT);
			ServerUI.setStoplight(1, 1);
		}
		
		doorStatusTimer.start();
		
		return response;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == doorStatusTimer)
		{
			doorStatusTimer.stop();
			
			if(YUN_CONNECTED)
			{
				getDoorStatusFromYun(Door.LEFT);
				getDoorStatusFromYun(Door.RIGHT);
			}
			else
			{
				leftDoorOpen = "false";
				rightDoorOpen = "false";
			}
			
			//set the stoplight status
			if(leftDoorOpen.equals("true"))
				ServerUI.setStoplight(0, 2);
			else
				ServerUI.setStoplight(0, 0);
			
			if(rightDoorOpen.equals("true"))
				ServerUI.setStoplight(1, 2);
			else
				ServerUI.setStoplight(1, 0);
			
//			System.out.println(String.format("STATUS_GARAGE_DOOR{\"bLeftDoorOpen\":%s,\"bRightDoorOpen\":%s}", 
//												leftDoorOpen, rightDoorOpen));
			doorStatusTimer.start();
		}
	}
	
	public enum Door
	{
		LEFT,
		RIGHT;
	}
}