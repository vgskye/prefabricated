plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("8.1.1")

}

group = "vg.skye"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.fabricmc.net")
        name = "FabricMC"
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    runtimeOnly("org.ow2.asm:asm:9.6")
    runtimeOnly("org.ow2.asm:asm-analysis:9.6")
    runtimeOnly("org.ow2.asm:asm-commons:9.6")
    runtimeOnly("org.ow2.asm:asm-tree:9.6")
    runtimeOnly("org.ow2.asm:asm-util:9.6")
    runtimeOnly("net.fabricmc:sponge-mixin:0.12.5+mixin.0.8.5") {
        isTransitive = false
    }

    implementation("net.fabricmc:mapping-io:0.5.0") {
        // Mapping-io depends on ASM, dont bundle
        isTransitive = false
    }
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "vg.skye.prefabricated.Main"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}