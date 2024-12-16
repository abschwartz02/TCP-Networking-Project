import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * Class for functionality on the peer side. Makes requests to the server and
 * other peers. uploads its information to the server, etc.
 *
 * @author Andrew Schwartz
 *
 */
public class Peer {

    /** IP address of server. CHANGE AS NEEDED */
    private static final String SERVER_HOSTNAME = "192.168.1.51";
    /** server well-known port number */
    private static final int    SERVER_PORT     = 7734;
    /** hostname of the client */
    private String              hostname;
    /** port number for upload sever that peer is listening to */
    private int                 uploadPort;
    /** list of clients personal RFCs */
    private LinkedList<RFC>     rfcs;

    /**
     * Constuctor for peer object
     */
    public Peer ( final String hostname, final int uploadPort, final LinkedList<RFC> rfcs ) {
        setHostname( hostname );
        setUploadPort( uploadPort );
        setRfcs( rfcs );
    }

    /**
     * Main method and start of the client program
     *
     * @param args
     *            hostname followed by txt files to be added as RFCs
     * @throws UnknownHostException
     * @throws ClassNotFoundException
     */
    public static void main ( final String[] args ) throws UnknownHostException {

        // before the peer does anything, it needs to establish its own
        // information

        // the first step is to read the args to register the peers initial RFCs
        final LinkedList<RFC> myRFCs = new LinkedList<RFC>();
        // used for generating a random RFC number
        final Random rand = new Random();
        // check that the hostname is valid
        final InetAddress myIPaddress = InetAddress.getLocalHost();
        final String myHostname = myIPaddress.getHostAddress().toString();

        System.out.println();
        for ( int i = 0; i < args.length; i++ ) {
            try {
                // create a file object from its title
                // SOMTHING WEIRD WITHIN ECLIPSE, REMOVE src\\ WHEN DONE TESTING
                final File f = new File( args[i] );
                // check that the file existed
                if ( f.exists() && f.isFile() ) {
                    // create an RFC from this file

                    // creating a random RFC number
                    final int RFCNum = 100000 + rand.nextInt( 900000 );
                    // make the new RFC object
                    final RFC newRFC = new RFC( RFCNum, f.getName(), myHostname, f );
                    // add it to list
                    myRFCs.addFirst( newRFC );

                }
                else {
                    System.out.println( "Invalid file name" );
                    System.exit( 0 );
                }

            }
            catch ( final Exception e ) {
                System.out.println( "Invalid file" );
                System.exit( 0 );
            }

        }

        // once the RFC have been read in, generate a random unused port number
        int myPort = -1;

        // loop until a usable port is found within the dynamic range
        while ( true ) {

            myPort = 49152 + rand.nextInt( 65535 - 49152 + 1 );
            try ( ServerSocket serverSocket = new ServerSocket( myPort ) ) {
                serverSocket.close();
                break;
            }
            catch ( final IOException e ) {
                // do nothing
            }
        }

        // All fields to initialize Peer object have been acquired
        final Peer myPeer = new Peer( myHostname, myPort, myRFCs );
        // start TCP communication

        myPeer.startTCP();

    }

