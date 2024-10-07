# Networking - Final Project

## PROJECT SUMMARY
This Java project simulates simulates client-to-server and client-to-client TCP communication by sending request messages as a terminal application. Clients can send their RFC text files that are stroed locally to be stored within the server, and they can send and receive RFC's with other clients connected to the server. See the commands section for different functionality within the project. 

## JAVA FILES
Server.java: Creates the server with a list of Peers and RFCs. Creates a thread for each connection with a peer, and handles the logic for incoming requests. <br />
Client.java: User interface for clients to interact with the server and other peers. Creates a thread to create the upload server for P2P communication.

 ## TEST FILES: 
Peer1.txt <---| <br />
Peer2.txt <---| - Test .txt files that should be in separate directories with Peer.java <br />
Peer3.txt <---| 

## !!!!!!!!!!!!VERT IMPORTANT!!!!!!!!!!!!!!! ##

Within the Client.java program. You must change the SERVER_HOSTNAME field to the IP address of the device running the server.

public class Peer {

    /** IP address of server. CHANGE AS NEEDED */
    private static final String SERVER_HOSTNAME   = "192.168.1.51";         <------------ UPDATE THIS!!!!!!!!!!!!!!!!!!!

## COMPILING
Within the command line of the directory containing the Server and Peer files:

> javac Server.java <br />
> javac Peer.java <br />
* The server must be running for the peer program to run, otherwise it will exit with an error message*


## USAGE

> java Server <br />
> java Peer (args)

Arguments when executing the Peer program are the names of .txt file you would like to initialize as RFC's and send to the server. <br />
Any .txt files not initialized on startup can only be added to the server by obtaining them through another peer. <br />
Upon startup, the server will begin listening, and peers will be made known of their hostname and port number. 

## COMMANDS

errors in the following commands will result in an error message. <br />

ADD: Will add an RFC in local storage to server. If a peer sends an RFC they have already sent to the server, the RFC in the server will be updated <br /> <br />
<pre>
ADD RFC rfcnum P2P-Cl/1.0      <------where rfcnum is the RFC number
Host: hostname                 <------where hostname is the peer's hostname 
Port: portnumber               <------where protnumber is the peer's upload server port number 
Title: title                   <------where title is the title of the RFC(the .txt file) 
(blank line)	               <------hitting enter twice to end the requests 
</pre>
LIST: lists all of the RFCs in the server with corresponding host's and upload ports <br /><br />

<pre>
LIST ALL P2P-Cl/1.0
Host: hostname                 <------where hostname is the peer's hostname
Port: portnumber               <------where protnumber is the peer's upload server port number
(blank line)                   <------hitting enter twice to end the requests
</pre>

LOOKUP: finds all RFCs of the given rfcnum<br /><br />

<pre>
LOOKUP RFC rfcnum P2P-Cl/1.0   <------where rfcnum is the RFC number
Host: hostname                 <------where hostname is the peer's hostname
Port: portnumber               <------where protnumber is the peer's upload server port number
Title: title                   <------where title is the title of the RFC(the .txt file)
(blank line)	               <------hitting enter twice to end the requests
</pre>

GET: connects to a peer upload server to receive an RFC. When the peer gets the RFC, the .txt file<br />
is added to their directory and local storage and can be sent to the server whenever. If peer already had the RFC before making the request,<br />
it is updated within their directory and local storage.<br /><br />

<pre>
GET RFC rfcnum P2P-Cl/1.0      <------where rfcnum is the RFC number
Host: hostname                 <------DIFFERENT THAN OTHER COMMANDS. hostname is the hostname of the peer you are sending a request to.
OS: os                         <------where os is your operating system (does not matter what you put)
(blank line)	               <------hitting enter twice to end the requests
</pre>
