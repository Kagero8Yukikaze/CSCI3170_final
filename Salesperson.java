import java.io.*;
import java.util.*;
import java.sql.*;
import java.text.*;

// Contains every action of Salesperson
public class Salesperson{
    private Connection connection;

    public Salesperson(Connection connection) {
        this.connection = connection;
    }

    public void showSalesMenu(){
        Scanner scanner = new Scanner(System.in);
        int choice;
        // Show selections
        do {
            System.out.println("-----Operations for salesperson menu-----");
            System.out.println("What kinds of operation would you like to perform?");
            System.out.println("1. Search for parts");
            System.out.println("2. Sell a part");
            System.out.println("3. Return to the main menu");
            System.out.print("Enter Your Choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    SearchForParts();
                    break;
                case 2:
                    SellPart();
                    break;
                case 3:
                    System.out.println("Returning to main menu...\n");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 1 to 3!");
                    break;
            }
        } while (choice != 3);
    }

    private void SearchForParts(){
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose the Search criterion:");
        System.out.println("1. Part Name");
        System.out.println("2. Manufacturer Name");
        System.out.print("Choose the search criterion: ");
        int searchCriterion = scanner.nextInt();
        scanner.nextLine(); 

        String searchKeyword = "";
        String searchQuery = "";

        if (searchCriterion == 1){
            System.out.print("Type in the Search Keyword: ");
            searchKeyword = scanner.nextLine().trim();
            searchQuery = "SELECT p.pID, p.pName, m.mName, c.cName, p.pAvailableQuantity, p.pWarrantyPeriod, p.pPrice " +
                    "FROM part p " +
                    "JOIN manufacturer m ON p.mID = m.mID " +
                    "JOIN category c ON p.cID = c.cID " +
                    "WHERE p.pName LIKE ?";
        }
        else if (searchCriterion ==2){
            System.out.print("Type in the Search Keyword: ");
            searchKeyword = scanner.nextLine().trim();
            searchQuery = "SELECT p.pID, p.pName, m.mName, c.cName, p.pAvailableQuantity, p.pWarrantyPeriod, p.pPrice " +
                    "FROM part p " +
                    "JOIN manufacturer m ON p.mID = m.mID " +
                    "JOIN category c ON p.cID = c.cID " +
                    "WHERE m.mName LIKE ?";
        }
        else{
            System.out.println("Invalid choice. Please enter 1 to 2!");
            return;
        }

        System.out.println("Choose ordering:");
        System.out.println("1. By price, ascending order");
        System.out.println("2. By price, descending order");
        System.out.print("Choose the search criterion: ");
        int sortChoice = scanner.nextInt();

        if (sortChoice == 1) {
            searchQuery += " ORDER BY p.pPrice ASC";
        } else if (sortChoice == 2) {
            searchQuery += " ORDER BY p.pPrice DESC";
        } else {
            System.out.println("Invalid choice. Please enter 1 to 2!");
            return;
        }

        try (PreparedStatement pstmt_search = connection.prepareStatement(searchQuery)) {
            pstmt_search.setString(1, "%" + searchKeyword + "%");
            ResultSet rs_search = pstmt_search.executeQuery();

            if(!rs_search.next()){
                System.out.println("No matching parts found!");
                return;
            }

            System.out.println("| ID | Name | Manufacturer | Category | Quantity | Warranty | Price |");
            while (rs_search.next()) {
                System.out.printf("| %d | %s | %s | %s | %d | %d | %d |\n",
                        rs_search.getInt("pID"),
                        rs_search.getString("pName"),
                        rs_search.getString("mName"),
                        rs_search.getString("cName"),
                        rs_search.getInt("pAvailableQuantity"),
                        rs_search.getInt("pWarrantyPeriod"),
                        rs_search.getInt("pPrice"));
            }
        } catch (SQLException e) {
            System.err.println("Error searching parts: " + e.getMessage());
        }

        System.out.println("End of Query\n");
    }

    private void SellPart(){
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter The Part ID: ");
        int partID = scanner.nextInt();

        String checkPartQuery = "SELECT pAvailableQuantity, pName FROM part WHERE pID = ?";
        try (PreparedStatement pstmt_part = connection.prepareStatement(checkPartQuery)) {
            pstmt_part.setInt(1, partID);
            ResultSet rs_part = pstmt_part.executeQuery();

            if (!rs_part.next()) {
                System.out.println("Error: Part ID " + partID + " does not exist.");
                return;
            }

            System.out.print("Enter The Salesperson ID: ");
            int salespersonID = scanner.nextInt();
            
            String checkSalespersonQuery = "SELECT COUNT(*) FROM salesperson WHERE sID = ?";
            try (PreparedStatement pstmt_sale = connection.prepareStatement(checkSalespersonQuery)) {
                pstmt_sale.setInt(1, salespersonID);
                ResultSet rs_sale = pstmt_sale.executeQuery();

                if (!rs_sale.next() || rs_sale.getInt(1) < 1){
                    System.out.println("Error: Salesperson ID " + salespersonID + " does not exist.");
                    return;
                }
            }

            int availableQuantity = rs_part.getInt("pAvailableQuantity");
            String partName = rs_part.getString("pName");

            if (availableQuantity > 0) {

                String updatePartQuery = "UPDATE part SET pAvailableQuantity = pAvailableQuantity - 1 WHERE pID = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updatePartQuery)) {
                    updateStmt.setInt(1, partID);
                    int rowsUpdated = updateStmt.executeUpdate();

                    if (rowsUpdated > 0) {
                        String selectMaxTIDQuery = "SELECT MAX(tID) FROM transaction";
                        // Need to insert a new transaction ID
                        try (Statement stmt = connection.createStatement()){
                            ResultSet rs_tid = stmt.executeQuery(selectMaxTIDQuery);
                            int newTID = 1;
                            if (rs_tid.next()) {
                                newTID = rs_tid.getInt(1) + 1; 
                            }

                            // Apply insertion of transaction
                            String insertTransactionQuery = "INSERT INTO transaction (tID, pID, sID, tDate) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement insertStmt = connection.prepareStatement(insertTransactionQuery)) {
                                insertStmt.setInt(1, newTID);
                                insertStmt.setInt(2, partID);
                                insertStmt.setInt(3, salespersonID);
                                insertStmt.setDate(4, new java.sql.Date(System.currentTimeMillis())); 
                                insertStmt.executeUpdate();
                            } catch (SQLException e) {
                                System.err.println("Error during inserting into transaction: " + e.getMessage());
                            }

                            System.out.print("Product: " + partName + "(id: " + partID +")");
                            System.out.print("Remaining Quality: ");
                            System.out.print(availableQuantity - 1 + "\n");
                            System.out.println("End of Query\n");

                        } catch (SQLException e) {
                            System.err.println("Error during finding transaction max ID: " + e.getMessage());
                        }
                        
                    }
                }
            } else {
                // If quantity of the part is zero
                System.out.println("Error: The part with ID " + partID + " is out of stock.");
                return;
            }
        } catch (SQLException e) {
            // Not a valid part ID
            System.err.println("Error during finding part ID: " + e.getMessage());
        }
    }
}