package server_j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import server_j.MachineSocket;


/**
 * ServerSocketHandler Class
 * 
 * Implements a multi-threaded server
 * supporting persistent connections.
 * 
 */
public class MachineSocket extends Thread {
    //#region Class Variables
    private int port, timeout, defaultTimeout = 100;
    private boolean shutdown = false;
    private ExecutorService executorPool;
    private DBController db = null;
    private static MachineSocket server= null;
    //#endregion Class Variables
	
    //#region Constructors & Instance Methods
    /**
     * Constructor
     * @param port The server port at which the web server listens > 1024
     * @param timeout The timeout value for detecting non-resposive clients, in milli-second units
     */
	private MachineSocket(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;
        this.executorPool = Executors.newFixedThreadPool(8);
    }

    /**
     * Returns the instance of this class.
     * @param port The server port at which the web server listens.
     * @param timeout The timeout value for detecting non-resposive clients, in ms.
     * @return MachineSocket The instance of this class.
     */
    public static MachineSocket getInstance(int port, int timeout) {
        if (server == null) { server = new MachineSocket(port, timeout); }
        return server;
    }
    //#endregion Constructors & Instance Methods

    //#region Methods
    /**
	 * Main server method.
	 * The server remains in listening mode and accepts connection requests from clients until the shutdown method is called.
     */
	public void run() {
        try {
            // Open the server socket and listen for TCP connections on port (port)
            ServerSocket serverSocket = new ServerSocket(this.port);

            /**
             * Set's the socket timeout to the default timeout of 100ms.
             * This forces the server thread to periodically time-out to return from the blocking method accept(), and check the status of the shutdown flag.
             */
            serverSocket.setSoTimeout(defaultTimeout);

            while(!shutdown) { // Loop until shutdown flag is set to true.
                try {
                    // Accept incoming TCP connection request and connect to client
                    Socket socket = serverSocket.accept();

                    // Open input stream to read the message from the socket to determine if it is coming from a client or another server
                    Scanner socketInput = new Scanner(socket.getInputStream());
                    String incomingConnection = socketInput.nextLine();

                    if (incomingConnection.equals("client")) {
                        // Print client information (IP address and port number) every time the server accepts a client connection
                        System.out.println("New client connection from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");

                        // Get Singleton instance of database connection for each TCP connection
                        this.db = DBController.getInstance();

                        // Create a MachineSocketHandler thread to handle the accepted TCP connection
                        this.executorPool.execute(new ClientSocketHandler(socket, this.timeout, db));
                    } else if(incomingConnection.equals("server")) {
                        // Print server information (IP address and port number) every time the server accepts a server connection
                        System.out.println("New server connection from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
                        
                        // Get Singleton instance of database connection for each TCP connection
                        this.db = DBController.getInstance();

                        // Create a MachineSocketHandler thread to handle the accepted TCP connection
                        this.executorPool.execute(new ServerSocketHandler(socket, this.timeout, db));
                    } else { // This should not happen
                        System.out.println("Invalid connection of type " + incomingConnection + " from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + ".");
                    }
                } catch (SocketTimeoutException e) {}
            }

            // Shutdown the executor and do not accept any new tasks
            this.executorPool.shutdown();

            // Wait 5 seconds for existing tasks to terminate
            try {
                if (!this.executorPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.executorPool.shutdownNow(); // Cancel currently executing tasks
                }
            } catch (InterruptedException e) {
                this.executorPool.shutdownNow();
            }

            // Close the server socket
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
    /**
     * Signals the server to shutdown.
     */
	public void shutdown() {
        this.shutdown = true;
    }
    //#endregion Methods
}