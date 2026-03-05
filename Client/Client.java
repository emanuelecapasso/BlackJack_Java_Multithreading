package blackjack.gioco;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * Client Blackjack
 * Si connette al server tramite socket TCP e gestisce due canali separati: 
 * - Un thread in background (daemon) che riceve i messaggi dal server. 
 * -Il thread principale che legge i comandi da tastiera e li invia.
 * Quando il server chiude la connessione, il thread di lettura stampa un messaggio e termina l'applicazione con System.exit(0).
 */ 

public class Client {

    private static final String HOST = "localhost";
    private static final int PORT = 12345;

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket(HOST, PORT);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));		// Messaggi Server-Client

        BufferedReader user = new BufferedReader(new InputStreamReader(System.in));					// Messaggi scritti dall'utente

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);							// Messaggi Client-Server

        // Creo e avvio il thread che legge i messaggi dal server
        Reader reader = new Reader(in);
        reader.start();

        String input;

        // Ciclo principale: legge comandi da tastiera e li invia al server
        while ((input = user.readLine()) != null) {
            out.println(input);
        }
    }
}