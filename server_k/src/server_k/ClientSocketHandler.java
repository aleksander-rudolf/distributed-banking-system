package server_k;

import java.io.*;
import Errors.*;
import java.sql.*;
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
 */
public class ClientSocketHandler extends Thread {
    //#region Class Variables
    private LockManager lockManager = LockManager.getInstance();

    private Socket socket;
    private int timeout;
    private DBController db;
    private PrintStream socketOut;
    //#endregion Class Variables

    /**
     * Constructor
     * @param socket Dedicated socket opened between currently connected client and server.
     * @param timeout Timeout used to disconnect a non-responsive client.
     * @param db Instance of the Singleton database connection.
     */
    public ClientSocketHandler(Socket socket, int timeout, DBController db) {
        this.socket = socket;
        this.timeout = timeout;
        this.db = db;
    }


    //#region Account database Methods
    /**
     * Balance controller method
     * @param id The ID of the client.
     * @param accountNo The account number of the client.
     * @return Double The balance of the client.
     */
    public double balance(String id, String accountNo) throws InvalidError, SQLException {
        return this.db.balance(accountNo, id);
    }

    /**
     * Withdraw controller method.
     * @param id Id of the user.
     * @param accountNo Account number to withdraw from.
     * @param amount Amount to withdraw.
     * @param randomNumber Random number generated by the client.
     * @return Double the new balance of the account.
     */
    public double withdraw(String id, String accountNo, double amount, String randomNumber) throws InvalidError, LogicError, SQLException, DuplicateTransactionError {
        while (lockManager.isLocked(accountNo, "W")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread failed in withdraw method: " + e.getStackTrace());
            }
        }

        boolean isLocked = lockManager.lock(accountNo, "W", randomNumber);
        if (!isLocked) { throw new LogicError(); }

        double result = this.db.withdraw(id, accountNo, amount);

        String response = lockManager.unlock(id, accountNo, "W", amount, Double.toString(result));
        String[] multiResponse = response.split(" ");

        boolean isUnlocked = Boolean.parseBoolean(multiResponse[0]);
        double newBalance = Double.parseDouble(multiResponse[1]);

        if (!isUnlocked) { throw new LogicError(); }

