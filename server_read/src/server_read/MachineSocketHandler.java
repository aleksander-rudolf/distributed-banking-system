package server_read;

import Errors.*;
import java.sql.*;
import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * MachineSocketHandler Class
 * 
 * Every connected client has its requests
 * processed by a dedicated MachineSocketHandler thread
 * 
 */
public class MachineSocketHandler extends Thread {

    private Socket socket;
    private int timeout;
    private DBController db;

    /**
     * Constructor
     * 
     * @param socket  Dedicated socket opened between currently connected client and
     *                server
     * @param timeout Timeout used to disconnect a non-responsive client
     * @param db      Instance of the Singleton database connection
     */
    public MachineSocketHandler(Socket socket, int timeout, DBController db) {
        this.socket = socket;
        this.timeout = timeout;
        this.db = db;
    }

    /**
     * Main read server request handler method
     * The server listens for incoming server requests
     * until the server disconnects or timesout and becomes
     * unresponsive
     * 
     * Server Request Format:
     * Check balance request:
     * B <id> <accountNo>
     * eg. B 2 1234567890
     * 
     * Update balance request:
     * X <id> <accountNo> <balance>
     * eg. X 2 1234567890 1000.00
     */
    public void run() {
        try {
            // Open output stream to write the response message to the socket
            PrintStream socketOut = new PrintStream(socket.getOutputStream());

            // Set socket timeout to detect non-responsive clients.
            // If the socket timeout expires while the worker thread is still blocked on
            // reading from the socket,
            // a SocketTimeoutException will be thrown, which indicates non-responsive
            // clients.
            socket.setSoTimeout(this.timeout);

            // Open input stream to read the request message from the socket being delivered
            // from the client
            Scanner socketInput = new Scanner(this.socket.getInputStream());

            String clientRequest;

            while (socket.isConnected()) {
                try {
                    clientRequest = socketInput.nextLine();
                } catch (NoSuchElementException e) {
                    // If the socket times out, the client is considered unresponsive and the worker
                    // thread is terminated.
                    System.out.println(
                            "Client or server timed out from " + socket.getInetAddress() + ":" + socket.getPort());
                    break;
                }
                if (clientRequest.equals("server")) {
                    continue;
                }

                System.out.println(clientRequest);
                String[] clientRequestTokens = clientRequest.split(" ");
                String opcode = clientRequestTokens[0],
                        id = opcode.equals("B") ? clientRequestTokens[1] : "",
                        usrname_accNo = opcode.equals("B") ? clientRequestTokens[2] : clientRequestTokens[1],
                        amount_str_sometimes = opcode.equals("B") ? "" : clientRequestTokens[2];

                Double status = null;

                switch (opcode) {
                    case "B":
                        try {
                            status = balance(id, usrname_accNo);
                        } catch (InvalidError IE) {
                            socketOut.println("The account number you entered was invalid!");
                            break;
                        } catch (SQLException SE) {
                            socketOut.println("There was a unknown issue in your request, please try again!");
                            break;
                        }
                        if (status <= 0) {
                            socketOut.println("There was a unknown issue in your request, please try again!");
                            break;
                        }
                        socketOut.println("Your balance is:\n" + status + "\n");
                        break;
                    case "X":
                        try {
                            status = update(usrname_accNo, amount_str_sometimes);
                        } catch (InvalidError IE) {
                            socketOut.println("The account number you wanted to transfer from was invalid!");
                            break;
                        } catch (SQLException SE) {
                            socketOut.println("There was a unknown issue in your request, please try again!");
                            break;
                        }
                        if (status <= 0) {
                            socketOut.println("There was a unknown issue in your request, please try again!");
                            break;
                        }
                        socketOut.println("Your balance after transfer:\n" + status + "\n");
                        break;
                }
            }

            socketInput.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Timeout due to unresponsive client");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // #region Database Methods
    /**
     * Balance controller method. Calls the database to retrieve the balance of an
     * account
     * 
     * @param id        The ID of the user.
     * @param accountNo The account number of the user.
     * @return Double The balance of the account.
     */
    public double balance(String id, String accountNo) throws InvalidError, SQLException {
        return this.db.balance(accountNo, id);
    }

    /**
     * Update db if specified. Calls the database method to update an account.
     * 
     * @param id        The ID of the user.
     * @param accountNo The account number of the user that is to be updated.
     * @param amount    The new amount which the account will be updated to.
     * @return True if the update was successful, false otherwise.
     */
    public double update(String accountNo, String amount) throws InvalidError, SQLException {
        return this.db.alterAccount(accountNo, Double.parseDouble(amount));
    }
    // #endregion Database Methods
}