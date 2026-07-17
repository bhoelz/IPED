package iped.parsers.chat;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstract base class for building display strings for a chat party (sender / recipient) in a
 * platform-specific format.
 *
 * <p>Lives in {@code iped-parsers-common} so that individual parser modules (e.g. {@code
 * iped-parser-whatsapp}) can subclass it without depending on {@code iped-parsers-impl}.
 */
public abstract class PartyStringBuilder {

  protected String userId;
  protected String name;
  protected String phoneNumber;
  protected String username;

  public PartyStringBuilder withParty(IParty party) {
    party
        .getReferencedContact()
        .ifPresentOrElse(
            ref -> {
              this.userId = StringUtils.firstNonBlank(party.getIdentifier(), ref.getUserID());
              this.name = StringUtils.firstNonBlank(party.getName(), ref.getName());
              this.phoneNumber = ref.getPhoneNumber();
              this.username = ref.getUsername();
            },
            () -> {
              this.userId = party.getIdentifier();
              this.name = party.getName();
            });
    return this;
  }

  public PartyStringBuilder withUserId(String userId) {
    this.userId = userId;
    return this;
  }

  public PartyStringBuilder withName(String name) {
    this.name = name;
    return this;
  }

  public PartyStringBuilder withPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
    return this;
  }

  public PartyStringBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  public abstract String build();
}
