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
   // FR-1
   public static void roomsAndRates(Connection conn){
      Statement stmt = null;
      try{
         stmt = conn.createStatement();
         String query = "select RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor, popularity, nextCheckIn, lastCheckOut, lastStay from ( select D.RoomCode, D.RoomName, D.Beds, D.bedType, D.maxOcc, D.basePrice, D.decor, D.popularity, D.CheckOut nextCheckIn, E.CheckOut lastCheckOut, DATEDIFF(E.CheckOut, E.CheckIn) lastStay, rank() over (partition by RoomCode order by E.CheckOut desc) revDateRank from ( select * from ( select *, rank() over (partition by RoomCode order by CheckOut) dateRank from ( select *, round(times/180,2) popularity from ( select Room selectedRoom, count(*) times from lab7_reservations where DATEDIFF(CURRENT_DATE, CheckIn) <= 180 group by selectedRoom ) as A join lab7_rooms on RoomCode=selectedRoom ) as B join lab7_reservations on Room=RoomCode where CheckOut > CURRENT_DATE ) as C where dateRank=1 ) as D join ( select * from lab7_reservations r where CheckOut < CURRENT_DATE ) as E on E.Room=RoomCode order by selectedRoom, revDateRank ) as F where revDateRank=1 order by popularity desc";
         ResultSet rs = stmt.executeQuery(query);
         System.out.format("| %-8s | %-30s | %-4s | %-10s | %-6s | %-9s | %-15s | %-4s | %11s | %12s | %9s |%n", "RoomCode", "RoomName", "Beds", "bedType", "maxOcc", "basePrice", "decor", "pop", "nextCheckIn", "lastCheckOut", "lastStay");
         while(rs.next()){
            String roomCode = rs.getString("RoomCode");
            String roomName = rs.getString("RoomName");
            int beds = rs.getInt("Beds");
            String bedType = rs.getString("bedType");
            int maxOcc = rs.getInt("maxOcc");
            int basePrice = rs.getInt("basePrice");
            String decor = rs.getString("decor");
            float p = rs.getFloat("popularity");
            String nextCheckIn = rs.getString("nextCheckIn");
            String lastCheckOut = rs.getString("lastCheckOut");
            int lastStay = rs.getInt("lastStay");
            System.out.format("| %-8s | %-30s | %4d | %-10s | %6d | %9d | %-15s | %.2f | %11s | %12s | %9d |%n", roomCode, roomName, beds, bedType, maxOcc, basePrice, decor, p, nextCheckIn, lastCheckOut, lastStay);
         }
      }
      catch(Exception e){
         System.out.println(e);
      }  
   }

   // FR-4
   public static void cancelRes(Connection conn){
      Statement stmt = null;
      Scanner input = new Scanner(System.in);
      try{
         System.out.print("Reservation Code: ");
         String userInput = input.nextLine();
         int resNum = Integer.parseInt(userInput);
         stmt = conn.createStatement();
         String query = "select * from lab7_reservations where CODE=" + Integer.toString(resNum);
         ResultSet rs = stmt.executeQuery(query);
         int count = 0;
         System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
         while(rs.next()){
            count++;
            String code = rs.getString("CODE");
            String room = rs.getString("Room");
            String checkIn = rs.getString("CheckIn");
            String checkOut = rs.getString("CheckOut");
            int rate = rs.getInt("Rate");
            String lastName = rs.getString("LastName");
            String firstName = rs.getString("FirstName");
            int adults = rs.getInt("Adults");
            int kids = rs.getInt("Kids");
            System.out.format("| %-5s | %-4s | %-10s | %-10s | %4d | %-20s | %-20s | %-6d | %-4d |%n", code, room, checkIn, checkOut, rate, lastName, firstName, adults, kids);
         }
         if(count==0){
            System.out.println("No record of reservation with given code");
            return;
         }
         System.out.print("\nConfirm Cancellation? (y/n): ");
         userInput = input.nextLine();
         switch(userInput){
            case "y":
               stmt = conn.createStatement();
               query = "delete from lab7_reservations where CODE=" + Integer.toString(resNum);
               stmt.executeUpdate(query);
               System.out.println("Cancellation Confirmed");
               break;
            default:
               break;
         }
      }
      catch(Exception e){
         System.out.println(e);
      }
   }

   public static void menu(Connection conn){
      Scanner input = new Scanner(System.in);
      System.out.print(">");
      String userInput = input.nextLine();
      switch(userInput){
         case "pop":
            roomsAndRates(conn);
            break;
         case "cancel":
            cancelRes(conn);
            break;
         default:
            System.out.println("AHH");
            System.exit(0);
      }
   }
   public static void main(String[] args){
      String jdbcUrl = System.getenv("JDBC_URL");
      String dbUser = System.getenv("JDBC_USER");
      String dbPass = System.getenv("JDBC_PASS");
      Connection conn = null;
      Statement stmt = null;
      try{
         conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPass);
         /*stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM INN.rooms");
         while (rs.next()){
            String rc = rs.getString("RoomCode");
            String rn = rs.getString("RoomName");
            int bp = rs.getInt("basePrice");
            System.out.format("%3s %30s %d %n", rc, rn, bp);
         }*/
         while(true){
            menu(conn);
         }
      }
      catch(Exception e){
         System.out.println(e);
      }
   }
}
