package server_i;

import java.sql.*;
import Errors.*;

public class DBController {
    //#region Class Variables
	private String DBURL = new String();
    private static DBController dbController = null;
    private Connection db;
    //#endregion Class Variables

    //#region Constructors & Instance Methods
    /**
     * Constructor
     * @implNote This constructor is private to ensure that only one instance of this class is created.
     */
    private DBController() {
        this.DBURL = "jdbc:sqlite:bankmanager_i.db"; // This is the path to the database file.
        connect();
    }

    /**
     * Returns the instance of this class.
     * @return DBController The instance of this class.
     */
    public static DBController getInstance() {
        if (dbController == null) { dbController = new DBController(); }
        return dbController;
    }
    //#endregion Constructors & Instance Methods

    /**
     * Establish connection to the MySQL database.
     */
    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.db = DriverManager.getConnection(this.DBURL);
            System.out.println("Connection to SQLite has been established.");
            try (Statement statement = this.db.createStatement()) {
                statement.execute("PRAGMA busy_timeout = " + 5000);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch(Exception e){
            System.out.println("Connection to SQLite has failed: " + e.getStackTrace());
            System.exit(1); // Exit the program if the connection fails, as it is required for the program to run.
        }
        
    }

    /**
     * Close the connection to the MySQL database.
     */
    public void close() {
        try {
            this.db.close();
        } catch (SQLException e) {
            System.out.println("Connection closure to SQLite has failed: " + e.getStackTrace());
        }
    }
    //#region Account Methods
    /**
     * This method will get the balace of an account given the uesr id
     * @param accountNo Account number to check balance of
     * @param id Id of the client 
     * @return double The balance of the account 
     * This mehod will return a double which is balance of that account,
     * if there is a sql error, it would return -3.0
     * if there is inavlid account number, it would return -2.0
     */
    public double balance(String accountNo, String id) throws InvalidError, SQLException {
        double balance;

        try {
            String selectStatement = "SELECT * FROM accounts WHERE account_number = ? AND user_id = ?";   
            PreparedStatement selectQuery = db.prepareStatement(selectStatement);
            
            selectQuery.setString(1, accountNo);
            selectQuery.setString(2, id);
            ResultSet res = selectQuery.executeQuery();
            
            if (res.next()) { //this means account and user_id were valid
                balance = res.getDouble("balance");
                System.out.println("Balance of account #" + accountNo + " was successfully retrieved, with a balance of " + balance + ".");
            } else {
                System.out.println("An invalid account number or user_id was used for balance, account number: " + accountNo + ".");
                throw new InvalidError();
            } 
        } catch (SQLException e) {
            System.out.println("Error in SQL query for balance.");
            throw new SQLException();
        }

        return balance;
    }

    /**
     * This method will get the balance of an account given the account
     * @param accountNo Account number to check balance of
     * @return double The balance of the account number
     * This is a special method used to check balance of the transferred 
     * toAccount without verifying the user_id of the toAccount
     * 
     * This method will return a double which is balance of that account,
     * if there is a sql error, it would return -3.0
     * if there is inavlid account number, it would return -2.0
     */
    public double balanceForTransfer(String accountNo) throws InvalidError, SQLException {
        double balance; 
        
        try {
            String selectStatement = "SELECT * FROM accounts WHERE account_number = ?";

            PreparedStatement selectQuery = db.prepareStatement(selectStatement);
            selectQuery.setString(1, accountNo);

            ResultSet res = selectQuery.executeQuery();
            if (res != null) {
                balance = res.getDouble("balance");
                System.out.println("Balance for transfer: " + balance + " into account: " + accountNo + ".");
            } else {
                System.out.println("An invalid account number or user_id was used for balanceTransfer, account number: " + accountNo + ".");
                throw new InvalidError();
            }  
        } catch (SQLException SE) {
            System.out.println("Error in SQL query for balanceForTransfer.");
            throw SE;
        }

        return balance;
    }

