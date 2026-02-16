import java.time.Duration

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kover)
    alias(libs.plugins.nmcp)
    id("signing")
    alias(libs.plugins.jmh)
}

group = "dev.androidbroadcast.rules.koin"
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.4.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    // Generate sources and javadoc JARs (required by Maven Central)
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                name.set("detekt-koin-rules")
                description.set(
                    "Detekt extension library with 29 rules for Koin 4.x to enforce " +
                    "best practices and catch common anti-patterns via static analysis"
                )
                url.set("https://github.com/androidbroadcast/Koin-Detekt")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("kirich1409")
                        name.set("Kirill Rozov")
                        email.set("kirill@androidbroadcast.dev")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/androidbroadcast/Koin-Detekt.git")
                    developerConnection.set("scm:git:ssh://github.com:androidbroadcast/Koin-Detekt.git")
                    url.set("https://github.com/androidbroadcast/Koin-Detekt")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/androidbroadcast/Koin-Detekt/issues")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/androidbroadcast/Koin-Detekt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Signing configuration for Maven Central
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

signing {
    // Use in-memory PGP keys from environment variables (for CI/CD)
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        logger.lifecycle("Using in-memory PGP keys for artifact signing")
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    } else {
        // For local development: use GPG agent
        // GPG key must be available in local keyring
        val signingKeyId = System.getenv("SIGNING_KEY_ID")
        if (signingKeyId != null) {
            logger.lifecycle("Using GPG command for artifact signing (key: $signingKeyId)")
            useGpgCmd()
            sign(publishing.publications["maven"])
        } else {
            logger.warn("No signing configuration found - artifacts will not be signed")
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// Maven Central Portal Publishing configuration
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

nmcp {
    publishAllPublicationsToCentralPortal {
        // Credentials from environment variables
        // NOTE: Use existing OSSRH_* secrets - they work with Central Portal
        username = System.getenv("OSSRH_USERNAME")
        password = System.getenv("OSSRH_PASSWORD")

        // Publication type: AUTOMATIC or USER_MANAGED
        // AUTOMATIC = auto-release after validation
        publishingType = "AUTOMATIC"
    }
}

// Configure reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dirPermissions {
        unix("rwxr-xr-x")  // 0755
    }
    filePermissions {
        unix("rw-r--r--")  // 0644
    }
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()

    // Parallel test execution
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    // Better test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
        showStackTraces = true
        showCauses = true
    }

    // JVM arguments for tests
    jvmArgs = listOf(
        "-Xmx1g",
        "-XX:MaxMetaspaceSize=256m"
    )
}

kotlin {
    compilerOptions {
        // Context receivers support
        freeCompilerArgs.add("-Xcontext-receivers")

        // Explicit JVM target (matches Java toolchain)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)

        // Progressive mode: Enable progressive compiler features
        progressiveMode.set(true)

        // Strict mode: All warnings as errors (library quality)
        allWarningsAsErrors.set(true)
    }

    // Explicit API mode: Enforce explicit visibility and return types
    // Strict mode: All public APIs must have explicit visibility and return types
    explicitApi()
}

// Code coverage configuration
kover {
    reports {
        // Configure what to exclude from coverage
        filters {
            excludes {
                // Exclude generated code
                classes("*.*BuildConfig*")

                // Exclude test utilities if any exist in main source set
                packages("io.github.krozov.detekt.koin.test")
            }
        }

        // Enable all report types
        total {
            html {
                onCheck = true
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
            xml {
                onCheck = true
                xmlFile = layout.buildDirectory.file("reports/kover/coverage.xml")
            }

            // Verification rules - enforce minimum coverage
            verify {
                onCheck = true

                rule {
                    // Minimum line coverage: 96% (v0.4.0: adjusted for infrastructure code)
                    minBound(96)
                }

                rule("Branch Coverage") {
                    // Minimum branch coverage: 70%
                    bound {
                        minValue = 70
                        coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                    }
                }
            }
        }
    }
}

// JMH benchmarking configuration
jmh {
    iterations = 3
    benchmarkMode = listOf("avgt")  // Average time
    timeUnit = "ms"
    fork = 2
    warmupIterations = 2
}
