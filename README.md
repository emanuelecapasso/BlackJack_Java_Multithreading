# Blackjack Multigiocatore in Java

Implementazione di un gioco di Blackjack multigiocatore in Java, basata su architettura **client-server** con comunicazione via **socket TCP**.

Il server gestisce fino a 4 giocatori simultaneamente, ognuno su un thread dedicato.

---

## Struttura del progetto

```
Server-BlackJack/
└── src/
    └── main/
        └── java/
            └── blackjack/
                └── gioco/
                    ├── ServerMain.java      # Server, presenta il game loop principale
                    ├── AcceptThread.java    # Thread che accetta nuove connessioni
                    ├── PlayerHandler.java   # Gestisce la sessione di gioco di ogni client
                    └── Deck.java            # Mazzo di carte thread-safe

Client-BlackJack/
└── src/
    └── main/
        └── java/
            └── blackjack/
                └── gioco/
                    ├── Client.java          # Client
                    └── Reader.java          # Thread che riceve i messaggi dal server
```

---

## Funzionalità

- Fino a **4 giocatori** connessi contemporaneamente
- **Turni paralleli**: tutti i giocatori giocano il proprio turno nello stesso momento
- **Mazzo condiviso** thread-safe con rimescolamento automatico
- **Timeout** di 1 minuto per turno: se scade, il turno viene saltato automaticamente
- Gestione della **disconnessione improvvisa** senza bloccare il server
- Partita **rifiutata** se il server è già pieno

---

## Comandi di gioco

| Comando | Descrizione |
|---|---|
| `BET <n>` | Effettua una puntata di `n` fiches |
| `HIT` | Pesca una carta aggiuntiva |
| `STAY` | Fermati con la mano attuale |
| `QUIT` | Disconnettiti dalla partita |

---

## Regole

- Ogni giocatore inizia con **100 fiches**
- Il banco pesca fino a raggiungere almeno **17**
- **Blackjack** (Asso + figura o carta con valore 10 con le prime 2 carte) paga **2.5x**
- Vittoria normale paga **2x**
- In caso di pareggio la puntata viene restituita
- Se le fiches si esauriscono il giocatore viene disconnesso

---

## Come avviare il gioco

### 1. Compilazione

La compilazione deve essere effettuata separatamente per il server e per il client, dalla rispettiva cartella radice.

**Windows**
```cmd
javac src\main\java\blackjack\gioco\*.java
```

**Linux / macOS**
```bash
javac src/main/java/blackjack/gioco/*.java
```

### 2. Avvio del server

Dalla cartella `src\main\java` del server:

**Windows**
```cmd
java blackjack.gioco.ServerMain
```

**Linux / macOS**
```bash
java blackjack.gioco.ServerMain
```

### 3. Avvio del client

Apri un nuovo terminale per ogni giocatore (fino a 4) e dalla cartella `src\main\java` del client:

**Windows**
```cmd
java blackjack.gioco.Client
```

**Linux / macOS**
```bash
java blackjack.gioco.Client
```

Il client si connette di default a `localhost:12345`. Per connettersi a un server remoto, modifica i campi `HOST` e `PORT` in `Client.java`.

---

## Esempio di partita

```
╔══════════════════════════════╗
║   BENVENUTO AL BLACKJACK!    ║
╚══════════════════════════════╝
Il tuo nome: Player1
Fiches iniziali: 100
Attendi l'inizio del round...

══════════════════════════════
E' IL TUO TURNO, Player1!
Fiches disponibili: 100
Comandi: BET <n> | QUIT
BET 50
Carta visibile del banco: [K] + [?]
La tua mano: [A, 7]  [18]
Digita HIT per pescare o STAY per fermarti.
STAY
Fermato con 18. Attendi il banco...
─────────────────────────────
Mano banco: [K, 8]  [18]
PAREGGIO - puntata restituita
Fiches attuali: 100
─────────────────────────────
```

---

## Architettura

Il server utilizza tre livelli di thread:

- **Main thread** — esegue il game loop: coordina i round, gestisce il banco e risolve i risultati
- **AcceptThread** — thread daemon in ascolto di nuove connessioni TCP
- **PlayerHandler thread** — un thread per ogni client connesso, rimane in ascolto dei comandi per tutta la durata della partita

La sincronizzazione tra il game loop e i thread dei giocatori avviene tramite `CountDownLatch`: il server avvia i turni in parallelo e attende che tutti i giocatori abbiano completato il proprio prima di procedere con il banco.
