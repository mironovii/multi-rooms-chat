import java.util.Scanner;

public class Main {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";


    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);

        System.out.println("Start Server or client? S(Server) / C (client)");
        if(in.hasNext()){
        char answer = Character.toLowerCase((in.nextLine().charAt(0)));
            if (answer == 's') {
                new Server();
            } else if (answer == 'c') {
                new Client();
            } else {
                System.out.println("Incorrect input character.");
            }
        }
    }
}