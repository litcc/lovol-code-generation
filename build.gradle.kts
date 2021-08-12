import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.jetbrains.kotlin.jvm")
}
val jdkUseVersion: String by project
group = "me.lovol"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    val kotlinVersion: String by project
    // val vertxVersion: String by project
    val junitJupiterVersion: String by project

    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("stdlib-jdk7"))
    implementation(kotlin("reflect"))
//    implementation("io.vertx:vertx-core:$vertxVersion")
//    implementation("io.vertx:vertx-lang-kotlin:$vertxVersion")
    implementation("com.squareup:kotlinpoet:1.9.0")



    //compileOnly("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.0")

    testImplementation(kotlin("test-junit5", kotlinVersion))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    //compileOnly(project(":lovol-core"))
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = jdkUseVersion
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

//tasks.withType<JavaCompile> {
//    options.fork(mapOf(Pair("jvmArgs", listOf("--add-opens", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED"))))
//}