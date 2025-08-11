package es.horm.kmap.processor

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class ParamMapping(val source: String, val target: String, val transformer: KSClassDeclaration?)
