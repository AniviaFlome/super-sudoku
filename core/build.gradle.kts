plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnit()
}

// Build-time solver: decodes puzzle.json, solves the ring, writes solution.json.
tasks.register<JavaExec>("solveSuper") {
    group = "puzzle"
    description = "Solve carykh's Super Sudoku into app/src/main/assets/solution.json"
    dependsOn("compileKotlin")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.supersudoku.core.SolveMainKt")
}

// Build-time minter: derives standalone 9x9 givens per variant grid.
tasks.register<JavaExec>("mintStandalone") {
    group = "puzzle"
    description = "Mint standalone variant givens into app/src/main/assets/standalone.json"
    dependsOn("compileKotlin")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.supersudoku.core.StandaloneMainKt")
}
