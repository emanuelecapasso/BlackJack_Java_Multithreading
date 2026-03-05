package blackjack.gioco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Mazzo composto da 52 carte.
 *
 * Ogni carta ha un simbolo ("2", "3", ..., "J", "Q", "K", "A") e un valore numerico
 * (2-10 per le carte numeriche, 10 per le figure, 11 per l'Asso).
 *
 * La classe è thread-safe: tutti i metodi pubblici sono synchronized.
 * Se le carte finiscono, il mazzo viene rigenerato e mescolato automaticamente.
 */

public class Deck {

    // Classe interna per rappresentare una carta, utilizzata per la gestione delle figure
    public static class Card {
        private final String label;
        private final int value;

        public Card(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { 
        	return label; 
        }
        
        public int getValue() { 
        	return value; 
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        reset();
    }

    // Rigenera il mazzo completo e lo mescola
    public synchronized void reset() {
        cards.clear();

        // Carte numeriche dal 2 al 10 (4 per ogni seme)
        for (int val = 2; val <= 10; val++) {
            for (int i = 0; i < 4; i++) {
                cards.add(new Card(String.valueOf(val), val));
            }
        }

        // Figure: J, Q e K hanno valore 10
        String[] figures = {"J", "Q", "K"};
        for (String f : figures) {
            for (int i = 0; i < 4; i++) {
                cards.add(new Card(f, 10));
            }
        }

        // Assi hanno valore 11
        for (int i = 0; i < 4; i++) {
            cards.add(new Card("A", 11));
        }

        Collections.shuffle(cards);
    }


    public synchronized Card draw() {
        if (cards.isEmpty()) {
            reset();
        }
        return cards.remove(0);
    }

    public synchronized int remaining() {
        return cards.size();
    }
}