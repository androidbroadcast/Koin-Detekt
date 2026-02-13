plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.kover)
}

group = "io.github.krozov"
version = "0.2.0"

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
                name.set("detekt-rules-koin")
                description.set(
                    "Detekt extension library with 24 rules for Koin 4.x to enforce " +
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
                    // Minimum line coverage: 98%
                    minBound(98)
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
