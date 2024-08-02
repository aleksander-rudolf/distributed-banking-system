package server_i;

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
public class ServerSocketHandler extends Thread {
    //#region Class Variables
    private LockManager lockManager = LockManager.getInstance();

    private Socket socket;
    private int timeout;
    private DBController db;
    private String newAmount = new String();
    private PrintStream socketOut;
    //#endregion Class Variables

    /**
     * Constructor
     * @param socket Dedicated socket opened between currently connected client and server
     * @param timeout Timeout used to disconnect a non-responsive client
     * @param db Instance of the Singleton database connection
     */
    public ServerSocketHandler(Socket socket, int timeout, DBController db) {
        this.socket = socket;
        this.timeout = timeout;
        this.db = db;
    }

    //#region database methods
    /**
     * Withdraw controller method. Calls the database method to withdraw money from an account
     * @param id Id of the logged in client
     * @param accountNo Account number to withdraw from 
     * @param amount Amount to withdraw from 
     * @return True or false depending on whether the withdraw was successful or not 
     */
    public String withdraw(String id, String accountNo, double amount) throws InvalidError, LogicError, SQLException {
        return Double.toString(this.db.withdraw(id, accountNo, amount));
    }

    /**
     * Deposit controller method. Calls the database method to deposit money into an account
     * @param id Id of the logged in client
     * @param accountNo Account number to deposit into
     * @param amount Amount to deposit
     * @return True or false depending on whether the deposit was successful or not 
     */
    public String deposit(String id, String accountNo, double amount) throws InvalidError, SQLException {
        return Double.toString(this.db.deposit(id, accountNo, amount));
    }

    /**
     * Transfer controller method. Calls the database method to transfer money from one account to another  
     * @param id Id of the logged in client
     * @param fromAccountNo Account number to transfer from
     * @param toAccountNo Account number to transfer to
     * @param amount Amount to transfer from account 1 to account 2
     * @return True or false depending on whether the transfer was successful or not 
     */
    public String transfer(String id, String fromAccountNo, String toAccountNo, double amount) throws InvalidError, LogicError, SQLException, DestinationError {
        return this.db.transfer(id, fromAccountNo, toAccountNo, amount);
    }
    
    /**
     * Register a new user and create a new account associated with the new user
     * @param id The ID of the user.
     * @param username username to register with
     * @param password password to register with
     * @param firstName firstname to register with
     * @param lastName lastname to register with
     * @param email email to register with
     * @param account bank account number to associate newly registered account with
     * @return
     */
    public boolean register(int id, String username, String password, String firstName, String lastName, String email, String account) throws SQLException {
        return this.db.registerAndCreateAccountForSync(id, username, password, firstName, lastName, email, account);
    }

    /**
     * Balance controller method. Calls the database to retrieve the balance of an account
     * @param id The ID of the user.
     * @param accountNo The account number of the user.
     * @return Double The balance of the account.
     */
    public double balance(String id, String accountNo) throws InvalidError, LogicError, SQLException {
        return this.db.balance(accountNo, id);
    }
    //#endregion database methods

    /**
     * Attempts to lock the account.
     * @param account The account number of the user.
     * @param opCode The operation code.
     * @param randomNumber The random number.
     * @return boolean True if the lock was successful, false otherwise.
     */
    public boolean lock(String account, String opCode, String randomNumber) throws DuplicateTransactionError {
        if (lockManager.isLocked(account, opCode)) { return false; }
        return lockManager.lockLocally(account, opCode, randomNumber);
    }

    /**
     * This method performs the transaction on its local database and unlocks the account locally
     * @param id The ID of the user.
     * @param account The account number to unlock and perform a transaction on
     * @param opCode Code to check if the server should deposit, withdraw or transfer
     * @param amount the amount to perform the transaction with
     * @return booelan True if the method successfully processes the transaction and unlocks the account, else returns false
     */
    public boolean unlock(String id, String account, String opCode, double amount) throws InvalidError, LogicError, SQLException, DestinationError {
        boolean returnValue;

        switch(opCode) {
            case "D": // Deposit
            	newAmount = this.deposit(id, account, amount);
                returnValue = lockManager.unlockLocally(account, opCode);  
                break;
            case "W": // Withdraw
            	newAmount = this.withdraw(id, account, amount);
                returnValue = lockManager.unlockLocally(account, opCode);  
                break;
            case "T": // Transfer
                String[] accounts = account.split(",");
                newAmount = this.transfer(id, accounts[0], accounts[1], amount);
                returnValue = lockManager.unlockLocally(accounts[0], opCode) && lockManager.unlockLocally(accounts[1], opCode);
                break;
            default:
                returnValue = false;
                System.out.println("Invalid operation code provided to the unlock method.");
        } 

        return returnValue;
    }

