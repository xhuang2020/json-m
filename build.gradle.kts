group = "net.json.jsonm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    antlr("org.antlr:antlr4:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0-M2")
    testImplementation("com.willowtreeapps.assertk:assertk:0.28.1")
}

plugins {
    kotlin("jvm") version "1.9.24"
    antlr
}
kotlin {
    jvmToolchain(21)
}
tasks.test {
    useJUnitPlatform()
}
tasks.generateGrammarSource {
    arguments = arguments + listOf("-no-listener", "-no-visitor", "-package", "net.json.jsonm.antlr4")
}
tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}

