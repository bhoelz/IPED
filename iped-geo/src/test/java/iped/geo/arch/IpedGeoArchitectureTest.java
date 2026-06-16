package iped.geo.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the geo data / presentation split.
 *
 * <p>Classes in {@code iped.geo.data} are the headless data layer: they must be
 * loadable in a server JVM (e.g. {@code iped-webapi}) without pulling in any UI
 * toolkit. This test fails if anyone accidentally adds an AWT, Swing, or JavaFX
 * import to that package.
 */
class IpedGeoArchitectureTest {

    private static final JavaClasses GEO_DATA_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("iped.geo.data");

    @Test
    void geoDataMustNotDependOnAwtOrSwing() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.geo.data..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("java.awt..", "javax.swing..", "sun.awt..")
                .because("iped.geo.data is a headless data layer — no AWT or Swing allowed; " +
                         "move UI concerns to iped.geo.impl or iped.geo.openstreet");
        rule.check(GEO_DATA_CLASSES);
    }

    @Test
    void geoDataMustNotDependOnJavaFx() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.geo.data..")
                .should().dependOnClassesThat()
                .resideInAPackage("javafx..")
                .because("iped.geo.data is a headless data layer — JavaFX must not be on " +
                         "the classpath when it is consumed from iped-webapi");
        rule.check(GEO_DATA_CLASSES);
    }

    @Test
    void geoDataMustNotDependOnGeoRendering() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("iped.geo.data..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "iped.geo.impl..",
                        "iped.geo.webkit..",
                        "iped.geo.openstreet..",
                        "iped.geo.js..",
                        "iped.webkit..")
                .because("iped.geo.data must not depend on the rendering layer; " +
                         "the data layer is a leaf in the dependency graph");
        rule.check(GEO_DATA_CLASSES);
    }
}
