import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    // عدل الإعدادات حسب جهازك
    private static final String URL = "jdbc:mysql://localhost:3306/jic_chat?useSSL=false";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // يرجع true لو اليوزر/باسورد صحيحة
    public static boolean checkLogin(String username, String password) {
        boolean ok = false;
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM users WHERE username=? AND password=?")) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ok = true;
            }
        } catch (SQLException e) {
            System.out.println("DB Error (login): " + e.getMessage());
        }
        return ok;
    }

    // يرجع قائمة بأسماء أصحاب هذا اليوزر من جدول contacts
    public static List<String> getContacts(String username) {
        List<String> list = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT friend FROM contacts WHERE owner=?")) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("friend"));
            }
        } catch (SQLException e) {
            System.out.println("DB Error (contacts): " + e.getMessage());
        }
        return list;
    }
}
