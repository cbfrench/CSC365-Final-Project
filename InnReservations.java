import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.util.Map;
import java.util.Scanner;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

public class InnReservations{
   public static void main(String[] args){
      String jdbcUrl = System.getenv("JDBC_URL");
      String dbUser = System.getenv("JDBC_USER");
      String dbPass = System.getenv("JDBC_PASS");
      Connection conn = null;
      Statement stmt = null;
      try{
         conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
         stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM INN.rooms");
         while (rs.next()){
            String rc = rs.getString("RoomCode");
            String rn = rs.getString("RoomName");
            int bp = rs.getInt("basePrice");
            System.out.format("%3s %30s %d %n", rc, rn, bp);
         }
      }
      catch(Exception e){
         System.out.println(e);
      }
   }
}
