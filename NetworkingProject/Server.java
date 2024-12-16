import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Server class that maintains index of information on peers and RFCs. Waits for
 * client connections and handles them with multithreading
 *
 * @author Andrew Schwartz
 *
 */
public class Server {

    /** well known port of the server */
    private static final int           PORT = 7734;

    /** list of peer information */
    private final LinkedList<PeerInfo> peers;

    private final LinkedList<RFCInfo>  rfcs;

    // ADD LOCKS IF NEEDED//

    /**
     * Constructor to iniliaze data structurs upon sever startup
     */
    public Server () {
        peers = new LinkedList<PeerInfo>();
        rfcs = new LinkedList<RFCInfo>();
    }

    /**
     * Main method and starting point of server program
     */
    public static void main ( final String[] args ) {
        // call constructor to initialize fields and initiate the server program
        final Server myServer = new Server();
        myServer.startTCP();
    }

    /**
     * Starting point for TCP socket programing and where server will listen for
     * new clients
     */
    public void startTCP () {
        // try to start the server
        try {
            // create TCP localhost server socket on port
            final InetAddress myIPaddress = InetAddress.getLocalHost();
            final ServerSocket socketServer = new ServerSocket( PORT, 500, myIPaddress );

            // show that the server has started
            System.out.println( "The server has started and is listening for clients" );
            // keep listening for new clients
            while ( true ) {
                // once the socket does starts a handshake, make a new thread
                final Socket peerConnectionSocket = socketServer.accept();
                // initialize threaad communication with the PeerThread inner
                // class
                final Thread thread = new Thread( new PeerThread( peerConnectionSocket ) );
                // being thread
                thread.start();

            }
        }
        catch ( final Exception e ) {
            System.out.println( "failed to start server" );
        }
    }

    /**
     * Where a new thread object is made and where server handles communication
     * between server and an individual client thread
     */
    private class PeerThread implements Runnable {
        /** peerConnectionSocket */
        private Socket socket;

        public PeerThread ( final Socket socket ) {
            setSocket( socket );
        }

        /**
         * @return the socket
         */
        private Socket getSocket () {
            return socket;
        }

        /**
         * @param socket
         *            the socket to set
         */
        private void setSocket ( final Socket socket ) {
            this.socket = socket;
        }

