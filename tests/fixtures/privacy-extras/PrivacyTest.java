package cat.ttcuz.framework;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import org.junit.*;

public class PrivacyTest {

    public interface Sender {
        void send(String text);
        String fetch();
        void fail();
    }

    public static final class Real implements Sender {

        int sends;

        public void send(String s) {
            sends++;
        }

        public String fetch() {
            return "native";
        }

        public void fail() {
            throw new IllegalArgumentException("native failure");
        }
    }

    public static final class Slot {

        public Object value;
    }

    @Test
    public void outboundGateSuppressesOnlySelectedMethodAndDelegatesWhenDisabled() {
        Real real = new Real();
        boolean[] on = { true };
        Sender sender = (Sender) PrivacyAdapters.proxy(
            Sender.class,
            real,
            (m, a) -> on[0] && m.getName().equals("send"),
            m -> null
        );
        sender.send("x");
        assertEquals(0, real.sends);
        assertEquals("native", sender.fetch());
        on[0] = false;
        sender.send("x");
        assertEquals(1, real.sends);
        assertTrue(sender.equals(sender));
        assertFalse(sender.equals(real));
    }

    @Test
    public void nativeErrorsAreUnwrapped() {
        Sender sender = (Sender) PrivacyAdapters.proxy(
            Sender.class,
            new Real(),
            (m, a) -> false,
            m -> null
        );
        try {
            sender.fail();
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals("native failure", expected.getMessage());
        }
    }

    @Test
    public void masterAndChannelMustBothBeEnabled() {
        Set<String> on = new HashSet<>();
        PrivacyAdapters mode = new PrivacyAdapters(on::contains, s -> {});
        on.add("ghost_read");
        assertFalse(mode.enabled("read"));
        on.add("ghost");
        assertTrue(mode.enabled("read"));
        assertFalse(mode.enabled("typing"));
        mode.close();
        assertFalse(mode.enabled("read"));
    }

    @Test
    public void restorationNeverOverwritesAnotherOwner() throws Exception {
        Slot slot = new Slot();
        Object original = new Object(),
            ours = new Object(),
            later = new Object();
        slot.value = original;
        PrivacyReflect.Patch patch = new PrivacyReflect.Patch(
            slot,
            Slot.class.getField("value"),
            ours
        );
        slot.value = later;
        patch.restore();
        assertSame(later, slot.value);
    }

    @Test
    public void initializedLazyRestoresDelegateInsteadOfSentinel() throws Exception {
        Slot slot = new Slot();
        Object sentinel = new Object(),
            delegate = new Object(),
            ours = new Object();
        slot.value = sentinel;
        PrivacyReflect.Patch patch = new PrivacyReflect.Patch(
            slot,
            Slot.class.getField("value"),
            ours,
            delegate
        );
        patch.restore();
        assertSame(delegate, slot.value);
    }
}
