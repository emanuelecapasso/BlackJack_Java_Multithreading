package blackjack.gioco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/*
 * Gestisce la sessione di gioco di un singolo client.
 *
 * Ogni istanza viene eseguita su un thread dedicato (implementa Runnable)
 * che rimane in ascolto dei comandi inviati dal client via socket TCP.
*/

public class PlayerHandler implements Runnable {

    private static final int STARTING_FICHES = 100;

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final String playerName;
    private final Deck deck;

    private volatile int fiches = STARTING_FICHES;
    private volatile int currentBet = 0;

    private Deck.Card dealerVisibleCard;

    private volatile boolean busted = false;
    private volatile boolean naturalBlackjack = false;
    private volatile boolean inRound = false;
    private volatile boolean active = true;
    private volatile boolean myTurn = false;

    private final List<Deck.Card> hand = new ArrayList<>();

    private volatile CountDownLatch turnLatch;

    public PlayerHandler(Socket socket, Deck deck, String playerName) {
        this.socket = socket;
        this.deck = deck;
        this.playerName = playerName;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            send("╔══════════════════════════════╗");
            send("║   BENVENUTO AL BLACKJACK!    ║");
            send("╚══════════════════════════════╝");
            send("Il tuo nome: " + playerName);
            send("Fiches iniziali: " + fiches);
            send("Attendi l'inizio del round...");

            while (active) {
                String line = in.readLine();

                if (line == null) {
                    disconnect();
                    break;
                }

                if (!myTurn) {
                    send("Non e' il tuo turno. Aspetta.");
                    continue;
                }

                handleCommand(line.trim().toUpperCase());
            }

        } catch (IOException e) {
            disconnect();
        }
    }

    private void handleCommand(String cmd) {
        if (cmd.startsWith("BET")) {
            handleBet(cmd);
        } else if (cmd.equals("HIT")) {
            handleHit();
        } else if (cmd.equals("STAY")) {
            handleStay();
        } else if (cmd.equals("QUIT")) {
            disconnect();
        } else {
            send("Comando non valido. Usa: BET <n> | HIT | STAY | QUIT");
        }
    }

    private void handleBet(String cmd) {
        if (!hand.isEmpty()) {
            send("Hai gia' puntato. Usa HIT o STAY.");
            return;
        }

        try {
            String[] parts = cmd.split(" ");

            if (parts.length < 2) {
                send("Formato errato. Usa: BET <importo>");
                return;
            }

            int bet = Integer.parseInt(parts[1]);

            if (bet <= 0 || bet > fiches) {
                send("Puntata non valida. Fiches disponibili: " + fiches);
                return;
            }

            currentBet = bet;
            fiches -= bet;
            busted = false;
            naturalBlackjack = false;
            inRound = true;

            send("Carta visibile del banco: [" + dealerVisibleCard.getLabel() + "] + [?]");

            startHand();

        } catch (NumberFormatException e) {
            send("Formato errato. Usa: BET <importo>");
        }
    }

    private void handleHit() {
        if (hand.isEmpty()) {
            send("Prima punta: BET <n>");
            return;
        }

        hand.add(deck.draw());
        int val = handValue();
        send("La tua mano: " + handToString() + "  [" + val + "]");

        if (val > 21) {
            busted = true;
            send("BUST! Hai sforato 21.");
            endTurn();
        } else if (val == 21) {
            send("Hai 21! Stay automatico.");
            endTurn();
        }
    }

    private void handleStay() {
        if (hand.isEmpty()) {
            send("Prima punta: BET <n>");
            return;
        }

        send("Fermato con " + handValue() + ". Attendi il banco...");
        endTurn();
    }

    private void startHand() {
        hand.clear();
        hand.add(deck.draw());
        hand.add(deck.draw());

        int val = handValue();
        send("La tua mano: " + handToString() + "  [" + val + "]");
        send("Fiches rimanenti: " + fiches);

        if (val == 21 && hand.size() == 2) {
            boolean hasAce = false;
            boolean hasTen = false;

            for (Deck.Card c : hand) {
                if (c.getValue() == 11) {
                    hasAce = true;
                } else if (c.getValue() == 10) {
                    hasTen = true;
                }

                if (hasAce && hasTen) {
                    break;
                }
            }
            
            if (naturalBlackjack) {
                send("HAI FATTO BLACKJACK!");
            }

            endTurn();
            return;
        }

        send("Digita HIT per pescare o STAY per fermarti.");
    }

    // FIX: parametro List<Deck.Card> invece di List<Integer>
    public static int calcHandValue(List<Deck.Card> h) {
        int total = 0;
        int aces  = 0;

        for (Deck.Card card : h) {
            total += card.getValue();
            if (card.getValue() == 11) {
                aces++;
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    private int handValue() {
        return calcHandValue(hand);
    }

    private String handToString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < hand.size(); i++) {
            sb.append(hand.get(i).getLabel());
            if (i < hand.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    // Mantenuto per compatibilità con ServerMain.dealerHandToString()
    public static String cardLabel(int v) {
        return (v == 11) ? "A" : String.valueOf(v);
    }

    public void resolveRound(int dealerValue, String dealerHandStr, boolean dealerNatural) {
        send("─────────────────────────────");
        send("Mano banco: " + dealerHandStr + "  [" + dealerValue + "]");

        if (busted) {
            send("Risultato: HAI PERSO (hai sforato 21)");

        } else if (dealerNatural && !naturalBlackjack) {
            send("Il banco ha Blackjack! HAI PERSO");

        } else if (dealerNatural && naturalBlackjack) {
            fiches += currentBet;
            send("Entrambi Blackjack! PAREGGIO - puntata restituita");

        } else if (naturalBlackjack) {
            int won = payout();
            fiches  += won;
            send("BLACKJACK! HAI VINTO +" + (won - currentBet) + " fiches");

        } else if (dealerValue > 21) {
            int won = payout();
            fiches  += won;
            send("Il banco ha sforato! HAI VINTO +" + (won - currentBet) + " fiches");

        } else {
            int playerValue = handValue();

            if (playerValue > dealerValue) {
                int won = payout();
                fiches  += won;
                send("HAI VINTO +" + (won - currentBet) + " fiches");

            } else if (playerValue == dealerValue) {
                fiches += currentBet;
                send("PAREGGIO - puntata restituita");

            } else {
                send("HAI PERSO");
            }
        }

        send("Fiches attuali: " + fiches);
        send("─────────────────────────────");

        inRound = false;
        currentBet = 0;

        if (fiches <= 0) {
            send("GAME OVER!");
            send("Fiches esaurite, grazie per aver giocato.");
            disconnect();
        }
    }

    private int payout() {
        if (naturalBlackjack) {
            return (int) Math.round(currentBet * 2.5);
        }
        return currentBet * 2;
    }

    public void startTurn(CountDownLatch latch, Deck.Card dealerVisible) {
        turnLatch = latch;
        myTurn = true;
        dealerVisibleCard = dealerVisible;
        hand.clear();

        send("══════════════════════════════");
        send("E' IL TUO TURNO, " + playerName + "!");
        send("Fiches disponibili: " + fiches);
        send("Comandi: BET <n> | QUIT");
    }

    public synchronized void endTurn() {
        if (!myTurn) return;

        myTurn = false;
        notifyAll();

        if (turnLatch != null) {
            turnLatch.countDown();
            turnLatch = null;
        }
    }

    public synchronized void disconnect() {
        if (!active) {
        	return;
        }

        active = false;
        myTurn = false;

        if (turnLatch != null) {
            turnLatch.countDown();
            turnLatch = null;
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        	
        }
    }

    public boolean isActive() {
        return active && (fiches > 0 || inRound);
    }

    public boolean isMyTurn() { 
    	return myTurn; 
    }
    
    public String  getName()  { 
    	return playerName; 
    }

    public void send(String msg) {
        if (out != null) {
        	out.println(msg);
        }
    }
}
