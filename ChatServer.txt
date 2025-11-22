import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class ChatServer {

    // خريطة فيها اليوزرنيم → ClientHandler (المتصلين حالياً)
    private static Map<String, ClientHandler> onlineUsers =
            Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        try (ServerSocket server = new ServerSocket(3000)) {
            System.out.println("Chat server running on port 3000...");

            while (true) {
                Socket soc = server.accept();
                System.out.println("New client connected");
                ClientHandler ch = new ClientHandler(soc);
                ch.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Thread لكل عميل
    static class ClientHandler extends Thread {
        private Socket soc;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private boolean loggedIn = false;

        public ClientHandler(Socket s) {
            this.soc = s;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(soc.getInputStream()));
                out = new PrintWriter(soc.getOutputStream(), true);

                // ====== مرحلة تسجيل الدخول ======
                out.println("WELCOME TO SIMPLE CHAT");
                out.println("ENTER USERNAME:");
                String user = in.readLine();
                out.println("ENTER PASSWORD:");
                String pass = in.readLine();

                if (!DatabaseHelper.checkLogin(user, pass)) {
                    out.println("LOGIN_FAILED");
                    System.out.println("Login failed for " + user);
                    closeEverything();
                    return;
                }

                this.username = user;
                loggedIn = true;
                onlineUsers.put(username, this);

                out.println("LOGIN_OK");
                System.out.println(username + " logged in.");

                // نجيب كونتاكتس هذا اليوزر من الـ DB
                List<String> myContacts = DatabaseHelper.getContacts(username);

                // نرسل له قائمة الكونتاكتس الأونلاين الآن
                out.println("YOUR CONTACTS ONLINE:");
                synchronized (onlineUsers) {
                    for (String c : myContacts) {
                        if (onlineUsers.containsKey(c)) {
                            out.println("  - " + c);
                        }
                    }
                }
                out.println("END_CONTACTS");

                // نبلّغ أصحابه الأونلاين إنه دخل
                notifyContactsHeIsOnline(myContacts);

                // ====== مرحلة الشات ======
                out.println("You can chat now.");
                out.println("Use: MSG <friendName> <your message>");
                out.println("Type LOGOUT to exit.");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.equalsIgnoreCase("LOGOUT")) {
                        break;
                    }

                    if (line.startsWith("MSG ")) {
                        // MSG friendName text....
                        handleMessage(line.substring(4)); // بدون "MSG "
                    } else {
                        out.println("Unknown command. Use: MSG <friend> <message> or LOGOUT.");
                    }
                }

            } catch (IOException e) {
                System.out.println("IO error with client: " + e.getMessage());
            } finally {
                logoutAndClose();
            }
        }

        private void handleMessage(String body) {
            // نفصل أول كلمة (اسم الصديق) عن باقي الرسالة
            String[] parts = body.split(" ", 2);
            if (parts.length < 2) {
                out.println("Invalid MSG format. Use: MSG <friend> <message>");
                return;
            }

            String friendName = parts[0];
            String msgText    = parts[1];

            ClientHandler friendHandler;
            synchronized (onlineUsers) {
                friendHandler = onlineUsers.get(friendName);
            }

            if (friendHandler != null && friendHandler.loggedIn) {
                friendHandler.out.println("[FROM " + username + "]: " + msgText);
                out.println("[TO " + friendName + "]: " + msgText);
            } else {
                out.println(friendName + " is not online now.");
            }
        }

        private void notifyContactsHeIsOnline(List<String> myContacts) {
            synchronized (onlineUsers) {
                for (String c : myContacts) {
                    ClientHandler ch = onlineUsers.get(c);
                    if (ch != null && ch.loggedIn) {
                        ch.out.println("NOTIFY: your friend " + username + " is now ONLINE.");
                    }
                }
            }
        }

        private void logoutAndClose() {
            if (username != null) {
                onlineUsers.remove(username);
                System.out.println(username + " logged out.");
            }
            closeEverything();
        }

        private void closeEverything() {
            try {
                if (in != null)  in.close();
                if (out != null) out.close();
                if (soc != null && !soc.isClosed()) soc.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
