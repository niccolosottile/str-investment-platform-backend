dependencies {
    implementation(project(":modules:location"))
    implementation(project(":modules:analysis"))
    implementation(project(":modules:scraping"))
    implementation(project(":modules:shared-kernel"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    // WebClient for external APIs
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.4.1")
    
    // Redis
    implementation("io.lettuce:lettuce-core")
    
    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Monitoring
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    
    // AOP for metrics
    implementation("org.springframework.boot:spring-boot-starter-aop")
    
    // Structured Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    
    // MapStruct for DTO mapping
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.4")
}

apply(plugin = "org.springframework.boot")
