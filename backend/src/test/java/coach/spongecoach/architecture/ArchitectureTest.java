package coach.spongecoach.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter().importPackages("coach.spongecoach");
    }

    @Test
    void domainMustNotDependOnSpringOrJpaOrAdapters() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "..adapter.."
                );
        rule.check(classes);
    }

    @Test
    void applicationMustNotDependOnAdapters() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..");
        rule.check(classes);
    }

    @Test
    void adaptersMustNotCrossFeatureBoundaries() {
        String[] features = {"club", "person", "team"};
        for (String feature : features) {
            for (String other : features) {
                if (!feature.equals(other)) {
                    ArchRule rule = noClasses()
                            .that().resideInAPackage("coach.spongecoach." + feature + ".adapter..")
                            .should().dependOnClassesThat()
                            .resideInAPackage("coach.spongecoach." + other + ".adapter..");
                    rule.check(classes);
                }
            }
        }
    }

    @Test
    void noFeatureCycles() {
        slices().matching("coach.spongecoach.(*)..").should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void restControllerOnlyInWebAdapter() {
        ArchRule rule = classes().that()
                .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage("..adapter.in.web..");
        rule.check(classes);
    }

    @Test
    void entityOnlyInPersistenceAdapter() {
        ArchRule rule = classes().that()
                .areAnnotatedWith("jakarta.persistence.Entity")
                .should().resideInAPackage("..adapter.out.persistence..");
        rule.check(classes);
    }

    @Test
    void springDataRepositoriesOnlyInPersistenceAdapter() {
        ArchRule rule = classes().that()
                .areAssignableTo("org.springframework.data.repository.Repository")
                .should().resideInAPackage("..adapter.out.persistence..");
        rule.check(classes);
    }

    @Test
    void domainModelMustNotCarryPersistenceOrWebAnnotations() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("org.springframework.web.bind.annotation.RestController");
        rule.check(classes);
    }
}
