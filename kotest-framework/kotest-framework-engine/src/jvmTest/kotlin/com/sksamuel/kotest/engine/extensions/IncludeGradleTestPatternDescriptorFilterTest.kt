package com.sksamuel.kotest.engine.extensions

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.annotation.LinuxOnlyGithubCondition
import io.kotest.core.descriptors.Descriptor
import io.kotest.core.descriptors.DescriptorId
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.extensions.filter.DescriptorFilterResult.Exclude
import io.kotest.engine.extensions.filter.DescriptorFilterResult.Include
import io.kotest.engine.extensions.filter.IncludePatternEnvDescriptorFilter
import io.kotest.matchers.shouldBe

// the expectations are what Gradle's --tests selects without the Kotest Gradle plugin
@EnabledIf(LinuxOnlyGithubCondition::class)
class IncludeGradleTestPatternDescriptorFilterTest : FunSpec({

   val specs = listOf("FooScreenTest", "BarScreenTest", "OtherTest", "ui.BazScreenTest")
      .map { Descriptor.SpecDescriptor(DescriptorId("com.example.$it")) }
   val foo = specs.first()

   context("selects specs like Gradle") {
      withData(
         nameFn = { it.key },
         ts = mapOf(
            "FooScreenTest" to "FooScreenTest",
            "*FooScreenTest" to "FooScreenTest",
            "*.FooScreenTest" to "FooScreenTest",
            "*ScreenTest" to "FooScreenTest,BarScreenTest,BazScreenTest",
            "com.example.*ScreenTest" to "FooScreenTest,BarScreenTest,BazScreenTest",
            "com.*.BazScreenTest" to "BazScreenTest",
            "com.ex*" to "FooScreenTest,BarScreenTest,OtherTest,BazScreenTest",
            "*Nothing*" to "",
         ).entries,
      ) { (pattern, expected) ->
         val selected = specs.filter { IncludePatternEnvDescriptorFilter.filter(pattern, it) == Include }
         selected.joinToString(",") { it.id.value.substringAfterLast(".") } shouldBe expected
      }
   }

   test("selects the matched test with its ancestors and descendants") {
      val a = foo.append("a")
      val pattern = "*ScreenTest.a -- b*"
      val descriptors = listOf(foo, a, a.append("b1"), a.append("b1").append("c"), a.append("x"), foo.append("d"))
      val results = descriptors.map { IncludePatternEnvDescriptorFilter.filter(pattern, it) }
      results shouldBe listOf(Include, Include, Include, Include, Exclude(null), Exclude(null))
   }
})
