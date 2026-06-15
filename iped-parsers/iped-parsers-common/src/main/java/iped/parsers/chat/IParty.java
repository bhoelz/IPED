package iped.parsers.chat;

import java.util.Optional;

/**
 * Minimal view of a chat party (sender / recipient). Implemented by concrete
 * models ({@code iped.parsers.ufed.model.Party}, etc.) so that
 * {@link PartyStringBuilder} can be shared in {@code iped-parsers-common}
 * without a compile-time dependency on those model classes.
 */
public interface IParty {

    /** Platform-specific identifier (phone number, JID, user-id, etc.). */
    String getIdentifier();

    /** Display name, or {@code null} if unknown. */
    String getName();

    /** Contact record linked to this party, if one was resolved. */
    Optional<? extends IReferencedContact> getReferencedContact();
}
