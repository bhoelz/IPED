package iped.app.bootstrap;

import ag.ion.bion.officelayer.application.IOfficeApplication;
import iped.app.processing.Main;
import iped.app.ui.AppMain;
import iped.app.ui.splash.SplashScreenManager;
import iped.app.ui.splash.StartUpControl;
import iped.viewers.util.LibreOfficeFinder;
import iped.viewers.util.UNOLibFinder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI-aware bootstrap that extends {@link Bootstrap} with:
 *
 * <ul>
 *   <li>Splash screen startup in {@link #configLoaded()}
 *   <li>Child-process PID registration for splash-screen progress in {@link
 *       #onChildProcessStarted(Process)}
 *   <li>LibreOffice UNO JAR discovery in {@link #extendClasspath(Main, String)}
 * </ul>
 *
 * <p>All AWT/Swing/LibreOffice imports are confined to this class so that the headless {@link
 * Bootstrap} remains loadable in a server JVM without a display.
 */
public class BootstrapUI extends Bootstrap {

  public static void main(String args[]) {
    new BootstrapUI().run(args);
  }

  @Override
  protected boolean isToDecodeArgs() {
    return false;
  }

  @Override
  protected String getDefaultClassPath(Main iped) {
    return iped.getRootPath() + "/lib/iped-search-app.jar";
  }

  @Override
  protected String getMainClassName() {
    return AppMain.class.getCanonicalName();
  }

  @Override
  protected float getRAMToHeapFactor() {
    return 0.5f;
  }

  @Override
  protected void configLoaded() {
    new SplashScreenManager().start();
  }

  @Override
  protected void onChildProcessStarted(Process process) {
    System.setProperty(StartUpControl.ipedChildProcessPID, String.valueOf(process.pid()));
  }

  @Override
  protected String extendClasspath(Main iped, String classpath) throws Exception {
    System.setProperty(
        IOfficeApplication.NOA_NATIVE_LIB_PATH,
        new File(iped.getRootPath(), "lib/nativeview").getAbsolutePath());
    LibreOfficeFinder loFinder = new LibreOfficeFinder(new File(iped.getRootPath()));
    boolean isNogui = iped.getCmdLineArgs().isNogui();
    if (loFinder.getLOPath(isNogui) != null) {
      List<File> jars = new ArrayList<>();
      UNOLibFinder.addUNOJars(loFinder.getLOPath(isNogui), jars);
      for (File jar : jars) {
        classpath += separator + jar.getCanonicalPath();
      }
    }
    return classpath;
  }
}
