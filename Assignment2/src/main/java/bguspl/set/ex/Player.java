package bguspl.set.ex;
import bguspl.set.Env;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    private final Dealer dealer;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */

    protected volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private boolean thirdToken = false;

    public boolean point = false;

    public boolean penalty = false;

    public ArrayBlockingQueue<Integer> keyPressesOfPlayer;

    private boolean isFrozen = false;

    private final int SECOND_IN_MILLIES = 1000;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.keyPressesOfPlayer = new ArrayBlockingQueue<Integer>(env.config.featureSize);
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            if(keyPressesOfPlayer.size() == env.config.featureSize && thirdToken) {
                dealer.playersToCheck.offer(this); //enqueue to queue of players that want to check if they have a set

                synchronized (this) {
                    try {
                        wait();
                    } catch (Exception e){}
                }

                if(point) {
                    point();
                    this.point = false;
                }
                else if(penalty) {
                    penalty();
                    this.penalty = false;
                }
                thirdToken = false;
            }
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        System.out.println("end of player- " + id);
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                int randomSlot = (int)Math.floor(Math.random() * env.config.tableSize);
                keyPressed(randomSlot);
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
        System.out.println("end of computer player- " + id);
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
    }

    /**
     * @PRE: player.keyPressesOfPlayer.IsFull == true
     *       checkSet(player) == true
     *       setWasFound == true
     * @POST: player.point == true
     */
    public void setPoint(){
        this.point = true;
    }

    /**
     * @PRE: player.keyPressesOfPlayer.IsFull == true
     *       checkSet(player)== false
     *       player.penalty = false
     * @POST:player.penalty = true
     */
    public void setPenalty(){
        this.penalty = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if(!isFrozen && !dealer.removingCards) {
            if (keyPressesOfPlayer.contains(slot)) {
                table.removeToken(id, slot);
                keyPressesOfPlayer.remove(slot);
            } else {
                if (keyPressesOfPlayer.size() < env.config.featureSize) {
                    if(table.slotToCard[slot] != null) {
                        if (keyPressesOfPlayer.size() == (env.config.featureSize) - 1)
                            thirdToken = true;
                        table.placeToken(id, slot);
                        keyPressesOfPlayer.offer(slot);
                    }
                }
            }
        }
        //wake player thread up
        synchronized (this) {
            this.notifyAll();
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        isFrozen = true;
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

        long freezeTime = env.config.pointFreezeMillis;
        for (long i = freezeTime; i >= 0; i = i - SECOND_IN_MILLIES) {
            env.ui.setFreeze(id, freezeTime);
            freezeTime = freezeTime - SECOND_IN_MILLIES;
            if (i > 0) {
                try {
                    Thread.sleep(SECOND_IN_MILLIES);
                } catch (InterruptedException ignored1) {}
            }
        }
        keyPressesOfPlayer.clear();
        isFrozen = false;
    }


    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        isFrozen = true;
        long freezeTime = env.config.penaltyFreezeMillis;
        for(long i = freezeTime; i >= 0; i = i - SECOND_IN_MILLIES) {
            env.ui.setFreeze(id, freezeTime);
            freezeTime = freezeTime - SECOND_IN_MILLIES;
            if(i > 0) {
                try {
                    Thread.sleep(SECOND_IN_MILLIES);
                } catch (InterruptedException ignored) {}
            }
        }
        isFrozen = false;
    }

    public int score() {
        return score;
    }

    public Thread getThread(){
        return playerThread;
    }

    public boolean getTerminate_test(){ // FOR TESTING
        return this.terminate;
    }

    public boolean getPoint_test(){ // FOR TESTING
        return this.point;
    }
}
