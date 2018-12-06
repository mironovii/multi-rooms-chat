import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private class ClientHandler implements Runnable {
        private Server server;
        private PrintWriter out;
        private BufferedReader in;
        private Socket clientSocket;
        private String clientName = "";
        private String roomAdmin = "";
        private boolean inRoom = false;

        ClientHandler(Socket socket, Server server) {
            try {
                this.server = server;
                this.clientSocket = socket;
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            serverMsg(Main.ANSI_GREEN +
                    "Welcome to localhost server! Please, login or signup to our server. (e.g. /signup , /login, /help)." + Main.ANSI_RESET);

            try {
                while (true) {
                    if (in.ready()) {
                        String clientMessage = in.readLine();
                        if (clientMessage.equalsIgnoreCase("exit")) break;
                        if ((clientMessage.toCharArray())[0] == '/') {
                            parser(clientMessage);
                            continue;
                        }
                        if (this.getInRoom()) {
                            server.sendMsgToClientsInRooms(clientMessage, this.getRoomAdmin(), this.getClientName());
                        } else {
                            server.sendMsgToAllClients(clientMessage, this.getClientName());
                        }
                    }
                    Thread.sleep(100);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                this.close(false);
            }
        }

        public void parser(String expression) {
            String[] command = expression.split(" ");

            switch (command[0]) {
                case "/signup":
                    System.out.println("in SIGNUP");
                    if (command.length == 3 && this.getClientName().equals("") && !regClients.containsKey(command[1])) {
                        String username = command[1];
                        String password = command[2];

                        clientName = username;
                        regClients.put(username, password);
                        currentClients.put(username, this);

                        server.sendMsgToAllClients(Main.ANSI_GREEN + "User " + username.toUpperCase() +
                                " add to chart. Clients in chart = " + currentClients.size() + Main.ANSI_RESET, "server");

                    } else serverMsg(Main.ANSI_RED + "This name is already taken. Try again." + Main.ANSI_RESET);
                    break;
                case "/login":
                    System.out.println("in LOGIN");
                    if (command.length == 3) {
                        String username = command[1];
                        String password = command[2];
                        if (regClients.containsKey(username) && regClients.get(username).equals(password)) {
                            clientName = username;
                            currentClients.put(username, this);

                            server.sendMsgToAllClients(Main.ANSI_GREEN + "User " + getClientName().toUpperCase() +
                                    " add to chart. Clients in chart = " + currentClients.size() + Main.ANSI_RESET, "server");
                        } else serverMsg(Main.ANSI_RED + "Invalid username or password. Try again." + Main.ANSI_RESET);
                    } else serverMsg(Main.ANSI_RED + "Invalid length of command. Try again." + Main.ANSI_RESET);
                    break;
                case "/join":
                    System.out.println("in JOIN");
                    String caller = this.getClientName();
                    String room_admin = command[1];
                    if (command.length == 2 && currentClients.containsKey(caller)) {
                        if (currentClients.containsKey(room_admin)) {
                            currentClients.get(room_admin).serverMsg(Main.ANSI_GREEN + this.getClientName().toUpperCase() +
                                    " add you to a private chat. Get out of private chat - /exitroom" + Main.ANSI_RESET);
                            serverMsg(Main.ANSI_GREEN + "Private chat created successfully. Get out of private chat - /exitroom" + Main.ANSI_RESET);

                            ArrayList<ClientHandler> clientsInRoomList = new ArrayList<>();
                            clientsInRoomList.add(currentClients.get(room_admin));
                            clientsInRoomList.add(currentClients.get(caller));

                            currentRooms.put(room_admin, clientsInRoomList);

                            currentClients.get(room_admin).setInRoom(true);
                            this.setInRoom(true);

                            currentClients.get(room_admin).setRoomAdmin(room_admin);
                            this.roomAdmin = room_admin;

                            currentClients.remove(room_admin);
                            currentClients.remove(caller);

                        } else if (currentRooms.containsKey(room_admin)) {
                            serverMsg(Main.ANSI_GREEN + "You in a private chart: " + room_admin.toUpperCase() + ".\n Get out of private chat - /exitroom" + Main.ANSI_RESET);

                            this.setInRoom(true);
                            this.setRoomAdmin(room_admin);

                            currentRooms.get(room_admin).add(this);
                            currentClients.remove(caller);

                            sendMsgToClientsInRooms(Main.ANSI_YELLOW + caller.toUpperCase() + Main.ANSI_GREEN +
                                    " has been added to the chat. Clients in chart = " + currentRooms.get(room_admin).size() + Main.ANSI_RESET, room_admin, "server");
                        }
                    } else {
                        serverMsg(Main.ANSI_RED + "Please, login/signup to our server or exit in current private room." + Main.ANSI_RESET);
                    }
                    break;
                case "/listusers":
                    if (currentClients.containsKey(this.getClientName()) || this.getInRoom()) {
                        serverMsg(Main.ANSI_GREEN + "List users in public chat :" + Main.ANSI_RESET);
                        currentClients.forEach((k, v) -> serverMsg(Main.ANSI_BLUE + k + Main.ANSI_RESET));
                        if (currentRooms.size() != 0) {
                            serverMsg(Main.ANSI_GREEN + "List users in private rooms :" + Main.ANSI_RESET);
                            currentRooms.forEach((k, v) -> {
                                for (ClientHandler o : v)
                                    serverMsg(Main.ANSI_BLUE + o.getClientName() + Main.ANSI_RESET);
                            });
                        } else {
                            serverMsg(Main.ANSI_GREEN + "No one users in rooms at the moment." + Main.ANSI_RESET);
                        }
                    } else {
                        serverMsg(Main.ANSI_RED + "Please, login or signup to our server. (e.g. /signup , /login)." + Main.ANSI_RESET);
                    }
                    break;
                case "/listrooms":
                    if (currentClients.containsKey(this.getClientName()) || this.getInRoom()) {
                        if (currentRooms.size() != 0) {
                            serverMsg(Main.ANSI_GREEN + "List rooms :" + Main.ANSI_RESET);
                            currentRooms.forEach((k, v) -> serverMsg(Main.ANSI_CYAN + k));
                        } else {
                            serverMsg(Main.ANSI_RED + "No one room at the moment. You can create it - /join username" + Main.ANSI_RESET);
                        }
                    } else {
                        serverMsg(Main.ANSI_RED + "Please, login or signup to our server. (e.g. /signup , /login)." + Main.ANSI_RESET);
                    }
                    break;
                case "/chg_pass":
                    if (currentClients.containsKey(this.getClientName())) {
                        serverMsg(Main.ANSI_GREEN + "Enter a new password:" + Main.ANSI_RESET);
                        try {
                            if (in.ready()) {
                                regClients.put(this.getClientName(), in.readLine());
                            } else {
                                serverMsg(Main.ANSI_RED + "Password cannot be empty!" + Main.ANSI_RESET);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        serverMsg(Main.ANSI_RED + "Please, login or signup to our server. (e.g. /signup , /login)." + Main.ANSI_RESET);
                    }
                    break;
                case "/exitroom":
                    System.out.println("in EXITROOM");
                    if (this.inRoom) {
                        sendMsgToClientsInRooms(Main.ANSI_YELLOW + this.getClientName().toUpperCase() + Main.ANSI_GREEN +
                                " left room. Clients in chart = " + currentRooms.get(this.getRoomAdmin()).size() + Main.ANSI_RESET, this.getRoomAdmin(), "server");
                        removeClient(this, "exitroom");
                        this.setInRoom(false);
                        this.setRoomAdmin("");
                    } else {
                        serverMsg(Main.ANSI_RED + "You are already in a public chat." + Main.ANSI_RESET);
                    }
                    break;
                case "/help":
                    serverMsg(Main.ANSI_RESET + "All commands:\n" + Main.ANSI_GREEN +
                            "/signup username password\n" +
                            "/login username password\n" +
                            "/join username\n" +
                            "/listusers\n" +
                            "/listrooms\n" +
                            "/chg_pass\n" +
                            "/exitroom\n" + Main.ANSI_RESET);
                    break;
                default:
                    serverMsg(Main.ANSI_RED + "Invalid command. Please, try again." + Main.ANSI_RESET);
                    break;
            }
        }

        public void sendClientMsg(String msg, String senderName) {
            try {
                out.println(Main.ANSI_YELLOW + senderName.toUpperCase() + Main.ANSI_RESET + ": " + msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void serverMsg(String msg) {
            try {
                out.println(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void close(Boolean serverStopped) {
            if (!serverStopped) {
                if (this.inRoom) {
                    server.sendMsgToClientsInRooms(Main.ANSI_YELLOW + this.getClientName().toUpperCase() + Main.ANSI_RESET +
                            (this.getClientName().equals(this.getRoomAdmin()) ?
                                    Main.ANSI_GREEN + " left chat. It was admin, so all users will be returned to the public chat." + Main.ANSI_RESET
                                    : Main.ANSI_GREEN + " left chat." + Main.ANSI_RESET), this.getRoomAdmin(), "server");
                } else {
                    server.sendMsgToAllClients(Main.ANSI_YELLOW + this.getClientName().toUpperCase() + Main.ANSI_RESET +
                            Main.ANSI_GREEN + " left chat." + Main.ANSI_RESET, "server");
                }
                server.removeClient(this, "exit");
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getRoomAdmin() {
            return roomAdmin;
        }

        public void setRoomAdmin(String roomAdmin) {
            this.roomAdmin = roomAdmin;
        }

        public String getClientName() {
            return this.clientName;
        }

        public void setInRoom(boolean inRoom) {
            this.inRoom = inRoom;
        }

        public boolean getInRoom() {
            return inRoom;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final int PORT = 5555;
    private Map<String, String> regClients;
    private Map<String, ClientHandler> currentClients;
    private Map<String, ArrayList<ClientHandler>> currentRooms;
    private BufferedReader inServer;

    public Server() {
        regClients = new HashMap<>();
        currentClients = new HashMap<>();
        currentRooms = new ConcurrentHashMap<>();
        inServer = new BufferedReader(new InputStreamReader(System.in));

        regClients.put("admin", "admin");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println(Main.ANSI_GREEN + "Server start. To stop server enter: /stop " + Main.ANSI_RESET);

            new Thread(() -> {
                while (true) {
                    try {
                        if (inServer.ready()) {
                            if (inServer.readLine().equalsIgnoreCase("/stop")) {
                                System.out.println(Main.ANSI_RED + "Start close server");
                                closeConnections();
                                System.exit(0);
                            }
                        }
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler client = new ClientHandler(clientSocket, this);
                new Thread(client).start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void sendMsgToAllClients(String msg, String senderName) {
        if (senderName.equals("server")) {
            currentClients.forEach((k, v) -> v.serverMsg(msg));
        } else {
            currentClients.forEach((k, v) -> v.sendClientMsg(msg, senderName));
        }
    }

    private void sendMsgToClientsInRooms(String msg, String roomName, String senderName) {
        currentRooms.forEach((k, v) -> {
            if (k.equals(roomName)) {
                if (senderName.equals("server")) {
                    for (ClientHandler o : v) o.serverMsg(msg);
                } else {
                    for (ClientHandler o : v) o.sendClientMsg(msg, senderName);
                }
            }
        });
    }

    private void removeClient(ClientHandler client, String exitFlag) {
        if (client.inRoom) {
            String roomname = client.getRoomAdmin();
            currentRooms.forEach((k, v) -> {
                        if (k.equals(roomname)) {
                            if (exitFlag.equalsIgnoreCase("exit")) {
                                v.remove(client);
                            } else if ((exitFlag.equalsIgnoreCase("exitroom"))) {
                                currentClients.put(client.getClientName(), v.get(v.indexOf(client)));
                                v.remove(client);
                                client.setInRoom(false);
                                client.setRoomAdmin("");
                            }
                            if (v.size() == 1 || client.getClientName().equals(roomname)) {
                                for (ClientHandler o : v) {
                                    o.serverMsg(Main.ANSI_GREEN + "You will be replaced to the public chat because the" +
                                            " admin has left the room or you are left alone in the chat."+ Main.ANSI_RESET);
                                    o.setInRoom(false);
                                    o.setRoomAdmin("");
                                    currentClients.put(o.getClientName(), o);
                                }
                                currentRooms.remove(roomname);
                            }
                        }
                    }
            );
        } else {
            currentClients.remove(client.getClientName());
        }
    }

    private void closeConnections() {
        currentClients.forEach((k, v) -> {
            v.serverMsg("##Server##stopped##");
            v.close(true);
        });
        currentRooms.forEach((k, v) -> {
            for (ClientHandler o : v) {
                o.serverMsg("##Server##stopped##");
                o.close(true);
            }
        });
    }
}