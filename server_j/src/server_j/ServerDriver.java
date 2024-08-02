package server_j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

public class ServerDriver {
    //#region Class Variables
    private static final int TERM_WAIT_TIME = 10 * 1000; // wait 10 seconds for server process to terminate
    private static final String SERVER_PORT_NUM = "2025";
    private static final String IDLE_TIMEOUT = "0"; // idle connection timeout in milli-seconds, 0 means infinity
    //#endregion Class Variables

    //#region Methods
    /**
     * Main method of the program.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        // Parse command line args
        HashMap<String, String> params = parseCommandLine(args);
         
        // Set the parameters
        int serverPort = Integer.parseInt(params.getOrDefault("-p", SERVER_PORT_NUM));
        int conTimeout = Integer.parseInt(params.getOrDefault("-t", IDLE_TIMEOUT));

        // Redirect out to our custom print stream.
        CustomPrintStream customOut = new CustomPrintStream(System.out);
        System.setOut(customOut);

        // Standard output
        System.out.println("Starting server on port " + serverPort + " with a connection timeout of " + conTimeout + "ms.");
        MachineSocket server = MachineSocket.getInstance(serverPort, conTimeout);
         
        // Start the server
        server.start();
        System.out.println("Server has been successfully started! Type 'quit' to stop the server.");
        System.out.println("Waiting for client connections...");

        // Wait for quit command
        waitForQuit(server);

        try {
            // shutdown the server
            System.out.println("Server is shutting down...");
            server.shutdown();
            server.join(TERM_WAIT_TIME);
            System.out.println("Server stopped.");
        } catch (InterruptedException e) {
            System.out.println("Server did not shutdown properly.");
        }
 
        System.exit(0);
    }
 
    /**
     * Waits for the user to type 'quit' in the console.
     * @param server The server thread.
     */
    private static void waitForQuit(Thread server) {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        try {
            while (server.isAlive()) {
                if (console.ready() && console.readLine().equals("quit")) { // Avoids blocking the console.
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading from console.");
        }
    }
 
    /**
     * Parses the command line arguments.
     * @param args The command line arguments.
     * @return HashMap<String, String> The parsed command line arguments.
     */
    private static HashMap<String, String> parseCommandLine(String[] args) {
        HashMap<String, String> params = new HashMap<String, String>();
 
        int i = 0;
        while ((i + 1) < args.length) {
            params.put(args[i], args[i + 1]);
            i += 2;
        }
         
        return params;
    } 
    //#endregion Methods
}