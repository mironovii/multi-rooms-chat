import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5555;

    private Socket clientSocket;
    private BufferedReader inMessage;
    private PrintWriter outMessage;
    private BufferedReader inUser;

    Client() {
        try {
            clientSocket = new Socket(SERVER_HOST, SERVER_PORT);
            inMessage = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outMessage = new PrintWriter(clientSocket.getOutputStream(), true);
            inUser = new BufferedReader(new InputStreamReader(System.in));

            new Thread(() -> {
                while (!clientSocket.isInputShutdown()) {
                    try {
                        String serverMes;
                        if (inMessage.ready()) {
                            serverMes = inMessage.readLine();
                            if (serverMes.equals("##Server##stopped##")) {
                                System.out.println("Server was stopped. Exit.");
                                closeConnections();
                                System.exit(1);
                            }
                            System.out.println(serverMes);
                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            String answer;
            while (!clientSocket.isOutputShutdown()) {
                if(inUser.ready()) {
                    answer = inUser.readLine();
                    outMessage.println(answer);
                    if (answer.equalsIgnoreCase("exit")) break;
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
            System.exit(0);
        }
    }

    private void closeConnections() {
        try {
            inUser.close();
            inMessage.close();
            outMessage.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

