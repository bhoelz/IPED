package iped.data;

import java.io.Serializable;

/**
 * Swing-free representation of a keyboard shortcut assigned to a bookmark.
 * The {@code keyCode} and {@code modifiers} match the values returned by
 * {@code KeyEvent.getKeyCode()} and {@code KeyStroke.getModifiers()} respectively,
 * so conversion to/from {@code javax.swing.KeyStroke} is straightforward in the
 * UI layer:
 * <pre>
 *   // BookmarkShortcut → KeyStroke (in iped-app)
 *   KeyStroke ks = KeyStroke.getKeyStroke(bs.keyCode(), bs.modifiers());
 *
 *   // KeyStroke → BookmarkShortcut (in iped-app)
 *   BookmarkShortcut bs = new BookmarkShortcut(ks.getKeyCode(), ks.getModifiers());
 * </pre>
 */
public record BookmarkShortcut(int keyCode, int modifiers) implements Serializable {}