    /**
     * This method will withdraw money with a given amount from an given account 
     * @param id Id of the client 
     * @param accountNo Account number to withdraw from
     * @param amount amount to withdraw from the account
     * @return double The new balance of the account 
     * This mehod will return a double which is balance of that account after withdraw,
     * if there is a sql error, it would return -3.0
     * if there is inavlid account number, it would return -2.0
     * if there is insufficent balance, it would return -1.0
     */
    public synchronized double withdraw(String id, String accountNo, double amount) throws InvalidError, SQLException, LogicError {
        double balance;

        try {
            balance = balance(accountNo, id);
            // Sufficient balance in the account
            if (balance > amount) {
                balance = balance - amount;
                String updateStatement = "UPDATE accounts SET balance = ? WHERE account_number = ? AND user_id = ?";
                PreparedStatement updateQuery = db.prepareStatement(updateStatement);
                updateQuery.setString(1, balance + "");
                updateQuery.setString(2, accountNo);
                updateQuery.setString(3, id);

                // Update was successful.
                if (updateQuery.executeUpdate() > 0) {
                    balance = this.balance(accountNo, id);
                    System.out.println("Balance before withdraw: " + (balance + amount) + ", after withdraw: " + balance + " into account: " + accountNo + ".");
                } else {
                    System.out.println("An invalid account number or user_id was used for withdraw, account number: " + accountNo + ".");
                    throw new InvalidError();
                }
            } else {
                System.out.println("Insufficient balance for withdraw.");
                throw new LogicError();
            }       
        } catch(SQLException SE) {
            System.out.println("Error in SQL query for withdraw.");
            throw SE;
        }

        return balance;
    }

    /**
     * This method will deposit money with a given amount into an given account 
     * @param id Id of the client 
     * @param accountNo Account number to deposit into
     * @param amount amount to deposit into the account
     * @return double return the new balance of the account 
     * This mehod will return a double which is balance of that account after deposit,
     * if there is a sql error, it would return -3.0
     * if there is inavlid account number, it would return -2.0
     * 
     */
    public synchronized double deposit(String id, String accountNo, double amount) throws InvalidError, SQLException {
        double balance;

        try {
            balance = balance(accountNo, id) + amount;
            String updateStatement = "UPDATE accounts SET balance = ? WHERE account_number = ? AND user_id = ?";

            PreparedStatement updateQuery = db.prepareStatement(updateStatement);
            updateQuery.setString(1, balance + "");
            updateQuery.setString(2, accountNo);
            updateQuery.setString(3, id);

            if (updateQuery.executeUpdate() > 0) {
                balance = this.balance(accountNo, id);
                System.out.println("Balance before deposit: " + (balance - amount) + ", after deposit: " + balance + " into account: " + accountNo + ".");
            } else {
                System.out.println("An invalid account number or user_id was used for deposit, account number: " + accountNo + ".");
                throw new InvalidError();
            }  
        } catch(SQLException SE) {
            System.out.println("Error in SQL query for deposit.");
            throw SE;
        }

        return balance;
    }
    
    /**
     * Deposits an amount of money into a given account. Used for transferring money
     * @param accountNo Account number to deposit into
     * @param amount amount to deposit into the account
     * @return double The new balance of the account 
     * 
     * This is a special method used to make a deposit for a transfer into
     * toAccount without verifying the user_id of the toAccount
     * 
     * This mehod will return a double which is balance of that account after transfer,
     * if there is a sql error, it would return -3.0
     * if there is inavlid account number, it would return -2.0
     */
    public double depositForTransfer(String accountNo, double amount) throws InvalidError, SQLException {
        double balance;

        try {
            balance = balanceForTransfer(accountNo) + amount;
            String updateStatement = "UPDATE accounts SET balance = ? WHERE account_number = ?";

            // there should be no id check when we deposit money, as we should be able to make deposit in any account
            PreparedStatement updateQuery = db.prepareStatement(updateStatement);
            updateQuery.setString(1, balance + "");
            updateQuery.setString(2, accountNo);

            if (updateQuery.executeUpdate() > 0) {
                balance = this.balanceForTransfer(accountNo);
                System.out.println("Balance before deposit (transfer): " + (balance - amount) + ", after deposit (transfer): " + balance + " into account: " + accountNo + ".");
            } else {
                System.out.println("An invalid account number or user_id was used for depositTransfer, account number: " + accountNo + ".");
                throw new InvalidError();
            }
        } catch(SQLException SE) { 
            System.out.println("Error in SQL query for depositForTransfer.");
            throw SE;
        }

        return balance;
    }

    /**
     * Transfer money from one account to another account given a client id 
     * @param id Id of the client 
     * @param fromAccountNo Account to transfer from
     * @param toAccountNo Account to transfer to
     * @param amount Amount to transfer
     * @return String The new balance of both accounts 
     * This method is to facilitate transfer from account to another account
     * from account has to be of the person who log in, so we need to verify user_id
     * and hence call withdraw() method to remove money from fromAccount
     * 
     * we have to call depositForTransfer() method to add money to toAccount because we do not
     * need to verify the user_id for the account we make transfer into
     * 
     */
    public synchronized String transfer(String id, String fromAccountNo, String toAccountNo, double amount) throws InvalidError, SQLException, LogicError, DestinationError {
        String fromBalance;
        String toBalance;
        double fromStatus, toStatus;
        
        fromStatus = this.withdraw(id, fromAccountNo, amount);
        fromBalance = Double.toString(fromStatus);
        try {
            toStatus = this.depositForTransfer(toAccountNo, amount);
        } catch(InvalidError IE) {
            throw new DestinationError();
        }
        toBalance = Double.toString(toStatus);

        return fromBalance + " " + toBalance; // This is the balance of the account where we transfered from, and the account where we transfered to
    }
    //#endregion Account methods

