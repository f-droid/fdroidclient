package org.whispersystems.witness

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

import java.security.MessageDigest

class WitnessPluginExtension {
    List verify
}

class WitnessPlugin implements Plugin<Project> {

    static String calculateSha256(file) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        file.eachByte 4096, {bytes, size ->
            md.update(bytes, 0, size);
        }
        return md.digest().collect {String.format "%02x", it}.join();
    }

    void apply(Project project) {
        project.extensions.create("dependencyVerification", WitnessPluginExtension)
        project.afterEvaluate {
            project.dependencyVerification.verify.each {
                assertion ->
                    List  parts  = assertion.tokenize(":")
                    String group = parts.get(0)
                    String name  = parts.get(1)
                    String hash  = parts.get(2)

                    ResolvedArtifact dependency = project.configurations.compile.resolvedConfiguration.resolvedArtifacts.find {
                        return it.name.equals(name) && it.moduleVersion.id.group.equals(group)
                    }

                    println "Verifying " + group + ":" + name

                    if (dependency == null) {
                        throw new InvalidUserDataException("No dependency for integrity assertion found: " + group + ":" + name)
                    }

                    if (!hash.equals(calculateSha256(dependency.file))) {
                        throw new InvalidUserDataException("Checksum failed for " + assertion)
                    }
            }
        }

        project.task('calculateChecksums') << {
            println "dependencyVerification {"
            println "    verify = ["

            project.configurations.compile.resolvedConfiguration.resolvedArtifacts.each {
                dep ->
                    println "        '" + dep.moduleVersion.id.group+ ":" + dep.name + ":" + calculateSha256(dep.file) + "',"
            }

            println "    ]"
            println "}"
        }
    }
}

