package me.lovol.annotation.processor


import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import me.lovol.annotation.extend.isKtSingletonsObject
import me.lovol.annotation.extend.logError
import me.lovol.annotation.extend.toTypeElementOrNull
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

/**
 * 日志注入
 * 需要引用依赖 org.apache.logging.log4j:log4j-slf4j18-impl
 *
 */

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class LogInject

@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes("me.lovol.annotation.processor.LogInject")
//@SupportedOptions(LogInjectProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class LogInjectProcessor : AbstractProcessor() {
//    companion object {
//        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
//    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        //processingEnv.logInfo("LogInjectProcessor::process")
        val annotatedElements = roundEnv.getElementsAnnotatedWith(LogInject::class.java)
        if (annotatedElements.isEmpty()) {
            return false
        }

//        val kaptKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
//            processingEnv.messager.printMessage(ERROR, "Can't find the target directory for generated Kotlin files.")
//            return false
//        }

        //processingEnv.logInfo("找到${annotatedElements.size}个使用@LogInject")
        annotatedElements.forEach {
            val element = it.toTypeElementOrNull() ?: return@forEach
            //val targetQualifiedName = element.qualifiedName.toString();
            //processingEnv.logInfo(targetQualifiedName)
            //简单名，如MainActivity
            val targetName = element.simpleName.toString()
            //processingEnv.logInfo(targetName)

            //1.获取包名
            val packageElement: PackageElement = processingEnv.elementUtils.getPackageOf(it)
            val pkName = packageElement.qualifiedName.toString()
            //processingEnv.logInfo("package = %s", pkName)

            //processingEnv.logInfo(it.isKtSingletonsObject().toString())

            val hoverboard = if (it.isKtSingletonsObject()) {
                ClassName(pkName, targetName)
            } else {
                ClassName(pkName, "${targetName}.Companion")
            }

            // 加载 org.slf4j.Logger 类
            val slf4jClass = try {
                Class.forName("org.slf4j.Logger")
            } catch (e: ClassNotFoundException) {
                processingEnv.logError("org.apache.logging.log4j:log4j-slf4j18-impl 依赖不存在")
                return@process false
            }


            val fun1 = PropertySpec.builder("log", slf4jClass)
                .receiver(hoverboard)
                .getter(
                    FunSpec.getterBuilder()
                        .addStatement("return org.slf4j.LoggerFactory.getLogger(this::class.java.name)")
                        .build()
                ).build()

            // 写出 kt文件名
            val file = FileSpec.builder(pkName, targetName + "Gen")
                .addProperty(fun1)
                .build()
            file.writeTo(processingEnv.filer)
        }
        return true
    }


}
