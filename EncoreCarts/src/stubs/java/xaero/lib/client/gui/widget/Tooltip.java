package xaero.lib.client.gui.widget;

/**
 * Compile-time stand-in for XaeroLib's tooltip widget, which Xaero's World Map ships only as jar-in-jar.
 * Only the members EncoreCarts links against are declared; the real class is used at runtime.
 */
public class Tooltip {
    public Tooltip(String text) {
        throw new AssertionError("compile-time stub");
    }
}
