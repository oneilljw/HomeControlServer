package homecontrolserver;

import homecontrolclient.GarageDoor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.Timer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class GarageDoorDB implements ActionListener
{
	private static final int POLLING_RATE = 1000 * 3;
	
	private static GarageDoorDB instance = null;
	String leftDoorOpen, rightDoorOpen;
	private Timer timer;
	
	private GarageDoorDB()
	{
		//initialize door status
//		leftDoorOpen = getDoorStatusFromYun(Door.LEFT);
//		rightDoorOpen = getDoorStatusFromYun(Door.LEFT);
		
		//Create the polling timer
    	timer = new Timer(POLLING_RATE, this);
    	timer.start();
	}
	
	public static GarageDoorDB getInstance()
	{
		if(instance == null)
			instance = new GarageDoorDB();

		return instance;
	}
	
	String getDoorStatusFromYun(Door door)
	{
		String stringURL = null;
		if(door == Door.LEFT)
			stringURL = String.format("http://arduino.local/arduino/readL");
		else
			stringURL = String.format("http://arduino.local/arduino/readR");
		
		String response = sendCommandToYun(stringURL);
		
		if(response != null && response.contains("set to 1")  && door == Door.LEFT)
			return "true";
		else if (response != null && response.contains("set to 0"))
			return "false";
		else
			return "false";
	}
	
	String toggleGarageDoor(Door door)
	{
		//form the String url request
		String stringURL;
		if(door == Door.LEFT)
			stringURL = "http://arduino.local/arduino/toggleL";	//
		else
			stringURL = "http://arduino.local/arduino/toggleR";
		
		String response = sendCommandToYun(stringURL);
		
		//if door was open, set status to closed. If door was closed, set status to open
		if(response != null && door == Door.LEFT && response.equals("") && leftDoorOpen.equals("false"))
			leftDoorOpen = "true";
		else if(response != null && door == Door.LEFT && response.equals("") && leftDoorOpen.equals("false"))
			leftDoorOpen = "false";
		else if(response != null && door == Door.RIGHT && response.equals("")  && rightDoorOpen.equals("false"))
			rightDoorOpen = "true";
		else if(response != null && door == Door.RIGHT && response.equals("") && rightDoorOpen.equals("true"))
			rightDoorOpen = "false";
		
		if(response != null)
			return String.format("UPDATED_GARAGE_DOOR{\"bLeftDoorOpen\":%s,\"bRightDoorOpen\":%s}", 
					leftDoorOpen, rightDoorOpen);
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
//		System.out.println("Arduino response: " + response.toString());
		return response.toString();
	}
	
	String getGarageDoorStatus()
	{ 
		return String.format("UPDATED_GARAGE_DOOR{\"bLeftDoorOpen\":%s,\"bRightDoorOpen\":%s}", 
								leftDoorOpen, rightDoorOpen);
	}
	
	String update(String json)
	{
		Gson gson = new Gson();
		GarageDoor garageDoor = gson.fromJson(json, GarageDoor.class);
		
		timer.stop();
		String response = null;
		
		if(garageDoor.isLeftDoorOpen() && leftDoorOpen.equals("false"))
			response = toggleGarageDoor(Door.LEFT);
		else if(!garageDoor.isLeftDoorOpen() && leftDoorOpen.equals("true"))
			response = toggleGarageDoor(Door.LEFT);
		else if(garageDoor.isRightDoorOpen() && rightDoorOpen.equals("false"))
			response = toggleGarageDoor(Door.RIGHT);
		else if(!garageDoor.isRightDoorOpen() && rightDoorOpen.equals("true"))
			response = toggleGarageDoor(Door.RIGHT);
		
		
		timer.start();
		
		return null; 
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == timer)
		{
			timer.stop();
			String newLeftDoorOpen = getDoorStatusFromYun(Door.LEFT);
			String newRightDoorOpen = getDoorStatusFromYun(Door.RIGHT);
			
			if(!leftDoorOpen.equals(newLeftDoorOpen) || !rightDoorOpen.equals(newRightDoorOpen))
			{
				//a change has been detected, pass it to connected clients
				
			}
			System.out.println(String.format("Left Door Open: %s",  leftDoorOpen));
			timer.start();
		}
	}
	
	public enum Door
	{
		LEFT,
		RIGHT;
	}
}
