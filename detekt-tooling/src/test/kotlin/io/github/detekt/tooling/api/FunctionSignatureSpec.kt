package io.github.detekt.tooling.api

import org.assertj.core.api.Assertions.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class FunctionSignatureSpec : Spek({
    describe("MethodSignature.fromString") {
        listOf(
            TestCase(
                testDescription = "should return method name and null params list in case of simplifies signature",
                methodSignature = "java.time.LocalDate.now",
                expectedFunctionSignature = FunctionSignature.Name("java.time.LocalDate.now"),
            ),
            TestCase(
                testDescription = "should return method name and empty params list for full signature parameterless method",
                methodSignature = "java.time.LocalDate.now()",
                expectedFunctionSignature = FunctionSignature.Parameters("java.time.LocalDate.now", emptyList()),
            ),
            TestCase(
                testDescription = "should return method name and params list for full signature method with single param",
                methodSignature = "java.time.LocalDate.now(java.time.Clock)",
                expectedFunctionSignature = FunctionSignature.Parameters(
                    "java.time.LocalDate.now",
                    listOf("java.time.Clock"),
                ),
            ),
            TestCase(
                testDescription = "should return method name and params list for full signature method with multiple params",
                methodSignature = "java.time.LocalDate.of(kotlin.Int, kotlin.Int, kotlin.Int)",
                expectedFunctionSignature = FunctionSignature.Parameters(
                    "java.time.LocalDate.of",
                    listOf("kotlin.Int", "kotlin.Int", "kotlin.Int"),
                ),
            ),
            TestCase(
                testDescription = "should return method name and params list for full signature method with multiple params " +
                    "where method name has spaces and special characters",
                methodSignature = "io.gitlab.arturbosch.detekt.SomeClass.`some , method`(kotlin.String)",
                expectedFunctionSignature = FunctionSignature.Parameters(
                    "io.gitlab.arturbosch.detekt.SomeClass.some , method",
                    listOf("kotlin.String"),
                ),
            )
        ).forEach { testCase ->
            it(testCase.testDescription) {
                val functionSignature = FunctionSignature.fromString(testCase.methodSignature)

                assertThat(functionSignature).isEqualTo(testCase.expectedFunctionSignature)
            }
        }
    }
})

private class TestCase(
    val testDescription: String,
    val methodSignature: String,
    val expectedFunctionSignature: FunctionSignature,
)
