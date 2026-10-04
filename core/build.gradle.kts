plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnit()
}

// Build-time puzzle tools: each entry is (task name, description, main class).
val puzzleTools = listOf(
    Triple(
        "solveSuper",
        "Solve carykh's Super Sudoku into app/src/main/assets/solution.json",
        "dev.supersudoku.core.SolveMainKt",
    ),
    Triple(
        "mintStandalone",
        "Mint standalone variant givens into app/src/main/assets/standalone.json",
        "dev.supersudoku.core.StandaloneMainKt",
    ),
)
for ((name, description, mainClassName) in puzzleTools) {
    tasks.register<JavaExec>(name) {
        group = "puzzle"
        this.description = description
        dependsOn("compileKotlin")
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(mainClassName)
    }
}
