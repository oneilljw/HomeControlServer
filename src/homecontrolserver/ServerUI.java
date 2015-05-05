package homecontrolserver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ServerUI extends JPanel implements ListSelectionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final int NUM_ROWS_TO_DISPLAY = 8;
	private static final int LOG_TEXT_FONT_SIZE = 13;
	
	private static ServerUI instance = null;	//Only one UI
	private transient ImageIcon imageIcons[];
	public JButton btnStartServer, btnStopServer, btnKillClient;
	private JTextArea logTA;
//	private StyledDocument logDoc;	//document that holds log text
	private JLabel lblNumClients;
	private JRadioButton rbStoplight;
//	private ONCTable clientTable;
	private JTable clientTable;
	private DefaultTableModel clientTableModel;
	private boolean bClientTableChanging;
	
	private ArrayList<Client> clientTableList;
	
//	private static String[] columnToolTips = {"ID", "First Name", "Last Name", 
//		  										"Permission", "Client Status", "Heart Beat",
//		  										"Database Year Client is Connected To",
//		  										"Time Logged In" };

	private static String[] columns = {"ID", "First Name", "Last Name", 
		  								"Perm", "State", "HB", "Year", "Ver", "Time Stamp" };

	private static int[] colWidths = {40, 80, 80, 80, 80, 28, 40, 52, 140};

	private static int [] center_cols = {0, 3, 5};
	
	public static ServerUI getInstance()
	{
		if(instance == null)
			instance = new ServerUI();
		
		return instance;
	}
	
	private ServerUI()
	{
		//Layout User I/F
		initIcons();

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		JPanel statusPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel statusPanelCenter = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel statusPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		JLabel lblONCicon = new JLabel(imageIcons[0]);
		statusPanelLeft.add(lblONCicon);
		
		lblNumClients = new JLabel("Clients Connected: 0");
		statusPanelCenter.add(lblNumClients);
	    
		rbStoplight = new JRadioButton(imageIcons[4]);
    	rbStoplight.setToolTipText("");
    	statusPanelRight.add(rbStoplight);
    	
    	statusPanel.add(statusPanelLeft);
    	statusPanel.add(statusPanelCenter);
    	statusPanel.add(statusPanelRight);
    	
    	//Set up the client table panel
    	JPanel tablepanel = new JPanel();
    	tablepanel.setLayout(new BorderLayout());
    	
    	bClientTableChanging = false;
//    	clientTable = new ONCTable(columnToolTips, new Color(240,248,255));
    	clientTable = new JTable();

    	//Set up the table model. Cells are not editable
    	clientTableModel = new DefaultTableModel(columns, 0) {
    		private static final long serialVersionUID = 1L;
    				
    		@Override
    		//All cells are locked from being changed by user
    		public boolean isCellEditable(int row, int column) {return false;}
    	};
    	
    	clientTableList = new ArrayList<Client>();	//List holds references of clients show in table

    	//Set the table model, select ability to select multiple rows and add a listener to 
    	//check if the user has selected a row. 
    	clientTable.setModel(clientTableModel);
    	clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//    	clientTable.getSelectionModel().addListSelectionListener(this);
//    	clientTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//    		@Override
//    		public void valueChanged(ListSelectionEvent arg0) {
//    			checkApplyChangesEnabled();	//Check to see if user postured to change delivery driver.	
//    		}
//    	});

    	//Set table column widths
    	int tablewidth = 0;
    	for(int i=0; i < colWidths.length; i++)
    	{
    		clientTable.getColumnModel().getColumn(i).setPreferredWidth(colWidths[i]);
    		tablewidth += colWidths[i];
    	}
   		tablewidth += 24; 	//Account for vertical scroll bar

    	//Set up the table header
    	JTableHeader anHeader = clientTable.getTableHeader();
    	anHeader.setForeground( Color.black);
    	anHeader.setBackground( new Color(161,202,241));

    	//mouse listener for table header click causes table to be sorted based on column selected
    	//uses family data base sort method to sort. Method requires ONCFamily array list to be sorted
    	//and column name
    	//anHeader.addMouseListener(new MouseAdapter() {
    	//	@Override
    	//    public void mouseClicked(MouseEvent e) {
    	//		//TODO: add table sorting code here
    	//   }
    	//});

    	//Center cell entries for specified cells
    	DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer();    
    	dtcr.setHorizontalAlignment(SwingConstants.CENTER);
    	for(int i=0; i<center_cols.length; i++)
    		clientTable.getColumnModel().getColumn(center_cols[i]).setCellRenderer(dtcr);

//    	clientTable.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));

    	clientTable.setFillsViewportHeight(true);
    	tablepanel.add(anHeader, BorderLayout.NORTH);
    	tablepanel.add(clientTable, BorderLayout.CENTER);
    	tablepanel.setPreferredSize(new Dimension(tablewidth, clientTable.getRowHeight()*NUM_ROWS_TO_DISPLAY));
/*    	
    	//Create the scroll pane and add the table to it.
    	JScrollPane clientScrollPane = new JScrollPane(clientTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
    												JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    	clientScrollPane.setPreferredSize(new Dimension(tablewidth, clientTable.getRowHeight()*NUM_ROWS_TO_DISPLAY));
*/   			
    	//Set up the client log pane
    	logTA = new JTextArea();
    	logTA.setToolTipText("Server log");
  	   	logTA.setEditable(false);
  	   	
  	   	//create the document that holds chat text
//      logDoc = logTP.getStyledDocument();
        
        //create the paragraph attributes
        SimpleAttributeSet paragraphAttribs = new SimpleAttributeSet();  
        StyleConstants.setAlignment(paragraphAttribs , StyleConstants.ALIGN_LEFT);
        StyleConstants.setFontSize(paragraphAttribs, LOG_TEXT_FONT_SIZE);
        StyleConstants.setSpaceBelow(paragraphAttribs, 3);
        
        //set the paragraph attributes, disable editing and set the pane size
//      logTA.setParagraphAttributes(paragraphAttribs, true);
  	   	
        DefaultCaret caret = (DefaultCaret) logTA.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
  	   	
	    //Create the ODB Wish List scroll pane and add the Wish List text pane to it.
        JScrollPane logScrollPane = new JScrollPane(logTA);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Server Log"));
        logScrollPane.setPreferredSize(new Dimension(tablewidth, 180));
        
        //Set up the control panel
        JPanel cntlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        btnKillClient = new JButton("Kill Client");
        btnKillClient.setVisible(false);
        btnKillClient.setEnabled(false);
      
        btnStartServer = new JButton("Start Server");
        btnStartServer.setVisible(false);
        
        btnStopServer = new JButton("Stop Server");
//      btnStopServer.setVisible(false);
        
        cntlPanel.add(btnKillClient);
        cntlPanel.add(btnStopServer);
        cntlPanel.add(btnStartServer);
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
        this.add(statusPanel);
        this.add(tablepanel);
//      this.add(clientScrollPane);
        this.add(logScrollPane);
        this.add(cntlPanel);
        
        this.setMinimumSize(new Dimension(tablewidth, 400));
	}
	
	/** Initialize icons */
	void initIcons()
	{
		imageIcons = new ImageIcon[5];
		
		//ONC Server logo
		imageIcons[0] = createImageIcon("HOUSE.png", "Logo");
		
		//Icons used for stop light status
		imageIcons[1] = createImageIcon("traffic-lights-green-icon.gif", "Green Light Icon");
		imageIcons[2] = createImageIcon("traffic-lights-yellow-icon.gif", "Yellow Light Icon");
		imageIcons[3] = createImageIcon("traffic-lights-red-icon.gif", "Red Light Icon");
		imageIcons[4] = createImageIcon("traffic-lights-off-icon.gif", "Off Light Icon");
		
		//Splash screen
//		imageIcons[15] = createImageIcon("oncsplash.gif", "ONC Full Screen Logo");
		
	}
	
	void displayClientTable(ArrayList<Client> cAL)
	{
		bClientTableChanging = true;
		
		clientTableList.clear();
		
		while (clientTableModel.getRowCount() > 0)	//Clear the current table
			clientTableModel.removeRow(0);
		
		for(Client ci:cAL)	//Build the new table
		{
			clientTableList.add(ci);
			clientTableModel.addRow(ci.getClientTableRow());
		}
		
		bClientTableChanging = false;
		
		btnKillClient.setVisible(cAL.size() > 0);
		btnKillClient.setEnabled(false);
		
		lblNumClients.setText("Clients Connected: " + Integer.toString(cAL.size()));
	}
	
	//returns a reference to the client that is selected in the client table
	Client getClientTableSelection()
	{
		if(clientTable.getSelectedRow() != -1)	//make sure row is selected
			return clientTableList.get(clientTable.getSelectedRow());
			
		else
			return null;
	}
	
	void addLogMessage(String mssg)
	{
		Calendar timestamp = Calendar.getInstance();
		
		String line = new SimpleDateFormat("MM/dd/yy H:mm:ss").format(timestamp.getTime());
		
		logTA.append(line + ": " + mssg + "\n");
		logTA.setCaretPosition(logTA.getDocument().getLength());
	}
	
	void setStoplight(int pos)	//0-green, 1-yellow, 2-red, 3-off
	{
		if(pos >= 0 && pos < 4)
			rbStoplight.setIcon(imageIcons[pos+1]);
	}
	
	ImageIcon getIcon(int index) { return imageIcons[index]; }
	
	 /** Returns an ImageIcon, or null if the path was invalid. */
	ImageIcon createImageIcon(String path, String description)
	{
		java.net.URL imgURL = getClass().getResource(path);
		if (imgURL != null) { return new ImageIcon(imgURL, description); } 
		else { System.err.println("Couldn't find file: " + path); return null; }
	}

	@Override
	public void valueChanged(ListSelectionEvent lse)
	{
		if(!lse.getValueIsAdjusting() &&lse.getSource() == clientTable.getSelectionModel() &&
				!bClientTableChanging)
		{
			btnKillClient.setEnabled(clientTable.getSelectedRowCount() > 0);
		}
	}
}
