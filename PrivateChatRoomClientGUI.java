// Benjamin Tang
// ECE 309 Lab #5
// September 17th,2016

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class PrivateChatRoomClientGUI implements ActionListener
{
// GUI OBJECTS  
private JFrame      chatWindow      	= new JFrame("Chat Room Client");
private JFrame		whosINwindow		= new JFrame("Who's in");
private JFrame		whosOUTwindow		= new JFrame("Who's NOT in");

private JPanel      topPanel        	= new JPanel();
private JPanel      middlePanel     	= new JPanel();
private JPanel		bottomPanel			= new JPanel();

private JLabel      nameLabel       	= new JLabel();

private JButton     sendToAllButton     = new JButton("Send To All");
private JButton		whosINbutton		= new JButton("See who's in the chat room!");
private JButton		whosOUTbutton		= new JButton("See who's NOT in the chat room!");
private JButton		clearSendSelections = new JButton("Clear Selections");
private JButton		clearSaveSelections  = new JButton("Clear Selections");
private JButton		sendPrivateButton	= new JButton("Send To Selected");
private JButton		savePrivateButton	= new JButton("Save For Selected");

private JTextArea   inTextArea     		= new JTextArea();
private JTextArea   outTextArea     	= new JTextArea();

private JScrollPane inScrollPane   		= new JScrollPane(inTextArea);
private JScrollPane outScrollPane   	= new JScrollPane(outTextArea);

private JList<String> whosINlist 		= new JList<String>();
private JList<String> whosOUTlist 		= new JList<String>();

private JScrollPane whosINScrollPane    = new JScrollPane(whosINlist);
private JScrollPane whosOUTScrollPane   = new JScrollPane(whosOUTlist);

private String      newLine         	= System.lineSeparator(); 
private String		chatName;

private ObjectOutputStream oos;
//================================================================================
//"constructor" method. The object loader ("new") calls here.
public PrivateChatRoomClientGUI(String chatName, ObjectOutputStream oos)
    {
	this.chatName = chatName;
	this.oos = oos; // Save pointer to oos for the show() method.
	// Set the "Java" 'look and feel' (called "Metal")
    try {UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");}
    catch(Exception e) {System.out.println(e);} // don't terminate if this fails.

    // Appearance of the GUI signals the user that they are in!
	// ***************  The GUI Object (the window, the button, the text fields)
	// Build the GUI *  were created above where they were declared. So here
	// ***************  we are placing them on the screen, but not making them.
	topPanel.setLayout(new GridLayout(1,2));
    topPanel.add(sendToAllButton);
	topPanel.add(nameLabel);
	chatWindow.getContentPane().add(topPanel,  "North");
	
	middlePanel.setLayout(new GridLayout(1,2));
	middlePanel.add(inScrollPane);
	middlePanel.add(outScrollPane);
	chatWindow.getContentPane().add(middlePanel, "Center"); 
	
	bottomPanel.setLayout(new GridLayout(1,2));
	bottomPanel.add(whosINbutton);
	bottomPanel.add(whosOUTbutton);
	chatWindow.getContentPane().add(bottomPanel, "South");
	
    //set attributes on GUI objects
	chatWindow.setTitle("Close the window to leave the chat room.");
	chatWindow.setSize(1600,1000);
	chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	whosINwindow.setTitle("Who's in");
	whosINwindow.setSize(800,500);
	whosINwindow.setLocation(1600,0);
	whosINwindow.add(whosINScrollPane);
	whosINwindow.add(clearSendSelections, "North");
	whosINwindow.add(sendPrivateButton, "South");
	
	whosOUTwindow.setTitle("Who's NOT in");
	whosOUTwindow.setSize(800,500);
	whosOUTwindow.setLocation(1600,800);
	whosOUTwindow.add(whosOUTScrollPane);
	whosOUTwindow.add(clearSaveSelections, "North");
	whosOUTwindow.add(savePrivateButton, "South");
	
	outTextArea.setEditable(false); // keep cursor out
	inTextArea.setFont (new Font("default",Font.BOLD,20));
	outTextArea.setFont(new Font("default",Font.BOLD,20));
    inTextArea.setLineWrap(true);
    outTextArea.setLineWrap(true);
	inTextArea.setWrapStyleWord(true);
	outTextArea.setWrapStyleWord(true);
	sendToAllButton.setBackground(Color.red);
	sendToAllButton.setForeground(Color.white);
	whosINbutton.setBackground(Color.blue);
	whosINbutton.setForeground(Color.white);
	whosOUTbutton.setBackground(Color.green);
	whosOUTbutton.setForeground(Color.white);
	sendPrivateButton.setBackground(Color.green);
	sendPrivateButton.setForeground(Color.black);
	savePrivateButton.setBackground(Color.blue);
	savePrivateButton.setForeground(Color.white);
	clearSaveSelections.setBackground(Color.white);
	clearSaveSelections.setForeground(Color.black);
	clearSendSelections.setBackground(Color.white);
	clearSendSelections.setForeground(Color.black);
	nameLabel.setText(" " + chatName + "'s PRIVATE CHAT ROOM");
	nameLabel.setFont(new Font("default", Font.BOLD, 20));
  
	// Sign up for event notification from "active" GUI objects
	sendToAllButton.addActionListener(this);      
	whosINbutton.addActionListener(this);
	whosOUTbutton.addActionListener(this);
	sendPrivateButton.addActionListener(this);
	savePrivateButton.addActionListener(this);
	clearSaveSelections.addActionListener(this);
	clearSendSelections.addActionListener(this);
	
	// show window
	chatWindow.setVisible(true); 
    }

//===================================================================================
// The sendToAllButton waits for the user to enter chat, then calls here.)

public void actionPerformed(ActionEvent ae)
    {
	nameLabel.setText(" " + chatName + "'s CHAT ROOM");
	nameLabel.setFont(new Font("default", Font.BOLD, 20));
	nameLabel.setForeground(Color.black);
	
	if (ae.getSource() == sendToAllButton)
	   {
	   System.out.println("sendToAllButton was pushed.");
	   // put all code that is currently in the actionPerformed() method here.
		String chat = inTextArea.getText().trim();//remove leading/trailing blanks
		if (chat.length() == 0) 
			{
			System.out.println("No message was entered.");
			nameLabel.setText("No message was entered.");
			nameLabel.setForeground(Color.red);
			return; // return early if nothing to send
			}
		System.out.println("Sending: " + chat); // for debug 
		inTextArea.setText(""); // write blank to clear input area.

		// send chat to server
	    try {
	    	if (!whosINlist.isSelectionEmpty())
	    	   {
	    	   System.out.println("SendToAll button was pushed, but private recipients "
	    	                    + "are selected.");
	    	   nameLabel.setText("SendToAll button was pushed, but private recipients "
	    	                    + "are selected.");
	    	   nameLabel.setForeground(Color.red);
	    	   return; // without sending
	    	   }
	    	
	    	else if (!whosOUTlist.isSelectionEmpty())
	    	   {
	    		System.out.println("SendToAll button was pushed, but save recipients "
	    		                    + "are selected.");
	    		nameLabel.setText("SendToAll button was pushed, but save recipients "
	                    + "are selected.");
	    		nameLabel.setForeground(Color.red);
	    		return; // without sending
	    	   }
	    	
	    	else oos.writeObject(chat);
	    	} 
	    catch(IOException ioe)
	         {
			 String errMsg = "ERROR: Connection to the ChatRoomServer has failed.";
			 outTextArea.append(newLine + errMsg);
			 outTextArea.setCaretPosition(outTextArea.getDocument().getLength());
			 inTextArea.setEditable(false);// keep cursor out
			 System.out.println(errMsg);
			 System.out.println("Must restart ChatRoomClient to reestablish connection.");
	         } 
	   } 
	if (ae.getSource() == whosINbutton)
	   {
	   System.out.println("whosINbutton was pushed.");
	   whosINwindow.setVisible(true); 
	   } 
	if (ae.getSource() == whosOUTbutton)
	   {
	   System.out.println("whosOUTbutton was pushed.");
	   whosOUTwindow.setVisible(true); 
	   } 
	if (ae.getSource() == sendPrivateButton)
	   {
		 System.out.println("sendPrivateButton was pushed.");
		   java.util.List sendPrivateList = whosINlist.getSelectedValuesList();
		   if (sendPrivateList.isEmpty())
		   {
		   System.out.println("No recipients are selected!");
		   nameLabel.setText("No recipients are selected!");
		   nameLabel.setForeground(Color.red);
		   }
		   System.out.println("Selected private message recipients are: " + sendPrivateList);
		   String message = inTextArea.getText().trim();
		   if(message.isEmpty()) 
			   {
			   System.out.println("No message was entered!");
			   nameLabel.setText("No message was entered!");
			   nameLabel.setForeground(Color.red);
			   }
		   else {
			   System.out.println("Private message sent is: " + message);
		   sendPrivateList.add(0,message);
		   String[] sendPrivateArray = (String[]) sendPrivateList.toArray(new String[0]);
		   try {
			oos.writeObject(sendPrivateArray);
			inTextArea.setText("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	   } 
	if (ae.getSource() == savePrivateButton)
	   {
		   System.out.println("savePrivateButton was pushed.");
		   java.util.List savePrivateList = whosOUTlist.getSelectedValuesList();
		   if (savePrivateList.isEmpty())
		   {
		   System.out.println("No recipients are selected!");
		   nameLabel.setText("No recipients are selected!");
		   nameLabel.setForeground(Color.red);
		   }
		   System.out.println("Saved private message recipients are: " + savePrivateList);
		   String message = inTextArea.getText().trim();
		   if(message.isEmpty()) 
		   {
		   System.out.println("No message was entered!");
		   nameLabel.setText("No message was entered!");
		   nameLabel.setForeground(Color.red);
		   }
		   else {
			   System.out.println("Private message saved is: " + message);
		   savePrivateList.add(0,message);
		   String[] savePrivateArray = (String[]) savePrivateList.toArray(new String[0]);
		   try {
				oos.writeObject(savePrivateArray);
				inTextArea.setText("");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		   }
	   } 
	if (ae.getSource() == clearSendSelections)
	   {
	   whosINlist.clearSelection();
	   System.out.println("clearSendSelections was pushed.");
	   } 
	if (ae.getSource() == clearSaveSelections)
	   {
	   whosOUTlist.clearSelection();
	   System.out.println("clearSaveSelections was pushed.");
	   } 
    } // GUI thread returns to sendToAllButton

//===================================================================================
//the main thread branches in here
//THIS IS THE "RECEIVE" METHOD (WRITES to the outTextArea)

public void show(String messageFromServer)
    {
    outTextArea.append(newLine + messageFromServer);
    // scroll to bottom (believe it or not!)
    outTextArea.setCaretPosition(outTextArea.getDocument().getLength()); 
    }

public void showWho(String[] chatNames, String chatName)
{
	java.util.List<String> whoList = Arrays.asList(chatNames);
	if (whoList.contains(chatName.toUpperCase()))
    {
		System.out.println("Currently in the chat room:");
		whosINlist.setListData(chatNames);
		System.out.println(whoList);
    }
	else
	{
		System.out.println("Currently NOT in the chat room:");
		whosOUTlist.setListData(chatNames);
		System.out.println(whoList); // a List can print itself!
	}
}
}// end of program (class)