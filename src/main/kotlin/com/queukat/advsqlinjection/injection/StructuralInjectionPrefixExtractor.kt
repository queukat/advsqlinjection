package com.queukat.advsqlinjection.injection

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

object StructuralInjectionPrefixExtractor {
    fun extract(host: PsiLanguageInjectionHost): List<String> =
        extractYamlBlockScalarPrefixes(host)

    private fun extractYamlBlockScalarPrefixes(
        host: PsiLanguageInjectionHost
    ): List<String> {
        if (!host.isInstanceOfQualifiedName(YAML_BLOCK_SCALAR_CLASS_NAME)) {
            return emptyList()
        }

        val fileText = host.containingFile?.text ?: return emptyList()
        val headerStart = fileText.lastIndexOf('\n', startIndex = host.textRange.startOffset).let { lineBreak ->
            if (lineBreak < 0) 0 else lineBreak + 1
        }
        val headerEnd = fileText.indexOf('\n', startIndex = host.textRange.startOffset).let { lineBreak ->
            if (lineBreak < 0) fileText.length else lineBreak
        }
        if (headerStart < 0 || headerEnd <= headerStart || headerEnd > fileText.length) {
            return emptyList()
        }

        val headerLine = fileText.substring(headerStart, headerEnd).trim()
        if (!headerLine.contains('|') && !headerLine.contains('>')) {
            return emptyList()
        }

        return listOf(headerLine)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun PsiElement.isInstanceOfQualifiedName(qualifiedName: String): Boolean =
        try {
            javaClass.hasQualifiedName(qualifiedName)
        } catch (_: LinkageError) {
            false
        }

    private fun Class<*>.hasQualifiedName(
        qualifiedName: String,
        visited: MutableSet<Class<*>> = mutableSetOf()
    ): Boolean {
        if (!visited.add(this)) {
            return false
        }
        if (name == qualifiedName) {
            return true
        }
        return interfaces.any { it.hasQualifiedName(qualifiedName, visited) } ||
            superclass?.hasQualifiedName(qualifiedName, visited) == true
    }

    private const val YAML_BLOCK_SCALAR_CLASS_NAME = "org.jetbrains.yaml.psi.YAMLBlockScalar"
}
