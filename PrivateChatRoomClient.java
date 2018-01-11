// Benjamin Tang
// ECE 309 Lab #5
// September 17th,2016


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;


public class PrivateChatRoomClient
{
public static void main(String[] args)
	{
    System.out.println("Made by: Benjamin Tang");
    
	if (args.length != 3) 
	   {
	   System.out.println("Restart. Provide your chat name (no blanks) as the first parameter.");
	   System.out.println("Provide your password (no blanks) as the second command line parameter.");
	   System.out.println("Provide the chat server network address as the third parameter."); 
	   return;
	   }
	String chatName = args[0];
	String password = args[1];
	String serverAddress = args[2];
	if (chatName.contains(" ") || password.contains(" ") || serverAddress.contains(" "))
	   {
	   System.out.println("ChatName or password or server address cannot contain blanks.");	
       return;
	   }
	System.out.println("Connecting " + chatName + " to the chat server at " + serverAddress);
    String newLine = System.lineSeparator();
	int serverPort = 5555;
    
	// ************************
	// Connect to Chat Server *
	// ************************
	Socket s = null;
	try {
	    s = new Socket(serverAddress, serverPort);
		System.out.println("Connected to chat server!");
        }
	catch (Exception e)
        {
		String errMsg = "The Chat Room Server is not responding."
			+ newLine + "Either the entered network address is incorrect,"
			+ newLine + "or the server computer is not up,"
    		+ newLine + "or the ChatServer program is not started,"
            + newLine + "or the ChatServer program is not at port 3333.";
        return;
        } 
	
	// **********************
	// "JOIN" the chat room *
	// **********************
	ObjectOutputStream oos = null;
	ObjectInputStream  ois = null;
	String     serverReply = null;
	try {
	    oos = new ObjectOutputStream(s.getOutputStream());
	    oos.writeObject(chatName + " " + password); // 1st message
        ois = new ObjectInputStream (s.getInputStream());
        System.out.println("Waiting for server reply to join request.");
	    serverReply = (String) ois.readObject();
	    // Show the serverReply to the user.
	    System.out.println(serverReply);
	    }
    catch(Exception e) // problem sending to/receiving from the server
        {
	    String errMsg = "The remote computer has rejected our join protocol."
	        + newLine + "So the remote application we have connected to is likely not the Chat Room Server!"
	        + newLine + "Correct the network address and restart ChatClient.";
        return;
        }
	
	// serverReply confirms accept or reject of chat name + password.
	// Note that serverReply is already printed on the console, whether
	// it is the welcome message or a password error message.
    if (!serverReply.startsWith("Welcome")) return; 

	// Load the ChatClientGUI 
    PrivateChatRoomClientGUI ccg = new PrivateChatRoomClientGUI(chatName, oos);

	// RECEIVE CHAT LOOP
	try { //A receive error exits the loop and terminates the client.	
	    while(true)// *Capture* our main thread in a loop 
	    	 {     
		     Object somethingFromServer = ois.readObject();
		     if (somethingFromServer instanceof String) // this is a chat message
		        {
		    	 String message = (String) somethingFromServer;
		    	 ccg.show(message);
		        }
		else if (somethingFromServer instanceof String[]) // this is an array-of-Strings
		        {
				String[] array = (String[]) somethingFromServer;
				ccg.showWho(array, chatName);
		        }
		else System.out.println("Unexpected object received from server: " + somethingFromServer);
		     }
	    }
	catch(Exception e)// we're done if connection fails!
	     {             //(so catch is OUTSIDE the loop)
	     String errMsg = "ERROR: Connection to the ChatRoomServer has failed.";
		 ccg.show(errMsg);
		 System.out.println(errMsg);
		 System.out.println("Must restart ChatRoomClient to reestablish connection.");
	     }
	}
}