package com.peoplemanager.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Hexagonal Architecture Rules")
class HexagonalArchitectureTest {

    private lateinit var importedClasses: JavaClasses

    @BeforeAll
    fun setUp() {
        importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.peoplemanager")
    }

    @Nested
    @DisplayName("Domain Layer Rules")
    inner class DomainLayerRules {

        @Test
        fun `domain should not depend on application layer`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")

            rule.check(importedClasses)
        }

        @Test
        fun `domain should not depend on infrastructure layer`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")

            rule.check(importedClasses)
        }

        @Test
        fun `domain should not depend on Spring framework`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")

            rule.check(importedClasses)
        }

        @Test
        fun `domain should not depend on JPA or Hibernate`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate.."
                )

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Application Layer Rules")
    inner class ApplicationLayerRules {

        @Test
        fun `application should not depend on infrastructure adapters`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")

            rule.check(importedClasses)
        }

        @Test
        fun `application should not depend on web controllers`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.web..")

            rule.check(importedClasses)
        }

        @Test
        fun `application should not depend on persistence adapters`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.persistence..")

            rule.check(importedClasses)
        }

        @Test
        fun `application should not use JPA annotations`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Infrastructure Layer Rules")
    inner class InfrastructureLayerRules {

        @Test
        fun `web controllers should only depend on application ports and domain`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..adapters.web..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.persistence..")

            rule.check(importedClasses)
        }

        @Test
        fun `persistence adapters should not depend on web controllers`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..adapters.persistence..")
                .should().dependOnClassesThat().resideInAPackage("..adapters.web..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Port Structure Rules")
    inner class PortStructureRules {

        @Test
        fun `input ports should not depend on output ports`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application.port.input..")
                .should().dependOnClassesThat().resideInAPackage("..application.port.output..")

            rule.check(importedClasses)
        }

        @Test
        fun `input ports should not depend on adapters`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application.port.input..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")

            rule.check(importedClasses)
        }

        @Test
        fun `output ports should not depend on adapters`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..application.port.output..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Domain Service Rules")
    inner class DomainServiceRules {

        @Test
        fun `domain services should not depend on application layer`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat().resideInAPackage("..application..")

            rule.check(importedClasses)
        }

        @Test
        fun `domain services should not depend on infrastructure`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat().resideInAPackage("..adapters..")

            rule.check(importedClasses)
        }

        @Test
        fun `domain services should not depend on Spring framework`() {
            val rule: ArchRule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")

            rule.check(importedClasses)
        }
    }

    @Nested
    @DisplayName("Dependency Direction Rules")
    inner class DependencyDirectionRules {

        @Test
        fun `dependencies should flow inward - adapters may depend on application`() {
            // This is an assertion that there are NO cycles between slices
            val rule = slices()
                .matching("com.peoplemanager.(*)..")
                .should().beFreeOfCycles()

            rule.check(importedClasses)
        }
    }
}
