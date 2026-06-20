package iped.parsers.util;

public class ConversationConstants {

    public static final String DIRECTION_INCOMING = "Incoming";
    public static final String DIRECTION_OUTGOING = "Outgoing";

    /**
     * Metadata property names shared by chat parsers (WhatsApp, Threema, Skype,
     * Telegram) for message-level attachment/status data. These were previously
     * hand-typed identically as string literals in each parser; centralizing them
     * here prevents one module from silently drifting (typo, case change) from the
     * others and fragmenting the same logical property into two index fields.
     */
    public static final String CHAT_ID = "chatId";
    public static final String MEDIA_NAME = "mediaName";
    public static final String MEDIA_MIME = "mediaMime";
    public static final String MEDIA_SIZE = "mediaSize";
    public static final String MESSAGE_DURATION = "duration";
    public static final String MESSAGE_STATUS = "messageStatus";

}
