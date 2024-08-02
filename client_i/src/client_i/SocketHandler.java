package client_i;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

class SocketHandler {
    //#region Class Variables
    private String host;
    private int port;

    private Socket socket;
    private PrintStream socketOut;

    private final int timeout = 7 * 1000; // 7 Second timeout
    private final int bufferLimit = 1024;
    //#endregion Class Variables
    //#region Constructors
    /**
     * Constructor
     * @param host The host to connect to.
     * @param port The port to connect to.
     */
    public SocketHandler(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            this.socket = new Socket(this.host, this.port);
            this.socketOut = new PrintStream(this.socket.getOutputStream());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //#endregion Constructors

    //#region Methods
    /**
     * This method reads a message from the socket.
     * @return String The message received from the socket.
     * @throws SocketTimeoutException
     * @throws SocketException
     * @throws IOException
     */
    public String read() throws SocketTimeoutException, SocketException, IOException {
        byte buffer[] = new byte[bufferLimit];
        this.socket.setSoTimeout(this.timeout); // Set the timeout, so we don't wait forever.
        this.socket.getInputStream().read(buffer);

        String response = new String(buffer).trim();
        if (response.equals("-1") || response.equals("")) { throw new SocketTimeoutException(); }

        return response;
    }

    /**
     * This method sends a message over the socket.
     * @param message The message to send over the socket.
     */
    public void send(String message) {
        this.socketOut.println(message);
        this.socketOut.flush();
    }

    /**
     * This method closes the socket and the output stream.
     */
    public void close() {
        this.socketOut.close();
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempts to reconnect to the socket.
     * @throws SocketTimeoutException
     * @throws SocketException
     * @throws IOException
     */
    public void reconnect(String userId) throws SocketTimeoutException, SocketException, IOException {
        try {
            this.socketOut.close();
            this.socket.close();

            this.socket = new Socket("559-1.rickybhatti.ca", 559);
            this.socketOut = new PrintStream(this.socket.getOutputStream(), true);
            this.send("client");
            Thread.sleep(1000); // Ensure the server has time to process the command, otherwise it will not be ready to receive the RE command.
            this.send("RE " + userId); // Send the RE command to the server to reauthenticate, plus the user ID.
        } catch (UnknownHostException | InterruptedException e) {
            e.printStackTrace();
        } 
    }
    //#endregion Methods

    //#region Getters and Setters
    /**
     * Getter for the host address.
     * @return String The host address.
     */
    public String getHost() { return this.host; }

    /**
     * Getter for the port number.
     * @return String The port number.
     */
	public String getPort() { return String.valueOf(this.port); }
    //#endregion Getters and Setters
}