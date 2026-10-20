package com.myshelf.app.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guard architectural boundaries without adding a runtime dependency. */
class DependencyBoundariesTest {
    private val sources = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .flatMap { root -> sequenceOf(File(root, "src/main/java/com/myshelf/app"), File(root, "app/src/main/java/com/myshelf/app")) }
        .first { File(it, "MainActivity.kt").isFile }

    private fun imports(file: File): List<String> = file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ") }

    @Test fun modelsAndDomainRulesStayFrameworkFree() {
        listOf("core/model", "domain").forEach { directory ->
            File(sources, directory).walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val allowed = listOf("java.", "javax.crypto.", "kotlin.", "com.myshelf.app.core.") +
                    if (directory == "domain") listOf("com.myshelf.app.domain.") else emptyList()
                val forbidden = imports(file).filterNot { dependency -> allowed.any(dependency::startsWith) }
                assertTrue("${file.name} crosses a model/domain boundary: $forbidden", forbidden.isEmpty())
            }
        }
    }

    @Test fun storageAndSharedUiDoNotDependOnFeatureScreens() {
        listOf("data", "ui").forEach { directory ->
            File(sources, directory).walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val forbidden = imports(file).filter { dependency ->
                    listOf("com.myshelf.app.features.", "com.myshelf.app.navigation.", "com.myshelf.app.state.")
                        .any(dependency::startsWith) || (directory == "data" && dependency.startsWith("androidx.compose."))
                }
                assertTrue("${file.name} depends on a presentation owner: $forbidden", forbidden.isEmpty())
            }
        }
    }

    @Test fun sourcePackagesMatchTheirDirectories() {
        sources.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val folder = requireNotNull(file.parentFile).relativeTo(sources).invariantSeparatorsPath
            val expected = "com.myshelf.app" + if (folder.isEmpty()) "" else ".${folder.replace('/', '.')}"
            val declaration = file.useLines { lines -> lines.first { it.startsWith("package ") }.removePrefix("package ") }
            assertEquals(file.name, expected, declaration)
        }
    }
}
