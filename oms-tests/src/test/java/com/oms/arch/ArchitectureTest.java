package com.oms.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.oms");
    }

    @Test
    void domainHasNoFrameworkDependencies() {
        noClasses().that().resideInAPackage("com.oms.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "com.fasterxml.jackson..",
                        "org.apache.kafka..",
                        "io.lettuce.."
                )
                .because("Domain must be framework-free")
                .check(importedClasses);
    }

    @Test
    void applicationDependsOnlyOnDomain() {
        noClasses().that().resideInAPackage("com.oms.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.oms.infrastructure..",
                        "com.oms.api..",
                        "org.springframework..",
                        "jakarta.persistence.."
                )
                .because("Application layer may only depend on domain")
                .check(importedClasses);
    }

    @Test
    void infrastructureMayNotDependOnApi() {
        noClasses().that().resideInAPackage("com.oms.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.oms.api..")
                .because("Infrastructure must not depend on API layer")
                .check(importedClasses);
    }

    @Test
    void apiMayNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("com.oms.api..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.oms.infrastructure..")
                .because("API must not depend on infrastructure layer")
                .check(importedClasses);
    }

    @Test
    void noFieldInjection() {
        noFields()
                .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .orShould().beAnnotatedWith("jakarta.inject.Inject")
                .because("Use constructor injection. Field injection hides dependencies.")
                .check(importedClasses);
    }

    @Test
    void domainExceptionsAreFinal() {
        classes().that().resideInAPackage("com.oms.domain.exception..")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("DomainException")
                .should().haveModifier(JavaModifier.FINAL)
                .because("Domain exceptions must be final (sealed hierarchy)")
                .check(importedClasses);
    }
}