    /**
     * Main server request handler method
     * The server listens for incoming server requests
     * until the server disconnects or timesout and becomes
     * unresponsive
     * 
     * Server Request Formats
     *  Transfer request:
     *      Format: T from to amount (T from,to amount)
     *      eg: T 12345678 87654321 100 
     * 
     *  Deposit request:
     *      Format: D to amount
     *      eg: D 12345678 100 
     * 
     *  Withdraw request:
     *      Format: W from amount
     *      eg: W 12345678  100
     */
    public void run() {
        String opCode = new String(),
            account = new String(),
            id,
            transactionCode,
            randomNumber,
            message;
        Double amount;

        try {
            // Open output stream to write the response message to the socket
            socketOut = new PrintStream(socket.getOutputStream());

            /*
             * Set's the socket timeout to detect non-responsive clients. 
             * If the socket timeout expires while the worker thread is still blocked on reading from the socket, 
             * a SocketTimeoutException will be thrown, which indicates non-responsive clients.
             */
            socket.setSoTimeout(this.timeout);

            // Open input stream to read the request message from the socket being delivered from the client
            Scanner socketInput = new Scanner(this.socket.getInputStream());
            String clientRequest;

            // Print the client's IP address and port number
            System.out.println("Server connected from " + socket.getInetAddress() + ":" + socket.getPort());

            while (socket.isConnected()) {
                try {
                    // Read the request message from the socket
                    clientRequest = socketInput.nextLine();
                } catch (NoSuchElementException e) {
                    // If the socket times out, the client is considered unresponsive and the worker thread is terminated.
                    System.out.println("Server timed out from " + socket.getInetAddress() + ":" + socket.getPort());
                    break;
                }
                System.out.println("Received request: '" + clientRequest + "' from " + socket.getInetAddress() + ":" + socket.getPort());

                String[] clientRequestTokens = clientRequest.split(" ");
                opCode = clientRequestTokens[0];
                boolean successful;
                message = "false";

                switch (opCode) {
                    case "L":
                        account = clientRequestTokens[1];
                        transactionCode = clientRequestTokens[2];
                        randomNumber = clientRequestTokens[3];
                        System.out.println("Locking account #" + account + " with transaction code " + transactionCode + ".");
                        try {
                            successful = lock(account, transactionCode, randomNumber);
                        } catch(DuplicateTransactionError DTE) {
                            successful = false;
                        }
                        message = successful ? "true" : "false";
                        socketOut.println(message);
                        socketOut.flush();
                        break;
                    case "U":
                        id = clientRequestTokens[1];
                        account = clientRequestTokens[2];
                        transactionCode = clientRequestTokens[3];
                        amount = Double.parseDouble(clientRequestTokens[4]);
                        try {
                            successful = unlock(id, account, transactionCode, amount);
                        } catch(Exception e) {
                            successful = false;
                        }
                        System.out.println("Unlocking account #" + account + " with transaction code " + transactionCode + ", with final amount: " + newAmount + ". Successful: " + successful + ".");
                        message = successful ? "true " + newAmount : "false";
                        socketOut.println(message);
                        socketOut.flush();
                        break;
                    case "R":
                        try {
                            successful = register(
                                Integer.parseInt(clientRequestTokens[1]), 
                                clientRequestTokens[2], 
                                clientRequestTokens[3], 
                                clientRequestTokens[4], 
                                clientRequestTokens[5], 
                                clientRequestTokens[6], 
                                clientRequestTokens[7]
                            );
                        } catch (Exception e) {
                            successful = false;
                        }
                        break;
                    case "O":
                        String dump = socketInput.nextLine();
                        try {
                            successful = db.overwriteDatabase(dump);
                        } catch(Exception e) {
                            successful = false;
                        }
                        break;
                    case "C":
                        account = clientRequestTokens[1];
                        amount = Double.parseDouble(clientRequestTokens[2]);
                        try {
                            successful = db.overwriteAccount(account, amount);
                        } catch(Exception e){
                            successful = false;
                        }
                        message = successful ? "true" : "false"; 
                        break;
                    default:
                        System.out.println("Invalid request received from server.");
                        break;
                }
            }

            // Close the socket
            socketInput.close();
            socket.close();
        } catch (SocketTimeoutException e) {
            if (opCode.equals("L")) { lockManager.unlockLocally(account, opCode); }
            System.out.println("Server error: " + e.getStackTrace());
        } catch (SocketException e) {
            System.out.println("Socket error in server thread: " + e.getStackTrace());
        } catch (IOException e) {
            System.out.println("IO Error in server thread: " + e.getStackTrace());
        }
    }
}