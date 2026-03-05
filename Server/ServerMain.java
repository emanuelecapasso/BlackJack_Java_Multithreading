package blackjack.gioco;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Server principale del gioco
 */

public class ServerMain {

    private static final int MAX_PLAYERS = 4;
    private static final int PORT = 12345;

    private static final List<PlayerHandler> players = new CopyOnWriteArrayList<>();

    private static final List<Deck.Card> dealerHand = new ArrayList<>();
    private static final Deck deck = new Deck();
    private static final AtomicInteger playerCounter = new AtomicInteger(1);

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[SERVER] Blackjack avviato sulla porta " + PORT);
        System.out.println("[SERVER] Posti disponibili: " + MAX_PLAYERS);

        AcceptThread acceptThread = new AcceptThread(serverSocket, players, deck, MAX_PLAYERS, playerCounter);
        acceptThread.setDaemon(true);
        acceptThread.start();

        while (true) {

            if (players.isEmpty()) {
                Thread.sleep(500);
                continue;
            }

            List<PlayerHandler> activePlayers = new ArrayList<>();
            for (PlayerHandler p : players) {
                if (p.isActive()) activePlayers.add(p);
            }

            if (activePlayers.isEmpty()) {
                List<PlayerHandler> daRimuovere = new ArrayList<>();
                for (PlayerHandler p : players) {
                    if (!p.isActive()) {
                        daRimuovere.add(p);
                    }
                }
                players.removeAll(daRimuovere);
                Thread.sleep(500);
                continue;
            }

            System.out.println("[SERVER] ─── Nuovo round con " + activePlayers.size() + " giocatori ───");

            dealerHand.clear();
            dealerHand.add(deck.draw());
            dealerHand.add(deck.draw());

            Deck.Card dealerVisible = dealerHand.get(0);
            System.out.println("[SERVER] Mano banco iniziale: " + dealerHandToString() + "  [" + handValue(dealerHand) + "]");

            CountDownLatch latch = new CountDownLatch(activePlayers.size());
            for (PlayerHandler p : activePlayers) {
                if (!p.isActive()) {
                    latch.countDown();
                    continue;
                }
                p.startTurn(latch, dealerVisible);
            }

            boolean allDone = latch.await(1, TimeUnit.MINUTES);
            if (!allDone) {
                for (PlayerHandler p : activePlayers) {
                    if (p.isMyTurn()) {
                        p.send("Tempo scaduto. Turno saltato automaticamente.");
                        p.endTurn();
                    }
                }
            }

            boolean dealerNatural = isNaturalBlackjack(dealerHand);
            if (!dealerNatural) dealerPlay();
            int dealerValue = handValue(dealerHand);
            String dealerHandStr = dealerHandToString();

            System.out.println("[SERVER] Mano banco finale: " + dealerHandStr + "  [" + dealerValue + "]" + (dealerNatural ? " BLACKJACK!" : ""));

            for (PlayerHandler p : activePlayers) {
                if (p.isActive()) {
                    p.resolveRound(dealerValue, dealerHandStr, dealerNatural);
                }
            }

            List<PlayerHandler> daRimuovere = new ArrayList<>();

            for (PlayerHandler p : players) {
                if (!p.isActive()) {
                    System.out.println("[SERVER] " + p.getName() + " rimosso dalla partita.");
                    daRimuovere.add(p);
                }
            }

            players.removeAll(daRimuovere);

            System.out.println("[SERVER] Fine round. Giocatori rimasti: " + players.size());
            Thread.sleep(2000);
        }
    }

    private static void dealerPlay() {
        while (handValue(dealerHand) < 17) {
            dealerHand.add(deck.draw());
        }
    }

    private static int handValue(List<Deck.Card> hand) {
        return PlayerHandler.calcHandValue(hand);
    }

    private static boolean isNaturalBlackjack(List<Deck.Card> hand) {
        if (hand.size() != 2) {
            return false;
        }

        boolean hasAce = false;
        boolean hasTen = false;

        for (Deck.Card c : hand) {
            if (c.getValue() == 11) {
            	hasAce = true;
            }
            if (c.getValue() == 10) {
            	hasTen = true;
            }
        }

        return hasAce && hasTen;
    }

    private static String dealerHandToString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dealerHand.size(); i++) {
            sb.append(dealerHand.get(i).getLabel());
            if (i < dealerHand.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}