    //#region Login and Register methods
    /**
     * Login to the system given user credentials 
     * @param username username of the client to login with
     * @param password password of the client to login with
     * @return String true or false depending if if the login is successful or not
     * This method verifies username and password and returns user_id if both are correct
     * else return a string "fail"
     */
    public String login(String username, String password) {
        String id = new String();

        try {
            String selectStatement = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement selectQuery = db.prepareStatement(selectStatement);
            selectQuery.setString(1, username);
            selectQuery.setString(2, password);

            ResultSet res = selectQuery.executeQuery();
            if (res.next()) {
                id = res.getString("id"); // Return the id of the user if the login is successful.
            } else {
                id = "fail"; // Someone else's account or invalid account number was provided.
            }
        } catch(SQLException e) { 
            System.out.println("Error in SQL query for login: " + e.getStackTrace());
            id = "fail"; // Return a failure, as there was an error in the SQL query.
        }

        return id;
    }
    
    /**
     * Register a new account with the system given user information
     * @param username username to register with
     * @param password password to register with
     * @param firstName firstname to register with
     * @param lastName lastname to register with
     * @param email email to register with
     * @return int id of newly registered user
     */
    public int register(String username, String password, String firstName, String lastName, String email) throws LogicError, SQLException {
        try {
            String selectStatement = "INSERT INTO users (username, password, first_name, last_name, email) VALUES (?,?,?,?,?);";
            PreparedStatement updateQuery = db.prepareStatement(selectStatement);
            updateQuery.setString(1, username);
            updateQuery.setString(2, password);
            updateQuery.setString(3, firstName);
            updateQuery.setString(4, lastName);
            updateQuery.setString(5, email);
            
            int rows = updateQuery.executeUpdate();
            System.out.println(rows + " rows inserted.");

            // Retrieve the generated keys
            ResultSet rs = updateQuery.getGeneratedKeys();
            if (rs.next()) {
               int id = rs.getInt(1);
               return id;
            }

            throw new LogicError();
        } catch(SQLException SE) {
            SE.printStackTrace();
            throw SE;
        }
   }

   /**
    * Register a new user and create a new account associated with the newly registered user
    * @param id Id to register with 
    * @param username username to register with
    * @param password password to register with
    * @param firstName firstname to register with
    * @param lastName lastname  to register with
    * @param email email to register with
    * @param accountNumber New account number with newly registered user
    * @return boolean true or false if the register and create account is successful
    */
    public boolean registerAndCreateAccountForSync(int id, String username, String password, String firstName, String lastName, String email, String accountNumber) throws SQLException {
        try {
            String insertStatement_1 = "INSERT INTO users (id, username, password, first_name, last_name, email) VALUES (?,?,?,?,?,?);";
            PreparedStatement updateQuery_1 = db.prepareStatement(insertStatement_1);
            updateQuery_1.setInt(1, id);
            updateQuery_1.setString(2, username);
            updateQuery_1.setString(3, password);
            updateQuery_1.setString(4, firstName);
            updateQuery_1.setString(5, lastName);
            updateQuery_1.setString(6, email);

            if (updateQuery_1.executeUpdate() < 0) { return false; }

            String insertStatement_2 = "INSERT INTO accounts (account_number, user_id, balance) VALUES (?,?,?);";
            PreparedStatement updateQuery_2 = db.prepareStatement(insertStatement_2);
            updateQuery_2.setString(1, accountNumber);
            updateQuery_2.setInt(2, id);
            updateQuery_2.setString(3, 0.0 + "");

            return updateQuery_2.executeUpdate() > 0;
        } catch(SQLException SE) {
            SE.printStackTrace();
            throw SE;
        }
    }

    /**
     * Create a new account associated with a specific client Id
     * @param id user id to associate new account with 
     * @return String newly created account number 
     */
    public String createAccount(int id) throws SQLException {
        try {
            String selectStatement = "SELECT * FROM accounts WHERE account_number = ?";
            PreparedStatement selectQuery = db.prepareStatement(selectStatement);
            StringBuilder accountNumber;
            
            while (true) {
                accountNumber = new StringBuilder();

                for(int i = 0; i < 10; i++) {
                    accountNumber.append((int) (Math.random() * 10));
                }

                selectQuery.setString(1, accountNumber.toString());
                ResultSet res = selectQuery.executeQuery();

                if (!res.next()) { break; }
            }    

            String insertStatement = "INSERT INTO accounts (account_number, user_id, balance) VALUES (?,?,?);";
            PreparedStatement updateQuery = db.prepareStatement(insertStatement);

            updateQuery.setString(1, accountNumber.toString());
            updateQuery.setInt(2, id);
            updateQuery.setString(3, 0.0 + "");

            if (updateQuery.executeUpdate() > 0) {
                return accountNumber.toString();
            } else {
                return "false";
            }
        } catch(SQLException SE) {
            SE.printStackTrace();
            throw SE;
        }
    }
    //#endregion Login/Register Methods

