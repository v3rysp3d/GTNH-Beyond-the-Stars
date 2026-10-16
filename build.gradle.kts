import java.util.zip.ZipFile

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// ------------------------------------------------------------------------------------------------------------------
// The release jar leaves the dev-only test tooling out (B-228). The `jar` task (the -dev jar) and every dev run
// (runClient / runServer run the compiled classes) keep all of it; only the reobfuscated jar that ships is filtered.
//
//  - rendertest/ (the video recorder, the render QA harness, the GUI screenshot harness, the packet spy, the automation
//    photo rigs) and devshots/ (the questbook screenshot harness): reached only through -D switches, by name, from
//    ClientProxy, which logs one line instead when they are absent.
//  - The four goldens (fieldsim, p5, quasar, smbh): read from src/main/resources by the tools/*-golden.js checks and
//    the Java ports' "golden <file>" mode; nothing in the game opens them.
//
// Kept on purpose: rendertest's ME benches (IntakeBench, MeBusBench, MeStockBench, MeParts). /btseye star and
// /btsquasar call them, and those test benches are documented for server owners (README "Commands").
// verifyReleaseJar (part of `check`) fails the build if the release jar carries any of the rest, or if anything left
// in it still names a class that was taken out.
// ------------------------------------------------------------------------------------------------------------------
val devOnlyPrefixes = listOf(
    "com/nateyburr/beyondthestars/rendertest/",
    "com/nateyburr/beyondthestars/devshots/",
)
val devOnlyFiles = listOf(
    "assets/beyondthestars/eye/fieldsim-golden.json",
    "assets/beyondthestars/eye/p5-golden.json",
    "assets/beyondthestars/quasar/quasar-golden.json",
    "assets/beyondthestars/quasar/smbh-golden.json",
)
val keptDevClasses = listOf("IntakeBench", "MeBusBench", "MeStockBench", "MeParts")
    .map { "com/nateyburr/beyondthestars/rendertest/$it" }

val releaseInputJar = tasks.register<Zip>("releaseInputJar") {
    description = "The -dev jar without the dev-only test tooling: what reobfJar turns into the release jar"
    val devJar = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val prefixes = devOnlyPrefixes
    val files = devOnlyFiles
    val kept = keptDevClasses
    // the exclude spec below is a lambda, which Gradle does not see as an input: the lists are
    inputs.property("devOnly", prefixes + files + kept)
    from(zipTree(devJar))
    exclude {
        val p = it.relativePath.pathString
        !it.isDirectory && (p in files || (prefixes.any { pre -> p.startsWith(pre) } &&
            kept.none { k -> p == "$k.class" || p.startsWith("$k$") }))
    }
    includeEmptyDirs = false
    archiveFileName.set("release-input.jar")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/releaseInputJar"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar>("reobfJar") {
    inputJar.set(releaseInputJar.flatMap { it.archiveFile })
}

val verifyReleaseJar = tasks.register("verifyReleaseJar") {
    description = "Fails if the release jar carries dev-only tooling, or still names a class that was left out of it"
    val reobf = tasks.named<com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar>("reobfJar")
        .flatMap { it.archiveFile }
    inputs.file(reobf)
    val prefixes = devOnlyPrefixes
    val files = devOnlyFiles
    val kept = keptDevClasses
    doLast {
        val jar = reobf.get().asFile
        val problems = mutableListOf<String>()
        ZipFile(jar).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            val classes = names.filter { it.endsWith(".class") }.map { it.removeSuffix(".class") }.toSet()
            for (n in names) {
                val dev = n in files || (prefixes.any { n.startsWith(it) } && n.endsWith(".class") &&
                    kept.none { n == "$it.class" || n.startsWith("$it$") })
                if (dev) problems.add("dev-only entry shipped: $n")
            }
            // every rendertest/ or devshots/ class a shipped class names (constant pool, slash form) must be in the jar
            val ref = Regex("com/nateyburr/beyondthestars/[A-Za-z0-9_/$]+")
            for (n in names.filter { it.endsWith(".class") }) {
                val text = String(zip.getInputStream(zip.getEntry(n)).readBytes(), Charsets.ISO_8859_1)
                for (m in ref.findAll(text)) {
                    val name = m.value
                    if (name !in classes && prefixes.any { name.startsWith(it) }) {
                        problems.add("$n names $name, which is not in the release jar")
                    }
                }
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("verifyReleaseJar:" + problems.distinct().joinToString("\n  ", "\n  "))
        }
        logger.lifecycle("verifyReleaseJar: ${jar.name} carries no dev-only tooling and names no missing class")
    }
}

tasks.named("check") { dependsOn(verifyReleaseJar) }
