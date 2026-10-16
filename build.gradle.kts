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

// ------------------------------------------------------------------------------------------------------------------
// B-249: every hatch element of ours names a lang key that en_US.lang has. GT's default key is hatch_type_<name>,
// which no lang file carries, so a socket without its own key printed "hatch_type_stellar" in the structure errors.
// Read from the sources: an element must override getDescriptionLangKey, an enum's keys are its prefix plus each
// constant in lower case, and a written-out "beyondthestars.hatch...." key must be in the file.
// ------------------------------------------------------------------------------------------------------------------
val checkHatchLangKeys = tasks.register("checkHatchLangKeys") {
    description = "Fails if a hatch element has no lang key of its own, or its key has no entry in en_US.lang"
    val srcDir = layout.projectDirectory.dir("src/main/java").asFile
    val langFile = layout.projectDirectory.file("src/main/resources/assets/beyondthestars/lang/en_US.lang").asFile
    inputs.dir(srcDir)
    inputs.file(langFile)
    doLast {
        val have = langFile.readLines()
            .map { it.trim() }
            .filter { !it.startsWith("#") && it.contains('=') }
            .map { it.substringBefore('=') }
            .toSet()
        val problems = mutableListOf<String>()
        var elements = 0
        val keys = mutableSetOf<String>()
        srcDir.walkTopDown().filter { it.isFile && it.name.endsWith(".java") }.forEach { f ->
            val text = f.readText()
            val impls = Regex("""implements\s+IHatchElement\b""").findAll(text).count() +
                Regex("""new\s+IHatchElement\s*<""").findAll(text).count()
            if (impls == 0) return@forEach
            elements += impls
            val overrides = Regex("""public\s+String\s+getDescriptionLangKey\s*\(""").findAll(text).count()
            if (overrides < impls) {
                problems.add("${f.name}: $impls hatch element(s), $overrides getDescriptionLangKey override(s)")
            }
            val prefix = Regex(""""(beyondthestars\.hatch\.[a-z0-9_.]*)"\s*\+\s*name\(\)\.toLowerCase""")
                .find(text)?.groupValues?.get(1)
            if (prefix != null) {
                val enumName = Regex("""enum\s+(\w+)""").find(text)?.groupValues?.get(1)
                Regex("""(?m)^    ([A-Z]\w*)\s*\(""").findAll(text).map { it.groupValues[1] }
                    .filter { it != enumName }
                    .forEach { c ->
                        val k = prefix + c.lowercase()
                        keys.add(k)
                        if (k !in have) problems.add("${f.name}: $c -> $k is not in en_US.lang")
                    }
            }
            Regex(""""(beyondthestars\.hatch\.[a-z0-9_.]*[a-z0-9])"""").findAll(text).forEach { m ->
                val k = m.groupValues[1]
                keys.add(k)
                if (k !in have) problems.add("${f.name}: $k is not in en_US.lang")
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("checkHatchLangKeys:" + problems.distinct().joinToString("\n  ", "\n  "))
        }
        logger.lifecycle("checkHatchLangKeys: $elements hatch elements, ${keys.size} lang keys, all in en_US.lang")
    }
}

tasks.named("check") { dependsOn(checkHatchLangKeys) }
