package me.lovol.annotation.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import me.lovol.annotation.extend.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

/**
 * 全局配置文件注解
 */

@Target(*[AnnotationTarget.FIELD])
@Retention(AnnotationRetention.SOURCE)
annotation class ConfigDefault(val value: Array<String>)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ConfigDefaultMain

@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes(
    "me.lovol.annotation.processor.ConfigDefaultMain",
    "me.lovol.annotation.processor.ConfigDefault"
)
//@SupportedOptions(LogInjectProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ConfigDefaultAnnotationProcessor : AbstractProcessor() {


    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        val configMain = roundEnv.getElementsAnnotatedWith(ConfigDefaultMain::class.java)
        if (configMain.isEmpty()) {
            //processingEnv.logInfo("找不到@ConfigDefaultMain")
            return false
        }
        if (configMain.size > 1) {
            processingEnv.logInfo("@ConfigDefaultMain，全局只能存在一个")
            return false
        }
        configMain.forEach {
            val element = it.toTypeElementOrNull() ?: return@forEach
            val targetName = element.simpleName.toString()

            val packageElement: PackageElement = processingEnv.elementUtils.getPackageOf(it)
            val pkName = packageElement.qualifiedName.toString()

            val hoverboard = if (it.isKtSingletonsObject()) {
                ClassName(pkName, targetName)
            } else {
                ClassName(pkName, "${targetName}.Companion")
            }


            val annotatedElements = roundEnv.getElementsAnnotatedWith(ConfigDefault::class.java)

            var code = """"""

            annotatedElements.forEach Item@{ itItem->
                val element2 = itItem.toVariableElementOrNull() ?: return@Item
                val param: Array<String> = element2.getAnnotation(ConfigDefault::class.java).value

                if (param.size != 2) {
                    processingEnv.logError("ConfigDefault注解，参数value字符串数组长度必须为2")
                    return false
                }
                when (param[0].lowercase()) {
                    "string" -> {
                        code += """"${element2.constantValue}" to "${param[1]}",
                            |
                        """.trimMargin()
                    }
                    "int" -> {
                        code += """"${element2.constantValue}" to ${param[1]},
                            |
                        """.trimMargin()
                    }
                    "boolean" -> {
                        if (param[1] == "true") {
                            code += """"${element2.constantValue}" to true,
                            |
                        """.trimMargin()
                        } else {
                            code += """"${element2.constantValue}" to false,
                            |
                        """.trimMargin()
                        }

                    }
                    "float" -> {
                        code += """"${element2.constantValue}" to ${param[1]}f,
                            |
                        """.trimMargin()
                    }
                }
                //processingEnv.logInfo("constantValue--->${element2.constantValue}")
            }

            val vertxJsonObjectClass = try {
                Class.forName("io.vertx.core.json.JsonObject")
            } catch (e: ClassNotFoundException) {
                processingEnv.logError("io.vertx:vertx-core 依赖不存在")
                return@process false
            }

            val fun1 = PropertySpec.builder("defaultConfig", vertxJsonObjectClass)
                .receiver(hoverboard)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return io.vertx.kotlin.core.json.jsonObjectOf($code)")
                        .build()
                ).build()

            // 写出 kt文件名
            val file = FileSpec.builder(pkName, targetName + "Gen")
                .addProperty(fun1)
                .build()

            file.writeTo(processingEnv.filer)
        }


        return true
//        TODO("Not yet implemented")
    }


}
