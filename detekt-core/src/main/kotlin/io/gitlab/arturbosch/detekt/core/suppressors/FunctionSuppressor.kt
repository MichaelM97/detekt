package io.gitlab.arturbosch.detekt.core.suppressors

import io.github.detekt.tooling.api.FunctionSignature
import io.gitlab.arturbosch.detekt.api.ConfigAware
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

internal fun functionSuppressorFactory(rule: ConfigAware, bindingContext: BindingContext): Suppressor? {
    val signatures = rule.valueOrDefault("ignoreFunction", emptyList<String>()).map(FunctionSignature::fromString)
    return if (signatures.isNotEmpty()) {
        { finding ->
            val element = finding.entity.ktElement
            element != null && functionSuppressor(element, bindingContext, signatures)
        }
    } else {
        null
    }
}

private fun functionSuppressor(
    element: KtElement,
    bindingContext: BindingContext,
    functionSignatures: List<FunctionSignature>,
): Boolean {
    return element.isInFunctionNamed(bindingContext, functionSignatures)
}

private fun KtElement.isInFunctionNamed(
    bindingContext: BindingContext,
    methodSignatures: List<FunctionSignature>,
): Boolean {
    return if (this is KtNamedFunction && methodSignatures.any { it.match(this, bindingContext) }) {
        true
    } else {
        getStrictParentOfType<KtNamedFunction>()?.isInFunctionNamed(bindingContext, methodSignatures) ?: false
    }
}
