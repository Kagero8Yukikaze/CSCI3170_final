import java.io.*;
import java.util.*;
import java.sql.*;
import java.text.*;

// Contains every action of administrator
public class Administrator{
    private Connection connection;

    public Administrator(Connection connection) {
        this.connection = connection;
    }

    public void showAdminMenu() {
        Scanner scanner = new Scanner(System.in);
        int choice;
        // Show selections
        do {
            System.out.println("-----Operations for administrator menu-----");
            System.out.println("What kinds of operation would you like to perform?");
            System.out.println("1. Create all tables");
            System.out.println("2. Delete all tables");
            System.out.println("3. Load from datafile");
            System.out.println("4. Show content of a table");
            System.out.println("5. Return to the main menu");
            System.out.print("Enter Your Choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    createAllDatabases();
                    break;
                case 2:
                    dropAllDatabases();
                    break;
                case 3:
                    loadData();
                    break;
                case 4:
                    showData();
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

    private void createAllDatabases() {
        System.out.print("Processing...");
        // Create all five tables
        String[] createTableSQL = {
            "CREATE TABLE category( " +
            "cID NUMBER NOT NULL PRIMARY KEY, " + 
            "cName VARCHAR2(20) NOT NULL" +  
            ")",

            "CREATE TABLE manufacturer( " +
            "mID NUMBER NOT NULL PRIMARY KEY," +
            "mName VARCHAR2(20) NOT NULL, " +
            "mAddress VARCHAR2(50) NOT NULL, " +
            "mPhoneNumber NUMBER NOT NULL " +
            ")",

            "CREATE TABLE part( " +
            "pID NUMBER NOT NULL PRIMARY KEY, " +
            "pName VARCHAR2(20) NOT NULL, " +
            "pPrice NUMBER NOT NULL, " +
            "mID NUMBER NOT NULL REFERENCES manufacturer(mID), " +
            "cID NUMBER NOT NULL REFERENCES category(cID), " +
            "pWarrantyPeriod NUMBER NOT NULL, " +
            "pAvailableQuantity NUMBER NOT NULL " +
            ")",
            
            "CREATE TABLE salesperson( " +
            "sID NUMBER NOT NULL PRIMARY KEY, " +
            "sName VARCHAR2(20) NOT NULL, " +
            "sAddress VARCHAR2(50) NOT NULL, " +
            "sPhoneNumber NUMBER NOT NULL, " +
            "sExperience NUMBER NOT NULL " +
            ")",

            "CREATE TABLE transaction( " +
            "tID NUMBER NOT NULL PRIMARY KEY, " +
            "pID NUMBER NOT NULL REFERENCES part(pID), " +
            "sID NUMBER NOT NULL REFERENCES salesperson(sID), " +
            "tDate DATE NOT NULL" +
            ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTableSQL) {
                try {
                    stmt.executeUpdate(sql);
                    // System.out.println("Create table");
                } catch (SQLException e) {
                    System.err.println("Error creating table: " + e.getMessage());
                }
            }
            System.out.println("Done! Database is initialized!\n");
        } catch (SQLException e) {
            System.err.println("Error creating databases: " + e.getMessage());
        }
    }

    private void dropAllDatabases() {
        System.out.print("Processing...");
        // Delete all five tables
        String[] dropTableSQL = {
            "DROP TABLE transaction",
            "DROP TABLE salesperson",
            "DROP TABLE part",
            "DROP TABLE manufacturer",
            "DROP TABLE category"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : dropTableSQL) {
                try {
                    stmt.executeUpdate(sql);
                    //System.out.println("Delete table");
                } catch (SQLException e) {
                    // if error code is 942, it means that tables are not existed, just simply ignore it
                    if(e.getErrorCode() != 942)
                        System.err.println("Error dropping table: " + e.getMessage());
                }
            }
            System.out.println("Done! Database is removed!\n");
        } catch (SQLException e) {
            System.err.println("Error dropping databases: " + e.getMessage());
        }
    }

    private void loadData() {
        // Load from datafile
        Scanner scanner = new Scanner(System.in);
        System.out.print("Type in the Source Data Folder Path: ");
        String folderPath = scanner.nextLine().trim();

        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            System.out.println("Invalid directory. Exiting.");
            return;
        }
        
        System.out.print("Processing...");
        // We need to process in order because of the foreign keys
        File categoryFile = new File(folder, "category.txt");
        if (categoryFile.exists()) {
            //System.out.println("Loading data for table: category");
            try (Scanner fileScanner = new Scanner(categoryFile)) {
                loadCategoryData(fileScanner);
            } catch (FileNotFoundException e) {
                System.out.println("Error reading file: category.txt");
            }
        } else {
            System.out.println("category.txt file not found.");
        }

        File manufacturerFile = new File(folder, "manufacturer.txt");
        if (manufacturerFile.exists()) {
            //System.out.println("Loading data for table: manufacturer");
            try (Scanner fileScanner = new Scanner(manufacturerFile)) {
                loadManufacturerData(fileScanner);
            } catch (FileNotFoundException e) {
                System.out.println("Error reading file: manufacturer.txt");
            }
        } else {
            System.out.println("manufacturer.txt file not found.");
        }

        File partFile = new File(folder, "part.txt");
        if (partFile.exists()) {
            //System.out.println("Loading data for table: part");
            try (Scanner fileScanner = new Scanner(partFile)) {
                loadPartData(fileScanner);
            } catch (FileNotFoundException e) {
                System.out.println("Error reading file: part.txt");
            }
        } else {
            System.out.println("part.txt file not found.");
        }

        File salespersonFile = new File(folder, "salesperson.txt");
        if (salespersonFile.exists()) {
            //System.out.println("Loading data for table: salesperson");
            try (Scanner fileScanner = new Scanner(salespersonFile)) {
                loadSalespersonData(fileScanner);
            } catch (FileNotFoundException e) {
                System.out.println("Error reading file: salesperson.txt");
            }
        } else {
            System.out.println("salesperson.txt file not found.");
        }

        File transactionFile = new File(folder, "transaction.txt");
        if (transactionFile.exists()) {
            //System.out.println("Loading data for table: transaction");
            try (Scanner fileScanner = new Scanner(transactionFile)) {
                loadTransactionData(fileScanner);
            } catch (FileNotFoundException e) {
                System.out.println("Error reading file: transaction.txt");
            }
        } else {
            System.out.println("transaction.txt file not found.");
        }
        System.out.println("Done! Data is inputted to the database!\n");
    }

