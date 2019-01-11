package urjc.isi.pruebasSparkJava;

import static spark.Spark.*;
import spark.Request;
import spark.Response;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import javax.servlet.MultipartConfigElement;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Main {
	
    // Connection to the SQLite database. Used by insert and select methods.
    // Initialized in main
    private static Connection connection;
	
    static int getHerokuAssignedPort() {
    	ProcessBuilder processBuilder = new ProcessBuilder();
    	if (processBuilder.environment().get("PORT") != null) {
    		return Integer.parseInt(processBuilder.environment().get("PORT"));
    	}
    	return 4600; //return default port if heroku-port isn't set (i.e. on localhost)
    }
    
    // Used to illustrate how to route requests to methods instead of
    // using lambda expressions
    public static String doSelect(Request request, Response response) {
    	return select (connection, request.params(":table"), request.params(":film"));
    }
    
    public static String select(Connection conn, String table, String film) {
    	String sql = "SELECT * FROM " + table + " WHERE film=?";
    	String result = new String();
	
    	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    		pstmt.setString(1, film);
    		ResultSet rs = pstmt.executeQuery();
    		// Commit after query is executed
    		connection.commit();

    		while (rs.next()) {
    			// read the result set
    			result += "film = " + rs.getString("film") + "\n";
    			System.out.println("film = "+rs.getString("film") + "\n");

    			result += "actor = " + rs.getString("actor") + "\n";
    			System.out.println("actor = "+rs.getString("actor")+"\n");
    		}
    	} catch (SQLException e) {
    		System.out.println(e.getMessage());
    	}
    	return result;
    }
    
    public static void insert(Connection conn, String film, String actor) {
    	String sql = "INSERT INTO films(film, actor) VALUES(?,?)";

    	try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    		pstmt.setString(1, film);
    		pstmt.setString(2, actor);
    		pstmt.executeUpdate();
    	} catch (SQLException e) {
    		System.out.println(e.getMessage());
    	}
    }
    
    public static String infoPost(Request request, Response response) throws 
    		ClassNotFoundException, URISyntaxException {
    	String result = new String("TODA LA INFORMACIÓN QUE QUIERAS SOBRE PELÍCULAS"
        							+ " A TRAVÉS DE UN POST");
    	return result;
    }

    public static String infoGet(Request request, Response response) throws
    		ClassNotFoundException, URISyntaxException {
    	String result = new String("TODA LA INFORMACIÓN QUE QUIERAS SOBRE PELÍCULAS"
        							+ " A TRAVÉS DE UN GET");
    	return result;
    }

    public static String doWork(Request request, Response response) throws
    		ClassNotFoundException, URISyntaxException {
    	String result = new String("Hello World");
    	return result;
    }

    
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
    	// Establecemos el puerto del server con el método getHerokuAssignedPort()
    	port(getHerokuAssignedPort());

    	// Connect to SQLite sample.db database
    	// connection will be reused by every query in this simplistic example
    	connection = DriverManager.getConnection("jdbc:sqlite:sample.db");

    	// SQLite default is to auto-commit (1 transaction / statement execution)
    	// Set it to false to improve performance
    	connection.setAutoCommit(false);

    	String home = "<html><body>" +
    		"<h1>Bienvenidos a la web de películas</h1>" +
    			"<form action='/info' method='post'>" +
    				"<div class='button'>" +
    					"Ir a info: <br/>" +
    					"<button type='submit'>Información</button>" +
    				"</div>" +
    			"</form></br>"+
    			"<form action='/hello' method='get'>" +
    				"<div class='button'>" +
    					"Ir a helloWorld: <br/>" +
    					"<button type='submit'>Hello</button>" +
    				"</div>" +
    			"</form>" +
    			"<form action='/upload_films' method='get'>" +
    				"<div class='button'>" +
    					"Subir fichero con películas: <br/>" +
    					"<button type='submit'>Upload Films</button>" +
    				"</div>" +
    			"</form>" +
    			"<form action='/addfilms' method='get'>" +
    				"<div class='button'>" +
    					"Añade película: <br/>" +
    					"<button type='submit'>Add Films</button>" +
    				"</div>" +
    			"</form>" +
    			"<a href='/filter'>Filtrado</a>" +
    		"</body></html>";

        // spark server
        get("/", (req, res) -> home);
        get("/info", Main::infoGet);
        post("/info", Main::infoPost);
        get("/hello", Main::doWork);

        // In this case we use a Java 8 method reference to specify
        // the method to be called when a GET /:table/:film HTTP request
        // Main::doWork will return the result of the SQL select
        // query. It could've been programmed using a lambda
        // expression instead, as illustrated in the next sentence.
        get("/:table/:film", Main::doSelect);

        // In this case we use a Java 8 Lambda function to process the
        // GET /upload_films HTTP request, and we return a form
        get("/upload_films", (req, res) -> 
        	"<form action='/upload' method='post' enctype='multipart/form-data'>" 
        	+ "    <input type='file' name='uploaded_films_file' accept='.txt'>"
        	+ "    <button>Upload file</button>" + "</form>");
        // You must use the name "uploaded_films_file" in the call to
        // getPart to retrieve the uploaded file. See next call:

        // Retrieves the file uploaded through the /upload_films HTML form
        // Creates table and stores uploaded file in a two-columns table
        post("/upload", (req, res) -> {
        	req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/tmp"));
        	String result = "File uploaded!";
        	
        	try (InputStream input = req.raw().getPart("uploaded_films_file").getInputStream()) { 
        		// getPart needs to use the same name "uploaded_films_file" used in the form

        		// Prepare SQL to create table
        		Statement statement = connection.createStatement();
        		statement.setQueryTimeout(30); // set timeout to 30 sec.
        		statement.executeUpdate("drop table if exists films");
        		statement.executeUpdate("create table films (film string, actor string)");

        		// Read contents of input stream that holds the uploaded file
        		InputStreamReader isr = new InputStreamReader(input);
        		BufferedReader br = new BufferedReader(isr);
        		String s;
        		
        		while ((s = br.readLine()) != null) {
        			System.out.println(s);

        			// Tokenize the film name and then the actors, separated by "/"
        			StringTokenizer tokenizer = new StringTokenizer(s, "/");

        			// First token is the film name(year)
        			String film = tokenizer.nextToken();

        			// Now get actors and insert them
        			while (tokenizer.hasMoreTokens()) {
        				insert(connection, film, tokenizer.nextToken());
        			}
        			// Commit only once, after all the inserts are done
        			// If done after each statement performance degrades
        			connection.commit();
        		}
        		input.close();
        	}
        	return result;
        });

        get("/addfilms", (req, res) ->
    		"<div style='color:#1A318C'><b>PÁGINA PARA AÑADIR PELÍCULA A LA BASE DE DATOS:</b>"
    		+"<form action='/add_films' method='post'>" +
    		"<label for='film'>Película que desea buscar: </label>" + //required: campo obligatorio
    		"<input type='text' required name='film' id='film'" +
    		"pattern=[A-Za-z0-9 ].{1,}>" + //Excluimos todos lo que no sean letras y numeros, y no se puede dejar el campo vacio.
    		"<p></p>"+
    		"<form action='/add_films' method='post'>" +
    		"<label for='year'>Año: </label>" + 
    		"<input type='text' required name='year' id='year'" + //required: Campo obligatorio
    		"pattern=[0-9]{4}>" + //Solo se pueden introducir 4 cifras
    		"<p></p>"+
    		"<form action='/add_films' method='post'>" +
    		"<label for='language'>Idioma: </label>" + 
    		"<input type='text' name='language' id='language'" +
    		"pattern=[A-Za-z]{0,}>" +
    		"<p></p>"+
    		"<form action='/add_films' method='post'>" +
    		"<label for='actor'>Actor: </label>" + 
    		"<input type='text' name='actor' id='actor'" +
    		"pattern=[A-Za-z]{0,}>" +
    		"<p><input type='submit' value='Enviar'></p>" +
		"</form>");
        //Incluido formulario para añadir películas
        
        post("/add_films", (req, res) -> {
        	String result = "Has añadido ->"
        		+ "</p>pelicula: " + req.queryParams("film")
        		+ "</p>year: " + req.queryParams("year") 
        		+ "</p>Idioma: " + req.queryParams("language")
        		+ "</p>Actor: " + req.queryParams("actor");
        	return result;	
        });
        
        // Recurso /filter encargado de la funcionalidad del filtrado
        get("/filter", (req, res) ->
        	"<form action='/filter_film' method='post'>" +
        		"<label for='film'>Película que desea buscar: </label>" + 
        		"<input type='text' name='film' id='film'> " +
        		"<input type='submit' value='Enviar'>" +
    		"</form>"
        );
        
        post("/filter_film", (req, res) -> {
        	// Con el atributo queryParams accedemos al valor del parametro "film" del form
        	return "Has buscado: " + req.queryParams("film");
        });

    }
}

