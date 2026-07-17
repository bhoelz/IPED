package iped.engine.task;

import iped.engine.config.*;
import iped.exception.IPEDException;
import iped.parsers.browsers.ie.IndexDatParser;
import iped.parsers.database.EDBParser;
import iped.parsers.external.ExternalParser;
import iped.parsers.external.ExternalParsersFactory;
import iped.parsers.fork.ForkParser;
import iped.parsers.mail.LibpffPSTParser;
import iped.parsers.misc.PDFTextParser;
import iped.parsers.ocr.OCRParser;
import iped.parsers.python.PythonParser;
import iped.parsers.registry.RegRipperParser;
import iped.parsers.standard.RawStringParser;
import iped.parsers.standard.StandardParser;
import iped.parsers.util.PDFToImage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.tika.exception.TikaException;
import org.apache.tika.utils.XMLReaderUtils;

public final class ParsingTaskBootstrap {

  private static final AtomicBoolean TIKA_SAX_POOL_SIZE_SET = new AtomicBoolean(false);

  private ParsingTaskBootstrap() {}

  public static int configure(ConfigurationManager configurationManager) {
    ParsingTaskConfig parsingConfig = configurationManager.findObject(ParsingTaskConfig.class);
    ParsersConfig parserConfig = configurationManager.findObject(ParsersConfig.class);
    System.setProperty("tika.config", parserConfig.getTmpConfigFile().getAbsolutePath());

    // we have seen very large records in valid docs
    org.apache.poi.hpsf.CodePageString.setMaxRecordLength(512_000);

    // heavy Tika configuration
    if (!TIKA_SAX_POOL_SIZE_SET.getAndSet(true)) {
      try {
        XMLReaderUtils.setPoolSize(Runtime.getRuntime().availableProcessors());
      } catch (TikaException e) {
        e.printStackTrace();
      }
    }

    int maxExpandingContainers;
    if (parsingConfig.isEnableExternalParsing()) {
      ForkParser.setEnabled(true);
      PluginConfig pluginConfig = configurationManager.findObject(PluginConfig.class);
      ForkParser.setPluginDir(pluginConfig.getPluginFolder().getAbsolutePath());
      ForkParser.setPoolSize(parsingConfig.getNumExternalParsers());
      maxExpandingContainers = parsingConfig.getNumExternalParsers() / 2;
      if (maxExpandingContainers == 0) {
        throw new IPEDException(
            "You must have a minimum of 2 external parsing processes! Adjust the '"
                + ParsingTaskConfig.NUM_EXTERNAL_PARSERS
                + "' option.");
      }
      ForkParser.setServerMaxHeap(parsingConfig.getExternalParsingMaxMem());
    } else {
      LocalConfig localConfig = configurationManager.findObject(LocalConfig.class);
      if (localConfig.getNumThreads() < 2) {
        throw new IPEDException(
            "You should have at least 2 processing workers! Please adjust '"
                + LocalConfig.NUM_THREADS
                + "' option.");
      }
      maxExpandingContainers = localConfig.getNumThreads() / 2;
    }

    String appRoot = Configuration.getInstance().appRoot;
    ExternalParsersConfig extParsersConfig =
        configurationManager.findObject(ExternalParsersConfig.class);
    System.setProperty(ExternalParser.EXTERNAL_PARSERS_ROOT, appRoot);
    System.setProperty(
        ExternalParsersFactory.EXTERNAL_PARSER_PROP, extParsersConfig.getTmpConfigFilePath());
    System.setProperty(
        StandardParser.FALLBACK_PARSER_PROP, String.valueOf(parsingConfig.isParseUnknownFiles()));
    System.setProperty(
        StandardParser.ERROR_PARSER_PROP, String.valueOf(parsingConfig.isParseCorruptedFiles()));
    System.setProperty(
        StandardParser.ENTROPY_TEST_PROP,
        String.valueOf(configurationManager.getEnableTaskProperty("entropyTest")));
    System.setProperty(
        PDFTextParser.SORT_PDF_CHARS, String.valueOf(parsingConfig.isSortPDFChars()));
    System.setProperty(
        PDFTextParser.PROCESS_INLINE_IMAGES, String.valueOf(parsingConfig.isProcessImagesInPDFs()));
    System.setProperty(
        RawStringParser.MIN_STRING_SIZE, String.valueOf(parsingConfig.getMinRawStringSize()));
    System.setProperty(PythonParser.PYTHON_PARSERS_FOLDER, appRoot + "/scripts/parsers");

    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
      System.setProperty(OCRParser.TOOL_PATH_PROP, appRoot + "/tools/tesseract"); // $NON-NLS-1$
      System.setProperty(EDBParser.TOOL_PATH_PROP, appRoot + "/tools/esedbexport/"); // $NON-NLS-1$
      System.setProperty(
          LibpffPSTParser.TOOL_PATH_PROP, appRoot + "/tools/pffexport/"); // $NON-NLS-1$
      System.setProperty(
          IndexDatParser.TOOL_PATH_PROP, appRoot + "/tools/msiecfexport/"); // $NON-NLS-1$
    }

    System.setProperty(
        RegRipperParser.TOOL_PATH_PROP, appRoot + "/tools/regripper/"); // $NON-NLS-1$

    OCRConfig ocrConfig = configurationManager.findObject(OCRConfig.class);
    setupOCROptions(ocrConfig);

    String value =
        parsingConfig.isEnableExternalParsing()
            ? Boolean.FALSE.toString()
            : ocrConfig.getExternalPdfToImgConv();
    System.setProperty(PDFToImage.EXTERNAL_CONV_PROP, value);
    return maxExpandingContainers;
  }

  private static void setupOCROptions(OCRConfig ocrConfig) {
    if (ocrConfig.isOCREnabled()) {
      System.setProperty(OCRParser.ENABLE_PROP, "true");
      System.setProperty(OCRParser.LANGUAGE_PROP, ocrConfig.getOcrLanguage());
      System.setProperty(
          OCRParser.SKIP_KNOWN_FILES_PROP, String.valueOf(ocrConfig.isSkipKnownFiles()));
      System.setProperty(OCRParser.MIN_SIZE_PROP, ocrConfig.getMinFileSize2OCR());
      System.setProperty(OCRParser.MAX_SIZE_PROP, ocrConfig.getMaxFileSize2OCR());
      System.setProperty(OCRParser.PAGE_SEGMODE_PROP, ocrConfig.getPageSegMode());
      System.setProperty(PDFToImage.RESOLUTION_PROP, ocrConfig.getPdfToImgResolution());
      System.setProperty(PDFToImage.PDFLIB_PROP, ocrConfig.getPdfToImgLib());
      System.setProperty(PDFToImage.EXTERNAL_CONV_MAXMEM_PROP, ocrConfig.getExternalConvMaxMem());
      System.setProperty(PDFTextParser.MAX_CHARS_TO_OCR, ocrConfig.getMaxPdfTextSize2OCR());
      System.setProperty(
          OCRParser.PROCESS_NON_STANDARD_FORMATS_PROP, ocrConfig.getProcessNonStandard());
      System.setProperty(OCRParser.MAX_CONV_IMAGE_SIZE_PROP, ocrConfig.getMaxConvImageSize());
    }
  }
}
