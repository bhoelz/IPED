package iped.app.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Verifies that the headless {@code Bootstrap} class (and everything in
 * {@code iped.app.bootstrap} except {@code BootstrapUI}) is free of AWT,
 * Swing, and JavaFX dependencies, so the processing entry-point can be loaded
 * in a headless server JVM without a display.
 */
class IpedAppBootstrapArchitectureTest {

    private static final JavaClasses BOOTSTRAP_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.app.bootstrap");

    @Test
    void bootstrapMustNotDependOnAwtOrSwing() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.app.bootstrap..")
                .and().haveSimpleNameNotContaining("BootstrapUI")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.awt..",
                        "javax.swing..",
                        "sun.awt.."
                )
                .because("Bootstrap must be loadable without a graphical environment; " +
                         "all AWT/Swing deps must be confined to BootstrapUI");
        rule.check(BOOTSTRAP_CLASSES);
    }

    @Test
    void bootstrapMustNotDependOnJavaFx() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.app.bootstrap..")
                .and().haveSimpleNameNotContaining("BootstrapUI")
                .should().dependOnClassesThat().resideInAPackage("javafx..")
                .because("Bootstrap must be loadable without JavaFX on the classpath");
        rule.check(BOOTSTRAP_CLASSES);
    }

    @Test
    void bootstrapMustNotDependOnIpedUiPackages() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.app.bootstrap..")
                .and().haveSimpleNameNotContaining("BootstrapUI")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "iped.app.ui..",
                        "iped.app.splash.."
                )
                .because("UI packages must be confined to BootstrapUI");
        rule.check(BOOTSTRAP_CLASSES);
    }
}
