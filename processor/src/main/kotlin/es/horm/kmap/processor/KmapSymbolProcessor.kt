package es.horm.kmap.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
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
            buildFunSpec(
                source,
                (it.arguments.first { it.name?.asString() == "target" }.value as KSType).declaration as KSClassDeclaration
            )
        }

        val fileSpec = FileSpec.builder("com.example.generated", "Test")
            .addFunctions(funs.toList())
            .build()

        fileSpec.writeTo(codeGenerator = generator, aggregating = false)
        generated = true

        return emptyList()
    }

    private fun buildFunSpec(
        source: KSClassDeclaration,
        target: KSClassDeclaration
    ): FunSpec {
        val sourceParams = source.getAllProperties().filter { it.isPublic() }.map {
            it.simpleName.asString() to it.type.resolve()
        }.toMap()
        val targetParams = target.getAllProperties().filter { it.isPublic() }.map {
            it.simpleName.asString() to it.type.resolve()
        }.toMap()

        if (!targetParams.all { sourceParams[it.key] == it.value }) error("Not all params of target are contained in source")

        val targetTypeName = target.asStarProjectedType().toTypeName()

        val constructorCall = CodeBlock.builder()
            .add("return %T(\n", targetTypeName)
            .indent()
            .apply {
                targetParams.forEach {
                    add("%L = %L,\n", it.key, it.key)
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
