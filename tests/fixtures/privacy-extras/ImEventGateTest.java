package cat.ttcuz.framework;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.*;

public class ImEventGateTest {

    public interface Wire {
        void send(int command, long sequence, Runnable failed);
        String query();
    }

    public static final class Real implements Wire {

        int sends;

        public void send(int c, long s, Runnable f) {
            sends++;
        }

        public String query() {
            return "native data";
        }
    }

    private final Set<String> enabled = new HashSet<>();
    private final List<String> observed = new ArrayList<>();
    private final PrivacyImGate gate = new PrivacyImGate(enabled::contains, observed::add, s -> {});

    private Wire wire(Real real) {
        return (Wire) gate.outbound(
            Wire.class,
            real,
            (m, a) -> m.getName().equals("send") ? gate.blocked((Integer) a[0], (Long) a[1]) : null,
            (m, a, c) -> ((Runnable) a[2]).run(),
            observed::add
        );
    }

    @Before
    public void define() {
        gate.defineForTest(600, "read");
        gate.defineForTest(411, "typing");
    }

    @Test
    public void readPacketNeverReachesTransportAndCompletesAsFailure() {
        enabled.add("read");
        Real real = new Real();
        int[] failed = { 0 };
        wire(real).send(600, 1L, () -> failed[0]++);
        assertEquals(0, real.sends);
        assertEquals(1, failed[0]);
        assertEquals(Arrays.asList("read"), observed);
    }

    @Test
    public void typingIsIndependentlyBlocked() {
        enabled.add("typing");
        Real real = new Real();
        Wire wire = wire(real);
        wire.send(600, 1, () -> fail());
        wire.send(411, 2, () -> {});
        assertEquals(1, real.sends);
        assertEquals(Arrays.asList("typing"), observed);
    }

    @Test
    public void ordinaryMessagesAndFetchAreUnaffected() {
        enabled.addAll(Arrays.asList("read", "typing"));
        Real real = new Real();
        Wire wire = wire(real);
        wire.send(100, 1, () -> fail());
        assertEquals(1, real.sends);
        assertEquals("native data", wire.query());
        assertTrue(observed.isEmpty());
    }

    @Test
    public void blockedRetryIsNotReleasedWhenGhostTurnsOff() {
        enabled.add("read");
        Real real = new Real();
        Wire wire = wire(real);
        wire.send(600, 77, () -> {});
        enabled.clear();
        wire.send(600, 77, () -> {});
        assertEquals(0, real.sends);
        wire.send(600, 78, () -> fail());
        assertEquals(1, real.sends);
    }

    @Test
    public void missingSequenceDoesNotDisableFutureEventsForever() {
        enabled.add("read");
        assertEquals("read", gate.blocked(600, 0));
        enabled.clear();
        assertNull(gate.blocked(600, 0));
    }

    @Test
    public void sameSequenceForOtherCommandIsNotBlocked() {
        enabled.add("read");
        Real real = new Real();
        Wire wire = wire(real);
        wire.send(600, 77, () -> {});
        wire.send(100, 77, () -> fail());
        assertEquals(1, real.sends);
    }

    @Test
    public void stopRestoresDelegation() {
        enabled.add("read");
        Real real = new Real();
        Wire wire = wire(real);
        wire.send(600, 1, () -> {});
        gate.close();
        wire.send(600, 1, () -> fail());
        assertEquals(1, real.sends);
    }

    @Test
    public void retrySequenceCannotBlockAnotherAccount() {
        String old = Account.uid;
        try {
            Account.uid = "123";
            enabled.add("read");
            assertEquals("read", gate.blocked(600, 77));
            enabled.clear();
            Account.uid = "456";
            assertNull(gate.blocked(600, 77));
            Account.uid = "123";
            assertEquals("read", gate.blocked(600, 77));
        } finally {
            Account.uid = old;
        }
    }

    @Test
    public void bothIndependentPathsUseTheSameRetryGuard() {
        enabled.add("read");
        Real http = new Real(),
            socket = new Real();
        Wire h = wire(http),
            s = wire(socket);
        s.send(600, 55, () -> {});
        enabled.clear();
        h.send(600, 55, () -> {});
        assertEquals(0, http.sends);
        assertEquals(0, socket.sends);
        assertEquals(2, observed.size());
    }
}
