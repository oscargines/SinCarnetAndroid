/**
 * Configuración de Dokka para SinCarnet Android
 *
 * Añade esta configuración al archivo app/build.gradle.kts para generar
 * documentación HTML navegable desde los comentarios KDoc.
 *
 * Instalación:
 * 1. Agregar el plugin en el apartado plugins { }:
 *    id("org.jetbrains.dokka") version "1.9.10"
 *
 * 2. Agregar esta configuración en el apartado android { } o en el root { }:
 *
 * ---
 */

// Opción 1: Configuración minimalista (recomendada para empezar)
dokka {
    dokkaSourceSets {
        named("main") {
            // Excluir rutas innecesarias
            skipDeprecated.set(true)

            // Incluir visibilidad interna (para documentar internals)
            documentedVisibilities.set(setOf(
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                org.jetbrains.dokka.DokkaConfiguration.Visibility.INTERNAL
            ))

            // Información del módulo
            moduleName.set("SinCarnet Android")

            // Archivos de documentación
            includes.from("README.md", "DOCUMENTACION_KDOC.md")
        }
    }

    // Configuración de salida HTML
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

// Opción 2: Configuración avanzada (con más controles)
/*
dokka {
    dokkaSourceSets {
        named("main") {
            // Visibilidad
            documentedVisibilities.set(setOf(
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PUBLIC,
                org.jetbrains.dokka.DokkaConfiguration.Visibility.INTERNAL,
                org.jetbrains.dokka.DokkaConfiguration.Visibility.PROTECTED
            ))

            // Links externos
            externalDocumentationLinks {
                create("https://developer.android.com/reference") {
                    packageListUrl.set(
                        file("$projectDir/.dokka/android-package-list")
                    )
                }
            }

            // Módulo
            moduleName.set("SinCarnet v1.3")

            // Supresiones
            skipDeprecated.set(false)
            suppressInheritedMembers.set(false)

            // Documentación
            includes.from(
                "README.md",
                "DOCUMENTACION_KDOC.md",
                "docs/ARQUITECTURA.md"
            )
        }
    }

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka"))
        includes.from("docs")
    }

    dokkaPublications.javadoc {
        outputDirectory.set(layout.buildDirectory.dir("javadoc"))
    }
}
*/

/**
 * Cómo generar documentación:
 *
 * 1. En terminal, desde la raíz del proyecto:
 *    ./gradlew app:dokkaHtml
 *
 * 2. Abrir resultado en navegador:
 *    Windows: start app\build\dokka\html\index.html
 *    Mac:     open app/build/dokka/html/index.html
 *    Linux:   xdg-open app/build/dokka/html/index.html
 *
 * 3. Alternativa: Generar Javadoc compatible
 *    ./gradlew app:dokkaJavadoc
 *
 * 4. Para CI/CD, agregar tarea a gradle:
 *    tasks.register("generateDocumentation") {
 *        dependsOn("dokkaHtml")
 *        doLast {
 *            println("Documentación generada en: app/build/dokka/html")
 *        }
 *    }
 */

