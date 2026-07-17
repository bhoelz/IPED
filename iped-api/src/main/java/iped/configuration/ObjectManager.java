package iped.configuration;

import java.util.Set;

/**
 * Interface for an object that owns other components of a given type.
 *
 * <p>COMMENT (Werneck): used to manage objects instantiated dynamically, such as plugins. Static
 * elements should be instantiated via DI (Guice or Spring).
 *
 * @param <T> the type of owned objects.
 * @author Bruno W. P. Hoelz
 */
public interface ObjectManager<T> {

  /**
   * Finds a component that implements/extends a given interface/class.
   *
   * @param clazz the desired class.
   * @return a component that implements/extends a given interface/class.
   */
  Set<? extends T> findObjects(Class<? extends T> clazz);

  /**
   * Finds a component based on its classname.
   *
   * @param className the desired classname.
   * @return a component of the given classname.
   */
  Set<T> findObjects(String className);

  /**
   * Gets the list of components registered with the platform's core.
   *
   * @return the set of components registered with the platform's core.
   */
  Set<T> getObjects();

  /**
   * Adds a component with the platform's core.
   *
   * @param aObject the component to be registered.
   */
  void addObject(T aObject);

  /**
   * Removes a component associated with this component owner.
   *
   * @param aObject component to be removed.
   */
  void removeObject(T aObject);
}
