package me.lovol.annotation.extend

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.tools.Diagnostic


fun ProcessingEnvironment.logInfo(format: String, vararg args: Any) {
    messager.printMessage(Diagnostic.Kind.NOTE, String.format(format, *args) + "\r\n")
}

fun ProcessingEnvironment.logInfo(arg: String) {
    messager.printMessage(Diagnostic.Kind.NOTE, arg + "\r\n")
}


fun ProcessingEnvironment.logError(format: String, vararg args: Any) {
    messager.printMessage(Diagnostic.Kind.ERROR, String.format(format, *args) + "\r\n")
}

fun ProcessingEnvironment.logError(arg: String) {
    messager.printMessage(Diagnostic.Kind.ERROR, arg + "\r\n")
}

fun ProcessingEnvironment.logWarning(format: String, vararg args: Any) {
    messager.printMessage(Diagnostic.Kind.WARNING, String.format(format, *args) + "\r\n")
}

fun ProcessingEnvironment.logWarning(arg: String) {
    messager.printMessage(Diagnostic.Kind.WARNING, arg + "\r\n")
}


fun Element.toTypeElementOrNull(): TypeElement? {
    if (this !is TypeElement) {
        return null
    }
    return this
}


fun Element.toVariableElementOrNull(): VariableElement? {
    if (this !is VariableElement) {
        return null
    }
    return this
}

/**
 * 用来调试
 */

fun Element.getElementType(env: ProcessingEnvironment) {
    ///== ElementKind.CLASS
    env.logInfo(this.kind.name)
    when (this) {
        is TypeElement -> env.logInfo("ElementType :: TypeElement")
        is PackageElement -> env.logInfo("ElementType :: PackageElement")
        is TypeParameterElement -> env.logInfo("ElementType :: TypeParameterElement")
        is VariableElement -> env.logInfo("ElementType :: VariableElement")
        is ExecutableElement -> env.logInfo("ElementType :: ExecutableElement")
        else ->{
            env.logInfo("ElementType :: 未知类型")
        }
    }
}


//private val log: Logger = org.slf4j.LoggerFactory.getLogger(this::class.java.name)
//
//inline fun <reified T : Annotation> Element.getAnnotationClassValue(f: T.() -> KClass<*>) = try {
//    getAnnotation(T::class.java).f()
//    throw Exception("Expected to get a MirroredTypeException")
//} catch (e: MirroredTypeException) {
//    e.typeMirror
//}


fun Element.isKt() = this.annotationMirrors.any { it.annotationType.toString() == "kotlin.Metadata" }


fun Element.isKtSingletonsObject(): Boolean {
    //if (element.modifiers.contains(Modifier.STATIC)) return true

//    val parent = this.enclosingElement
//    val typeElement = parent.toTypeElementOrNull()
////    processingEnv.logInfo(
////        "isStatic--->" + if (typeElement == null) {
////            "为空"
////        } else {
////            "不为空"
////        }
//    )
    if (!this.isKt()) return false
    val type = this.toTypeElementOrNull() ?: return false
    val instances = type.enclosedElements
        .filter { "INSTANCE" == it.simpleName.toString() }
        .filter { it.modifiers.contains(Modifier.STATIC) }
        .filter { it.kind.isField }
    return instances.isNotEmpty()
}
