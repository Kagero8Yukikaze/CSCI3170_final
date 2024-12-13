import java.io.*;
import java.util.*;
import java.sql.*;
import java.text.*;

// Contains every action of Manager
public class Manager{
    private Connection connection;

    public Manager(Connection connection) {
        this.connection = connection;
    }

    public void showManaMenu(){
        Scanner scanner = new Scanner(System.in);
        int choice;
        // Show selections
        do {
            System.out.println("-----Operations for manager menu-----");
            System.out.println("What kinds of operation would you like to perform?");
            System.out.println("1. List all salespersons");
            System.out.println("2. Count the no. of sales record of each salesperson under " +
                "a specific range on years of experience");
            System.out.println("3. Show the total sales value of each manufacturer");
            System.out.println("4. Show the N most popular part");
            System.out.println("5. Return to the main menu");
            System.out.print("Enter Your Choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    listSalespersons();
                    break;
                case 2:
                    countRecord();
                    break;
                case 3:
                    sortTotalSale();
                    break;
                case 4:
                    showNpart();
                    break;
                case 5:
                    System.out.println("Returning to main menu...\n");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 to 5!");
                    break;
            }
        } while (choice != 5);
    }

    private void listSalespersons(){
        // List all salespersons
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose ordering:");
        System.out.println("1. By ascending order");
        System.out.println("2. By descending order");
        System.out.print("Choose the list ordering: ");
        int sortChoice = scanner.nextInt();

        String searchQuery = "SELECT sID, sName, sPhoneNumber, sExperience " +
                "FROM salesperson " +
                "ORDER BY sExperience ";
        if (sortChoice == 1){
            searchQuery += "ASC, sID ASC";
        }
        else if (sortChoice == 2){
            searchQuery += "DESC, sID ASC";
        }
        else {
            System.out.println("Invalid choice. Please enter 1 to 2!");
            return;
        }
        try (Statement stmt = connection.createStatement(); 
            ResultSet rs_sale = stmt.executeQuery(searchQuery)) {
            System.out.println("| ID | Name | Mobile Phone | Years of Experience |");

            while (rs_sale.next()) {
                System.out.printf("| %d | %s | %d | %d |\n",
                        rs_sale.getInt("sID"),
                        rs_sale.getString("sName"),
                        rs_sale.getInt("sPhoneNumber"),
                        rs_sale.getInt("sExperience"));
            }

            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.err.println("Error listing salespeople: " + e.getMessage());
        }
    }

    private void countRecord(){
        // Count the no. of sales record of each salesperson under a specific range on years of experience
        Scanner scanner = new Scanner(System.in);

        System.out.print("Type in the lower bound for years of experience: ");
        int minExperience = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Type in the upper bound for years of experience: ");
        int maxExperience = scanner.nextInt();

        if(maxExperience < minExperience || minExperience <= 0 || maxExperience <= 0){
            System.out.println("Invalid input!");
            return;
        }

        String query = "SELECT s.sID, s.sName, s.sExperience, COUNT(t.tID) AS transactionCount " +
                "FROM salesperson s " +
                "LEFT JOIN transaction t ON s.sID = t.sID " +
                "WHERE s.sExperience BETWEEN ? AND ? " +
                "GROUP BY s.sID, s.sName, s.sExperience " +
                "ORDER BY s.sID DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, minExperience);
            pstmt.setInt(2, maxExperience);

            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Transaction Record:");
                System.out.println("| ID | Name | Years of Experience | Numbers of Transaction |");

                while (rs.next()) {
                    System.out.printf("| %d | %s | %d | %d |\n",
                            rs.getInt("sID"),
                            rs.getString("sName"),
                            rs.getInt("sExperience"),
                            rs.getInt("transactionCount"));
                }

                System.out.println("End of Query\n");
            } catch (SQLException e) {
                System.err.println("Error executing query: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.err.println("Error counting transactions: " + e.getMessage());
        }
    }

    private void sortTotalSale(){
        // Show the total sales value of each manufacturer
        String query = "SELECT m.mID, m.mName, COALESCE(SUM(p.pPrice), 0) AS totalSalesValue " +
                       "FROM manufacturer m " +
                       "JOIN part p ON m.mID = p.mID " +
                       "JOIN transaction t ON p.pID = t.pID " +
                       "GROUP BY m.mID, m.mName " +
                       "ORDER BY totalSalesValue DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("| Manufacturer ID | Manufacturer Name | Total Sales Value |");

            while (rs.next()) {
                System.out.printf("| %d | %s | %d |\n",
                        rs.getInt("mID"),
                        rs.getString("mName"),
                        rs.getInt("totalSalesValue"));
            }

            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.err.println("Error sorting manufacturers by total sales: " + e.getMessage());
        }
    }

    private void showNpart(){
        // Show the N most popular part
        Scanner scanner = new Scanner(System.in);

        System.out.print("Type in the number of parts: ");
        int N = scanner.nextInt();

        if (N <= 0){
            System.out.print("Invalid input!");
            return;
        }

        String query = "SELECT p.pID, p.pName, COUNT(t.tID) AS totalTransactions " +
            "FROM part p " +
            "JOIN transaction t ON p.pID = t.pID " +
            "GROUP BY p.pID, p.pName " +
            "HAVING COUNT(t.tID) > 0 " +
            "ORDER BY totalTransactions DESC " +
            "FETCH FIRST ? ROWS ONLY"; 
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, N);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("| Part ID | Part Name | No. of Transactions |");

            while (rs.next()) {
                System.out.printf("| %d | %s | %d |\n",
                        rs.getInt("pID"),
                        rs.getString("pName"),
                        rs.getInt("totalTransactions"));
            }

            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.err.println("Error fetching popular parts: " + e.getMessage());
        }
    }
}