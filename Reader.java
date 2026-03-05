package blackjack.gioco;

import java.io.BufferedReader;
import java.io.IOException;

public class Reader extends Thread{
	private BufferedReader in;

    public Reader(BufferedReader in) {
        this.in = in;
    }

    public void run() {
        try {
            String msg;

            while ((msg = in.readLine()) != null) {
                System.out.println(msg);
            }

        } catch (IOException e) {
            System.out.println("Errore di lettura");
        }

        System.out.println("[Connessione al server chiusa]");
        System.exit(0);
    }
}

