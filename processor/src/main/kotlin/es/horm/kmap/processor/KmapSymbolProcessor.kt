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
import es.horm.kmap.runtime.annotation.Aggregator
import es.horm.kmap.runtime.annotation.Mapping
import es.horm.kmap.runtime.NOOP
import es.horm.kmap.runtime.annotation.KmapFrom
import es.horm.kmap.runtime.annotation.KmapTo
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class KmapSymbolProcessor(
    private val generator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    var generated = false

    fun generateFunSpecs(ksClass: KSClassDeclaration): List<FunSpec> {
        val kmapToAnnotations = ksClass.annotations.filter { it.shortName.asString() == KmapTo::class.simpleName!! }
        val kmapFromAnnotations = ksClass.annotations.filter { it.shortName.asString() == KmapFrom::class.simpleName!! }

        val funSpecs = mutableListOf<FunSpec>()
        for (instance in kmapToAnnotations) {
            val targetClass =
                (instance.arguments.first { it.name?.asString() == KmapTo::target.name }.value as KSType).declaration as KSClassDeclaration
            funSpecs.add(
                buildMapperFromMapTo(
                    source = ksClass,
                    target = targetClass,
                    mappings = instance.assembleParamMappings(),
                    aggregators = instance.assembleParamAggregations(),
                )
            )
        }

        for(instance in kmapFromAnnotations) {
            logger.info("KmapFrom: ${KmapFrom::source.name}")
            logger.info("Instance args ${instance.arguments}")
            logger.info("Any? ${instance.arguments.any { it.name?.asString() == KmapFrom::source.name }}")
            logger.info("KmapFrom: ${KmapFrom::source.name} Test ${instance.arguments.any { it.name?.asString() == KmapFrom::source.name }.toString()}")
            val sourceClass = (instance.arguments.firstOrNull { it.name?.asString() == KmapFrom::source.name }?.value as? KSType)?.declaration as? KSClassDeclaration ?: break
            funSpecs.add(
                buildMapperFromMapTo(
                    source = sourceClass,
                    target = ksClass,
                    mappings = instance.assembleParamMappings(),
                    aggregators = instance.assembleParamAggregations(),
                )
            )
        }

        return funSpecs
    }

    private fun KSAnnotation.assembleParamMappings(): List<ParamMapping> {
        val mappingsArg = arguments.firstOrNull { it.name?.asString() == KmapTo::mappings.name }?.value as? List<*>
        return mappingsArg?.mapNotNull {
            val mappingAnnotation = it as? KSAnnotation ?: return@mapNotNull null

            val sourceArg = mappingAnnotation.getArgument(Mapping::source) ?: return@mapNotNull null
            val targetArg = mappingAnnotation.getArgument(Mapping::target) ?: return@mapNotNull null
            val transformerArg =
                (mappingAnnotation.arguments.first { it.name?.asString() == "transformer" }.value as? KSType)?.declaration as? KSClassDeclaration

            ParamMapping(sourceArg, targetArg, transformerArg)
        } ?: emptyList()
    }

    private fun KSAnnotation.assembleParamAggregations(): List<ParamAggregation> {
        val aggregatorArg = arguments.firstOrNull { it.name?.asString() == KmapTo::aggregators.name }?.value as? List<*>
        return aggregatorArg?.mapNotNull {
            val aggregatorAnnotation = it as? KSAnnotation ?: return@mapNotNull null

            val targetArg = aggregatorAnnotation.getArgument(Aggregator::target) ?: return@mapNotNull null
            val transformerArg =
                (aggregatorAnnotation.arguments.first { it.name?.asString() == Aggregator::transformer.name }.value as? KSType)?.declaration as? KSClassDeclaration
                    ?: return@mapNotNull null
            ParamAggregation(targetArg, transformerArg)
        } ?: emptyList()
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val kmapToAnnotatedClasses = resolver.getClassesWithAnnotation(KmapTo::class)
        val kmapFromAnnotatedClasses = resolver.getClassesWithAnnotation(KmapFrom::class)

        val annotatedClasses = kmapToAnnotatedClasses.plus(kmapFromAnnotatedClasses).distinct()

        val allFunSpecs = annotatedClasses.flatMap { generateFunSpecs(it) }

        val fileSpec = FileSpec.builder("com.example.generated", "Test")
            .addFunctions(allFunSpecs)
            .addAnnotation(AnnotationSpec.builder(Suppress::class).addMember(""""all"""").build())
            .build()

        fileSpec.writeTo(codeGenerator = generator, aggregating = false)
        generated = true

        return emptyList()
    }

    private fun Resolver.getClassesWithAnnotation(annotation: KClass<*>): List<KSClassDeclaration> =
        getSymbolsWithAnnotation(annotation.qualifiedName!!)
            .toList()
            .filterIsInstance<KSClassDeclaration>()

    private fun buildMapperFromMapTo(
        source: KSClassDeclaration,
        target: KSClassDeclaration,
        mappings: List<ParamMapping> = listOf(),
        aggregators: List<ParamAggregation> = listOf()
    ): FunSpec {
        val sourceParams = source.getAllPublicProperties().associate { it.simpleName.asString() to it.type.resolve() }
        val targetParams =
            target.primaryConstructor!!.parameters.associate { it.name!!.asString() to it.type.resolve() }

        val matchingTargetParams = targetParams.filter { sourceParams[it.key] == it.value }
        val nonMatchingTargetParams = targetParams.filterNot { sourceParams[it.key] == it.value }

        val paramsNotSatisfiedByMappings = nonMatchingTargetParams.filterNot { (targetParamName, targetParamType) ->
            val mappingSource =
                mappings.firstOrNull() { it.target == targetParamName }?.source ?: return@filterNot false
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
                    if (it.transformer?.simpleName?.asString() == NOOP::class.simpleName) {
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

fun KSClassDeclaration.getAllPublicProperties() = getAllProperties().filter { it.isPublic() }