        return newBalance;
    }

    /**
     * Deposit controller method.
     * @param id Id of the user.
     * @param accountNo Account number to deposit to.
     * @param amount Amount to deposit.
     * @param randomNumber Random number generated by the client.
     * @return Double the new balance of the account.
     */
    public double deposit(String id, String accountNo, double amount, String randomNumber) throws InvalidError, SQLException, LogicError, DuplicateTransactionError {
        while (lockManager.isLocked(accountNo, "D")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread failed in deposit method: " + e.getStackTrace());
            }
        }

        boolean isLocked = lockManager.lock(accountNo, "D", randomNumber);
        if (!isLocked) { throw new LogicError(); }

        double result = this.db.deposit(id, accountNo, amount);
        String response = lockManager.unlock(id, accountNo, "D", amount, Double.toString(result));
        String[] multiResponse = response.split(" ");

        boolean isUnlocked = Boolean.parseBoolean(multiResponse[0]);
        double newBalance = Double.parseDouble(multiResponse[1]);

        if (!isUnlocked) { throw new LogicError(); }

        return newBalance;
    }

    /**
     * Transfer controller method.
     * @param id id of the user.
     * @param fromAccountNo Account number of the account to transfer from.
     * @param toAccountNo Account number of the account to transfer to.
     * @param amount Amount to transfer.
     * @param randomNumber Random number generated by the client.
     * @return Double The new balance of the account.
     */
    public double transfer(String id, String fromAccountNo, String toAccountNo, double amount, String randomNumber) throws InvalidError, LogicError, SQLException, DestinationError, DuplicateTransactionError {
        while (lockManager.isLocked(fromAccountNo, "T") || lockManager.isLocked(toAccountNo, "T")) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread failed in transfer method: " + e.getStackTrace());
            }
        }

        boolean isLocked = lockManager.lock(fromAccountNo, "T", randomNumber);
        boolean isLocked2 = lockManager.lock(toAccountNo, "T", randomNumber);
        if (!isLocked || !isLocked2) { throw new LogicError(); }

        String result = this.db.transfer(id, fromAccountNo, toAccountNo, amount);

        String response = lockManager.unlock(id, fromAccountNo + "," + toAccountNo, "T", amount, result);
        String[] multiResponse = response.split(" ");

        boolean isUnlocked = Boolean.parseBoolean(multiResponse[0]);
        double newBalance = Double.parseDouble(multiResponse[1]);

        if (!isUnlocked) { throw new LogicError(); }

        return newBalance;
    }
    //#endregion Account database Methods

    //#region Login and Register database Methods
    /**
     * Login controller method
     * @param username The username of the client.
     * @param password The password of the client.
     * @return String The ID of the client.
     */
    public String login(String username, String password) {
        return this.db.login(username, password);
    }

    /**
     * Register controller method
     * @param username The username to register of the user
     * @param password The password to register of the user
     * @param firstName First name to register of the user
     * @param lastName Last name to register of the user
     * @param email email to user when registering
     * @return return id of the new registered user
     */
    public int register(String username, String password, String firstName, String lastName, String email) throws LogicError, SQLException {
        return this.db.register(username, password, firstName, lastName, email);
    }
    //#endregion Login and Register database Methods

    /**
     * Main client request handler method
     * The server listens for incoming client requests
     * until the client disconnects or timesout and becomes
     * unresponsive
     * 
     * Client Request Format
     *  Transfer request:
     *      Format: T from to amount
     *      eg: T 12345678 87654321 100 
     * 
     *  Deposit request:
     *      Format: D to amount
     *      eg: D 12345678 100 
     * 
     *  Withdraw request:
     *      Format: W from amount
     *      eg: W 12345678  100
     * 
     *  View Balance:
     *      Format: B account
     *      eg: B 12345678
     * 
     *  Login:
     *      Format: L id password
     *      eg: L 2 tcEnergy
     * 
     *  Register:
     *      Format: R username password firstname lastname email
     *      eg: R Bob Marley Bob Marleykon bob@gmail.com
     */
    public void run() {
        try {
            // Open output stream to write the response message to the socket
            socketOut = new PrintStream(socket.getOutputStream());

            // Set socket timeout to detect non-responsive clients.
            // If the socket timeout expires while the worker thread is still blocked on reading from the socket, 
            // a SocketTimeoutException will be thrown, which indicates non-responsive clients.
            socket.setSoTimeout(this.timeout);

            // Open input stream to read the request message from the socket being delivered from the client
            Scanner socketInput = new Scanner(this.socket.getInputStream());

            String clientRequest;
            String id = new String();

            // Print the client's IP address and port number
            System.out.println("Client connected from " + socket.getInetAddress() + ":" + socket.getPort());

            while (socket.isConnected()) {
                try {
                    // Read the request message from the socket
                    clientRequest = socketInput.nextLine();
                } catch (NoSuchElementException e) {
                    // If the socket times out, the client is considered unresponsive and the worker thread is terminated.
                    System.out.println("Client timed out from " + socket.getInetAddress() + ":" + socket.getPort());
                    break;
                }
                System.out.println("Received request: '" + clientRequest + "' from " + socket.getInetAddress() + ":" + socket.getPort());

                String[] clientRequestTokens = clientRequest.split(" ");
                String opcode = clientRequestTokens[0];
                
                switch (opcode) {
                    case "L":
                    	id = checkLogin(clientRequestTokens);
                    	break;
                    case "RE":
                        id = clientRequestTokens[1];
                        socketOut.println("OK"); // Inform the client, it has been re-registerd.
                        break;
                    case "R":
                    	registerAccount(clientRequestTokens);
                    	break;
                    case "D":
                    	depositMoney(id, clientRequestTokens);
                    	break;
                    case "W":
                    	withdrawMoney(id, clientRequestTokens);
                    	break;
                    case "B":
                    	checkBalance(id, clientRequestTokens);
                    	break;
                    case "T":
                    	transferMoney(id, clientRequestTokens);
                    	break;
                }
            }

            // Close the socket
            socketInput.close();
            socket.close();
        } catch (SocketTimeoutException e) {
            System.out.println("Client error: " + e.getStackTrace());
        } catch (SocketException e) {
            System.out.println("Socket error in client thread: " + e.getStackTrace());
        } catch (IOException e) {
            System.out.println("IO Error in client thread: " + e.getStackTrace());
        }
    }

    //#region Account helper methods
    /**
     * Checks the balance of an account.
     * @param id Id of the client.
     * @param clientRequestTokens Array of string containing client information - account number.
     */
    private void checkBalance(String id, String[] clientRequestTokens) {
    	String accountNo = clientRequestTokens[2];

        double status = -1;
        try {
            status = balance(id, accountNo);
        } catch(InvalidError IE) {
            socketOut.println("The account number you entered was invalid!");
            return;
        } catch(SQLException SE) {
            socketOut.println("There was a unknown issue in your request, please try again!"); 
            return;
        }

        if (status <= 0) {
            socketOut.println("There was a unknown issue in your request that could not be processed, please try again!");
            return;
        }
        
        socketOut.println("Your balance is: " + status + ".\n");         	
    }
    
    /**
     * Deposit money into an account.
     * @param id Id of the client.
     * @param clientRequestTokens Array of string containing client information.
     */
    private void depositMoney(String id, String[] clientRequestTokens) {
    	String accountNo = clientRequestTokens[1];
    	double amount = Double.parseDouble(clientRequestTokens[2]);
        String randomNumber = clientRequestTokens[3];

        double status = -1;
        try {
            status = deposit(id, accountNo, amount, randomNumber);
        } catch(InvalidError IE) {
            socketOut.println("The account number you entered was invalid!");
            return;
        } catch(SQLException SE) {
            socketOut.println("There was a unknown issue in your request, please try again!"); 
            return;
        } catch(LogicError LE) {
            socketOut.println("There was a unknown issue in your request that could not be processed, please try again!");
            return;
        } catch(DuplicateTransactionError DTE) {
            socketOut.println("DTE");
            return;
        }

        if (status <= 0) {
            socketOut.println("There was a unknown issue in your request that could not be processed, please try again!");
            return;
        }

        socketOut.println("Your balance before deposit was: " + (status - amount) + ".\nYour balance after deposit is: " + status + ".\n");         	
    }
    
    /**
     * Withdraws money from the account.
     * @param id Id of the client.
     * @param clientRequestTokens Array of string containing client information.
     */
    private void withdrawMoney(String id, String[] clientRequestTokens) {
    	String account = clientRequestTokens[1];
    	double amount = Double.parseDouble(clientRequestTokens[2]);
        String randomNumber = clientRequestTokens[3];

        double status = -1;
        try {
            status = withdraw(id, account, amount, randomNumber);
        } catch(InvalidError IE) {
            socketOut.println("The account number you entered was invalid!"); 
            return;
        } catch(SQLException SE) {
            socketOut.println("There was a unknown issue in your request, please try again!"); 
            return;
        } catch(LogicError LE) {
            socketOut.println("The balance in the given account is insufficient for withdraw!"); 
            return;
        } catch(DuplicateTransactionError DTE) {
            socketOut.println("DTE");
            return;
        }

        if (status <= 0) {
            socketOut.println("There was a unknown issue in your request, please try again!"); // Shouldn't this be a different message?
            return;
        }

        socketOut.println("Your balance before withdraw was: " + (status + amount) + ".\nYour balance after withdraw is: " + status + ".\n");        	
    }
    
    /**
     * Transfer money from one account to another.
     * @param id Id of the client.
     * @param clientRequestTokens Array of string containing client accounts to transfer from and to, and the amount to transfer.
     */
    private void transferMoney(String id, String[] clientRequestTokens) {
    	String account1 = clientRequestTokens[1];
    	String account2 = clientRequestTokens[2];
    	double amount = Double.parseDouble(clientRequestTokens[3]);
        String randomNumber = clientRequestTokens[4];

        double status = -1;
        try {
            status = transfer(id, account1, account2, amount, randomNumber);
        } catch(InvalidError IE) {
            socketOut.println("The account number you wanted to transfer from was invalid!"); 
            return;
        } catch(SQLException SE) {
            socketOut.println("There was a unknown issue in your request, please try again!"); 
            return;
        } catch(LogicError LE) {
            socketOut.println("The balance in the given account is insufficient to make this transfer!"); 
            return;
        } catch(DestinationError DE) {
            socketOut.println("The account number you wanted to transfer was invalid!"); 
            return;
        } catch(DuplicateTransactionError DTE) {
            socketOut.println("DTE");
            return;
        }
        
        if (status <= 0) {
            socketOut.println("There was a unknown issue in your request, please try again!"); // Shouldn't this be a different message?
            return;     
        }

        socketOut.println("Your balance after transfer: " + status + ".\n"); 
    }
    //#endregion Account helper methods

    //#region Login and register helper methods
    /**
     * Checks if the login is successful or not.
     * @param clientRequestTokens Array of string containing client information.
     * @return Returns If login is successful or not.
     */
    private String checkLogin(String[] clientRequestTokens) {
    	String username = clientRequestTokens[1];
    	String password = clientRequestTokens[2];
    	String user_id = login(username, password);

        if (user_id.equals("fail")) {
        	socketOut.println("Incorrect username or password!");
        } else {
        	socketOut.println(user_id + "%OK");
        }

    	return user_id;
    }

    /**
     * Registers an account for the user.
     * @param clientRequestTokens The tokens of the client request to register an account.
     */
    private void registerAccount(String[] clientRequestTokens) {
    	String username = clientRequestTokens[1];
    	String password = clientRequestTokens[2];
        String firstName = clientRequestTokens[3];
        String lastName = clientRequestTokens[4];
        String email = clientRequestTokens[5];
        String accountNumber;
        int id;

        try {
            id = register(username, password, firstName, lastName, email);
            accountNumber = this.db.createAccount(id);
        } catch (LogicError LE) {
            socketOut.println("Your registration failed!");
            return;
        } catch (SQLException SE) {
            return;
        }

        if (!accountNumber.equals("false")) {
            lockManager.registrationSync(id, username, password, firstName, lastName, email, accountNumber);
            socketOut.println("You have successfully registered your account! Your account number is: " + accountNumber);
        } else {
            socketOut.println("Your registration was successful but there was some issue in creating your account!");
        }
    }
    //#endregion Login and register helper methods
}