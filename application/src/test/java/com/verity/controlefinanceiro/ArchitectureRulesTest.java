package com.verity.controlefinanceiro;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.verity.controlefinanceiro")
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure = noClasses()
        .that().resideInAnyPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring = noClasses()
        .that().resideInAnyPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");
}