package iped.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture rules for the iped-api module.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Naming conventions (I-prefix → interface, *Exception → exception class)</li>
 *   <li>Layer isolation: the API module must not depend on engine or app layers</li>
 * </ul>
 */
@AnalyzeClasses(packages = "iped", importOptions = ImportOption.DoNotIncludeTests.class)
public class ApiArchTest {

    /**
     * Types whose simple name matches {@code I[UpperCase]...} (e.g. {@code IItem},
     * {@code IDataSource}) must be interfaces. The {@code iped.exception} package is
     * excluded because {@code IPEDException} intentionally breaks the pattern.
     */
    @ArchTest
    static final ArchRule types_prefixed_with_I_must_be_interfaces =
        classes()
            .that().haveSimpleNameMatching("I[A-Z].*")
            .and().resideOutsideOfPackage("iped.exception..")
            .should().beInterfaces()
            .because("the 'I' prefix in IPED API packages is reserved for interface types");

    /**
     * Every class that extends {@link Exception} must have a name ending with
     * {@code Exception}, making exception types immediately recognisable.
     */
    @ArchTest
    static final ArchRule exceptions_must_end_with_Exception =
        classes()
            .that().areAssignableTo(Exception.class)
            .should().haveSimpleNameEndingWith("Exception")
            .because("exception class names must end with 'Exception' for clarity");

    /**
     * The API module is the foundation layer — it must not reach into the engine
     * implementation, which would invert the dependency direction.
     */
    @ArchTest
    static final ArchRule api_must_not_depend_on_engine =
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("iped.engine..")
            .because("iped-api is the foundation; depending on iped-engine would invert the layer hierarchy");

    /**
     * The API module must also not reach up into the application layer.
     */
    @ArchTest
    static final ArchRule api_must_not_depend_on_app =
        noClasses()
            .should().dependOnClassesThat().resideInAPackage("iped.app..")
            .because("iped-api is the foundation; depending on iped-app would invert the layer hierarchy");
}