    //#region Overwrite Database Methods
    /**
     * Creates a dump of the current database, to send to another recovering node.
     * @return String The database dump in SQL format.
     * @throws SQLException
     */
	public String createDatabaseDump() throws SQLException {
	    // Drop all current tables SQL query
	    String dropTablesSQL = "DROP TABLE IF EXISTS accounts;" +
	            "DROP TABLE IF EXISTS users;";
	    
        //Create new tables SQL query
	    String createTablesSQL = "CREATE TABLE users (id INTEGER PRIMARY KEY, username TEXT, password TEXT, first_name TEXT, last_name TEXT, email TEXT);" +
	            "CREATE TABLE accounts (account_number INTEGER PRIMARY KEY, user_id INTEGER, balance REAL, FOREIGN KEY (user_id) REFERENCES users(id));";
	    
	    String dumpSQL = "O\n" +  dropTablesSQL + createTablesSQL;

	    StringBuilder dumpBuilder = new StringBuilder();

	    // Dump the 'accounts' table data
	    try (ResultSet resultSet = db.createStatement().executeQuery("SELECT * FROM accounts")) {
	        ResultSetMetaData metaData = resultSet.getMetaData();
	        int columnCount = metaData.getColumnCount();

	        while (resultSet.next()) {
	            dumpBuilder.append("INSERT INTO accounts VALUES (");

	            for (int i = 1; i <= columnCount; i++) {
	                if (i > 1) {
	                    dumpBuilder.append(",");
	                }
	                dumpBuilder.append("'" + resultSet.getString(i) + "'");
	            }
	            dumpBuilder.append(");");
	        }
	    }

	    // Dump the 'users' table data
	    try (ResultSet resultSet = db.createStatement().executeQuery("SELECT * FROM users")) {
	        ResultSetMetaData metaData = resultSet.getMetaData();
	        int columnCount = metaData.getColumnCount();

	        while (resultSet.next()) {
	            dumpBuilder.append("INSERT INTO users VALUES (");

	            for (int i = 1; i <= columnCount; i++) {
	                if (i > 1) {
	                    dumpBuilder.append(",");
	                }
	                dumpBuilder.append("'" + resultSet.getString(i) + "'");
	            }
	            dumpBuilder.append(");");
	        }
	    }

	    dumpSQL += dumpBuilder.toString();

	    return dumpSQL;
	}
    
    /**
     * This method overwrites the current database with the given database dump (allows for synchronization).
     * @param dump The database dump that will overwrite the current database.
     * @return boolean If the database was successfully overwritten.
     */
    public synchronized boolean overwriteDatabase(String dump) throws SQLException {
        Statement statement;

        try {
            statement = db.createStatement();
            String[] statements = dump.split(";");

            for (String sql : statements) {
                if (!sql.trim().isEmpty()) {
                    try {
                        statement.execute(sql);
                    } catch (SQLException SE) {
                        System.err.println("Error executing SQL statement: " + SE.getMessage());
                        throw SE;
                    }
                }
            }

            System.out.println("Overwrite successful");
            return true;
        } catch (SQLException SE) {
            SE.printStackTrace();
            throw SE;
        }
    }

    /**
     * Updates the balance of the account with the given account number, for situations where the balance is not in sync with the other servers.
     * @param accountNo The account number to be overwritten.
     * @param balance The new balance of the account.
     * @return boolean If the account was successfully overwritten.
     */
	public boolean overwriteAccount(String accountNo, Double balance) throws InvalidError, SQLException {
        System.out.println("Overwriting account: " + accountNo + " with amount " + balance);

        String updateStatement = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        // there should be no id check when we deposit money, as we should be able to make deposit in any account
        PreparedStatement updateQuery;

		try {
			updateQuery = db.prepareStatement(updateStatement);
	        updateQuery.setString(1, balance + "");
	        updateQuery.setString(2, accountNo);

	        if (updateQuery.executeUpdate() > 0) {
	            System.out.println("Overwrote account with new balance : " + balance);
	            return true;
	        } else {
	            System.out.println("Invalid account number for depositForTransfer.");
	            throw new InvalidError();
	        }
		} catch (SQLException SE) {
			SE.printStackTrace();
            throw SE;
		}
	}
    //#endregion Overwrite Database Methods
}