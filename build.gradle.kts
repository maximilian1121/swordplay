plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "ca.maximilian.swordfight"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.viaversion.com")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.0")
    implementation("org.apache.logging.log4j:log4j-core:2.26.0")

    implementation("net.sourceforge.argparse4j:argparse4j:0.9.0")
    implementation("net.minestom:minestom:2026.08.07-26.2")

    implementation("net.bytebuddy:byte-buddy:1.18.11")
    implementation("net.bytebuddy:byte-buddy-agent:1.18.11")
}

tasks.test {
    useJUnitPlatform()
}

val productionResources = sourceSets.create("production")

tasks.named<Copy>("processProductionResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.shadowJar {
    archiveClassifier.set("")

    manifest {
        attributes["Main-Class"] = "ca.maximilian.swordfight.SwordFight"
    }

    from(productionResources.output)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
