package homecontrolserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.Timer;

public class ClientManager implements ActionListener
{
	private static final boolean CLIENT_TIMER_ENABLED = true;
	private static final int CLIENT_HEARTBEAT_SAMPLE_RATE = 1000 * 60 * 1; //one minute
	private static final int CLIENT_INACTIVE_LIMIT = 1000 * 60 * 3; //three minutes
	private static final int CLIENT_TERMINAL_LIMIT = 1000 * 60 * 10; //ten minutes
	
	//client timeouts used for test only
//	private static final int CLIENT_HEARTBEAT_SAMPLE_RATE = 1000 * 10; //ten seconds
//	private static final int CLIENT_INACTIVE_LIMIT = 1000 * 30; //thirty seconds
//	private static final int CLIENT_TERMINAL_LIMIT = 1000 * 60 * 1; //one minutes
	
	private static ClientManager instance = null;
	
	private ArrayList<Client> clientAL;	//list of clients connected to server
	private int clientID;	
	private ServerUI serverUI;
	
	private Timer clientTimer;
	
	private ClientManager()
	{
		clientAL = new ArrayList<Client>();
		clientID = 0;
		
		serverUI = ServerUI.getInstance();	//reference for client manager to communicate with UI
		serverUI.btnKillClient.addActionListener(this);
		
		//Create the client timer and enable it if CLIENT_TIMER_ENABLED
		clientTimer = new Timer(CLIENT_HEARTBEAT_SAMPLE_RATE, new ClientTimerListener());
				if(CLIENT_TIMER_ENABLED)
					clientTimer.start();
	}
	
	public static ClientManager getInstance()
	{
		if(instance == null)
			instance = new ClientManager();
		
		return instance;
	}
	
	void clientDied(Client c)
	{
		c.closeClientSocket();
		clientAL.remove(c);
		serverUI.displayClientTable(clientAL);
	}
	
	void clientQuit(Client c)
	{
		serverUI.addLogMessage(String.format("Client %d quit", c.getClientID()));
		c.closeClientSocket();
		clientAL.remove(c);
		serverUI.displayClientTable(clientAL);
	}
	
	void killClient(Client c)
	{
		serverUI.addLogMessage(String.format("Client %d killed", c.getClientID()));
		c.closeClientSocket();
		clientAL.remove(c);
		serverUI.displayClientTable(clientAL);
	}
	
	void clientLoggedOut(Client c)
	{
		serverUI.addLogMessage(String.format("Client %d, %s logged out", c.getClientID(),
												c.getClientName()));
		c.closeClientSocket();
		clientAL.remove(c);
		serverUI.displayClientTable(clientAL);
	}
	
	synchronized Client addClient(Socket socket) throws IOException
	{
		Client c = new Client(socket, clientID);
		clientAL.add(c);
		serverUI.displayClientTable(clientAL);
		c.start();
		serverUI.addLogMessage(String.format("Client %d connected", clientID));
		clientID++;
		
		return c; 
	}
	
	/*************************************************************************************
	 * Find a client by client id. If client id is not logged into the server, return null,
	 * otherwise return a reference to the client.
	 * **********************************************************************************/
	Client findClient(long clientID)
	{
		//Search for client
		int index = 0;
		while(index < clientAL.size() && clientAL.get(index).getClientID() != clientID)
			index++;
			
		if(index < clientAL.size())
			return clientAL.get(index);
		else
			return null;	//client not found
	}
	
	void clientLoginAttempt(boolean bValid, String mssg)
	{
		serverUI.addLogMessage(mssg);
		
		if(bValid)	//redraw table, we now know who the client is
			serverUI.displayClientTable(clientAL);
	}
	
	void clientStateChanged()
	{
		serverUI.displayClientTable(clientAL);
	}
	
	void addLogMessage(String mssg)
	{
		serverUI.addLogMessage(mssg);
	}
	
	void dataChanged(Client requestingClient, String change)
	{
		//Need to add change to all client changes lists so they can poll for the change
		for(Client c:clientAL )
		{
			//Add change to the change queue's of every other client
			if(c != requestingClient)	
				c.addChange(change);		
		}
	}
	
