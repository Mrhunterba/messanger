import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {
        Scanner kb = new Scanner(System.in);

        try {
            Socket soc = new Socket("localhost", 3000);
            System.out.println("Connected to chat server.");

            BufferedReader in  = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            PrintWriter out = new PrintWriter(soc.getOutputStream(), true);

            // Thread يستقبل الرسائل من السيرفر ويطبعها
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("[SERVER] " + line);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            reader.start();

            // نرسل المدخلات من الكيبورد للسيرفر
            while (true) {
                String cmd = kb.nextLine();
                out.println(cmd);
                if (cmd.equalsIgnoreCase("LOGOUT")) {
                    break;
                }
            }

            soc.close();
            System.out.println("Client closed.");

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