        /**
         * Execution space of a thread
         */
        @Override
        public void run () {
            // The server first needs to register the peer information, upon
            // connection, the Peer will send it's hostname and its port number

            // get the TCP buffers
            String peerHostname = "";
            int peerUploadServer = -1;
            try {
                final PrintWriter outgoing = new PrintWriter( socket.getOutputStream(), true );
                final BufferedReader incoming = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

                // get the hostname of the peer
                peerHostname = incoming.readLine();
                peerUploadServer = Integer.parseInt( incoming.readLine() );
                System.out.println(
                        peerHostname + " has joined the server and is listening on port " + peerUploadServer + "." );

                peers.add( new PeerInfo( peerHostname, peerUploadServer ) );

                // keep reading from the Peer

                // read in the first line of a request
                String request = incoming.readLine() + "\n";
                while ( request != null ) {

                    // read in the whole request
                    while ( !request.contains( "\n\n" ) ) {
                        request = request + incoming.readLine() + "\n";
                    }

                    // scanner to process the request line by line
                    final Scanner requestProcessor = new Scanner( request );
                    // value for if the request is good

                    String firstLine = "";
                    if ( requestProcessor.hasNext() ) {
                        firstLine = requestProcessor.nextLine();
                    }
                    else {
                        // SEND ERROR
                        request = null;
                        outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                        request = incoming.readLine();
                        continue;
                    }

                    // process the first line to see what the request command is
                    final Scanner firstLineScanner = new Scanner( firstLine );
                    if ( !firstLineScanner.hasNext() ) {
                        request = null;
                        outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                        request = incoming.readLine();
                        continue;
                    }
                    final String command = firstLineScanner.next();

                    if ( command.equals( "ADD" ) ) {
                        // process the rest of the first line for ADD REQUEST
                        if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "RFC" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int RFCNumber = firstLineScanner.nextInt();
                        if ( !firstLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.next().equals( "P2P-Cl/1.0" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error " + 505 + " P2P-Cl Version not Supported\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        // command should end here
                        if ( firstLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        firstLineScanner.close();
                        // if it makes it this far in the loop, then the first
                        // line is good, start processing the second line
                        String secondLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            secondLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner secondLineScanner = new Scanner( secondLine );
                        if ( !secondLineScanner.hasNext() || !secondLineScanner.next().equals( "Host:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !secondLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String hostname = secondLineScanner.next();

                        // 2nd line should end here
                        if ( secondLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        secondLineScanner.close();
                        // if it makes it this far, the second line was good,
                        // start processing the third line
                        String thirdLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            thirdLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner thirdLineScanner = new Scanner( thirdLine );
                        if ( !thirdLineScanner.hasNext() || !thirdLineScanner.next().equals( "Port:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !thirdLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int portNumber = thirdLineScanner.nextInt();
                        // third line should end here
                        if ( thirdLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        thirdLineScanner.close();
                        // if it makes it this far, the third line was good,
                        // start processing the fourth line
                        String fourthLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            fourthLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner fourthLineScanner = new Scanner( fourthLine );
                        if ( !fourthLineScanner.hasNext() || !fourthLineScanner.next().equals( "Title:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !fourthLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String title = fourthLineScanner.next();
                        // fourth line should end here
                        if ( fourthLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        fourthLineScanner.close();
                        // the command should end here
                        if ( !requestProcessor.nextLine().equals( "" ) ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        // if the request makes it this far, it was formatted
                        // correctly,
                        // now check to make sure the fields are valid\

                        // check that the peer is in the list of peers with the
                        // correct port
                        boolean hasHost = false;
                        boolean correctPort = false;
                        PeerInfo p = null;
                        for ( int i = 0; i < peers.size(); i++ ) {
                            if ( peers.get( i ).getHostname().equals( hostname ) ) {
                                hasHost = true;

                                for ( int j = 0; j < peers.size(); j++ ) {
                                    if ( peers.get( j ).getHostname().equals( hostname )
                                            && peers.get( j ).getPortNum() == portNumber ) {
                                        correctPort = true;
                                        p = peers.get( j );
                                    }
                                }
                            }
                        }

                        // fields are good, so add the RFC to the index and send
                        // a response
                        if ( hasHost && correctPort ) {

                            // dont add if already in system
                            boolean inSystem = false;
                            for ( int i = 0; i < rfcs.size(); i++ ) {
                                if ( rfcs.get( i ).getNumber() == RFCNumber
                                        && rfcs.get( i ).getPeer().getHostname().equals( hostname )
                                        && rfcs.get( i ).getPeer().getPortNum() == portNumber
                                        && rfcs.get( i ).getTitle().equals( title ) ) {
                                    inSystem = true;
                                }
                            }

                            if ( !inSystem ) {
                                rfcs.add( new RFCInfo( RFCNumber, title, p ) );
                                outgoing.println( "P2P-Cl/1.0 " + 200 + " OK" );
                                outgoing.println(
                                        "RFC " + RFCNumber + " " + title + " " + hostname + " " + portNumber );
                            }
                            else {
                                outgoing.println(
                                        "P2P-Cl/1.0 Error 400 Bad Request\nYou have already uploaded this RFC\n" );
                            }

                        }
                        else {
                            outgoing.println( "P2P-Cl/1.0 Error " + 400
                                    + " Bad Request\nEnter your correct hostname and upload server port\n" );

                        }

                    }
                    else if ( command.equals( "LOOKUP" ) ) {
                        if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "RFC" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int RFCNumber = firstLineScanner.nextInt();
                        if ( !firstLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.next().equals( "P2P-Cl/1.0" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error " + 505 + " P2P-Cl Version not Supported\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        // command should end here
                        if ( firstLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        firstLineScanner.close();
                        // if it makes it this far in the loop, then the first
                        // line is good, start processing the second line
                        String secondLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            secondLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner secondLineScanner = new Scanner( secondLine );
                        if ( !secondLineScanner.hasNext() || !secondLineScanner.next().equals( "Host:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !secondLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String hostname = secondLineScanner.next();

                        // 2nd line should end here
                        if ( secondLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        secondLineScanner.close();
                        // if it makes it this far, the second line was good,
                        // start processing the third line
                        String thirdLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            thirdLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner thirdLineScanner = new Scanner( thirdLine );
                        if ( !thirdLineScanner.hasNext() || !thirdLineScanner.next().equals( "Port:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !thirdLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int portNumber = thirdLineScanner.nextInt();
                        // third line should end here
                        if ( thirdLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        thirdLineScanner.close();
                        // if it makes it this far, the third line was good,
                        // start processing the fourth line
                        String fourthLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            fourthLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner fourthLineScanner = new Scanner( fourthLine );
                        if ( !fourthLineScanner.hasNext() || !fourthLineScanner.next().equals( "Title:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !fourthLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String title = fourthLineScanner.next();
                        // fourth line should end here
                        if ( fourthLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        fourthLineScanner.close();
                        // the command should end here
                        if ( !requestProcessor.nextLine().equals( "" ) ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        // if the request makes it this far, it was formatted
                        // correctly,
                        // now check to make sure the fields are valid\
                        boolean hasHost = false;
                        boolean correctPort = false;
                        PeerInfo p = null;
                        for ( int i = 0; i < peers.size(); i++ ) {
                            if ( peers.get( i ).getHostname().equals( hostname ) ) {
                                hasHost = true;
                                for ( int j = 0; j < peers.size(); j++ ) {
                                    if ( peers.get( j ).getHostname().equals( hostname )
                                            && peers.get( j ).getPortNum() == portNumber ) {
                                        correctPort = true;
                                        p = peers.get( j );
                                    }
                                }
                            }
                        }

                        // fields are good, so add the RFC to the index and send
                        // a response
                        if ( hasHost && correctPort ) {

                            boolean hasRFC = false;
                            // check each rfc
                            for ( int i = 0; i < rfcs.size(); i++ ) {
                                if ( ( rfcs.get( i ).getNumber() == RFCNumber )
                                        && rfcs.get( i ).getTitle().equals( title ) ) {
                                    hasRFC = true;
                                }
                            }

                            // Send the designated RFCs to the peer
                            if ( !hasRFC ) {
                                // send 404 not found
                                outgoing.println( "P2P-Cl/1.0 Error 404 Not Found\nRFC " + RFCNumber
                                        + " with and title " + title + " not found\n" );
                            }
                            else {
                                outgoing.println( "P2P-Cl/1.0 " + 200 + " OK" );
                                for ( int i = 0; i < rfcs.size(); i++ ) {
                                    if ( ( rfcs.get( i ).getNumber() == RFCNumber )
                                            && rfcs.get( i ).getTitle().equals( title ) ) {
                                        outgoing.println( "RFC " + rfcs.get( i ).getNumber() + " "
                                                + rfcs.get( i ).getTitle() + " " + rfcs.get( i ).getPeer().getHostname()
                                                + " " + rfcs.get( i ).getPeer().getPortNum() );
                                    }
                                }
                                outgoing.println();
                            }

                        }
                        else {
                            outgoing.println( "P2P-Cl/1.0 Error " + 400
                                    + " Bad Request\nEnter your correct hostname and upload server port\n" );
                        }

                    }
                    else if ( command.equals( "LIST" ) ) {
                        // process the rest of the first line for ADD REQUEST
                        if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "ALL" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.next().equals( "P2P-Cl/1.0" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error " + 505 + " P2P-Cl Version not Supported\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        // first line should be done
                        if ( firstLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        firstLineScanner.close();
                        // if it makes it this far in the loop, then the first
                        // line is good, start processing the second line
                        String secondLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            secondLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner secondLineScanner = new Scanner( secondLine );
                        if ( !secondLineScanner.hasNext() || !secondLineScanner.next().equals( "Host:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !secondLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String hostname = secondLineScanner.next();

                        // 2nd line should end here
                        if ( secondLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        secondLineScanner.close();
                        // if it makes it this far, the second line was good,
                        // start processing the third line
                        String thirdLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            thirdLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner thirdLineScanner = new Scanner( thirdLine );
                        if ( !thirdLineScanner.hasNext() || !thirdLineScanner.next().equals( "Port:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !thirdLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int portNumber = thirdLineScanner.nextInt();
                        // the command should end here
                        if ( thirdLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        thirdLineScanner.close();
                        // the command should end here
                        if ( !requestProcessor.nextLine().equals( "" ) ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        // if it makes it this far, the command was formatted
                        // correctly, check that the fields are valid

                        // check that the peer is in the list of peers with the
                        // correct port
                        boolean hasHost = false;
                        boolean correctPort = false;

                        for ( int i = 0; i < peers.size(); i++ ) {
                            if ( peers.get( i ).getHostname().equals( hostname ) ) {
                                hasHost = true;

                                for ( int j = 0; j < peers.size(); j++ ) {
                                    if ( peers.get( j ).getHostname().equals( hostname )
                                            && peers.get( j ).getPortNum() == portNumber ) {
                                        correctPort = true;
                                    }
                                }
                            }
                        }

                        // fields are good, so list the RFCs and send
                        // a response
                        if ( hasHost && correctPort ) {
                            outgoing.println( "P2P-Cl/1.0 " + 200 + " OK" );
                            for ( int i = 0; i < rfcs.size(); i++ ) {
                                outgoing.println( "RFC " + rfcs.get( i ).getNumber() + " " + rfcs.get( i ).getTitle()
                                        + " " + rfcs.get( i ).getPeer().getHostname() + " "
                                        + rfcs.get( i ).getPeer().getPortNum() );
                            }
                            outgoing.println();

                        }
                        else {
                            outgoing.println( "P2P-Cl/1.0 Error " + 400
                                    + " Bad Request\nEnter your correct hostname and upload server port\n" );

                        }

                    }
                    else if ( command.equals( "GET" ) ) {
                        // check the request and return the hosntame and port
                        // number to the peer
                        if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "RFC" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.hasNextInt() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final int RFCNumber = firstLineScanner.nextInt();
                        if ( !firstLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !firstLineScanner.next().equals( "P2P-Cl/1.0" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error " + 505 + " P2P-Cl Version not Supported\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        firstLineScanner.close();
                        // if it makes it this far in the loop, then the first
                        // line is good, start processing the second line
                        String secondLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            secondLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Forma\nt" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner secondLineScanner = new Scanner( secondLine );
                        if ( !secondLineScanner.hasNext() || !secondLineScanner.next().equals( "Host:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !secondLineScanner.hasNext() ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final String hostname = secondLineScanner.next();

                        // 2nd line should end here
                        if ( secondLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        secondLineScanner.close();

                        String thirdLine = "";
                        if ( requestProcessor.hasNextLine() ) {
                            thirdLine = requestProcessor.nextLine();
                        }
                        else {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        final Scanner thirdLineScanner = new Scanner( thirdLine );
                        if ( !thirdLineScanner.hasNext() || !thirdLineScanner.next().equals( "OS:" ) ) {
                            // SEND ERROR
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        if ( !thirdLineScanner.hasNext() ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }
                        thirdLineScanner.close();
                        // get command should end here
                        if ( !requestProcessor.nextLine().equals( "" ) ) {
                            request = null;
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                            request = incoming.readLine();

                            continue;
                        }

                        // find the rfc with the specified hostname
                        boolean hostExist = false;
                        for ( int i = 0; i < peers.size(); i++ ) {
                            if ( peers.get( i ).getHostname().equals( hostname ) ) {
                                hostExist = true;
                            }
                        }

                        boolean rfcFoundNumber = false;
                        boolean foundHost = false;
                        int foundIndex = -1;
                        for ( int i = 0; i < rfcs.size(); i++ ) {
                            if ( rfcs.get( i ).getNumber() == RFCNumber ) {
                                rfcFoundNumber = true;
                                if ( rfcs.get( i ).getPeer().getHostname().equals( hostname ) ) {
                                    foundHost = true;
                                    foundIndex = i;
                                    break;
                                }
                            }
                        }

                        if ( !rfcFoundNumber ) {
                            outgoing.println( "P2P-Cl/1.0 Error 404 Not Found\nRFC " + RFCNumber + " not found\n" );
                        }
                        else if ( !hostExist ) {
                            outgoing.println( "P2P-Cl/1.0 Error 404 Not Found\nHost " + hostname + " Not Found\n" );
                        }
                        else if ( rfcFoundNumber && !foundHost ) {
                            outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\n" + hostname + " Does Not Own RFC "
                                    + RFCNumber + "\n" );
                        }
                        else {
                            outgoing.println( rfcs.get( foundIndex ).getPeer().portNum + " " + hostname + "\n" );
                        }

                    }
                    else {
                        outgoing.println( "P2P-Cl/1.0 Error 400 Bad Request\nInvalid Request Format\n" );
                    }
                    outgoing.flush();
                    request = null;
                    request = incoming.readLine();

                }

            }
            catch ( final IOException e ) {
                System.out.println( peerHostname + " listening on port" + peerUploadServer + " has disconnected." );
                removePeer( peerHostname, peerUploadServer );

            }

            // read the Peer information frmo the incoming 2 lines
        }

    }

    /**
     * Removes the PeerInfo and any RFC with the hostname
     */
    public void removePeer ( final String hostname, final int port ) {
        // remove an RFCs the peers has uploaded to the server
        int size = rfcs.size();
        for ( int i = 0; i < size; i++ ) {
            if ( rfcs.get( i ).getPeer().getHostname().equals( hostname )
                    && rfcs.get( i ).getPeer().getPortNum() == port ) {
                rfcs.remove( rfcs.get( i ) );
                i--;
                size = rfcs.size();
            }
        }
        // remove peer from list of peer

        for ( int i = 0; i < peers.size(); i++ ) {
            if ( peers.get( i ).getHostname().equals( hostname ) && peers.get( i ).getPortNum() == port ) {
                peers.remove( i );
                i--;

            }
        }

    }

    /**
     * Private class for RFC information (same class in client)
     */
    private static class RFCInfo {
        /** RFC Number */
        private int      number;
        /** title of RFC */
        private String   title;
        /** hostname of owner of RFC */
        private PeerInfo peer;

        // missing somthing to link to RFC text file //
        public RFCInfo ( final int number, final String title, final PeerInfo peer ) {
            setNumber( number );
            setTitle( title );
            setPeer( peer );
        }

        /**
         * @return the number
         */
        private int getNumber () {
            return number;
        }

        /**
         * @param number
         *            the number to set
         */
        private void setNumber ( final int number ) {
            this.number = number;
        }

        /**
         * @return the title
         */
        private String getTitle () {
            return title;
        }

        /**
         * @param title
         *            the title to set
         */
        private void setTitle ( final String title ) {
            this.title = title;
        }

        /**
         * @return the hostname
         */
        private PeerInfo getPeer () {
            return peer;
        }

        /**
         * @param hostname
         *            the hostname to set
         */
        private void setPeer ( final PeerInfo peer ) {
            this.peer = peer;
        }

    }

    /**
     * Private class that represent PeerInformation object
     */
    private static class PeerInfo {
        /** peer hostname */
        private String hostname;
        /** port that peer is listening on */
        private int    portNum;

        /**
         * Basic Constructor
         */
        public PeerInfo ( final String hostname, final int portNum ) {
            setHostname( hostname );
            setPortNum( portNum );
        }

        /**
         * Setter for portNum
         *
         * @param portNum
         *            portNum
         */
        private void setPortNum ( final int portNum ) {
            this.portNum = portNum;

        }

        /**
         * Setter for Hostname
         *
         * @param hostname
         *            hostname
         */
        private void setHostname ( final String hostname ) {
            this.hostname = hostname;

        }

        /**
         * Getter for port number
         *
         * @return portNum
         */
        private int getPortNum () {
            return this.portNum;
        }

        /**
         * Getter for hostname
         *
         * @return hostname
         */
        private String getHostname () {
            return this.hostname;
        }
    }
}
