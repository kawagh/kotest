package io.kotest.engine.extensions.filter

import io.kotest.core.Logger
import io.kotest.core.descriptors.Descriptor
import io.kotest.engine.extensions.TestPattern
import io.kotest.engine.extensions.TestPatternParser

abstract class TestPatternIncludeDescriptorFilter : DescriptorFilter {

   private val logger = Logger(TestPatternIncludeDescriptorFilter::class)

   fun filter(pattern: String, descriptor: Descriptor): DescriptorFilterResult {
      logger.log { Pair(descriptor.toString(), "Testing against include pattern $pattern") }
      val testPattern = TestPatternParser.parse(pattern)
      return if (match(testPattern, descriptor))
         DescriptorFilterResult.Include
      else
         DescriptorFilterResult.Exclude(null)
   }

   // each part of the pattern can contain * wildcards, which match any characters as in Gradle's --tests
   private fun match(pattern: TestPattern, descriptor: Descriptor): Boolean {
      logger.log { Pair(descriptor.toString(), "Testing pattern $pattern against $descriptor") }
      val spec = descriptor.spec().id.value
      if (pattern.className == null) return packageMatch(pattern, spec.substringBeforeLast("."))
      val specMatched = if (pattern.packageName.isEmpty())
         pattern.className.toWildcardRegex().matches(spec.substringAfterLast(".")) // a simple name like *Test
      else
         "${pattern.packageName}.${pattern.className}".toWildcardRegex().matches(spec)
      if (!specMatched) return false
      // includes the matched tests along with their ancestors and descendants, so they can be reached and run
      val testParts = descriptor.testParts().map { it.removeLineBreaks() }
      return testParts.zip(pattern.contexts).all { (part, context) -> context.toWildcardRegex().matches(part) }
   }

   private fun String.removeLineBreaks(): String = replace(Regex("\r\n|\n|\r"), " ")

   private fun packageMatch(pattern: TestPattern, pck: String): Boolean = when {
      !pattern.subpackages -> pattern.packageName.toWildcardRegex().matches(pck)
      pattern.packageName.isEmpty() -> true
      else -> "${pattern.packageName}.*".toWildcardRegex().matches("$pck.")
   }

   private fun String.toWildcardRegex(): Regex = split("*").joinToString(".*") { Regex.escape(it) }.toRegex()
}
