plugins {
    id("java")
}

group = "org.example.interceptor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://packages.confluent.io/maven/") }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:7.3.2-ccs")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.5")
    implementation("org.slf4j:slf4j-api:2.0.6")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}