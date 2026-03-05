package blackjack.gioco;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Classe separata per il thread che accetta nuovi giocatori
class AcceptThread extends Thread {

    private ServerSocket serverSocket;
    private List<PlayerHandler> players;
    private Deck deck;
    private int MAX_PLAYERS;
    private AtomicInteger playerCounter;

    public AcceptThread(ServerSocket serverSocket, List<PlayerHandler> players, Deck deck, int MAX_PLAYERS, AtomicInteger playerCounter) {
        this.serverSocket = serverSocket;
        this.players = players;
        this.deck = deck;
        this.MAX_PLAYERS = MAX_PLAYERS;
        this.playerCounter = playerCounter;
    }

    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket s = serverSocket.accept();

                // Controllo limite giocatori
                if (players.size() >= MAX_PLAYERS) {
                    PrintWriter tmpOut = new PrintWriter(s.getOutputStream(), true);
                    tmpOut.println("╔══════════════════════════════╗");
                    tmpOut.println("║       PARTITA PIENA!         ║");
                    tmpOut.println("║  Max " + MAX_PLAYERS + " giocatori ammessi.  ║");
                    tmpOut.println("║  Riprova piu' tardi.         ║");
                    tmpOut.println("╚══════════════════════════════╝");
                    s.close();
                    System.out.println("[SERVER] Connessione rifiutata: partita piena.");
                    continue;
                }

                // Creo il nuovo giocatore
                String name = "Player" + playerCounter.getAndIncrement();
                PlayerHandler ph = new PlayerHandler(s, deck, name);
                players.add(ph);

                // Avvio il thread del giocatore
                Thread playerThread = new Thread(ph);
                playerThread.start();

                System.out.println("[SERVER] " + name + " connesso. (" + players.size() + "/" + MAX_PLAYERS + ")");
                broadcast("[INFO] " + name + " si e' unito alla partita.", ph);

            } catch (Exception e) {
                if (!serverSocket.isClosed()) e.printStackTrace();
            }
        }
    }

    // Metodo di utilità per mandare messaggi a tutti tranne uno
    private void broadcast(String msg, PlayerHandler exclude) {
        for (PlayerHandler p : players) {
            if (p != exclude) {
                p.send(msg);
            }
        }
    }
}