package bguspl.set.ex;
import bguspl.set.Env;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    protected final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Integer.MAX_VALUE;
    private boolean shouldReshuffle = true;

    public LinkedBlockingQueue<Player> playersToCheck;

    protected int[] slots; //slots of sets

    public boolean removingCards = false;

    private boolean setWasFound = false;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playersToCheck = new LinkedBlockingQueue<Player>();
        this.slots = new int[env.config.featureSize];
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");

        //create thread for each player
        for(Player player: players) {
            Thread playerThread = new Thread(player,"player-" + player.id);
            playerThread.start();
        }
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis + 2000; //added 2 sec to add the time it takes to get to the func

        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);
            removeAllCardsFromTable();
        }
        announceWinners();
        terminatePlayerThreads();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        System.out.println("end of dealer");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime && findSetsInDeckAndTable()) {
            sleepUntilWokenOrTimeout();
            while (!playersToCheck.isEmpty()) {
                    Player player = playersToCheck.remove();
                    if(player.keyPressesOfPlayer.size() == env.config.featureSize) {
                        if(checkSet(player)) {
                            setWasFound = true;
                            player.setPoint();
                            synchronized (player) {
                                player.notifyAll();
                            }
                            removeCardsFromTable(); //remove cards in slots of set
                            shouldReshuffle = false;
                            placeCardsOnTable();
                            updateTimerDisplay(true);
                            setWasFound = false;
                        }
                        else {
                            player.setPenalty();
                            synchronized (player) {
                                player.notifyAll();
                            }
                        }
                    }
                    else { //player doesnt have set because card was removed- wake thread up and continue playing
                        synchronized (player) {
                            player.notifyAll();
                        }
                    }
            }
            updateTimerDisplay(false);
        }
    }

    /**
     * Called when the game should be terminated.
     *
     * @PRE: terminate = false
     * @POST: terminate = true
     */
    public void terminate() {
        terminate = true;
    }

    public void terminatePlayerThreads() {
        for(int i = 0; i < env.config.players; i++) {
            Player p = players[i];
            p.terminate();
            while (p.getThread().isAlive()) {
                synchronized (players[i]) {
                    players[i].notifyAll();
                }
            }
        }
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0 || !findSetsInDeckAndTable();
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        removingCards = true;
        for(int slot: slots) {
            table.removeTokenFromSlot(slot);
            for(Player player: players) { //cards from set removed- remove the slot from all players queues
                if(player.keyPressesOfPlayer.contains(slot)) {
                    player.keyPressesOfPlayer.remove(slot);
                }
            }
            table.removeCard(slot);
        }
        if(!findSetsInDeckAndTable())
            terminate();
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        if (shouldReshuffle) { // only when 60 sec goes by
            table.removeAllTokens();
            for(Player player: players) //clear all keyPressesOfPlayer queues
                player.keyPressesOfPlayer.clear();
            removeAllCardsFromTable();
            updateTimerDisplay(true);
            Collections.shuffle(deck);
            for (int i = 0; i < env.config.tableSize; i++) { // adds cards to table
                table.placeCard(deck.remove(0), i);
            }
            shouldReshuffle = false;
        }
        else if(setWasFound && !deck.isEmpty()) {
            Integer card;
            for(int i = 0; i < env.config.tableSize; i++) {
                if(table.slotToCard[i] == null) {
                    try {
                    card = deck.remove(0);
                    } catch (IndexOutOfBoundsException e) {card = null;}
                    if(card != null)
                      table.placeCard(card, i);
                }
            }
        }
        removingCards = false;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            Thread.currentThread().sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        long currTime = reshuffleTime - System.currentTimeMillis();
        if (reset) { //happens if 60 sec are up and we need to reshuffle and update timer
            env.ui.setCountdown(env.config.turnTimeoutMillis + 999, false); //added 999 to fix timing issues
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis + 999; //added 999 to fix timing issues
            shouldReshuffle = true;
        } else {
            if (currTime > env.config.turnTimeoutWarningMillis) { //check if timer should become red
                env.ui.setCountdown(currTime, false);
            } else {
                env.ui.setCountdown(currTime, true);
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */

     /**
     * @PRE:  table.IsEmpty == false
     * @POST: table.IsEmpty == true
     *        playersToCheck.isEmpty == true
     */
    protected void removeAllCardsFromTable() { // CHANGED TO PROTECTED FOR TESTING
        removingCards = true;
        table.removeAllTokens();
        for(int i = 0; i < env.config.tableSize; i++) {
            table.removeTokenFromSlot(i);
            if(table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);
                table.removeCardPutBackInDeck(i);
            }
        }
        playersToCheck.clear();
        if(env.util.findSets(deck, 1).size() == 0)
            terminate();
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = 0;
        LinkedList<Integer> playersIds = new LinkedList<Integer>();
        for(Player player: players) {
            if(player.score() > maxScore) {
                playersIds.clear();
                playersIds.add(player.id);
                maxScore = player.score();
            }
            else if(player.score() == maxScore) {
                playersIds.add(player.id);
            }
        }
        int[] playerIdsArray = new int[playersIds.size()]; //array because announceWinner input is array
        int i = 0;
        for(int player : playersIds) {
            playerIdsArray[i] = player;
            i++;
        }
        table.removeAllTokens();
        table.announceWinner(playerIdsArray);
        terminate();
    }

    public boolean checkSet(Player playerToCheck) {
        int[] cards = table.getCardsFromSlots(playerToCheck.keyPressesOfPlayer);
        if (table.testSet(playerToCheck,cards)) {
            findSlotsToClear(playerToCheck);
            return true;
        }
        return false;
    }

    public void findSlotsToClear(Player playerToCheck) { 
        int i = 0;
        for(int slot : playerToCheck.keyPressesOfPlayer) {
            if(i < env.config.featureSize) {
                slots[i] = slot;
                i++;
            }
        }
    }

    private boolean findSetsInDeckAndTable() {
        List<Integer> tempDeck = new ArrayList<>();
        if(env.config.deckSize != 0) {
            for(Integer card: deck) {
                tempDeck.add(card);
            }
        }
        for(int i = 0; i < env.config.tableSize; i++) {
            if(table.slotToCard[i] != null)
               tempDeck.add(table.slotToCard[i]);
        }
        if(env.util.findSets(tempDeck, 1).size() != 0)
            return true;
        return false;
    }
    
     public boolean getTerminate(){ //ADDED FOR TESTING
        return this.terminate;
     }

     public int[] getSlots(){ //ADDED FOR TESTING
        return this.slots;
     }
}
