# Distributed Banking System

This project consists of 4 main sub-project folders: client_i, client_j, client_k, server_i, server_j, server_k, and server_read. Each of these folders are its own project.

# How to run:

1. Ensure you have the latest Java version and Java JDK 17 for compilation.
2. Clone the repository on your local machine.
3. You may run the project in one of three ways, using Eclipse, VSCode, or Terminal.

<ins>Eclipse</ins> <br/>

1. Open eclipse and import the distributed-banking-system project. Ensure all sub-projects are also imported correctly (The subprojects are client_i, client_j, client_k, server_i, server_j, server_k, and server_read).
2. You must first run each server. There are a total of 4 servers: server_i, server_j, server_k, server_read.
3. To start server_i, open the src folder for, then open the server_i folder. Run the `ServerDriver.java` file, which contains the main method for the server. You will have a total of 4 servers running. Repeat these steps for server_j, server_k, and server_read.
4. Now, navigate to any of client_i, client_j, or client_k folders, you may run the `Client.java`to run the client. You may have one client running or multiple clients running.
5. Once the client is running, you may login using the credentials found in the database in the root directory, and test all transaction methods (deposit, transfer, withdraw, view balance). You may also stop servers and re run them to test fault tolerance, synchronization and consistency.

<ins>VS code</ins> <br />
Please follow steps 2-6 from above to run the project in VS code.

<ins>Terminal</ins> <br />

1. In each of the sub-projects (The subprojects are client_i, client_j, client_k, server_i, server_j, server_k, and server_read), you must first run each server. There are a total of 4 servers: server_i, server_j, server_k, server_read.
2. To start server_i, open the src folder for, then open the server_i folder. Compile all files in this folder using `javac *.java`, and run the ServerDriver.java file using `java ServerDriver`.
3. Repeat the above step for server_j, server_k, and server_read. You will now have a total of 4 servers running on 4 different terminals.
4. Now, navigate to any of client_i, client_j, or client_k folders, and you have to compile all the Client files using `javac *.java`, and run the main Client class using `java Client`. You may have one client running or multiple clients running.
5. Once the client is running, you may login using the credentials found in the database in the root directory, and test all transaction methods (deposit, transfer, withdraw). You may also stop servers and re run them to test fault tolerance, synchronization and consistency.
