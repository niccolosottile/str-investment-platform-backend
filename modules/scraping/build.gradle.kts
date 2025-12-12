dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:location"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework:spring-messaging")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}
