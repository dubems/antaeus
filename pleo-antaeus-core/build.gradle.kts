plugins {
    kotlin("jvm")
}

kotlinProject()
dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))

    testImplementation("org.testcontainers:testcontainers:1.17.3")
    testImplementation("org.testcontainers:junit-jupiter:1.17.3")
    testImplementation("org.testcontainers:kafka:1.17.3")
    testImplementation("org.testcontainers:postgresql:1.17.3")
    testImplementation("org.postgresql:postgresql:42.4.0")
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.awaitility:awaitility:4.2.0")


}