package iped.engine.config;

import iped.utils.TomlProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ExportByCategoriesConfig extends AbstractTaskConfig<Set<String>> {

  /** */
  private static final long serialVersionUID = 1L;

  public static final String CONFIG_FILE = "CategoriesToExport.toml"; // $NON-NLS-1$
  private static final String CATEGORIES_KEY = "categories";
  public static final String ENABLE_PARAM = "enableAutomaticExportFiles";

  private Set<String> categoriesToExport = new HashSet<String>();

  public boolean hasCategoryToExport() {
    return categoriesToExport.size() > 0;
  }

  public boolean isToExportCategory(String category) {
    return categoriesToExport.contains(category);
  }

  @Override
  public boolean isEnabled() {
    return hasCategoryToExport();
  }

  @Override
  public Set<String> getConfiguration() {
    return categoriesToExport;
  }

  @Override
  public void setConfiguration(Set<String> config) {
    this.categoriesToExport = config;
  }

  @Override
  public String getTaskEnableProperty() {
    return ENABLE_PARAM;
  }

  @Override
  public String getTaskConfigFileName() {
    return CONFIG_FILE;
  }

  @Override
  public void processTaskConfig(Path resource) throws IOException {
    TomlProperties properties = new TomlProperties();
    properties.load(resource);
    categoriesToExport.addAll(properties.getListProperty(CATEGORIES_KEY));
  }
}
