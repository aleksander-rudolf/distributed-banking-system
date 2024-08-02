package client_i;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SocketManager {
    //#region Class Variables
    private SocketHandler writeSocketHandler;
    private SocketHandler readSocketHandler;
    //#endregion Class Variables
    
    //#region Constructors
    /**
     * Constructor
     */
    public SocketManager() {
        // Initialize the write and read socket handlers
        this.writeSocketHandler = new SocketHandler("559-1.rickybhatti.ca", 559);
        this.writeSocketHandler.send("client");
        this.readSocketHandler = new SocketHandler("559-2.rickybhatti.ca", 560);
    }
    //#endregion Constructors
    
    //#region Methods
    /**
     * This method queries the server.
     * @param isWrite Whether or not the query is a write query. (Read queries go to the read server, write queries go to the write server.)
     * @param userId The user ID of the user making the query.
     * @param message The message to send to the server.
     * @return String The response from the server.
     */
    public String query(boolean isWrite, String userId, String message) {
        SocketHandler socketHandler = isWrite ? this.writeSocketHandler : this.readSocketHandler;
        String response = null;
        boolean readFailed = false;
        
        while (response == null && !readFailed) {
            try {
                socketHandler.send(message);
                response = socketHandler.read();
            } catch (SocketTimeoutException | SocketException e) { // If the socket timed out, reconnect and try again
                try {
                    socketHandler.reconnect(userId);
                    response = socketHandler.read();
                    
                    if (response.equals("OK")) {
                        socketHandler.send(message);
                        response = socketHandler.read();
                        return response;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                if (!isWrite) { // For read queries, try the write server if the read server times out
                    socketHandler = this.writeSocketHandler;
                }
            } catch (IOException e) {
                e.printStackTrace();
                readFailed = true;
            }
        }

        if (response == "DTE") { return ""; } // Duplicate transaction error, return an empty string. (This means the transaction was already processed by the server, aka the client already sent the transaction to the server, but the client never received the response. This is a rare case, but it can happen.)
        
        return response;
    }
    
    /**
     * This method closes the socket.
     */
    public void close() {
        this.writeSocketHandler.close();
        this.readSocketHandler.close();
    }
    //#endregion Methods
}