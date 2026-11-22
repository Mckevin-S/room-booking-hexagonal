package com.mckevin.roombooking.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Règles de l'architecture hexagonale, vérifiées à chaque build. */
@AnalyzeClasses(packages = "com.mckevin.roombooking", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule le_domaine_ne_depend_que_du_jdk = classes()
            .that().resideInAPackage("..domain..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..domain..", "java..");

    @ArchTest
    static final ArchRule l_application_ne_depend_d_aucun_framework = classes()
            .that().resideInAPackage("..application..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("..application..", "..domain..", "java..");

    @ArchTest
    static final ArchRule architecture_en_oignon = onionArchitecture()
            .domainModels("..domain..")
            .applicationServices("..application..")
            .adapter("web", "..adapter.in.web..")
            .adapter("evenements", "..adapter.in.event..")
            .adapter("planificateur", "..adapter.in.scheduler..")
            .adapter("persistance", "..adapter.out.persistence..")
            .adapter("securite", "..adapter.out.security..")
            .adapter("bus-evenements", "..adapter.out.event..");

    @ArchTest
    static final ArchRule seule_la_configuration_assemble_les_cas_d_usage = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().resideInAPackage("..config..");
}
