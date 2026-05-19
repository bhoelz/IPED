package iped.engine.task;

import org.apache.tika.parser.ParseContext;

import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.core.Worker;
import iped.engine.data.IPEDSource;
import iped.parsers.standard.StandardParser;

public final class ParsingTaskContextFactory {

    private ParsingTaskContextFactory() {
    }

    public static ParseContext create(IItem item, StandardParser parser, ConfigurationManager configurationManager,
            Worker worker, boolean extractEmbedded) throws Exception {
        ParsingTask expander = new ParsingTask(item, parser);
        expander.setWorker(worker);
        expander.init(configurationManager);
        ParseContext context = expander.getTikaContext();
        expander.setExtractEmbedded(extractEmbedded);
        return context;
    }

    public static ParseContext create(IItem item, StandardParser parser, ConfigurationManager configurationManager,
            IPEDSource source, boolean extractEmbedded) throws Exception {
        ParsingTask expander = new ParsingTask(item, parser);
        expander.init(configurationManager);
        ParseContext context = expander.getTikaContext(source);
        expander.setExtractEmbedded(extractEmbedded);
        return context;
    }
}
