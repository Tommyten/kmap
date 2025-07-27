package es.horm.kmap.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import es.horm.kmap.runtime.KmapTo

class KmapSymbolProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    var generated = false

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val source = resolver.getSymbolsWithAnnotation(KmapTo::class.qualifiedName!!).single() as KSClassDeclaration
        val annotations = source.annotations.filter { it.shortName.asString() == "KmapTo" }

        val funs = annotations.map {

            val mappingsArg = it.arguments.firstOrNull() { it.name?.asString() == "mappings" }?.value as? List<*>
            val mappings = mappingsArg?.mapNotNull {
                val mappingAnnotation = it as? KSAnnotation ?: return@mapNotNull null

                val sourceArg = mappingAnnotation.arguments.first { it.name?.asString() == "source" }.value as? String ?: return@mapNotNull null
                val targetArg = mappingAnnotation.arguments.first { it.name?.asString() == "target" }.value as? String ?: return@mapNotNull null
                ParamMapping(sourceArg, targetArg)
            }?.associate { it.target to it.source }

            buildFunSpec(
                source,
                (it.arguments.first { it.name?.asString() == "target" }.value as KSType).declaration as KSClassDeclaration,
                mappings ?: mapOf()
            )
        }

        val fileSpec = FileSpec.builder("com.example.generated", "Test")
            .addFunctions(funs.toList())
            .build()

        fileSpec.writeTo(codeGenerator = generator, aggregating = false)
        generated = true

        return emptyList()
    }

    data class ParamMapping(val source: String, val target: String)

    fun KSClassDeclaration.getAllPublicProperties() = getAllProperties().filter { it.isPublic() }

    private fun buildFunSpec(
        source: KSClassDeclaration,
        target: KSClassDeclaration,
        mappings: Map<String, String> = mapOf()
    ): FunSpec {
        val sourceParams = source.getAllPublicProperties().associate { it.simpleName.asString() to it.type.resolve() }
        val targetParams = target.primaryConstructor!!.parameters.associate { it.name!!.asString() to it.type.resolve() }

//        if (!targetParams.all { sourceParams[it.key] == it.value }) error("Not all params of target are contained in source")

        val matchingTargetParams = targetParams.filter { sourceParams[it.key] == it.value}
        val nonMatchingTargetParams = targetParams.filterNot { sourceParams[it.key] == it.value }

        val test = nonMatchingTargetParams.all { (targetParamName, targetParamType) ->
            val (mappingTarget, mappingSource) = mappings.entries.first { it.key == targetParamName }
            val src = sourceParams[mappingSource]
            src == targetParamType
        }
        check(test)

        val targetTypeName = target.asStarProjectedType().toTypeName()

        val constructorCall = CodeBlock.builder()
            .add("return %T(\n", targetTypeName)
            .indent()
            .apply {
                matchingTargetParams.forEach {
                    add("%L = %L,\n", it.key, it.key)
                }
                mappings.forEach {
                    add("%L = %L,\n", it.key, it.value)
                }
            }
            .unindent()
            .add(")")
            .build()

        return FunSpec.builder("to${target.simpleName.asString()}")
            .receiver(source.asStarProjectedType().toTypeName())
            .returns(targetTypeName)
            .addCode(constructorCall)
            .build()
    }
}
