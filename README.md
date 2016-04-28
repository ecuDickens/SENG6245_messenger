# SENG6245_messenger

This project implements a simple server-client chat application.  The server manages the connections to all of the clients while each client provides a simple gui for viewing, inviting, and talking to other clients. 

Main Parts:
<br/>Server: 
An instance of this class listens for incoming client connection requests on a given port.  Once a request is received it will start a new ServerThread to send and receive messages from that connection and add that thread to the thread pool.  If a session is started between two clients that is tracked as well.   

ServerThread: 
A thread started by the server to send and receive messages to a particular client.  Any received message is parsed and information routed depending on the contents of the message ie. information sent back to the client of the a request forwarded to another client via the server thread pool.   

Client: 
The client contains all the methods needed to construct and display the login, main, and chat windows.  When the client first starts up it will also start a ClientThread to send and receive messages with the server.   

ClientThread: 
Like the ServerThread, this thread knows how to parse and route incoming messages, though generally this results in a display or information change in the GUI section of the client.

Message:
A message is a representation of a single request against the server or a target client.  The message type denotes its purpose, the source and target user names denotes how to route the message, and the text field contains any pertinent information.  See the MessageTypeEnum class for a description of all the message types and their use case.
  
Primary activity flow:
Once a client has successfully connected to the server it will display a login window where the user can input a user name.  If successful the client will then show the main window that shows all active user names.  From main the user can select another user name and invite them, refresh the active user name list, or logout back to the login window.  If the user invites another user, a pop up will show for that user asking if they want to accept the invite.  If accepted a chat window will open for both users.  Typing in the message box will show that the user is typing in the other chat window.  If the message box is cleared then the 'is typing' status will go away.  Closing the chat window will notify the other user of the closure.  If the chat window is opened back up in a new session the previous chat history will be saved for the duration of the client execution.

Running:
First, run the main method in the ServerStart class to get the server running and acception connections.  Then run the main method in the ClientStart class as many times as desired to spin up multiple chat applications. 

Further work:
There are a lot of improvements that I would want to implement if I revisited this project.  I'd like to get more familiar with Swing and optimize how the graphical elements are designed and flow from one state to the other (starting with having the chat application be one dynamic window). I'd like to add actual persistence to the chat history and user names by integrating with a database.  I would add persistent chat rooms in addition to the 1 on 1 sessions. Finally, I got around to testing last and did not have time to develop the mocks needed to properly unit test the socket messaging.
