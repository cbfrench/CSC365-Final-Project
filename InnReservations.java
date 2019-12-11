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
import java.util.HashMap;

public class InnReservations{
   
/******************************FUNCTIONS********************************/
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
         System.out.println("Room Code ('any' for no preference): ");
         String roomCode = input.nextLine();
         System.out.println("Bed Type ('any' for no preference): ");
         String bedType = input.nextLine();
         System.out.println("Start Date (yyyy-mm-dd): ");
         String startDate = input.nextLine();
         System.out.println("End Date (yyyy-mm-dd): ");
         String endDate = input.nextLine();
         System.out.println("Number of Children: ");
         String children = input.nextLine();
         System.out.println("Number of Adults: ");
         String adults = input.nextLine();
         if(firstName.isEmpty() || lastName.isEmpty() || roomCode.isEmpty() || bedType.isEmpty() || startDate.isEmpty() || endDate.isEmpty() || children.isEmpty() || adults.isEmpty()){
            System.out.println("All values are required to book a room, please try again");
            return;
         }
         int childrenNum = Integer.parseInt(children);
         int adultNum = Integer.parseInt(adults);
         int occ = childrenNum + adultNum;
         int count = 0;
         boolean found = false;
         boolean roomCodeFound = false;
         boolean bedTypeFound = false; 
         String query = "select *, rank() over (order by RoomCode) roomOption from lab7_rooms where RoomCode not in (select Room from lab7_reservations join lab7_rooms on Room=RoomCode where (CheckIn <= ? and CheckOut > ?) and maxOcc >= ?)";
         if(!roomCode.toLowerCase().equals("any")){
            roomCodeFound = true;
            query += " and RoomCode = ?";
         }
         if(!bedType.toLowerCase().equals("any")){
            bedTypeFound = true;
            query += " and bedType = ?";
         }
         pstmt = conn.prepareStatement(query);
         pstmt.setString(1, endDate);
         pstmt.setString(2, startDate);
         pstmt.setInt(3, occ);
         if(roomCodeFound){
            pstmt.setString(4, roomCode);
            if(bedTypeFound){
               pstmt.setString(5, bedType);
            }
         }
         else{
            if(bedTypeFound){
               pstmt.setString(4, bedType);
            }
         }
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
               System.out.format("\n| %-6s | %-8s | %-30s | %-4s | %-10s | %-10s | %-10s | %-15s |%n", "Option", "Code", "Name", "Beds", "BedType", "Max Guests", "base price", "Decor");
            }
            System.out.format("| %-6s | %-8s | %-30s | %4d | %-10s | %10d | %10d | %-15s |%n", roomOption, roomCode, roomName, beds, bedType, maxOcc, basePrice, decor);
            if(maxOcc > maximumOccupancy){
               maximumOccupancy = maxOcc;
            }
         }
         System.out.println();
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
            System.out.println("We're sorry, we are currently unable to host any parties larger than " + Integer.toString(maximumOccupancy) + " people, please consider booking multiple rooms");
            return;
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
                  System.out.println("\nReservation Confirmed");
                  System.out.println("Reservation Code: " + reservationCode);
                  break;
               default:
                  System.out.println("\nReservation not confirmed, returning to menu");
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
         pstmt = conn.prepareStatement("select * from lab7_reservations join lab7_rooms on Room=RoomCode where Code = ?");
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
         int maxOcc = 0;
         ResultSet rs = pstmt.executeQuery();
         System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
         while(rs.next()){
            checkCount++;
            room = rs.getString("Room");
            checkIn = rs.getString("CheckIn");
            checkOut = rs.getString("CheckOut");
            rate = rs.getInt("Rate");
            lastName = rs.getString("LastName");
            firstName = rs.getString("FirstName");
            adults = rs.getInt("Adults");
            children = rs.getInt("Kids");
            maxOcc = rs.getInt("maxOcc");
            System.out.format("| %-5s | %-4s | %-10s | %-10s | %4d | %-20s | %-20s | %-6d | %-4d | %-6d |%n", code, room, checkIn, checkOut, rate, lastName, firstName, adults, children, maxOcc);
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
         boolean occChange = false;
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
            occChange = true;
            children = Integer.parseInt(newChildren);
         }
         if(!newAdults.isEmpty()){
            changeFound = true;
            occChange = true;
            adults = Integer.parseInt(newAdults);
         }
         if(!changeFound){
            System.out.println("Reservation change cancelled");
            return;
         }
         if(occChange){
            if(adults + children > maxOcc){
               System.out.println("We're sorry, the room you are booked in can only handle a maximum of " + Integer.toString(maxOcc) + " people");
                  return;
            }
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

   // FR-5
   public static void searchRes(Connection conn, Scanner input){
      PreparedStatement pstmt = null;
      try {
         int firstWhere = 0;
         System.out.println("\nSEARCH\nSearch Options: Firstname, Lastname, DateRange, RoomCode, ReservationCode\nLeave option blank if you wish not to use it");
         System.out.print("First Name: ");
         String FirstName = input.nextLine();
         if (FirstName.isEmpty()) {
            FirstName = "any";
         }
         System.out.print("Last Name: ");
         String LastName = input.nextLine();
         if (LastName.isEmpty()) {
            LastName = "any";
         }
         System.out.println("Date Range: Start date - End date");
         System.out.print("Start Date (yyyy-mm-dd): ");
         String StartDate = input.nextLine();
         System.out.print("End Date (yyyy-mm-dd): ");
         String EndDate = input.nextLine();
         if (StartDate.isEmpty()) {
            StartDate = "any";
         }
         if (EndDate.isEmpty()){
            EndDate = "any";
         }
         /*if (!StartDate.equals("any") && EndDate.equals("any") || StartDate.equals("any") && !EndDate.equals("any") ) {
            System.out.println("Date Range requires both Start and End Date, or neither");
            return;
         }*/
         System.out.print("Room Code: ");
         String RoomCode = input.nextLine();
         if (RoomCode.isEmpty()) {
            RoomCode = "any";
         }
         System.out.print("Reservation Code: ");
         String ResCode = input.nextLine();
         if (ResCode.isEmpty()) {
            ResCode = "any";
         }
         String whereClause = "where ";
         boolean fnFound = false;
         boolean lnFound = false;
         boolean roomcFound = false;
         boolean rescFound = false;
         boolean sdFound = false;
         boolean edFound = false;
         if (!FirstName.toLowerCase().equals("any")) {
            firstWhere = 1;
            whereClause += "FirstName like ?";
            fnFound = true;
         }
         if (!LastName.toLowerCase().equals("any")) {
            if (firstWhere == 1) {
               whereClause += " and ";
            }
            firstWhere = 1;
            whereClause += "LastName like ?";
            lnFound = true;
         }
         if (!RoomCode.toLowerCase().equals("any")) {
            if (firstWhere == 1) {
               whereClause += " and ";
            }
            firstWhere = 1;
            whereClause += "Room like ?";
            roomcFound = true;
         }
         if (!ResCode.toLowerCase().equals("any")) {
            if (firstWhere == 1) {
               whereClause += " and ";
            }
            firstWhere = 1;
            whereClause += "Code like ?";
            rescFound = true;
         }
         if (!StartDate.toLowerCase().equals("any")){
            if (firstWhere == 1) {
               whereClause += " and ";
            }
            whereClause += "CheckIn = ?";
            sdFound = true;
         }
         if (!EndDate.toLowerCase().equals("any")){
            if (firstWhere == 1) {
               whereClause += " and ";
            }
            whereClause += "CheckOut = ?";
            edFound = true;
         }
         String query = "select * from lab7_reservations " + whereClause + ";";
         System.out.println(query);
         pstmt = conn.prepareStatement(query);
         int setCount = 1;
         if(fnFound){
            pstmt.setString(setCount, FirstName + "%");
            setCount++;
         }
         if(lnFound){
            pstmt.setString(setCount, LastName + "%");
            setCount++; 
         }
         if(roomcFound){
            pstmt.setString(setCount, RoomCode + "%");
            setCount++;
         }
         if(rescFound){
            pstmt.setString(setCount, ResCode + "%");
            setCount++;
         }
         if(sdFound){
            pstmt.setString(setCount, StartDate);
            setCount++;
         }
         if(edFound){
            pstmt.setString(setCount, EndDate);
            setCount++;
         }
         ResultSet rs = pstmt.executeQuery();
         System.out.format("| %-5s | %-4s | %-10s | %-10s | %-4s | %-20s | %-20s | %-6s | %-4s |%n", "Code", "Room", "CheckIn", "CheckOut", "Rate", "LastName", "FirstName", "Adults", "Kids");
         while (rs.next()) {
            String code = rs.getString("Code");
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
      } catch(Exception e){
         System.out.println(e);
      }
   }

   // FR-6
   public static void getRevenue(Connection conn){
      Statement stmt = null;
      Statement stmt2 = null;
      PreparedStatement pstmt = null;
      ResultSet rs = null;
      String query = "";
      try {
         query = "create table  IF NOT EXISTS revenue (\n" +
                 "\n" +
                 "Room            CHAR(10),\n" +
                 "January         DECIMAL(10,2),\n" +
                 "February        DECIMAL(10,2),\n" +
                 "March           DECIMAL(10,2),\n" +
                 "April           DECIMAL(10,2),\n" +
                 "May             DECIMAL(10,2),\n" +
                 "June            DECIMAL(10,2),\n" +
                 "July            DECIMAL(10,2),\n" +
                 "August          DECIMAL(10,2),\n" +
                 "September       DECIMAL(10,2),\n" +
                 "October         DECIMAL(10,2),\n" +
                 "November       DECIMAL(10,2),\n" +
                 "December        DECIMAL(10,2),\n" +
                 "YearRevenue    DECIMAL(10,2),\n" +
                 "\n" +
                 "PRIMARY KEY (Room)\n" +
                 ");";
         pstmt = conn.prepareStatement(query);
         pstmt.executeUpdate();
         query = "SELECT room, MONTHNAME(STR_TO_DATE(month_checkin, '%m')), ROUND(SUM(MonthRev),0) as MonthRevenue\n" +
                 "FROM (\n" +
                 "    (SELECT room, month_checkin, SUM(CheckInMonthRev) as MonthRev\n" +
                 "    from (\n" +
                 "        (select room,\n" +
                 "            MONTH(checkin) as month_checkin, \n" +
                 "        \n" +
                 "            SUM(ROUND( (DATEDIFF(LAST_DAY(checkin), checkin)+1) * Rate,2)) as CheckInMonthRev\n" +
                 "            \n" +
                 "        from lab7_reservations\n" +
                 "        where MONTH(checkin) != MONTH(checkout)\n" +
                 "        GROUP BY MONTH(checkin), MONTH(checkout), room\n" +
                 "        order by room, month_checkin)\n" +
                 "        \n" +
                 "        UNION ALL\n" +
                 "        \n" +
                 "        \n" +
                 "        (select room,\n" +
                 "            MONTH(checkout) as month_checkin,\n" +
                 "            SUM(ROUND((DAY(checkout) - 1) * Rate,2)) as CheckInMonthRev\n" +
                 "        from lab7_reservations\n" +
                 "        where MONTH(checkin) != MONTH(checkout)\n" +
                 "        GROUP BY MONTH(checkin), MONTH(checkout), room\n" +
                 "        order by room, month_checkin)\n" +
                 "    ) as a1\n" +
                 "    GROUP BY room, month_checkin\n" +
                 "    ORDER BY month_checkin\n" +
                 "    )\n" +
                 "    \n" +
                 "    UNION ALL\n" +
                 "    \n" +
                 "    (select room, \n" +
                 "            MONTH(checkin) as month_checkin, \n" +
                 "            SUM(ROUND(DATEDIFF(checkout,checkin) * Rate,2)) as MonthRev\n" +
                 "        from lab7_reservations\n" +
                 "        where MONTH(checkin) = MONTH(checkout)\n" +
                 "        GROUP BY MONTH(checkin), MONTH(checkout), room\n" +
                 "        order by room, month_checkin\n" +
                 "    )\n" +
                 ") as m1\n" +
                 "GROUP BY room, month_checkin\n" +
                 "ORDER BY room, month_checkin\n" +
                 "        \n" +
                 ";";
         pstmt = conn.prepareStatement(query);
         rs = pstmt.executeQuery();
         int count = 0;
         float sum = 0;
         String revz = "";
         while (rs.next()) {
            String m_revenue = rs.getString("MonthRevenue");
            String room = rs.getString("Room");
            if (count == 11){
               revz = revz + m_revenue;
               sum = sum + Float.parseFloat(m_revenue);
               query = "INSERT INTO revenue(Room, January, February, March, April, May, June, July, August, September, October, November, December, YearRevenue) VALUES(?, " + revz + ", ?);";
               pstmt = conn.prepareStatement(query);
               pstmt.setString(1, room);
               pstmt.setFloat(2, sum);
               pstmt.executeUpdate();
               revz = "";
               count = 0;
            }
            else {
               sum = sum + Float.parseFloat(m_revenue);
               revz = revz  + m_revenue + ", ";
               count++;
            }
         }
         query = "INSERT INTO revenue(Room, January, February, March, April, \n" +
                 "May, June, July, August, September, October, \n" +
                 "November, December, YearRevenue)\n" +
                 "SELECT Room, Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Decem, Rev\n" +
                 "FROM(\n" +
                 "\n" +
                 "    SELECT 'Total' as Room,\n" +
                 "    ROUND(SUM(January),0) as Jan, \n" +
                 "    ROUND(SUM(February),0) as Feb, \n" +
                 "    ROUND(SUM(March),0) as Mar,\n" +
                 "        ROUND(SUM(April),0) as Apr,\n" +
                 "        ROUND(SUM(May),0) as May,\n" +
                 "        ROUND(SUM(June),0) as Jun,\n" +
                 "        ROUND(SUM(July),0) as Jul,\n" +
                 "        ROUND(SUM(August),0) as Aug,\n" +
                 "        ROUND(SUM(September),0) as Sep,\n" +
                 "        ROUND(SUM(October),0) as Oct,\n" +
                 "        ROUND(SUM(November),0) as Nov,\n" +
                 "        ROUND(SUM(December),0) as Decem,\n" +
                 "        ROUND(SUM(YearRevenue),0) as Rev\n" +
                 "    from revenue\n" +
                 "    ) as w1;";
         pstmt = conn.prepareStatement(query);
         pstmt.executeUpdate();
         query = "select * from revenue";
         pstmt = conn.prepareStatement(query);
         rs = pstmt.executeQuery();
         System.out.format("| %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s| %-8s| %-8s | %-9s | %-8s | %-8s | %-8s | %-11s |%n", "Room", "January", "February", "March",
                 "April", "May", "June", "July", "August", "September", "October", "November", "December", "YearRevenue");
         while (rs.next()) {
            String room = rs.getString("Room");
            String January = rs.getString("January");
            String February = rs.getString("February");
            String March = rs.getString("March");
            String April = rs.getString("April");
            String May = rs.getString("May");
            String June = rs.getString("June");
            String July = rs.getString("July");
            String August = rs.getString("August");
            String September = rs.getString("September");
            String October = rs.getString("October");
            String November = rs.getString("November");
            String December = rs.getString("December");
            String YearRevenue = rs.getString("YearRevenue");
            System.out.format("| %-8s | %-8s | %-8s | %-8s | %-8s | %-8s | %-8s| %-8s| %-8s | %-9s | %-8s | %-8s | %-8s | %-11s |%n", room, January, February, March,
                    April, May, June, July, August, September, October, November, December, YearRevenue);
         }
         query = "drop table revenue;";
         pstmt = conn.prepareStatement(query);
         pstmt.executeUpdate();
      } catch (Exception e){
         System.out.println(e);
      }
   }

/**********************************MAIN*********************************/
   public static void menu(Connection conn){
      Scanner input = new Scanner(System.in);
      boolean quit = false;
      System.out.println("Possible Commands\npop - see room popularity for the last 180 days\nbook - book a reservation\nchange - change a reservation\ncancel - cancel a reservation\nsearch - search for a reservation\nrevenue - get a breakdown of the inn's revenue for the year\nquit - exit program\n");
      System.out.print(">");
      String userInput = input.nextLine();
      System.out.println();
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
         case "search":
            searchRes(conn, input);
            break;
         case "revenue":
            getRevenue(conn);
            break;
         case "quit":
            System.out.println("Have a good day!");
            quit = true;
            break;
         default:
            System.out.println("I'm sorry, please try entering the command again");
      }
      System.out.println();
      if(quit){
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
         System.out.println("\nWelcome to the Inn Reservation System (IRS)\n");
         while(true){
            menu(conn);
         }
      }
      catch(Exception e){
         System.out.println(e);
      }
   }
}




