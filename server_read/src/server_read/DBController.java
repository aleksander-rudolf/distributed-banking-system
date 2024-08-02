package server_read;

import Errors.*;
import java.sql.*;

public class DBController {
    // #region Class Variables
    private String DBURL = new String();
    private static DBController dbController = null;
    private Connection db;
    // #endregion Class Variables

    // #region Constructors & Instance Methods
    /**
     * Constructor
     * 
     * @implNote This constructor is private to ensure that only one instance of
     *           this class is created.
     */
    private DBController() {
        this.DBURL = "jdbc:sqlite:bankmanager_read.db"; // This is the path to the database file.
        connect();
    }

    /**
     * Returns the instance of this class.
     * 
     * @return DBController The instance of this class.
     */
    public static DBController getInstance() {
        if (dbController == null) {
            dbController = new DBController();
        }
        return dbController;
    }
    // #endregion Constructors & Instance Methods

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
        } catch (Exception e) {
            System.out.println("Connection to SQLite has failed.");
            System.exit(1); // Exit the program if the connection fails, as it is required for the program
                            // to run.
        }
    }

    /**
     * Close the connection to the MySQL database.
     */
    public void close() {
        try {
            this.db.close();
        } catch (SQLException e) {
            System.out.println("Connection closure to SQLite has failed.");
        }
    }

    // #region Misc Methods
    /**
     * This method will get the balace of an account given the uesr id
     * 
     * @param accountNo Account number to check balance of
     * @param id        Id of the client
     * @return double The balance of the account
     *         This mehod will return a double which is balance of that account,
     *         if there is a sql error, it would return -3.0
     *         if there is inavlid account number, it would return -2.0
     */
    public double balance(String accountNo, String id) throws InvalidError, SQLException {
        double balance;

        try {
            String selectStatement = "SELECT * FROM accounts WHERE account_number = ? AND user_id = ?";
            PreparedStatement selectQuery = db.prepareStatement(selectStatement);

            selectQuery.setString(1, accountNo);
            selectQuery.setString(2, id);
            ResultSet res = selectQuery.executeQuery();

            if (res.next()) { // this means account and user_id were valid
                balance = res.getDouble("balance");
                System.out.println("Balance of account #" + accountNo
                        + " was successfully retrieved, with a balance of " + balance + ".");
            } else {
                System.out.println("An invalid account number or user_id was used for balance, account number: "
                        + accountNo + ".");
                throw new InvalidError();
            }
        } catch (SQLException e) {
            System.out.println("Error in SQL query for balance.");
            throw new SQLException();
        }

        return balance;
    }

    /**
     * This method will alter the balance of an account given the uesr id
     * 
     * @param id             Id of the client
     * @param Account        Account number to alter
     * @param amountToUpdate New amount to alter the account with
     * @return the new balance of the account
     */
    public double alterAccount(String Account, double amountToUpdate) throws InvalidError, SQLException {
        double balance = -1.0;
        try {
            String selectStatement = "SELECT * FROM accounts WHERE account_number = ?";
            PreparedStatement selectQuery = db.prepareStatement(selectStatement);
            selectQuery.setString(1, Account);
            ResultSet res = selectQuery.executeQuery();

            if (res.next()) {
                balance = amountToUpdate;
                String updateStatement = "UPDATE accounts SET balance = ? WHERE account_number = ?";
                PreparedStatement updateQuery = db.prepareStatement(updateStatement);

                updateQuery.setDouble(1, balance);
                updateQuery.setString(2, Account);
                updateQuery.executeUpdate();
            } else {
                // some one else's account or invalid account number
                throw new InvalidError();
            }
        } catch (SQLException SE) {
            SE.printStackTrace();
            throw SE;
        }
        return balance;
    }

    /**
     * Login to the system given user credentials
     * 
     * @param username username of the client to login with
     * @param password password of the client to login with
     * @return true or false depending if if the login is successful or not
     *         This method verifies username and password and returns user_id if
     *         both are correct
     *         else return a string "fail"
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
        } catch (SQLException e) {
            System.out.println("Error in SQL query for login: " + e.getStackTrace());
            id = "fail"; // Return a failure, as there was an error in the SQL query.
        }

        return id;
    }
}