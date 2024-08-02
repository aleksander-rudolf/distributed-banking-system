package client_k;

import java.security.SecureRandom;

public class Client {
    private static SecureRandom random = new SecureRandom();
    private static final int upperBound = 1000000;
    private static SocketManager socketManager = null;
    private boolean isLoggedIn = false;
    private String userId = null;

    /**
     * Generates a random number.
     * @return int The random number.
     */
    public int generateRandomNumber() {
        return random.nextInt(upperBound);
    }
    
    /**
     * Validates the user's input.
     * @param min Minimum value to accept.
     * @param max Maximum value to accept.
     * @return int The user's input.
     */
    public int parseUserInput(int min, int max) {
        int option = 0;
        while (option < min || option > max) {
            option = Integer.parseInt(System.console().readLine());
        }

        return option;
    }

    /**
     * The transfer method of the Client class.
     */
    public void transfer() {
        System.out.println("Please enter the account number you wish to transfer FROM:");
        String accountFrom = System.console().readLine();
        System.out.println("Please enter the account number you wish to transfer TO:");
        String accountTo = System.console().readLine();

        System.out.println("Please enter the amount you wish to transfer:");
        String amount = System.console().readLine();

        try {
            Integer.parseInt(accountFrom);
            Integer.parseInt(accountTo);
            Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            System.out.println("Account numbers and amount must be a number");
            return;
        }

        System.out.println("Transferring...");
        String response = socketManager.query(true, userId, "T " + accountFrom + " " + accountTo + " " + amount + " " + generateRandomNumber());
        System.out.println(response);
    }

    /**
     * The deposit method of the Client class.
     */
    public void deposit() {
        System.out.println("Please enter the account you wish to deposit to:");
        String accountNo = System.console().readLine();
        System.out.println("Please enter the amount you wish to deposit:");
        String amount = System.console().readLine();

        try {
            Integer.parseInt(accountNo);
            Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            System.out.println("Account number and amount must be a number\n");
            return;
        }

        System.out.println("Depositing...");
        String response = socketManager.query(true, userId, "D " + accountNo + " " + amount + " " + generateRandomNumber());
        System.out.println(response);
    }

    /**
     * The withdraw method of the Client class.
     */
    public void withdraw() {
        System.out.println("Please enter the account you wish to withdraw from:");
        String accountNo = System.console().readLine();

        System.out.println("Please enter the dollar amount you wish to withdraw:");
        String amount = System.console().readLine();

        try {
            Integer.parseInt(accountNo);
            Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            System.out.println("Account number and amount must be a number\n");
            return;
        }

        System.out.println("Withdrawing...");
        String response = socketManager.query(true, userId, "W " + accountNo + " " + amount + " " + generateRandomNumber());
        System.out.println(response);
    }

    /**
     * The balance method of the Client class.
     */
    public void balance() {
        System.out.println("Please enter account number to check balance...");
        String account = System.console().readLine();

        try {
            Integer.parseInt(account);
        } catch (NumberFormatException e) {
            System.out.println("Account number must be a number\n");
            return;
        }

        String response = socketManager.query(false, userId, "B " + userId + " " + account);
        System.out.println(response);
    }

    /**
     * The logout method of the Client class.
     */
    public void logout() {
        System.out.println("Are you sure you want to logout?");

        System.out.println("1. Yes");
        System.out.println("2. No");

        int option = parseUserInput(1, 2);

        switch (option) {
            case 1:
                System.out.println("Logging out...");
                isLoggedIn = false;
                menu();
                break;
            case 2:
                accountMenu();
                break;
        }
    }

    /**
     * The accountMenu method of the Client class.
     */
    public void accountMenu() {
        System.out.println("Please select an option:");
        System.out.println("1. Transfer");
        System.out.println("2. Deposit");
        System.out.println("3. Withdraw");
        System.out.println("4. Balance");
        System.out.println("5. Logout");

        int option = parseUserInput(1, 5);

        switch (option) {
            case 1:
                transfer();
                break;
            case 2:
                deposit();
                break;
            case 3:
                withdraw();
                break;
            case 4:
                balance();
                break;
            case 5:
                logout(); // Could just call menu() here.
                break;
        }
    }

    /**
     * The login method of the Client class.
     */
    public void login() {
        System.out.println("Please enter your username:");
        String username = System.console().readLine();

        System.out.println("Please enter your password:");
        String password = System.console().readLine();

        System.out.println("Logging in...");
        String response = socketManager.query(true, "", "L " + username + " " + password);

        if (response.equals("Incorrect username or password!")) {
            System.out.println("Login failed!");
            isLoggedIn = false;
        } else {
            String[] split = response.split("%");
        	userId = split[0];
            if (split[1].equals("OK")) {
                System.out.println("Login successful!");
                isLoggedIn = true;
            } else { // It should never go in this else
                System.out.println("Login failed!");
                isLoggedIn = false;
            }
        }        
        
        if (isLoggedIn) {
            accountMenu();
        } else {
            menu();
        }
    }

    /**
     * The register method of the Client class.
     */
    public void register() {
        System.out.println("Please enter your desired username:");
        String username = System.console().readLine();

        System.out.println("Please enter your desired password:");
        String password = System.console().readLine();

        System.out.println("Please enter your first name:");
        String firstName = System.console().readLine();

        System.out.println("Please enter your last name:");
        String lastName = System.console().readLine();

        System.out.println("Please enter your email:");
        String email = System.console().readLine();

        System.out.println("Registering...");
        String response = socketManager.query(true, "", "R " + username + " " + password + " " + firstName + " " + lastName + " " + email);
        System.out.println(response);
        
        menu();
    }

    /**
     * The exit method of the Client class.
     */
    public void exit() {
        System.out.println("Are you sure you want to exit?");
        System.out.println("1. Yes");
        System.out.println("2. No");

        int option = parseUserInput(1, 2);

        switch (option) {
            case 1:
                System.out.println("Exiting...");
                System.exit(0);
                break;
            case 2:
                menu();
                break;
        }
    }

    /**
     * The menu method of the Client class.
     */
    public void menu() {
        /*
         * Main menu for a banking client.
         * - Login
         *  - Account
         *  - Transfer
         *  - Deposit
         *  - Withdraw
         *  - Balance
         * - Register
         * - Exit
         * 
         * Above are all the methods, move them into a separate class. It's chaotic right now.
         * But at least the general idea is there.
         */

        System.out.println("Welcome to the banking client!");
        System.out.println("Please select an option:");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Exit");

        int option = parseUserInput(1, 3);

        switch (option) {
            case 1:
                login();
                break;
            case 2:
                register();
                break;
            case 3:
                exit();
                break;
        }
    }

    /**
     * Checks if the user is logged in.
     * @return boolean Returns true if the user is logged in, false otherwise.
     */
    public boolean getIsLoggedIn() {
    	return isLoggedIn;
    }

    /**
     * The main method of the Client class.
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        socketManager = new SocketManager();

        Client client  = new Client();
        while (true) {
        	if (client.getIsLoggedIn()) {
        		client.accountMenu();
        	} else {
        		client.menu();
        	}
        }
    }
}