    /**
     * Where TCP begins for Peer to listen for other peers and where it connects
     * to the server and makes server requests
     *
     * @throws ClassNotFoundException
     */
    public void startTCP () {

        // begin the thread

        // connect to the server using the well known port and hostname
        try {

            final Socket serverConnection = new Socket( SERVER_HOSTNAME, SERVER_PORT );
            // Make a new thread for the client to begin listening for other
            // clients

            System.out.println( "Server connection successful" );
            for ( int i = 0; i < rfcs.size(); i++ ) {
                System.out.println( "File " + rfcs.get( i ).getTitle() + " has been initialized with RFC Number "
                        + rfcs.get( i ).getNumber() );
            }
            System.out.println();
            final Thread uploadServer = new Thread( new UploadServerThread( getUploadPort(), getHostname() ) );
            uploadServer.start();
            // get the TCP buffers to send and recieve data to and from server
            final PrintWriter outgoing = new PrintWriter( serverConnection.getOutputStream(), true );
            final BufferedReader incoming = new BufferedReader(
                    new InputStreamReader( serverConnection.getInputStream() ) );

            // send to the reciever the Peer hostname and upload port
            outgoing.println( hostname );
            outgoing.println( uploadPort );

            // FOR TESTING, USED TO AUTO ADD ON STARTUP, 
            /**
            for ( int i = 0; i < rfcs.size(); i++ ) {
                final String message = "ADD RFC " + rfcs.get( i ).getNumber() + " P2P-Cl/1.0\n" + "Host: "
                        + getHostname() + "\n" + "Port: " + getUploadPort() + "\n" + "Title: "
                        + rfcs.get( i ).getTitle() + "\n" + "\n";
                System.out.print( message );
                // send to server
                outgoing.println( message );
                // wait and print a response from the server
                final String responseCode = incoming.readLine();
                System.out.println( responseCode );
                if ( !responseCode.contains( "Error" ) ) {
                    final String echo = incoming.readLine();
                    System.out.println( echo + "\n" );
                }

            }
            */
            // let Peer make new requests messages
            final Scanner scnr = new Scanner( System.in );
            String message = "";
            // read until Peer closes
            while ( message != null ) {
                message = scnr.nextLine();
                message = message + "\n";
                // keep reading request until a double line brake is detected
                while ( !message.contains( "\n\n" ) ) {
                    message = message + scnr.nextLine() + "\n";
                }

                // once the double line break has been detected,
                // the first word needs to be scanned to determine if the
                // message gets sent to the server or another client
                if ( message.equals( "\n\n" ) ) {
                    System.out.println( "Error " + 400 + " Bad Request\n" );
                    message = "";
                    continue;
                }
                final Scanner firstWordScanner = new Scanner( message );

                final String method = firstWordScanner.next();

                if ( method.equals( "GET" ) ) {
                    // START OVER -- samantha.
                    // send to server
                    outgoing.println( message );
                    // wait and print a response from the server
                    String responseCode = incoming.readLine();
                    final Scanner infoCatcher = new Scanner( responseCode );
                    int clientUploadPort = -1;
                    String clientHostname = "";

                    boolean goodResponse = true;
                    while ( !responseCode.equals( "" ) ) {
                        if ( !responseCode.contains( "Error" ) && goodResponse ) {
                            clientUploadPort = infoCatcher.nextInt();
                            clientHostname = infoCatcher.next();
                            responseCode = incoming.readLine();
                        }
                        else {
                            goodResponse = false;
                            System.out.println( responseCode );
                            if ( !responseCode.contains( "Error" ) ) {
                                System.out.println();
                            }
                            responseCode = incoming.readLine();
                        }

                    }
                    if ( goodResponse ) {
                        if ( clientHostname.equals( getHostname() ) && clientUploadPort == uploadPort ) {
                            System.out.println( "Error 400 Bad request\nYou cannot use GET command on yourself\n" );
                        }
                        else {
                            // open a connection with the client
                            final Socket clientConnection = new Socket( clientHostname, clientUploadPort );
                            final PrintWriter p2pOutgoing = new PrintWriter( clientConnection.getOutputStream(), true );
                            final BufferedReader p2pIncoming = new BufferedReader(
                                    new InputStreamReader( clientConnection.getInputStream() ) );

                            // send the original request to the client
                            p2pOutgoing.println( message );

                            String p2pResponse = p2pIncoming.readLine();

                            // catch the rfc Number and title
                            String title = null;
                            int RFCNumber = -1;
                            boolean error = true;
                            if ( !p2pResponse.contains( "Error" ) ) {
                                final Scanner rfcInfo = new Scanner( p2pResponse );
                                RFCNumber = rfcInfo.nextInt();
                                title = rfcInfo.next();
                                error = false;
                                p2pResponse = p2pIncoming.readLine();
                                rfcInfo.close();
                            }

                            // catch and print the remain string info
                            while ( !p2pResponse.equals( "" ) ) {
                                System.out.println( p2pResponse );
                                p2pResponse = p2pIncoming.readLine();
                            }
                            System.out.println();
                            if ( !error ) {

                                String fileLine = p2pIncoming.readLine() + "\n";
                                String fileContent = "";

                                while ( !fileLine.contains( "EOF" ) ) {
                                    fileContent += fileLine;
                                    System.out.print( fileLine );
                                    fileLine = p2pIncoming.readLine() + "\n";
                                }

                                final File newFile = new File( title );
                                final BufferedWriter fileWriter = new BufferedWriter(
                                        new FileWriter( newFile.getAbsolutePath() ) );

                                fileWriter.write( fileContent );
                                fileWriter.flush();
                                fileWriter.close();

                                System.out.println( "\nFile " + title + " is in your directory" );
                                // check if the RFC number is already in storage

                                boolean noUpdate = true;
                                for ( int i = 0; i < rfcs.size(); i++ ) {
                                    if ( rfcs.get( i ).getNumber() == RFCNumber ) {
                                        rfcs.get( i ).setFile( newFile );
                                        rfcs.get( i ).setTitle( title );
                                        noUpdate = false;
                                    }
                                }

                                if ( noUpdate ) {
                                    rfcs.add( new RFC( RFCNumber, title, hostname, newFile ) );
                                }

                                System.out.println( "RFC " + RFCNumber
                                        + " is in your local storage and can added to the server\n" );
                            }
                        }
                    }

                }
                else if ( method.equals( "ADD" ) ) {
                    // Perform client side check to ensure rfc exists with the
                    // correct number
                    final Scanner requestProcessor = new Scanner( message );

                    final String firstLine = requestProcessor.nextLine();
                    final Scanner firstLineScanner = new Scanner( firstLine );
                    firstLineScanner.next();

                    if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "RFC" ) ) {
                        System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );

                    }
                    else if ( !firstLineScanner.hasNextInt() ) {
                        System.out.println( "Error 404 Bad Request\n" );
                    }
                    else {
                        final int RFCNumber = firstLineScanner.nextInt();
                        if ( !firstLineScanner.hasNext() || !firstLineScanner.next().equals( "P2P-Cl/1.0" ) ) {
                            // SEND ERROR
                            System.out.println( "Error " + 505 + " P2P-Cl Version not Supported\n" );
                        }
                        // command should end here
                        else if ( firstLineScanner.hasNext() ) {
                            System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                        }
                        else {
                            firstLineScanner.close();
                            String secondLine = "";
                            if ( requestProcessor.hasNextLine() ) {
                                secondLine = requestProcessor.nextLine();
                                final Scanner secondLineScanner = new Scanner( secondLine );
                                if ( !secondLineScanner.hasNext() || !secondLineScanner.next().equals( "Host:" ) ) {
                                    // SEND ERROR
                                    System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                }
                                else if ( !secondLineScanner.hasNext() ) {
                                    // SEND ERROR
                                    System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                }
                                else {
                                    final String addHostname = secondLineScanner.next();
                                    if ( secondLineScanner.hasNext() ) {
                                        System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                    }
                                    else {
                                        secondLineScanner.close();
                                        String thirdLine = "";
                                        if ( requestProcessor.hasNextLine() ) {
                                            thirdLine = requestProcessor.nextLine();
                                            final Scanner thirdLineScanner = new Scanner( thirdLine );
                                            if ( !thirdLineScanner.hasNext()
                                                    || !thirdLineScanner.next().equals( "Port:" ) ) {
                                                // SEND ERROR
                                                System.out.println(
                                                        "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                            }
                                            else if ( !thirdLineScanner.hasNextInt() ) {
                                                System.out.println(
                                                        "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                            }
                                            else {
                                                final int portNumber = thirdLineScanner.nextInt();
                                                if ( thirdLineScanner.hasNext() ) {
                                                    System.out.println(
                                                            "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                                }
                                                else {
                                                    thirdLineScanner.close();
                                                    String fourthLine = "";
                                                    if ( requestProcessor.hasNextLine() ) {
                                                        fourthLine = requestProcessor.nextLine();
                                                        final Scanner fourthLineScanner = new Scanner( fourthLine );
                                                        if ( !fourthLineScanner.hasNext()
                                                                || !fourthLineScanner.next().equals( "Title:" ) ) {
                                                            // SEND ERROR
                                                            System.out.println(
                                                                    "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                                        }
                                                        else if ( !fourthLineScanner.hasNext() ) {
                                                            // SEND ERROR
                                                            System.out.println(
                                                                    "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                                        }
                                                        else {
                                                            final String title = fourthLineScanner.next();
                                                            if ( fourthLineScanner.hasNext() ) {
                                                                outgoing.println( "Error " + 400 + " Bad Request\n" );
                                                            }
                                                            else {
                                                                fourthLineScanner.close();
                                                                // check that
                                                                // the hostname
                                                                // is valid
                                                                if ( !addHostname.equals( hostname ) ) {
                                                                    System.out.println( "Error 400 Bad Request\n"
                                                                            + addHostname + " Is Not Your addresss\n" );
                                                                }
                                                                else if ( portNumber != uploadPort ) {
                                                                    System.out.println(
                                                                            "Error 400 Bad Request\n" + portNumber
                                                                                    + " Is Not Your Port Number\n" );
                                                                }
                                                                else {
                                                                    boolean validNumber = false;
                                                                    boolean validTitle = false;
                                                                    for ( int i = 0; i < rfcs.size(); i++ ) {
                                                                        if ( rfcs.get( i ).getNumber() == RFCNumber ) {
                                                                            validNumber = true;
                                                                            if ( rfcs.get( i ).getTitle()
                                                                                    .equals( title ) ) {
                                                                                validTitle = true;
                                                                            }
                                                                        }
                                                                    }
                                                                    if ( !validNumber ) {
                                                                        System.out.println( "Error 404 RFC " + RFCNumber
                                                                                + " Not Found\n" );
                                                                    }
                                                                    else if ( !validTitle ) {
                                                                        System.out.println(
                                                                                "Error 400 Bad Request\nIncorrect Filename For RFC "
                                                                                        + RFCNumber + "\n" );
                                                                    }
                                                                    else {
                                                                        outgoing.println( message );
                                                                        final String addResponseCode = incoming
                                                                                .readLine();
                                                                        System.out.println( addResponseCode );
                                                                        if ( !addResponseCode.contains( "Error" ) ) {
                                                                            final String echo = incoming.readLine();
                                                                            System.out.println( echo + "\n" );

                                                                        }
                                                                        else {
                                                                            System.out.println();
                                                                        }
                                                                        message = "";
                                                                        continue;

                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    else {
                                                        // SEND ERROR
                                                        System.out.println(
                                                                "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                                    }
                                                }
                                            }
                                        }
                                        else {
                                            System.out
                                                    .println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                                        }
                                    }
                                }
                            }
                            else {
                                // SEND ERROR
                                System.out.println( "P2P-Cl/1.0 400 Bad Request\nInvalid Request Format\n" );
                            }

                        }

                    }
                }
                else {
                    // send to server
                    outgoing.println( message );
                    // wait and print a response from the server
                    String responseCode = incoming.readLine();
                    while ( !responseCode.equals( "" ) ) {
                        System.out.println( responseCode );
                        responseCode = incoming.readLine();
                    }
                    System.out.println();
                }

                // process request space,
                // outgoing.println( request );
                message = "";
            }

        }

        catch ( final Exception e ) {
            System.out.println( "You are disconnected from the server " );
            System.exit( 0 );
        }
    }

    /**
     * inner class where the Peer will listen for incoming p2p connections
     */
    private class UploadServerThread implements Runnable {
        /* uplaod port */
        private int port;

        /* hostname */
        String      hostname;

        public UploadServerThread ( final int port, final String hostname ) {
            setPort( port );
            setHostname( hostname );
        }

        private void setHostname ( final String hostname ) {
            this.hostname = hostname;
        }

        /**
         * @return the port
         */
        private int getPort () {
            return port;
        }

        /**
         * @param port
         *            the port to set
         */
        private void setPort ( final int port ) {
            this.port = port;
        }

        /**
         * Where the logic of the p2p upload server is, like a mini server
         * class. Each peer can only talk with one peer at a time
         */
        @SuppressWarnings ( "deprecation" )
        @Override
        public void run () {
            try {
                // create TCP host server socket on port

                final InetAddress myIPaddress = InetAddress.getLocalHost();
                final ServerSocket peerUploadServer = new ServerSocket( getPort() );

                // show that the server has started
                System.out.println( "Host " + hostname + " is listening for other peers on port " + getPort() + "\n" );
                // keep listening for new clients
                while ( true ) {
                    // wait for a handshake from another peer
                    final Socket peerConnectionSocket = peerUploadServer.accept();
                    final PrintWriter outgoing = new PrintWriter( peerConnectionSocket.getOutputStream(), true );
                    final BufferedReader incoming = new BufferedReader(
                            new InputStreamReader( peerConnectionSocket.getInputStream() ) );

                    // client reads the valid GET request and processes the
                    // information
                    String request = incoming.readLine() + "\n";
                    // read in the whole request
                    while ( !request.contains( "\n\n" ) ) {
                        request = request + incoming.readLine() + "\n";
                    }

                    // scanner to process the request for the RFC number
                    final Scanner requestProcessor = new Scanner( request );
                    // ignore "GET"
                    requestProcessor.next();
                    // ignore "RFC"
                    requestProcessor.next();
                    // get the RFC number //
                    final int RFCNumber = requestProcessor.nextInt();

                    RFC sendRFC = null;
                    int index = -1;
                    for ( int i = 0; i < rfcs.size(); i++ ) {
                        if ( rfcs.get( i ).getNumber() == RFCNumber ) {
                            sendRFC = rfcs.get( i );
                            index = i;
                        }
                    }
                    if ( sendRFC == null ) {
                        // Peer did not have RFC, send 404 error
                        outgoing.println( "Error " + 404 + " RFC Not Found\n" );
                        outgoing.close();
                        incoming.close();
                    }
                    else {
                        // peer does have RFC, send 200 status and then send
                        // serialized RFC
                        outgoing.println( sendRFC.getNumber() + " " + sendRFC.getTitle() );
                        outgoing.println( "P2P-Cl/1.0 " + 200 + " OK" );
                        // get the date
                        final Date date = new Date();
                        // date format
                        final SimpleDateFormat format = new SimpleDateFormat( "EEE, dd MMM yyyy HH:mm:ss z" );
                        format.setTimeZone( TimeZone.getTimeZone( "EST" ) );
                        final String dateString = format.format( date );

                        outgoing.println( "Date: " + date );
                        outgoing.println(
                                "OS: " + System.getProperty( "os.name" ) + " " + System.getProperty( "os.version" ) );

                        // get the lasttime the file was modified
                        final Date lastModded = new Date( sendRFC.getFile().lastModified() );
                        final String lastMod = format.format( lastModded );
                        outgoing.println( "Last-Modified: " + lastMod );
                        outgoing.println( "Content-Length: " + sendRFC.getFile().length() );
                        // all RFCs are text objects
                        outgoing.println( "Content-Type: text/plain" );
                        outgoing.println( "RFC Number: " + sendRFC.getNumber() );
                        outgoing.println( "Filename: " + sendRFC.getTitle() );
                        outgoing.println( "File Content:\n" );

                        // open stream to send RFC object

                        // open stream to send rfc info
                        final StringBuilder fileContent = new StringBuilder();
                        final BufferedReader fileReader = new BufferedReader( new FileReader( sendRFC.getFile() ) );

                        String lineRead;
                        while ( ( lineRead = fileReader.readLine() ) != null ) {
                            fileContent.append( lineRead ).append( "\n" );
                        }

                        final String finalContent = fileContent.toString() + "\n";
                        outgoing.println( finalContent );
                        outgoing.println( "EOF" );
                        // reset the rfc file

                        incoming.close();
                        peerConnectionSocket.close();

                    }

                }
            }
            catch ( final Exception e ) {
                System.out.println( "Peer failed to start upload server " );
                System.exit( 0 );
            }

        }

    }

    /**
     * @return the hostname
     */
    public String getHostname () {
        return hostname;
    }

    /**
     * @param hostname
     *            the hostname to set
     */
    private void setHostname ( final String hostname ) {
        this.hostname = hostname;
    }

    /**
     * @return the uploadPort
     */
    public int getUploadPort () {
        return uploadPort;
    }

    /**
     * @param uploadPort
     *            the uploadPort to set
     */
    private void setUploadPort ( final int uploadPort ) {
        this.uploadPort = uploadPort;
    }

    /**
     * @return the rfcs
     */
    public LinkedList<RFC> getRfcs () {
        return rfcs;
    }

    /**
     * @param rfcs
     *            the rfcs to set
     */
    private void setRfcs ( final LinkedList<RFC> rfcs ) {
        this.rfcs = rfcs;
    }

    /**
     * Private class for RFC information. Serializable so it can be sent of TCP
     */
    private static class RFC implements Serializable {
        /** for serialization */
        private static final long serialVersionUID = 1L;
        /** RFC Number */
        private int               number;
        /** title of RFC */
        private String            title;
        /** hostname of owner of RFC */
        private String            hostname;
        /** file of the RFC */
        private File              file;

        // missing somthing to link to RFC text file //
        public RFC ( final int number, final String title, final String hostname, final File file ) {
            setNumber( number );
            setTitle( title );
            setHostname( hostname );
            setFile( file );
        }

        /**
         * @param number
         *            the number to set
         */
        private void setFile ( final File file ) {
            this.file = file;
        }

        /**
         * @return the file
         */
        private File getFile () {
            return file;
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
        private String getHostname () {
            return hostname;
        }

        /**
         * @param hostname
         *            the hostname to set
         */
        private void setHostname ( final String hostname ) {
            this.hostname = hostname;
        }

    }
}