	ImageIcon getAppIcon() { return serverUI.getIcon(0); }
	
	/***************************************************************************************
	 * This method checks all active clients to assess their heart beat according to the 
	 * heart beat state diagram. The method creates a list of all client heart beats that
	 * have gone terminal and are still lost, in order to kill the client and remove them 
	 * from the client manager queue 
	 */
	void checkClientHeartbeat()
	{
		//For each client that is supposed to have a heart beat (State is Logged_In or DB_Selected)
		//check that they still have a heart beat. If they don't have a heart beat, create a list
		//for notification and kill. For clients that are running but haven't logged in, if they
		//exceed the log in time, kill them
		ArrayList<Client> killClientList = new ArrayList<Client>();
		
//		if(clientAL.size() > 0)	//add a hb check mssg to log if there any clients
//			addLogMessage("Server Checking Client heart beats");
		
		for(Client c: clientAL)	
		{
			ClientState clientState = c.getClientState();
			long timeSinceLastHeartbeat = System.currentTimeMillis() - c.getTimeLastActiveInMillis();
			
			if(clientState == ClientState.Running  && timeSinceLastHeartbeat > CLIENT_INACTIVE_LIMIT)
			{
				killClientList.add(c);
			}
			else if(clientState == ClientState.Logged_In || clientState == ClientState.DB_Selected)
			{
//				System.out.println(String.format("Checking if Client %d still has a heart beat", c.getClientID()));
				if(c.getClientHeartbeat() == Heartbeat.Terminal && timeSinceLastHeartbeat  > CLIENT_TERMINAL_LIMIT)
				{
					//Heart beat is terminal  and remained lost past the terminal time limit
					//kill the client by closing the socket which causes an IO exception which
					//will terminate the client thread
					killClientList.add(c);
				}
				else if(c.getClientHeartbeat() == Heartbeat.Lost && timeSinceLastHeartbeat  > CLIENT_TERMINAL_LIMIT)
				{
					//Heart beat was lost and remained lost past the terminal time limit
					c.setClientHeartbeat(Heartbeat.Terminal);
					serverUI.displayClientTable(clientAL);
				
					String mssg = String.format("Client %d heart beat terminal, not detected in %d seconds",
													c.getClientID(), timeSinceLastHeartbeat/1000);
				
					addLogMessage(mssg);
				}
				else if(c.getClientHeartbeat() == Heartbeat.Active && timeSinceLastHeartbeat > CLIENT_INACTIVE_LIMIT)
				{
					//Heart beat was not detected
					c.setClientHeartbeat(Heartbeat.Lost);
					serverUI.displayClientTable(clientAL);
				
					String mssg = String.format("Client %d heart beat lost, not detected in %d seconds",
													c.getClientID(), timeSinceLastHeartbeat/1000);
				
					addLogMessage(mssg);		
				}
				else if((c.getClientHeartbeat() == Heartbeat.Lost || c.getClientHeartbeat() == Heartbeat.Terminal) && 
							timeSinceLastHeartbeat < CLIENT_INACTIVE_LIMIT)
				{
					//Heart beat was lost and is still lost or went terminal and re-recovered prior to
					//killing the client
					c.setClientHeartbeat(Heartbeat.Active);
					serverUI.displayClientTable(clientAL);
				
					String mssg = String.format("Client %d heart beat recovered, detected in %d seconds",
													c.getClientID(), timeSinceLastHeartbeat/1000);
				
					addLogMessage(mssg);		
				}
			}
		}
			
		//if there are any clients to kill, kill them here
		for(Client kc : killClientList)
		{
			kc.setClientState(ClientState.Ended);
			kc.closeClientSocket();
			clientDied(kc);
			
			String mssg = String.format("Client %d heart beat remained terminal, client killed", kc.getClientID());					
			addLogMessage(mssg);
		}
	}	
	
	private class ClientTimerListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0) 
		{
			checkClientHeartbeat();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if(e.getSource() == serverUI.btnKillClient)
		{
			Client c = serverUI.getClientTableSelection();
			if(c != null)
			{
				killClient(c);
			}
		}	
	}
}
