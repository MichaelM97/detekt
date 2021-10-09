package io.github.detekt.tooling.api

import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.rules.setupKotlinEnvironment
import io.gitlab.arturbosch.detekt.test.getContextForPaths
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class FunctionSignatureSpec : Spek({
    setupKotlinEnvironment()
    val env: KotlinCoreEnvironment by memoized()

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

    describe("match KtNamedFunctions") {
        matrixCase.forEach { (methodSignature, cases) ->
            context("When $methodSignature") {
                cases.forEach { (code, result) ->
                    it("in case $code it returns $result") {
                        val (function, bindingContext) = buildKtFunction(env, code)
                        assertThat(methodSignature.match(function, bindingContext)).apply {
                            if (result) isTrue() else isFalse()
                        }
                    }
                }
            }
        }
    }
})

private val matrixCase: Map<FunctionSignature, Map<String, Boolean>> = run {
    val functions = arrayOf(
        "fun toString() = Unit",
        "fun toString(hello: String) = Unit",
        "fun toString(hello: String, world: Int) = Unit",
        "fun compare() = Unit",
        "fun compare(hello: String) = Unit",
        "fun compare(hello: String, world: Int) = Unit",
    )

    linkedMapOf(
        FunctionSignature.fromString("toString") to linkedMapOf(
            functions[0] to true, // fun toString()
            functions[1] to true, // fun toString(hello: String)
            functions[2] to true, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
        FunctionSignature.fromString("toString()") to linkedMapOf(
            functions[0] to true, // fun toString()
            functions[1] to false, // fun toString(hello: String)
            functions[2] to false, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
        FunctionSignature.fromString("toString(kotlin.String)") to linkedMapOf(
            functions[0] to false, // fun toString()
            functions[1] to true, // fun toString(hello: String)
            functions[2] to false, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
        FunctionSignature.fromString("toString(kotlin.Int)") to linkedMapOf(
            functions[0] to false, // fun toString()
            functions[1] to false, // fun toString(hello: String)
            functions[2] to false, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
        FunctionSignature.fromString("toString(kotlin.String, kotlin.Int)") to linkedMapOf(
            functions[0] to false, // fun toString()
            functions[1] to false, // fun toString(hello: String)
            functions[2] to true, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
        FunctionSignature.fromString("toString(String)") to linkedMapOf(
            functions[0] to false, // fun toString()
            functions[1] to false, // fun toString(hello: String)
            functions[2] to false, // fun toString(hello: String, world: Int)
            functions[3] to false, // fun compare()
            functions[4] to false, // fun compare(hello: String)
            functions[5] to false, // fun compare(hello: String, world: Int)
        ),
    )
}

private class TestCase(
    val testDescription: String,
    val methodSignature: String,
    val expectedFunctionSignature: FunctionSignature,
)

private fun buildKtFunction(environment: KotlinCoreEnvironment, code: String): Pair<KtNamedFunction, BindingContext> {
    val ktFile = compileContentForTest(code)
    val bindingContext = getContextForPaths(environment, listOf(ktFile))
    return ktFile.findChildByClass(KtNamedFunction::class.java)!! to bindingContext
}
