package iped.engine.config;

import iped.engine.data.Category;
import iped.utils.TomlProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class CategoryToExpandConfig extends AbstractTaskConfig<Set<String>> {

  /** */
  private static final long serialVersionUID = 1L;

  public static final String CONFIG_FILE = "CategoriesToExpand.toml";
  private static final String CATEGORIES_KEY = "categories";
  private static final String ENABLED = "expandContainers";

  private Set<String> categoriesToExpand = new HashSet<String>();
  CategoryConfig categoryConfig = null;

  public boolean isToBeExpanded(Collection<String> categories) {

    if (!super.isEnabled()) {
      return false;
    }

    for (String category : categories) {
      if (categoriesToExpand.contains(category)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getTaskEnableProperty() {
    return ENABLED;
  }

  @Override
  public String getTaskConfigFileName() {
    return CONFIG_FILE;
  }

  @Override
  public void processTaskConfig(Path resource) throws IOException {
    TomlProperties properties = new TomlProperties();
    properties.load(resource);
    if (categoryConfig == null) {
      categoryConfig = ConfigurationManager.get().findObject(CategoryConfig.class);
    }
    for (String name : properties.getListProperty(CATEGORIES_KEY)) {
      Category root = categoryConfig.getCategoryFromName(name);
      if (root == null) {
        continue; // category not found in config (e.g. config not yet loaded)
      }
      LinkedList<Category> cats = new LinkedList<>();
      cats.push(root);
      while (cats.size() > 0) {
        Category cat = cats.pop();
        categoriesToExpand.add(cat.getName());
        cats.addAll(cat.getChildren());
      }
    }
  }

  @Override
  public Set<String> getConfiguration() {
    return categoriesToExpand;
  }

  @Override
  public void setConfiguration(Set<String> config) {
    categoriesToExpand = config;
  }
}
