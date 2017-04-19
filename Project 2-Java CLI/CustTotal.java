import java.util.*;
import java.net.*;
import java.text.*;
import java.lang.*;
import java.io.*;
import java.sql.*;



public class CustTotal {
    private Connection conDB; // Connection to the database system.
    private String url;
    private String id;
    private Integer custID;
    private String custName; // Name of that customer.
    private String custCity;
    private String inputID;
    private String inCat;
    private Integer flag = 1;
    private ArrayList < String > cat;
    private ArrayList < String > titles;
    private String title;
    private Integer year;
    private String language;
    private Integer weight;
    private Double minPrice;
    private Integer quantity;
    private Double totalPrice;
    private String clubs;
    private boolean bCat;
    private Integer choice = 0;
    private String purchase = "";
    private boolean contains;
    Map < Integer, ArrayList < String >> map = new HashMap < Integer, ArrayList < String >> ();
    int mapSize;
    //   private Double zero=0.0;


    // Constructor
    public CustTotal() {
        // Set up the DB connection.
        try {
            // Register the driver with DriverManager.
            Class.forName("com.ibm.db2.jcc.DB2Driver").newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (InstantiationException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(0); // Who are we tallying?
        }

        // URL: Which database?
        url = "jdbc:db2:c3421m";

        // Initialize the connection.
        try {
            // Connect with a fall-thru id & password
            conDB = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.print("\nSQL: database connection error.\n");
            System.out.println(e.toString());
            System.exit(0);
        }


        System.out.print("Enter Customer ID: ");
        Scanner in = new Scanner(System.in);
        Scanner in2 = new Scanner(System.in);


        id = in .nextLine();

        while (!find_customer(id)) {
            System.out.print("The ID that you have provided isn't in our database. Try again: ");
            id = in .nextLine();
        }

        while (flag == 1) {
            flag = 0;
            cat = new ArrayList < String > ();
            titles = new ArrayList < String > ();

            cat = fetch_categories();

            System.out.println();
            while (mapSize < 1) {
                System.out.println("Categories are:");
                for (String s: cat) {
                    System.out.print(s + "|");
                }
                System.out.println();
                System.out.print("\nEnter one of the above categories fam: ");
                inCat = in .nextLine().toLowerCase();
                System.out.println();

                while (!cat.contains(inCat)) {
                    System.out.println("The category entered is incorrect! Try again. ");
                    System.out.println("\nCategories are:");

                    for (String s: cat) {
                        System.out.print(s + "|");
                    }
                    System.out.print("\nEnter one of the above categories fam: ");
                    inCat = in .nextLine().toLowerCase();
                }

                System.out.println("Books associated with the category: " + inCat + "\n");
                titles = get_titles(inCat);

                for (String s: titles) {
                    System.out.println(s);
                }

                System.out.print("\nEnter the title from the list above: ");
                title = in .nextLine();
                bCat = find_book(title, inCat);

                mapSize = map.size();

                if (mapSize == 0) {
                    System.out.println("No book exists! Try again!\n");
                    continue;
                } else {
                    while (choice < 1 || choice > mapSize) {
                        System.out.print("Enter the book number to pick the book: ");
                        choice = in .nextInt();
                    }

                    title = map.get(choice).get(0);
                    year = Integer.parseInt(map.get(choice).get(1));
                    language = map.get(choice).get(2);
                    weight = Integer.parseInt((map.get(choice).get(3)));

                }

            }

            while (flag < 2) {
                System.out.print("Buy book? (Enter yes or no): ");
                purchase = in2.nextLine().toLowerCase();

                if (purchase.equals("yes")) {
                    contains = min_price(inCat, title, year, custID);
                    fetch_club(minPrice);
                    /*if (!contains) { //
                        System.out.println("This book doesn't exist in clubs that you are a member of. Pick another book.");
                        flag = 2; //breaks out			
                        System.out.println(flag);
                        break;
                    }*/

                    while (flag < 1) {
                        System.out.printf("Minimum price for the book: $%.2f\n", minPrice);

                        System.out.print("Enter quantity: ");
                        quantity = in2.nextInt();
                        while (quantity <= 0) {
                            System.out.print("The number you have entered makes no sense. Try Again: ");
                            quantity = in2.nextInt();
                        }
                        totalPrice = minPrice * quantity;
                        insert_purchase(custID, clubs, title, year, quantity);
                        System.out.println("\nTransaction Complete!\n");
                        System.out.printf("Here are the details: \nCustomer ID: %d        Club: %s        Title: %s        Year: %d       Quantity: %d       Price: $%.2f", custID, clubs, title, year, quantity, totalPrice);
                        System.out.println();
                        System.out.println("Have a nice day and please visit us again");
                        flag = 1;
                    }
                    flag = 2;
                } else if (purchase.equals("no")) {
                    System.out.println("That's too bad. Have a nice day.");
                    break;
                } else {
                    System.out.println("Wrong choice. Try again!\n");
                }
            }

        }


        // Let's have autocommit turned off.  No particular reason here.
        try {
            conDB.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.print("\nFailed trying to turn autocommit off.\n");
            e.printStackTrace();
            System.exit(0);
        }


        // Commit.  Okay, here nothing to commit really, but why not... 
        try {
            conDB.commit();
        } catch (SQLException e) {
            System.out.print("\nFailed trying to commit.\n");
            e.printStackTrace();
            System.exit(0);
        }


        // Close the connection.
        try {
            conDB.close();
        } catch (SQLException e) {
            System.out.print("\nFailed trying to close the connection.\n");
            e.printStackTrace();
            System.exit(0);
        }

    }

//Method 1: Fetches information associated with the customer
    public boolean find_customer(String ID) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.

        boolean inDB = false; // Return.

        queryText =
            "Select *" +"from yrb_customer " +"where cid = ? ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#1 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.http://eecs.lassonde.yorku.ca/
        try {
            querySt.setInt(1, Integer.parseInt(ID));
            answers = querySt.executeQuery();
        } catch (SQLException e) {
            System.out.println("SQL#1 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;
                custID = answers.getInt("cid");
                custName = answers.getString("name");
                custCity = answers.getString("city");
                System.out.println("Customer ID: " + custID + "       Customer Name: " + custName + "        City: " + custCity);
            } else {
                inDB = false;
                custName = null;
            }
        } catch (SQLException e) {
            System.out.println("SQL#1 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#1 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#1 failed closing the handle.\n");
            System.out.println(e.toString());

            System.exit(0);
        }

        return inDB;
    }

//Method 2: Displays all categories for the user to pick from
    public ArrayList < String > fetch_categories() {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.
        ArrayList < String > str = new ArrayList < String > ();
        queryText = "Select *" + "from yrb_category ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#2 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            //querySt.setInt(1, Integer.parseInt(ID));
            answers = querySt.executeQuery();
        } catch (SQLException e) {
            System.out.println("SQL#2 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            for (int i = 1; answers.next(); i++) {
                String category = answers.getString("cat");
                str.add(category);
            }
        } catch (SQLException e) {
            System.out.println("SQL#2 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#2 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#2 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return str;
    }

//Method 3: Gets the titles of books from a certain category which are offered by the club that each person belongs to
    public ArrayList < String > get_titles(String categories) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.
        ArrayList < String > str = new ArrayList < String > ();
        queryText = "Select distinct title " + " from yrb_book where cat = ? "+"and title in (select o.title from " +
            		  "yrb_offer o " + "where o.club in " + "(Select club " + "from yrb_member " +
            		  "where cid = ?)) and year in " + "(select o.year " + "from yrb_offer o " +
            		  "Where o.club in (Select club " + "from yrb_member " + "where cid = ?))";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#3 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, categories);
            querySt.setInt(2, custID);
            querySt.setInt(3, custID);
            answers = querySt.executeQuery();
        } catch (SQLException e) {
            System.out.println("SQL#3 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            for (int i = 1; answers.next(); i++) {
                String titles = answers.getString("title");
                str.add(titles);
            }
        } catch (SQLException e) {
            System.out.println("SQL#3 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#3 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#3 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        return str;
    }

//Method 4: Finds the entered book and displays its details to the customer
    public boolean find_book(String bTitle, String category) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.
        // ArrayList<String> books=new ArrayList<String>();
        queryText = "Select * " + "from yrb_book " + "where title = ?" + " and cat = ?";
        boolean inDB = false;
		  String titles;
        Integer weights;
        String languages;
        Integer years;
    
        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#4 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, bTitle);
            querySt.setString(2, category);
            //querySt.setInt(1, Integer.parseInt(ID));
            answers = querySt.executeQuery();
        } catch (SQLException e) {

            System.out.println("SQL#4 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }
        int i;
        // Any answer?
        try {
            for (i = 0; answers.next(); i++) {
                titles = answers.getString("title");
                years = answers.getInt("year");
                languages = answers.getString("language");
                weights = answers.getInt("weight");
                map.put(i + 1, new ArrayList < String > (Arrays.asList(titles, Integer.toString(years), languages, Integer.toString(weights))));
                //System.out.println("Title: "+titles+"\tYear: "+years +"\tLanguage: "+languages+ "\tWeight: "+weights);	   

            }
            if (i > 0) {
                inDB = true;
                int j = 1;
                while (j <= i) {
                    titles = map.get(j).get(0);
                    years = Integer.parseInt(map.get(j).get(1));
                    languages = map.get(j).get(2);
                    weights = Integer.parseInt((map.get(j).get(3)));
                    System.out.println("\nBook #" + j);
                    System.out.println("Title:  " + titles + "        Year:  " + years + "        Language:  " + languages + "        Weight:  " + weights);
                    j++;
                }
            } else {
                inDB = false;

            }

        } catch (SQLException e) {
            System.out.println("SQL#4 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#4 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#4 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
        return inDB;
    }

//Method 5: Returns the minimum price associated with the title from the clubs
    public boolean min_price(String category, String titles, int years, int customerID) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.
        //      ArrayList<String> books=new ArrayList<String>();
        Double price = 0.0;
        boolean inDB = false;
        queryText = " Select min(price)" + "from yrb_offer where title = ? AND year = ? " +
                    "and club in (select club FROM yrb_member where cid = ?)";
        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#5 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {
            querySt.setString(1, titles);
            //querySt.setString(2, category);
            querySt.setInt(2, years);
            querySt.setInt(3, customerID);
            //querySt.setInt(1, Integer.parseInt(ID));
            answers = querySt.executeQuery();
        } catch (SQLException e) {

            System.out.println("SQL#5 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Any answer?
        try {
            if (answers.next()) {
                inDB = true;
                minPrice = answers.getDouble(1);
                //clubs=answers.getString("club");

            } else {
                inDB = false;
            }


        } catch (SQLException e) {
            System.out.println("SQL#5 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#5 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#5 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
        return inDB;
    }

//Method 6: Inserts the details of each purchase in the database
    public void insert_purchase(int cid, String club, String titles, int years, int qnty) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.

        Timestamp timeStamp = new Timestamp(System.currentTimeMillis());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss");
        String finalStamp = dateFormat.format(timeStamp);

        queryText = "Insert into yrb_purchase values (?,?,?,?,?,?) ";
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#6 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }

        // Execute the query.
        try {

            querySt.setInt(1, cid);
            querySt.setString(2, club);
            querySt.setString(3, titles);
            querySt.setInt(4, years);
            querySt.setString(5, finalStamp);
            querySt.setInt(6, qnty);
            querySt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("SQL#6 failed in update");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#6 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

    }

//Helper method that returns the club for each purchase
    private void fetch_club(double price) {
        String queryText = ""; // The SQL text.
        PreparedStatement querySt = null; // The query handle.
        ResultSet answers = null; // A cursor.


        queryText = "Select o.club FROM yrb_member m, yrb_offer o" +" where o.club = m.club AND o.year = ? " +
            		  "and m.cid = ? and o.title = ? and o.price = ? ";

        // Prepare the query.
        try {
            querySt = conDB.prepareStatement(queryText);
        } catch (SQLException e) {
            System.out.println("SQL#7 failed in prepare");
            System.out.println(e.toString());
            System.exit(0);
        }
        // Execute the query.
        try {
            querySt.setInt(1, year);
            querySt.setInt(2, custID);
            querySt.setString(3, title);
            querySt.setDouble(4, minPrice);
            answers = querySt.executeQuery();
        } catch (SQLException e) {
            System.out.println("SQL#7 failed in execute");
            System.out.println(e.toString());
            System.exit(0);
        }

        // see if it works 
        try {
            if (answers.next()) {
                clubs = answers.getString(1);
            }
        } catch (SQLException e) {
            System.out.println("SQL#7 failed in cursor.");
            System.out.println(e.toString());
            System.exit(0);
        }
        // Close the cursor.
        try {
            answers.close();
        } catch (SQLException e) {
            System.out.print("SQL#7 failed closing cursor.\n");
            System.out.println(e.toString());
            System.exit(0);
        }

        // We're done with the handle.
        try {
            querySt.close();
        } catch (SQLException e) {
            System.out.print("SQL#7 failed closing the handle.\n");
            System.out.println(e.toString());
            System.exit(0);
        }
    }

    public static void main(String[] args) {

        CustTotal ct = new CustTotal();
    }
}