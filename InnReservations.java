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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;

public class InnReservations{
   // FR-1
   public static void roomsAndRates(Connection conn){
      PreparedStatement pstmt = null;
      try{
         String query = "select RoomCode, RoomName, Beds, bedType, maxOcc, basePrice, decor, popularity, nextCheckIn, lastCheckOut, lastStay from ( select D.RoomCode, D.RoomName, D.Beds, D.bedType, D.maxOcc, D.basePrice, D.decor, D.popularity, D.CheckOut nextCheckIn, E.CheckOut lastCheckOut, DATEDIFF(E.CheckOut, E.CheckIn) lastStay, rank() over (partition by RoomCode order by E.CheckOut desc) revDateRank from ( select * from ( select *, rank() over (partition by RoomCode order by CheckOut) dateRank from ( select *, round(times/180,2) popularity from ( select Room selectedRoom, count(*) times from lab7_reservations where DATEDIFF(CURRENT_DATE, CheckIn) <= 180 group by selectedRoom ) as A join lab7_rooms on RoomCode=selectedRoom ) as B join lab7_reservations on Room=RoomCode where CheckOut > CURRENT_DATE ) as C where dateRank=1 ) as D join ( select * from lab7_reservations r where CheckOut < CURRENT_DATE ) as E on E.Room=RoomCode order by selectedRoom, revDateRank ) as F where revDateRank=1 order by popularity desc";
         pstmt = conn.prepareStatement(query);
         ResultSet rs = pstmt.executeQuery();
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

   // FR-2
   public static void bookRes(Connection conn, Scanner input){
      PreparedStatement pstmt = null;
      try{
         pstmt = conn.prepareStatement("select * from lab7_reservations");
         ResultSet rs = pstmt.executeQuery();
         int maxRes = 0;
         while(rs.next()){
            int resNum = rs.getInt("CODE");
            if(resNum > maxRes){
               maxRes = resNum;
            }
         }
         maxRes++;
         System.out.print("First Name: ");
         String firstName = input.nextLine();
         System.out.print("Last Name: ");
         String lastName = input.nextLine();
         System.out.println("Room Code (blank for no preference): ");
         String roomCode = input.nextLine();
         System.out.println("Bed Type (blank for no preference): ");
         String bedType = input.nextLine();
         System.out.println("Start Date (yyyy-mm-dd): ");
         String startDate = input.nextLine();
         System.out.println("End Date (yyyy-mm-dd): ");
         String endDate = input.nextLine();
         System.out.println("Number of Children: ");
         String children = input.nextLine();
         System.out.println("Number of Adults: ");
         String adults = input.nextLine();
         int childrenNum = Integer.parseInt(children);
         int adultNum = Integer.parseInt(adults);
         int occ = childrenNum + adultNum;
         int count = 0;
         boolean found = false;
         String optionalWhere = "";
         if(!roomCode.isEmpty()){
            optionalWhere += " and RoomCode='" + roomCode + "'";
         }
         if(!bedType.isEmpty()){
            optionalWhere += " and bedType='" + bedType + "'";
         }
         String query = "select *, rank() over (order by RoomCode) roomOption from lab7_rooms where RoomCode not in (select Room from lab7_reservations join lab7_rooms on Room=RoomCode where (CheckIn <= ? and CheckOut > ?) and maxOcc >= ?)";
         query += optionalWhere;
         pstmt = conn.prepareStatement(query);
         pstmt.setString(1, endDate);
         pstmt.setString(2, startDate);
         pstmt.setInt(3, occ);
         rs = pstmt.executeQuery();
         int maximumOccupancy = 0;
         ArrayList<String[]> rooms = new ArrayList<String[]>();
         while(rs.next()){
            count++;
            found = true;
            String roomOption = rs.getString("roomOption");
            roomCode = rs.getString("RoomCode");;
            String roomName = rs.getString("RoomName");
            int beds = rs.getInt("Beds");
            bedType = rs.getString("bedType");
            int maxOcc = rs.getInt("maxOcc");
            int basePrice = rs.getInt("basePrice");
            String[] roomInfo = new String[]{roomCode, roomName, bedType, Integer.toString(basePrice)};
            rooms.add(roomInfo);
            String decor = rs.getString("decor");
            if(count==1){
               System.out.format("| %-6s | %-8s | %-30s | %-4s | %-10s | %-10s | %-10s | %-15s |%n", "Option", "Code", "Name", "Beds", "BedType", "Max Guests", "base price", "Decor");
            }
            System.out.format("| %-6s | %-8s | %-30s | %4d | %-10s | %10d | %10d | %-15s |%n", roomOption, roomCode, roomName, beds, bedType, maxOcc, basePrice, decor);
            if(maxOcc > maximumOccupancy){
               maximumOccupancy = maxOcc;
            }
         }
         if(count == 0){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(startDate));
            c.add(Calendar.DAY_OF_MONTH, 3);
            endDate = sdf.format(c.getTime());
            System.out.println("\nCould not find a matching room during this interval, instead showing rooms between " + startDate + " and " + endDate);
            pstmt = conn.prepareStatement("select *, rank() over(order by RoomCode) roomOption from lab7_rooms where RoomCode not in (select Room from lab7_rooms join lab7_reservations on Room=RoomCode where (CheckIn <= ? and CheckOut > ?) and maxOcc >= ?)");
            pstmt.setString(1, endDate);
            pstmt.setString(2, startDate);
            pstmt.setInt(3, occ);
            rs = pstmt.executeQuery();
            while(rs.next()){
               count++;
               found = true;
               String roomOption = rs.getString("roomOption");
               roomCode = rs.getString("RoomCode");
               String roomName = rs.getString("RoomName");
               int beds = rs.getInt("Beds");
               bedType = rs.getString("bedType");
               int maxOcc = rs.getInt("maxOcc");
               int basePrice = rs.getInt("basePrice");
               String[] roomInfo = new String[]{roomCode, roomName, bedType, Integer.toString(basePrice)};
               rooms.add(roomInfo);
               String decor = rs.getString("decor");
               if(count==1){
                  System.out.format("| %-6s | %-8s | %-30s | %-4s | %-10s | %-10s | %-10s | %-15s |%n", "Option", "Code", "Name", "Beds", "BedType", "Max Guests", "base price", "Decor");
               }
               System.out.format("| %-6s | %-8s | %-30s | %4d | %-10s | %10d | %10d | %-15s |%n", roomOption, roomCode, roomName, beds, bedType, maxOcc, basePrice, decor);
               if(maxOcc > maximumOccupancy){
                  maximumOccupancy = maxOcc;
               }
            }
         }
         if(maximumOccupancy < occ){
            System.out.println("We're sorry, we are unable to room a party of your size in only one room");
         }
         if(found){
            SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");
            Calendar startC = Calendar.getInstance();
            startC.setTime(df.parse(startDate));
            Calendar endC = Calendar.getInstance();
            endC.setTime(df.parse(endDate));
            int workDays = 0;
            int totalDays = 0;
            while(startC.getTimeInMillis() < endC.getTimeInMillis()){
               if(startC.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startC.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY){
                  workDays++;
               }
               totalDays++;
               startC.add(Calendar.DAY_OF_MONTH, 1);
            }
            System.out.println("Enter the Option number of the room you'd like to select: ");
            int selectedRoom = Integer.parseInt(input.nextLine());
            String[] chosenRoom = rooms.get(selectedRoom-1);
            int bp = Integer.parseInt(chosenRoom[3]);
            double totalCost = (bp * workDays + bp * 1.1 * (totalDays - workDays)) * 1.18;
            System.out.println("\nFirst Name: " + firstName);
            System.out.println("Last Name: " + lastName);
            System.out.println("Room Code: " + chosenRoom[0]);
            System.out.println("Room Name: " + chosenRoom[1]);
            System.out.println("Bed Type: " + chosenRoom[2]);
            System.out.println("Start Date: " + startDate);
            System.out.println("End Date: " + endDate);
            System.out.println("Adults: " + adults);
            System.out.println("Children: " + children);
            System.out.println("Total Cost: $" + String.valueOf(totalCost));
            System.out.println("\nConfirm? (y/n): ");
            String confirmation = input.nextLine();
            switch(confirmation){
               case "y":
                  String reservationCode = Integer.toString(maxRes);
                  pstmt = conn.prepareStatement("insert into lab7_reservations (Code, Room, CheckIn, CheckOut, Rate, LastName, FirstName, Adults, Kids) values (?, ?, ?, ?, ?, ?, ?, ?, ?);");
                  pstmt.setString(1, reservationCode);                  
                  pstmt.setString(2, chosenRoom[0]);
                  pstmt.setString(3, startDate);
                  pstmt.setString(4, endDate); 
                  pstmt.setInt(5, bp);
                  pstmt.setString(6, lastName.toUpperCase());
                  pstmt.setString(7, firstName.toUpperCase());
                  pstmt.setInt(8, adultNum);
                  pstmt.setInt(9, childrenNum);
                  pstmt.executeUpdate();
                  System.out.println("Reservation Confirmed");
                  System.out.println("Reservation Code: " + reservationCode);
                  break;
               default:
                  System.out.println("Reservation not confirmed, returning to menu");
            }
         }
      }
      catch(Exception e){
         System.out.println(e);
      }  
   }

   // FR-3
   public static void alterRes(Connection conn, Scanner input){
      PreparedStatement pstmt = null;
      try{
         pstmt = conn.prepareStatement("select * from lab7_reservations where Code = ?");
         System.out.println("Enter your reservation code: ");
         String code = input.nextLine();
         pstmt.setString(1, code);
         String room = "";
         String checkIn = "";
         String checkOut = "";
         int rate = 0;
         String lastName = "";
         String firstName = "";
         int adults = 0;
         int children = 0;
         int checkCount = 0;
         ResultSet rs = pstmt.executeQuery();
         System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
         while(rs.next()){
            checkCount = 0;
            room = rs.getString("Room");
            checkIn = rs.getString("CheckIn");
            checkOut = rs.getString("CheckOut");
            rate = rs.getInt("Rate");
            lastName = rs.getString("LastName");
            firstName = rs.getString("FirstName");
            adults = rs.getInt("Adults");
            children = rs.getInt("Kids");
            System.out.format("| %-5s | %-4s | %-10s | %-10s | %4d | %-20s | %-20s | %-6d | %-4d |%n", code, room, checkIn, checkOut, rate, lastName, firstName, adults, children);
         }
         if(checkCount == 0){
            System.out.println("No reservation exists with given reservation code");
            return;
         }
         System.out.println("\nUpdating this reservation, leave blank if no change requested for a given field");
         System.out.println("First Name: ");
         String newFirstName = input.nextLine();
         System.out.println("Last Name: ");
         String newLastName = input.nextLine();
         System.out.println("Start Date(yyyy-MM-dd): ");
         String newCheckIn = input.nextLine();
         System.out.println("End Date(yyyy-MM-dd): ");
         String newCheckOut = input.nextLine();
         System.out.println("Children: ");
         String newChildren = input.nextLine();
         System.out.println("Adults: ");
         String newAdults = input.nextLine();
         boolean changeFound = false;
         boolean dateChange = false;
         if(!newFirstName.isEmpty()){
            changeFound = true;
            firstName = newFirstName.toUpperCase();
         }   
         if(!newLastName.isEmpty()){
            changeFound = true;
            lastName = newLastName.toUpperCase();
         }
         if(!newCheckIn.isEmpty()){
            changeFound = true;
            checkIn = newCheckIn;
            dateChange = true;
         }
         if(!newCheckOut.isEmpty()){
            changeFound = true;
            checkOut = newCheckOut;
            dateChange = true;
         }
         if(!newChildren.isEmpty()){
            changeFound = true;
            children = Integer.parseInt(newChildren);
         }
         if(!newAdults.isEmpty()){
            changeFound = true;
            adults = Integer.parseInt(newAdults);
         }
         if(!changeFound){
            System.out.println("Reservation change cancelled");
            return;
         }
         if(dateChange){
            String query = "select *, rank() over (order by RoomCode) roomOption from lab7_rooms where RoomCode not in (select Room from lab7_reservations join lab7_rooms on Room=RoomCode where (CheckIn <= ? and CheckOut > ?)) where Room = ?";        
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, checkOut);
            pstmt.setString(2, checkIn);
            pstmt.setString(3, room);
            rs = pstmt.executeQuery();
            int count = 0;
            while(rs.next()){
               count++;
            }
            if(count == 0){
               System.out.println("Date was not able to be rescheduled due to existing reservation");
            }
            else{
               System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
               System.out.format("| %-5s | %-4s | %-10s | %-10s | %4d | %-20s | %-20s | %-6d | %-4d |%n", code, room, checkIn, checkOut, rate, lastName, firstName, adults, children);
               System.out.println("Confirm Changes? (y/n): ");
               String confirmation = input.nextLine();
               switch(confirmation){
                  case "y":
                     pstmt = conn.prepareStatement("update lab7_reservations set FirstName = ?, LastName = ?, CheckIn = ?, CheckOut = ?, Kids = ?, Adults = ? where Code = ?");
                     pstmt.setString(1, firstName);
                     pstmt.setString(2, lastName);
                     pstmt.setString(3, checkIn);
                     pstmt.setString(4, checkOut);
                     pstmt.setInt(5, children);
                     pstmt.setInt(6, adults);
                     pstmt.setString(7, code);
                     pstmt.executeUpdate();
                     System.out.println("Change Completed");
                     break;
                  default:
                     System.out.println("Reservation change cancelled");
                     break;
               }
            }
         }
         else{
            System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
            System.out.format("| %-5s | %-4s | %-10s | %-10s | %4d | %-20s | %-20s | %-6d | %-4d |%n", code, room, checkIn, checkOut, rate, lastName, firstName, adults, children);
            System.out.println("Confirm Changes? (y/n): ");
            String confirmation = input.nextLine();
            switch(confirmation){
               case "y":
                  pstmt = conn.prepareStatement("update lab7_reservations set FirstName = ?, LastName = ?, CheckIn = ?, CheckOut = ?, Kids = ?, Adults = ? where Code = ?");
                  pstmt.setString(1, firstName);
                  pstmt.setString(2, lastName);
                  pstmt.setString(3, checkIn);
                  pstmt.setString(4, checkOut);
                  pstmt.setInt(5, children);
                  pstmt.setInt(6, adults);
                  pstmt.setString(7, code);
                  pstmt.executeUpdate();
                  System.out.println("Change Completed");
                  break;
               default:
                  System.out.println("Reservation change cancelled");
                  break;
            }
         }
      }
      catch(Exception e){
         System.out.println(e);
      } 
        
   }

   // FR-4
   public static void cancelRes(Connection conn, Scanner input){
      PreparedStatement pstmt = null;
      try{
         System.out.print("Reservation Code: ");
         String userInput = input.nextLine();
         int resNum = Integer.parseInt(userInput);
         pstmt = conn.prepareStatement("select * from lab7_reservations where CODE = ?");
         pstmt.setString(1, Integer.toString(resNum));
         ResultSet rs = pstmt.executeQuery();
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
               pstmt = conn.prepareStatement("delete from lab7_reservations where CODE = ?");
               pstmt.setString(1, Integer.toString(resNum));
               pstmt.executeUpdate();
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
         case "book":
            bookRes(conn, input);
            break;
         case "change":
            alterRes(conn, input);
            break;
         case "cancel":
            cancelRes(conn, input);
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
         while(true){
            menu(conn);
         }
      }
      catch(Exception e){
         System.out.println(e);
      }
   }
}
