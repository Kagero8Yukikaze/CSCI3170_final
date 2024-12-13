import java.sql.*;
import java.util.Scanner;

public class DataBase {
    // Main menu
    public void showMenu(Connection connection) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            // Show selections
            System.out.println("Welcome to sales system!\n");
            System.out.println("-----Main menu-----");
            System.out.println("What kinds of operation would you like to perform?");
            System.out.println("1. Operations for administrator");
            System.out.println("2. Operations for salesperson");
            System.out.println("3. Operations for manager");
            System.out.println("4. Exit this program");
            System.out.print("Enter Your Choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    Administrator admin = new Administrator(connection);
                    admin.showAdminMenu();
                    break;
                case 2:
                    Salesperson sales = new Salesperson(connection);
                    sales.showSalesMenu();
                    break;
                case 3:
                    Manager mana = new Manager(connection);
                    mana.showManaMenu();
                    break;
                case 4:
                    System.out.println("Exit Oracle Database");
                    break;
                default:
                    System.out.println("Invalid! Please enter between 1 to 4!");
                    break;
            }
        } while (choice != 4);

        scanner.close();
    }

    public static void main(String[] args) {
        DataBase dbms = new DataBase();

        Connection connection = DatabaseConnection.getConnection();
        if (connection == null) {
            System.out.println("Failed to establish database connection. Exiting...");
            return;
        }

        dbms.showMenu(connection);

        DatabaseConnection.closeConnection(); 
        
    }
}
