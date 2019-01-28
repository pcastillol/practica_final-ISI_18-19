package urjc.isi.pruebasSparkJava;

import java.util.List;

import spark.Request;

public class Comment {
	//Comentarios
	
	//COMPLETO!!!
	public String newComment(String text, int user, String film) {
		if (text.equals(null)) {
			throw new IllegalArgumentException("Comentario invalido");
		}else if (user<0) {
			throw new IllegalArgumentException("Usuario invalido");
		}else if (film.equals(null)) {
			throw new IllegalArgumentException("Pelicula invalida");
		}else {			
				Injector connector = new Injector("JDBC_DATABASE_URL");
				connector.insertUser(user);
				List<String> info_film=connector.filterByName(film);
				int id_film=Integer.parseInt(info_film.get(6));
				connector.insertComments(id_film, user, text);
				return "Comentario almacenado";
			}
		//Obtengo id de la pelicula
		//Almaceno el nuevo comentario
	}
	
	//COMPLETO!!!
	public String commentsFilm(String film){
		if (film.equals(null)) {
			throw new IllegalArgumentException("Pelicula invalida");
		}else{
			try {
				Injector connector = new Injector("JDBC_DATABASE_URL");
				
				String comments[][]=connector.userandcomments(film);
				
				String commentString=commentToString(comments);
				
				return commentString;
			} catch(Exception e) {
				return e.getMessage();
			}
		}

	}
	
	//COMPLETO!!!
	public String commentToString(String matrix_coment[][])
	{
		String text = "<u><b>Comentarios:</b></u><br>";
		for (int x = 0; x < matrix_coment.length; x++){
			String coments = " ";
			text += coments + matrix_coment[x][0] + ":" + matrix_coment[x][1]+"<br>";
		}
		text +=" ";
		return text;
	}
	
	//COMPLETO!!!
	public String postComment(Request request) {
		String comment=request.queryParams("comment");
		
		String user_string=request.queryParams("user");
		int user=Integer.parseInt(user_string);
		
		String film=request.queryParams("film");
		try {
			String result=newComment(comment, user, film);
			return result;
		}catch (IllegalArgumentException e) {
			return e.getMessage();
		}
	}
}

