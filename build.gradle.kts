plugins {
    // No plugins applied directly to the root project.
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
