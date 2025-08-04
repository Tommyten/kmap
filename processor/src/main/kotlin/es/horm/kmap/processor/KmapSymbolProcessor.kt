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
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import es.horm.kmap.runtime.Aggregator
import es.horm.kmap.runtime.KmapTo
import es.horm.kmap.runtime.Mapping
import es.horm.kmap.runtime.NOOP
import kotlin.reflect.KProperty

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

        // iterate over all kmapTo Annotations the class is annotated with
        val mapperFunSpecs = annotations.map { kmapToInstance ->
            // get all mappings in the mapping array arg and assemble a ParamMapping Instance with its arguments
            val mappingsArg = kmapToInstance.arguments.firstOrNull { it.name?.asString() == "mappings" }?.value as? List<*>
            val mappings = mappingsArg?.mapNotNull {
                val mappingAnnotation = it as? KSAnnotation ?: return@mapNotNull null

                val sourceArg = mappingAnnotation.getArgument(Mapping::source) ?: return@mapNotNull null
                val targetArg = mappingAnnotation.getArgument(Mapping::target) ?: return@mapNotNull null
                val transformerArg = (mappingAnnotation.arguments.first { it.name?.asString() == "transformer" }.value as? KSType)?.declaration as? KSClassDeclaration

                ParamMapping(sourceArg, targetArg, transformerArg)
            }

            // get all aggregators in the aggregator array arg and assemble a ParamAggregration Instance with its arguments
            val aggregatorArg = kmapToInstance.arguments.firstOrNull { it.name?.asString() == "aggregators" }?.value as? List<*>
            val aggregators = aggregatorArg?.mapNotNull {
                val aggregatorAnnotation = it as? KSAnnotation ?: return@mapNotNull null

                val targetArg = aggregatorAnnotation.getArgument(Aggregator::target) ?: return@mapNotNull null
                val transformerArg = (aggregatorAnnotation.arguments.first { it.name?.asString() == Aggregator::transformer.name }.value as? KSType)?.declaration as? KSClassDeclaration ?: return@mapNotNull null
                ParamAggregation(targetArg, transformerArg)
            }

            val targetClass = (kmapToInstance.arguments.first { it.name?.asString() == "target" }.value as KSType).declaration as KSClassDeclaration
            buildFunSpec(
                source = source,
                target = targetClass,
                mappings = mappings ?: listOf(),
                aggregators = aggregators ?: listOf(),
            )
        }

        val fileSpec = FileSpec.builder("com.example.generated", "Test")
            .addFunctions(mapperFunSpecs.toList())
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember(""""all"""").build())
            .build()

        fileSpec.writeTo(codeGenerator = generator, aggregating = false)
        generated = true

        return emptyList()
    }

    data class ParamMapping(val source: String, val target: String, val transformer: KSClassDeclaration?)
    data class ParamAggregation(val target: String, val transformer: KSClassDeclaration)

    fun KSClassDeclaration.getAllPublicProperties() = getAllProperties().filter { it.isPublic() }

    private fun buildFunSpec(
        source: KSClassDeclaration,
        target: KSClassDeclaration,
        mappings: List<ParamMapping> = listOf(),
        aggregators: List<ParamAggregation> = listOf()
    ): FunSpec {
        val sourceParams = source.getAllPublicProperties().associate { it.simpleName.asString() to it.type.resolve() }
        val targetParams = target.primaryConstructor!!.parameters.associate { it.name!!.asString() to it.type.resolve() }

        val matchingTargetParams = targetParams.filter { sourceParams[it.key] == it.value}
        val nonMatchingTargetParams = targetParams.filterNot { sourceParams[it.key] == it.value }

        val paramsNotSatisfiedByMappings = nonMatchingTargetParams.filterNot { (targetParamName, targetParamType) ->
            val mappingSource = mappings.firstOrNull() { it.target == targetParamName }?.source ?: return@filterNot false
            val src = sourceParams[mappingSource]
            src == targetParamType
        }
        val paramsNotSatisfiedByAggregation = paramsNotSatisfiedByMappings.filterNot { (targetParamName, _) ->
            aggregators.firstOrNull { it.target == targetParamName } ?: return@filterNot false
            true
        }
        check(paramsNotSatisfiedByAggregation.isEmpty())

        val targetTypeName = target.asStarProjectedType().toTypeName()

        val constructorCall = CodeBlock.builder()
            .add("return %T(\n", targetTypeName)
            .indent()
            .apply {
                matchingTargetParams.forEach {
                    add("%L = %L,\n", it.key, it.key)
                }
                mappings.forEach {
                    if(it.transformer?.simpleName?.asString() == NOOP::class.simpleName) {
                        add("%L = %L,\n", it.target, it.source)
                    } else {
                        add("%L = %T().transform(%L),\n", it.target, it.transformer?.toClassName(), it.source)
                    }
                }
                aggregators.forEach {
                    add("%L = %T().transform(this),\n", it.target, it.transformer.toClassName())
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

@Suppress("UNCHECKED_CAST")
fun <T> KSAnnotation.getArgument(property: KProperty<T>): T? {
    return arguments.first { it.name?.asString() == property.name }.value as T?
}