    // Load Data one by one
    private void loadCategoryData(Scanner fileScanner) {
        String insertSQL = "INSERT INTO category (cID, cName) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split("\t");
                if (data.length == 2) {
                    pstmt.setInt(1, Integer.parseInt(data[0].trim()));
                    pstmt.setString(2, data[1].trim());
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data into category table: " + e.getMessage());
            return;
        }
    }

    private void loadManufacturerData(Scanner fileScanner) {
        String insertSQL = "INSERT INTO manufacturer (mID, mName, mAddress, mPhoneNumber) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split("\t");
                if (data.length == 4) {
                    pstmt.setInt(1, Integer.parseInt(data[0].trim()));
                    pstmt.setString(2, data[1].trim());
                    pstmt.setString(3, data[2].trim());
                    pstmt.setInt(4, Integer.parseInt(data[3].trim()));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data into manufacturer table: " + e.getMessage());
        }
    }

    private void loadSalespersonData(Scanner fileScanner) {
        String insertSQL = "INSERT INTO salesperson (sID, sName, sAddress, sPhoneNumber, sExperience) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split("\t");
                if (data.length == 5) {
                    pstmt.setInt(1, Integer.parseInt(data[0].trim()));
                    pstmt.setString(2, data[1].trim());
                    pstmt.setString(3, data[2].trim());
                    pstmt.setInt(4, Integer.parseInt(data[3].trim()));
                    pstmt.setInt(5, Integer.parseInt(data[4].trim()));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data into salesperson table: " + e.getMessage());
        }
    }

    private void loadPartData(Scanner fileScanner) {
        String insertSQL = "INSERT INTO part (pID, pName, pPrice, mID, cID, pWarrantyPeriod, pAvailableQuantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split("\t");
                if (data.length == 7) {
                    pstmt.setInt(1, Integer.parseInt(data[0].trim()));
                    pstmt.setString(2, data[1].trim());
                    pstmt.setInt(3, Integer.parseInt(data[2].trim()));
                    pstmt.setInt(4, Integer.parseInt(data[3].trim()));
                    pstmt.setInt(5, Integer.parseInt(data[4].trim()));
                    pstmt.setInt(6, Integer.parseInt(data[5].trim()));
                    pstmt.setInt(7, Integer.parseInt(data[6].trim()));
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data into part table: " + e.getMessage());
        }
    }

    private void loadTransactionData(Scanner fileScanner) {
        String insertSQL = "INSERT INTO transaction (tID, pID, sID, tDate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                String[] data = line.split("\t");
                if (data.length == 4) {
                    pstmt.setInt(1, Integer.parseInt(data[0].trim()));
                    pstmt.setInt(2, Integer.parseInt(data[1].trim()));
                    pstmt.setInt(3, Integer.parseInt(data[2].trim()));

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    java.util.Date date = null;
                    try {
                        date = sdf.parse(data[3]);
                    } catch (ParseException e) {
                        System.err.println("Error parsing date for line: " + line);
                        continue; 
                    }
                    java.sql.Date sqlDate = new java.sql.Date(date.getTime());

                    pstmt.setDate(4, sqlDate);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting data into transaction table: " + e.getMessage());
        }
    }

    private void showData(){
        // Show content of a table
        Scanner scanner = new Scanner(System.in);
        System.out.print("Which table would you like to show: ");
        String tableName = scanner.nextLine().trim();

        // handle corner cases (invalid name or table not created)
        if(!validateTable(tableName)){
            return;
        }
        System.out.println("Content of table " + tableName + ":");

        String query = "SELECT * FROM " + tableName;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            for (int i = 1; i <= columnCount; i++) {
                System.out.print("| " + metaData.getColumnName(i) + " ");
            }
            System.out.println("|");

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    int columnType = metaData.getColumnType(i);
                    // Print Date in dd/mm/yyyy
                    if (columnType == Types.DATE || columnType == Types.TIMESTAMP) {
                        java.sql.Date sqlDate = rs.getDate(i);
                        if (sqlDate != null) {
                            System.out.print("| " + sdf.format(sqlDate) + " ");
                        } else {
                            System.out.print("| NULL ");
                        }
                    } else {
                        System.out.print("| " + rs.getString(i) + " ");
                    }
                }
                System.out.println("|");
            }

            System.out.println("End of Query\n");
        } catch (SQLException e) {
            System.err.println("Error fetching table content: " + e.getMessage());
        }
    }

    private boolean validateTable(String tableName){
        boolean NameExist = false;
        if (tableName.equals("category") || tableName.equals("manufacturer") || 
            tableName.equals("part") || tableName.equals("salesperson") ||
            tableName.equals("transaction")){
            NameExist = true;
        }
        if (NameExist){
            String query = "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setString(1, tableName.toUpperCase());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("Error checking table existence: " + e.getMessage());
            }
            System.out.println("Table has not been created!\n");
            return false;
        }
        else{
            System.out.println("Table Name doesn't exist!\n");
            return false;
        }
    }
}