package io.kotest.engine.extensions

/**
 * Parses an env variable of the form some.package.Class.test
 *
 * This parser expects a package name, a class name, and a test name.
 * The package is required, the class is optional, and the test is optional.
 * The test can be a nested test.
 *
 * Package names are expected to be lower case.
 * The class name is the first part that starts with an upper case letter, ignoring leading `*`s.
 * The test context is whatever comes after the class name.
 *
 * Any part can contain `*` wildcards, as in Gradle's `--tests`, for example `*Test` or `io.*.SomeTest`.
 *
 * Examples:
 *
 * - io.kotest (package only)
 * - io.kotest.SomeTest (spec only)
 * - io.kotest.SomeTest.my test (root test)
 * - io.kotest.SomeTest.my test -- nested test
 *
 */
internal object TestPatternParser {

   // this token is used to delimit nested tests
   const val DELIMITER = " -- "

   fun parse(arg: String): TestPattern {
      require(arg.isNotBlank())
      val parts = arg.split(".")

      val packageParts = parts.takeWhile { it.trimStart('*').firstOrNull()?.isUpperCase() != true }
      val simpleName = parts.drop(packageParts.size).firstOrNull()
      if (simpleName === null) {
         return if (packageParts.last() == "*")
            TestPattern(packageParts.dropLast(1).joinToString("."), true, null, emptyList())
         else
            TestPattern(packageParts.joinToString("."), false, null, emptyList())
      }

      val test = parts
         .drop(packageParts.size) // drop the package parts
         .drop(1) // drop the class name
         .joinToString(".") // test names can contain dots, so restore them
         .ifEmpty { null }

      if (test == null) return TestPattern(packageParts.joinToString("."), false, simpleName, emptyList())

      val contexts = test.split(DELIMITER).filter { it.isNotBlank() }
      return TestPattern(packageParts.joinToString("."), false, simpleName, contexts)
   }
}

internal data class TestPattern(
   val packageName: String,
   val subpackages: Boolean,
   val className: String?,
   val contexts: List<String>
)
