// Benjamin Tang
// ECE 309 Lab #5
// September 17th,2016


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateChatRoomServer implements Runnable 
{
public static void main(String[] args) throws Exception
    {
	System.out.println("Made by: Benjamin Tang");
	
    if (args.length > 0)	
       System.out.println("Command line parameters are not accepted by ChatServer.");
    new PrivateChatRoomServer();
    }

private ServerSocket ss;
private int serverPort = 5555;

// key will be the chat name of user in the chat room.
// the associated String will be the password for that chatName.
private ConcurrentHashMap<String,String> chatNames = new ConcurrentHashMap<String,String>();
private ConcurrentHashMap<String,ObjectOutputStream> clients = new ConcurrentHashMap<String,ObjectOutputStream>();   
private ConcurrentHashMap<String, Vector<String>>  savedMessages = new ConcurrentHashMap<String, Vector<String>>(); 

//========================================================
public PrivateChatRoomServer() throws Exception // CONSTRUCTOR
   {
   ss = new ServerSocket(serverPort);
    
   try {
        FileInputStream fis = new FileInputStream("passwords.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);
        chatNames = (ConcurrentHashMap<String,String>) ois.readObject();
        ois.close();
        }
    catch(FileNotFoundException fnfe)
        {
    	System.out.println("passwords.ser is not found, so an empty collection will be used.");
        }
   System.out.println("Previously in the chat room: ");
   System.out.println(chatNames);
   System.out.println("ChatServer is up at "
           + InetAddress.getLocalHost().getHostAddress()
           + " on port " + ss.getLocalPort());

   try {
       FileInputStream fis = new FileInputStream("savedMessages.ser");
       ObjectInputStream ois = new ObjectInputStream(fis);
       savedMessages = (ConcurrentHashMap<String,Vector<String>>) ois.readObject();
       ois.close();
       }
   catch(FileNotFoundException fnfe)
       {
   	System.out.println("savedMessages.ser is not found, so an empty collection will be used.");
       }
   System.out.println(savedMessages);
   
   new Thread(this).start(); // create 1st client Thread 
   }                         // to execute run() method.

//=========================================================
/** Each client goes through 4 stages:
 * 1. connection   processing 
 * 2. join         processing 
 * 3. send/receive processing
 * 4. leave        processing
 * (See these sections in the run() method below.)
 */
public void run()//each client application thread enters here!
    {
    String        chatName = null;//These are local variables.
    Socket               s = null;//Each client thread will 
    ObjectInputStream  ois = null;//have it's own copy of 
    ObjectOutputStream oos = null;//each variable on their
    String enteredPassword = null;//own thread's stack.
    try {
	    s = ss.accept();          // Wait for client connect.
        new Thread(this).start(); // Make thread for next client.
                                  // Next client thread enters run()
                                  // and waits in ss.accept().
        // 1. CONNECTION PROCESSING
	    ois = new ObjectInputStream (s.getInputStream());
	    Object firstMessage = ois.readObject(); 
	    oos = new ObjectOutputStream(s.getOutputStream());
        System.out.println("First message received: " + firstMessage);
         if (firstMessage instanceof String)
           {	
          String joinMessage = ((String) firstMessage).trim();
           int spaceOffset = joinMessage.indexOf(" ");
           if (spaceOffset < 0)
              {
              oos.writeObject("Invalid format. " // 1) send err msg
                            + "Are you calling the right address and port?");
              oos.close();                         // 2) hang up
              System.out.println("Invalid 1st message received (no space separator): " + joinMessage);
              return;                              // 3) kill this client thread.
              }
           chatName = joinMessage.substring(0,spaceOffset).toUpperCase();
           enteredPassword = joinMessage.substring(spaceOffset).trim();
           if (enteredPassword.contains(" "))
              {
              oos.writeObject("Invalid format. " // 1) send err msg
                            + "Are you calling the right address and port?");
              oos.close();                         // 2) hang up
              System.out.println("Invalid 1st message received (space in name or pw): " + joinMessage);
              return;                              // 3) kill this client thread.
              }
           }
         else // this caller is not a chat client! 
           {
           oos.writeObject("Invalid protocol. " // 1) send err msg
                         + "Are you calling the right address and port?");
           oos.close();                         // 2) hang up
           System.out.println("Non-String 1st message received: " + firstMessage);
           return;                              // 3) kill this client thread.
           }
    if (chatNames.containsKey(chatName)) // this name already in?
           {
    	   String storedPassword = chatNames.get(chatName);
    	   if (!enteredPassword.equals(storedPassword))
    	      {
              oos.writeObject("Your entered password " + enteredPassword + " is not the same as the password stored for chat name " + chatName);
              oos.close(); // hang up.
              System.out.println("Invalid password: " + enteredPassword + " instead of " + storedPassword);
              return;      // and kill this client thread
              }
           }
        else // this chatName has never joined
           { // so join them!
           chatNames.put(chatName, enteredPassword);
           saveChatNames();
           }
    
        // 2. "JOIN processing" for this client
        oos.writeObject("Welcome to the chat room " + chatName + "!"); // confirm to client that they are in!
        // note that if write above fails, put & send below doesn't happen...
        // Is this a RECONNECT? (a re-join from a new location of 
        // someone that was ALREADY IN the char room?
        if (!clients.containsKey(chatName)) // not already in
           {
           sendToAllClients("Welcome to " + chatName + " who has just joined the chat room!");
           clients.put(chatName,oos); // add new-join client to collection
           System.out.println(chatName + " is joining");
           }
         else // THIS CLIENT IS ALREADY IN THE CHAT ROOM!
              // e.g. This person left work without leaving the chat
        	  // room and is now signing on from home. We know it is 
        	  // them because they passed the password test above.
        	  // So we are going to SHUT DOWN their old session, and
        	  // then REPLACE their old oos in the clients collection
        	  // with their new oos.
           {
           ObjectOutputStream oldOOS = clients.get(chatName);
           oldOOS.writeObject("Session shut down due to rejoin from new location.");;
           oldOOS.close(); // also closes the Socket
           clients.replace(chatName, oos);
           // And DON'T send the "Welcome" message to everyone for 
           // this client because they never left the chat room...
           }
        }
    catch (Exception e)
        {                         
        System.out.println("Connection failure during join processing: " + e);
        if (s.isConnected())
           {
           try {s.close();}         // hang up
           catch(IOException ioe){} // already hung up!
           }
        return; // kill this client's thread
        }
    // Show who's in the chat room
    sendWho();
    
    // Show any messages that has been saved up
    Vector<String> savedMessageList = savedMessages.get(chatName);
    if (savedMessageList != null) // any messages?
    {
    while (!savedMessageList.isEmpty())
        {
        String savedMessage = savedMessageList.remove(0);
        try{oos.writeObject(savedMessage);}
        catch(Exception e){}
        }
    saveSavedMessages(); 
    }
         
   // 3. "SEND/RECEIVE processing" for this client.
   try { 
       while (true) // loop forever
           {  
           Object something = ois.readObject(); // wait for this client to say something
           if (something instanceof String)
              {	
              String chatMessage = ((String) something).trim();
              System.out.println("Received '" + chatMessage + "' from " + chatName);
              sendToAllClients(chatName + " says: " + chatMessage);
              }
           else if (something instanceof String[]) // client sent an array of Strings!
           {	
           String[] array = (String[]) something; // cast to an array pointer.
           System.out.println("Received array from " + chatName);
           for (String line : array)              // print contents
                System.out.println(line);
           if (clients.containsKey(array[1]))
        	   sendPrivateMessage(chatName, array);
           else saveMessage(chatName, array);
           }
            else
              {	 
              System.out.println("Received from " + chatName + ": " + something );
              sendToAllClients(something);
              }
           }        	   
       }
   
    // 4. "LEAVE processing" for this client.
    // The user closes the client window to leave the chat
    // room, terminating the client program and taking down
    // the connection. So the server will go to the catch
    // below from ois.readObject() above (which is then failing).
    catch (Exception e) 
       {
       // Hello! Somehow, when the oos fails, we have to determine
       // whether Bubba has LEFT the chat room (in which case we want
       // to remove his oos) or has just REJOINED (in which case we DON'T
       // want to remove his now-new oos from the collection).
       ObjectOutputStream currentOOS = clients.get(chatName);
       if (currentOOS == oos) // same oos as when they joined
          {
    	  System.out.println(chatName + " is leaving.");
          clients.remove(chatName); // This is the ONLY place the client gets removed!
          sendToAllClients("Goodbye to " + chatName
    		             + " who has just left the chat room!");
          }
       else // The retrieved oos is NOT the same, so DON'T DO ANYTHING.
    	    // This thread will die but Bubba's new client thread is active.
          { 
    	  System.out.println(chatName + " is rejoining.");
          }
       //Show who's in the chat room
       sendWho();
       }
    } // end of run(). client thread returns to the Thread
      // object and is terminated! (It's finished running.)

//=========================================================
private synchronized void sendToAllClients(Object whatever) 
   {
   System.out.println("Sending '" + whatever + "' to everyone.");	
   // synchronization ensures that all clients will get all
   // messages in the same sequence. (Another client thread
   // cannot enter even if the thread currently in the 
   // method is suspended by the O/S!)
   ObjectOutputStream[] oosList = clients.values().toArray(new ObjectOutputStream[0]);
   
   for (ObjectOutputStream clientOOS : oosList)
       {
       try {clientOOS.writeObject(whatever);}
       catch (IOException e) {} 
       }
	   // Keep looping if a send to one of the clients fails!
       // No action need be taken here if the communications
       // writeObject() fails because we can count on that
       // client's thread, parked on a ois.readObject(),
       // also seeing the failure and removing the failed
       // oos from the collection! 
   }
//=========================================================
private synchronized void saveChatNames() 
  { 
  try {
      FileOutputStream fos = new FileOutputStream("passwords.ser");
	  ObjectOutputStream oos = new ObjectOutputStream(fos);
	  oos.writeObject(chatNames);
	  oos.close();
	  }
  catch(Exception e)
	  {
	  System.out.println("passwords.ser cannot be saved: " + e);
	  }
  }

void sendWho()
   {
	// Making copies of the whosIn keys and password keys as sets
	Set whosInSet    = clients.keySet();
	Set passwordsSet = chatNames.keySet();
	
	// Sorting the sets
	TreeSet whosInSortedSet    = new TreeSet(whosInSet);
	TreeSet passwordsSortedSet = new TreeSet(passwordsSet);
	
	// Turns the passwaordsSortedSet into the "difference" collection
	passwordsSortedSet.removeAll(whosInSortedSet);
	
	// Turning the TreeSets into arrays
	String[] whosNotInArray = (String[]) passwordsSortedSet.toArray(new String[0]);
	String[] whosInArray    = (String[]) whosInSortedSet.toArray(new String[0]);
	
	//Sending the arrays to the clients
	sendToAllClients(whosNotInArray);
	sendToAllClients(whosInArray);
	
	//Used as a debug trace
	System.out.println("Currently in the chat room: " + whosNotInArray);
	System.out.println("Currently NOT in the chat room: " + whosInArray);
   }

private void sendPrivateMessage(String sender, String[] whoTo)
{
	  String sendMessage = whoTo[0] + " SENT ONLY to "; 
	  for (int i = 1; i < whoTo.length; i++)
	      {
	      sendMessage = sendMessage + whoTo[i] + " ";
	      }	
	  
	  ObjectOutputStream senderOOS = clients.get(sender);
	  try {senderOOS.writeObject("Your PRIVATE message: " + sendMessage);}
	  catch (Exception e) {}
	  
	  for (int i = 1; i < whoTo.length; i++)
      {
      ObjectOutputStream recipientOOS = clients.get(whoTo[i]);
      try {recipientOOS.writeObject(sender + " says: " + sendMessage);}
      catch(Exception e){}
      }
}

private void saveMessage(String sender, String[] whoFor)
{
	for (int i = 1; i < whoFor.length; i++)
	  {
	  Vector messageList = savedMessages.get(whoFor[i]);
	  if (messageList == null) // this client has no saved messages
	      {                    // so create a message Vector and add it.
	      messageList = new Vector();
	      savedMessages.put(whoFor[i], messageList);
	      }
	  messageList.add(sender + " said on " + new Date() + " " + whoFor[0]);
	  }
	saveSavedMessages(); 
	
	 // Send a confirmation message to the saver.
	 String confirmationMessage = whoFor[0] + " was SAVED for "; 
	 for (int i = 1; i < whoFor.length; i++)
	     {
	     confirmationMessage = confirmationMessage + whoFor[i] + " ";
	     }
	 ObjectOutputStream saverOOS = clients.get(sender);
	 try {saverOOS.writeObject(confirmationMessage);}
	 catch(Exception e) {}
}

private synchronized void saveSavedMessages() 
{
	  try {
	      FileOutputStream fos = new FileOutputStream("savedMessages.ser");
		  ObjectOutputStream oos = new ObjectOutputStream(fos);
		  oos.writeObject(savedMessages);
		  oos.close();
		  }
	  catch(Exception e)
		  {
		  System.out.println("savedMessages.ser cannot be saved: " + e);
		  }
	  System.out.println(savedMessages);
}
}

