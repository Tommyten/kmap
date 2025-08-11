package es.horm.kmap.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class ParamAggregation(val target: String, val transformer: KSClassDeclaration)
