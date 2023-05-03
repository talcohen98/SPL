package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.plugins.PluginSwitch;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        Player [] players = new Player[env.config.players];
        for(int player =0; player < players.length; player++){
            players[player] = new Player(env, dealer, table, player, false);
        }
        table = new Table(env);
        dealer = new Dealer(env, table, players);
    }

    private void fillAllSlots() { // added
        for (int i = 0; i < table.slotToCard.length; ++i) {
            table.slotToCard[i] = i;
            table.cardToSlot[i] = i;
        }
    }

    //added
    @Test
    void terminate() {
        dealer.terminate();
        assertEquals(true ,dealer.getTerminate());
    }

    @Test
    void removeAllCardsFromTable(){
        fillAllSlots();
        dealer.removeAllCardsFromTable();
        for (int i = 0; i < 12; ++i) {
            assertEquals(null ,table.slotToCard[i]);
        }
    }

    
}