group = "net.json.jsonm"
version = "0.0-SNAPSHOT"

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
    `maven-publish`
}
kotlin {
    jvmToolchain(17)
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
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/xhuang2020/json-m")
            credentials {
                username = project.findProperty("gpr.github_user") as String
                password = project.findProperty("gpr.github_token") as String
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["kotlin"])
        }
    }
}