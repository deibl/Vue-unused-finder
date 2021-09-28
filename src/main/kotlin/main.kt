import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList
import kotlin.io.path.name
import kotlin.io.path.pathString

fun main(args: Array<String>) {
    if (args.size != 1) {
        throw RuntimeException("One parameter (project source root path) required")
    }
    val allVuePaths: MutableList<Path> = getPathsByFileType(args, arrayOf(".vue"))
    val allRelevantSourcePaths: MutableList<Path> = getPathsByFileType(args, arrayOf(".vue", ".ts"))

    checkForUnusedComponents(allVuePaths, allRelevantSourcePaths)
    checkForUnusedImportedComponents(allVuePaths)
}

private fun checkForUnusedImportedComponents(allVuePaths: MutableList<Path>) {
    val regex = Regex("('@.+\\.vue')")
    allVuePaths.forEach { path ->
        val fileContent = path.toFile().readText()
        val matches = regex.findAll(fileContent)
        val expectedNamesInTemplate = matches.map { it.groupValues[1] }
                .toList()
                .map { fullName -> fullName.substringAfterLast("/").substringBefore(".") }
                .map { componentName -> componentNameToTemplateName(componentName) }
        val unusedComponents = expectedNamesInTemplate.filter { expectedNameInTemplate -> !fileContent.contains(expectedNameInTemplate) }
        if (unusedComponents.isNotEmpty()) {
            println("Found unused imported vue component in ${path.name}: $unusedComponents")
        }
    }
}

fun componentNameToTemplateName(componentName: String): String {
    return "<" + componentName
            .map { char ->
                when {
                    char.isUpperCase() -> "-" + char.lowercaseChar()
                    else -> char
                }
            }
            .joinToString("")
            .substring(1)
}

private fun checkForUnusedComponents(allVuePaths: MutableList<Path>, allRelevantSourcePaths: MutableList<Path>) {
    val allVueFileNames = allVuePaths.map { file -> file.fileName.name }.toMutableList()
    allRelevantSourcePaths.forEach { path ->
        val fileContent = path.toFile().readText()
        val importedFiles = allVueFileNames.filter { fileName -> fileContent.contains(fileName) }
        allVueFileNames.removeAll(importedFiles)
    }
    if (allVueFileNames.isNotEmpty()) {
        println("Found unused vue components: $allVueFileNames")
    }
}

private fun getPathsByFileType(args: Array<String>, fileType: Array<String>): MutableList<Path> {
    return Files
            .walk(Paths.get(args[0]))
            .filter { file -> fileType.any { fileType -> file.name.endsWith(fileType) } }
            .filter { file -> !file.pathString.contains("__test__") }
            .collect(toList())
}
