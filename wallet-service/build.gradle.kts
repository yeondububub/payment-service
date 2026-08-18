plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation ("org.springframework.boot:spring-boot-starter-web")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation ("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly ("com.mysql:mysql-connector-j")
    testImplementation ("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly ("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
