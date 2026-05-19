package iped.engine.task;

import org.apache.tika.parser.ParseContext;

import iped.data.IItem;
import iped.engine.config.ConfigurationManager;
import iped.engine.core.Worker;
import iped.engine.data.IPEDSource;
import iped.parsers.standard.StandardParser;

public final class ParsingTaskContextFactory {

    private static final String TASK_CLASS = "iped.engine.task.ParsingTask";

    private ParsingTaskContextFactory() {
    }

    public static ParseContext create(IItem item, StandardParser parser, ConfigurationManager configurationManager,
            Worker worker, boolean extractEmbedded) throws Exception {
        Class<?> cls = Class.forName(TASK_CLASS);
        Object expander = cls.getConstructor(IItem.class, StandardParser.class).newInstance(item, parser);
        cls.getMethod("setWorker", Worker.class).invoke(expander, worker);
        cls.getMethod("init", ConfigurationManager.class).invoke(expander, configurationManager);
        ParseContext context = (ParseContext) cls.getMethod("getTikaContext").invoke(expander);
        cls.getMethod("setExtractEmbedded", boolean.class).invoke(expander, extractEmbedded);
        return context;
    }

    public static ParseContext create(IItem item, StandardParser parser, ConfigurationManager configurationManager,
            IPEDSource source, boolean extractEmbedded) throws Exception {
        Class<?> cls = Class.forName(TASK_CLASS);
        Object expander = cls.getConstructor(IItem.class, StandardParser.class).newInstance(item, parser);
        cls.getMethod("init", ConfigurationManager.class).invoke(expander, configurationManager);
        ParseContext context = (ParseContext) cls.getMethod("getTikaContext", IPEDSource.class).invoke(expander, source);
        cls.getMethod("setExtractEmbedded", boolean.class).invoke(expander, extractEmbedded);
        return context;
    }
}
