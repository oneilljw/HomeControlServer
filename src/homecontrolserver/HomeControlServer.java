package homecontrolserver;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class HomeControlServer
{
	private static final String APPNAME = "Home Control Server";
	private static final String HCC_SERVER_VERSION = "Home Control Server Version 1.0\n";
	private static final String HCC_COPYRIGHT = "\u00A92015 John W. O'Neill";
	private ServerUI serverUI;	//User IF
	private ServerLoop serverIF; 	//Server loop
	private ClientManager clientMgr; //Manages all connected clients
	
	private boolean bServerRunning;
//	private Timer dbSaveTimer;
//	private List<ONCServerDB> dbList;
	
	//GUI Objects
	private JFrame hcsFrame;
	
	//Check if we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
    private static boolean MAC_OS_X = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));

	public HomeControlServer() throws IOException
	{
		//If running under MAC OSX, use the system menu bar and set the application title appropriately and
    	//set up our application to respond to the Mac OS X application menu
        if (MAC_OS_X) 
        {          	
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APPNAME);
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();}
			catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			catch (UnsupportedLookAndFeelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); }
			
            // Generic registration with the Mac OS X application, attempts to register with the Apple EAWT
            // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs
            try
            {
                // Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
                // use as delegates for various com.apple.eawt.ApplicationListener methods
                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
                OSXAdapter.setAboutHandler(this,getClass().getDeclaredMethod("about", (Class[])null));
 //             OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
 //             OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadImageFile", new Class[] { String.class }));
            } 
            catch (Exception e)
            {
                System.err.println("Error while loading the OSXAdapter:");
                e.printStackTrace();
            }
        }
        
        //create mainframe window for the application and add button listeners to start/stop the sever
        createandshowGUI();
        serverUI.btnStartServer.addActionListener(new UIButtonListener());
        serverUI.btnStopServer.addActionListener(new UIButtonListener());
        
		//Set up client manager
		clientMgr = ClientManager.getInstance();
		
		//set up database
		GarageDoorDB.getInstance();
	
		//Create the client listener socket and start the loop		
		startServer();	//Start the server on app start up
    }
	
	 // General quit handler; fed to the OSXAdapter as the method to call when a system quit event occurs
    // A quit event is triggered by Cmd-Q, selecting Quit from the application or Dock menu, or logging out
    public boolean quit()
    {
    	if(bServerRunning)	//Did user forget to stop the server prior to quitting? 
    		serverIF.terminateServer();
    	return true;
    }
    
    // General info dialog; fed to the OSXAdapter as the method to call when 
    // "About OSXAdapter" is selected from the application menu   
    public void about()
    {
    	JOptionPane.showMessageDialog(hcsFrame, HCC_SERVER_VERSION + HCC_COPYRIGHT, "About the Home Control Server",
    			JOptionPane.INFORMATION_MESSAGE, clientMgr.getAppIcon());
    }
    
    private void createandshowGUI()
	{
    	serverUI = ServerUI.getInstance();
    	
    	hcsFrame =  new JFrame(APPNAME);
    	hcsFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we)
			 {
				if(bServerRunning)	//Did user forget to stop the server prior to quitting? 
		    		serverIF.terminateServer();

				System.exit(0);	  
			 }});
        hcsFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);	//On close, user is prompted to confirm
        hcsFrame.setMinimumSize(new Dimension(200, 200));
        hcsFrame.setLocationByPlatform(true);
        
        hcsFrame.setContentPane(serverUI); 
        hcsFrame.setSize(500, 450);
        hcsFrame.setVisible(true);
	}
    
    void startServer()
    {
    	//Create and start the server loop
    	serverIF = new ServerLoop(clientMgr);
    	
    	serverIF.start();
		serverUI.setStoplight(0);	//Set server status to green - running
		
		serverUI.addLogMessage("Server Interface Loop started");
		
		serverUI.btnStartServer.setVisible(false);
		serverUI.btnStopServer.setVisible(true);
		
		bServerRunning = true;
    }
    
    void stopServer()
    {
		serverIF.stopServer();
		try 
		{
			serverIF.join();
			bServerRunning = serverIF.terminateServer();
		} 
		catch (InterruptedException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		serverUI.setStoplight(2);	//Set server status to green - running
		
		serverUI.addLogMessage("Server Interface Loop stopped");
		
		serverUI.btnStartServer.setVisible(true);
		serverUI.btnStopServer.setVisible(false);
    }
    
    private class UIButtonListener implements ActionListener
    {
    	public void actionPerformed(ActionEvent e)
    	{
    		if(e.getSource() == serverUI.btnStartServer)
    			startServer();
    		else if(e.getSource() == serverUI.btnStopServer)
    			stopServer();
    	}	
    }
	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable() {
			 public void run() {
						try {
							new HomeControlServer();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
		}});
	}
}
