package iped.engine.config;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

class ChildFirstClassLoader extends URLClassLoader {

  private final List<String> parentFirstPrefixes;

  ChildFirstClassLoader(URL[] urls, ClassLoader parent, List<String> parentFirstPrefixes) {
    super(urls, parent);
    this.parentFirstPrefixes = parentFirstPrefixes;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> loadedClass = findLoadedClass(name);
      if (loadedClass == null) {
        if (isParentFirst(name)) {
          loadedClass = super.loadClass(name, false);
        } else {
          try {
            loadedClass = findClass(name);
          } catch (ClassNotFoundException e) {
            loadedClass = super.loadClass(name, false);
          }
        }
      }
      if (resolve) {
        resolveClass(loadedClass);
      }
      return loadedClass;
    }
  }

  private boolean isParentFirst(String className) {
    for (String prefix : parentFirstPrefixes) {
      if (className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
