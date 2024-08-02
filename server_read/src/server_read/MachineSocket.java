package server_read;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
//import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ServerSocketHandler Class
 * 
 * Implements a multi-threaded server
 * supporting persistent connections.
 * 
 */
public class MachineSocket extends Thread {

    private int port;
    private int timeout;
    private boolean shutdown = false;
    private ExecutorService executorPool;
    private DBController db = null;
    private static MachineSocket server = null;
    // private Queue<String> transactions = null;

    /**
     * Constructor to initialize the web server
     * 
     * @param port    The server port at which the web server listens > 1024
     * @param timeout The timeout value for detecting non-resposive clients, in
     *                milli-second units
     * 
     */
    private MachineSocket(int port, int timeout) {
        this.port = port;
        this.timeout = timeout;
        this.executorPool = Executors.newFixedThreadPool(8);
    }

    /**
     * Returns the instance of this class.
     * 
     * @param port    The server port at which the web server listens.
     * @param timeout The timeout value for detecting non-resposive clients, in ms.
     * @return MachineSocket The instance of this class.
     */
    public static MachineSocket getInstance(int port, int timeout) {
        if (server == null) {
            server = new MachineSocket(port, timeout);
        }

        return server;

    }

    /**
     * Main server method.
     * The server remains in listening mode
     * and accepts connection requests from clients
     * until the shutdown method is called.
     *
     */
    public void run() {
        try {
            // Open the server socket and listen for TCP connections on port (port)
            ServerSocket serverSocket = new ServerSocket(this.port);

            // Set socket timeout to force the server thread to periodically time-out to
            // return from the blocking method accept() (on line 71), and check the status
            // of the shutdown flag
            serverSocket.setSoTimeout(100);

            while (!shutdown) {
                try {
                    // Accept incoming TCP connection request and connect to client
                    Socket socket = serverSocket.accept();

                    // Print client information (IP address and port number) every time the server
                    // accepts a client connection
                    System.out.println("New connection from " + socket.getInetAddress().getHostAddress() + ":"
                            + socket.getPort() + ".");

                    // Get Singleton instance of database connection for each TCP connection
                    this.db = DBController.getInstance();

                    // Create a MachineSocketHandler thread to handle the accepted TCP connection
                    this.executorPool.execute(new MachineSocketHandler(socket, this.timeout, db));

                } catch (SocketTimeoutException e) {
                }
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
     *
     */
    public void shutdown() {
        this.shutdown = true;
    }
}