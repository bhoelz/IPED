package iped.engine.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public final class TaskRuntime {

  private TaskRuntime() {}

  public static void setTaskEnabled(String fqcn, boolean enabled) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Method m = cls.getMethod("setEnabled", boolean.class);
      m.invoke(null, enabled);
    } catch (Throwable t) {
      // task not available in classpath for this profile
    }
  }

  public static void invokeStaticVoid(String fqcn, String methodName) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Method m = cls.getMethod(methodName);
      m.invoke(null);
    } catch (Throwable t) {
      // optional runtime integration
    }
  }

  public static void invokeStaticVoid(
      String fqcn, String methodName, Class<?> paramType, Object param) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Method m = cls.getMethod(methodName, paramType);
      m.invoke(null, param);
    } catch (Throwable t) {
      // optional runtime integration
    }
  }

  public static void invokeConstructedVoid(
      String fqcn,
      Class<?> constructorParamType,
      Object constructorParam,
      String methodName,
      Class<?> methodParamType,
      Object methodParam) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Constructor<?> c = cls.getConstructor(constructorParamType);
      Object instance = c.newInstance(constructorParam);
      Method m = cls.getMethod(methodName, methodParamType);
      m.invoke(instance, methodParam);
    } catch (Throwable t) {
      // optional runtime integration
    }
  }

  @SuppressWarnings("unchecked")
  public static double invokeVideoScore(String fqcn, Map<String, Double> scores, double fallback) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Method m = cls.getMethod("videoScore", Map.class);
      Object value = m.invoke(null, scores);
      if (value instanceof Number) {
        return ((Number) value).doubleValue();
      }
    } catch (Throwable t) {
      // optional runtime integration
    }
    return fallback;
  }

  public static double invokeVideoScoreList(String fqcn, List<Double> scores, double fallback) {
    try {
      Class<?> cls = Class.forName(fqcn);
      Method m = cls.getMethod("videoScore", List.class);
      Object value = m.invoke(null, scores);
      if (value instanceof Number) {
        return ((Number) value).doubleValue();
      }
    } catch (Throwable t) {
      // optional runtime integration
    }
    return fallback;
  }
}
