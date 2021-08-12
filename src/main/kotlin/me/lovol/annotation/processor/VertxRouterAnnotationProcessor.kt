package me.lovol.annotation.processor

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import me.lovol.annotation.ApplicationMain
import me.lovol.annotation.DIMain
import me.lovol.annotation.extend.logError
import me.lovol.annotation.extend.logInfo
import me.lovol.annotation.extend.toTypeElementOrNull
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.PackageElement
import javax.lang.model.element.TypeElement

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Component

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Controller(val method: Array<HttpMethods>, val path: String)

@Suppress("unused")
enum class HttpMethods {
    OPTIONS,
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    TRACE,
    CONNECT,
    PATCH,
    PROPFIND,
    PROPPATCH,
    MKCOL,
    COPY,
    MOVE,
    LOCK,
    UNLOCK,
    MKCALENDAR,
    VERSION_CONTROL,
    REPORT,
    CHECKIN,
    CHECKOUT,
    UNCHECKOUT,
    MKWORKSPACE,
    UPDATE,
    LABEL,
    MERGE,
    BASELINE_CONTROL,
    MKACTIVITY,
    ORDERPATCH,
    ACL,
    SEARCH
}


@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes(
    "me.lovol.annotation.ApplicationMain",
    "me.lovol.annotation.DIMain",
    "me.lovol.annotation.processor.Component",
    "me.lovol.annotation.processor.Controller"
)
class VertxRouterInjectProcessor : AbstractProcessor() {


    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {

        /**
         * 获取入口类相关路径
         */
        val mainElements = roundEnv.getElementsAnnotatedWith(ApplicationMain::class.java)
        if (mainElements.isEmpty()) {
            //processingEnv.logInfo("请设置程序入口 @ApplicationMain")
            return false
        }
        if (mainElements.size > 1) {
            processingEnv.logInfo("全局只能存在一个入口注解 @ApplicationMain")
            return false
        }
        val MainPackageElement: PackageElement = processingEnv.elementUtils.getPackageOf(mainElements.last())
        val MainPkName = MainPackageElement.qualifiedName.toString()

        /**
         * 获取DI相关路径
         */
        val DIElements = roundEnv.getElementsAnnotatedWith(DIMain::class.java)
        if (DIElements.isEmpty()) {
            //processingEnv.logInfo("请设置DI注解 @DIMain")
            return false
        }
        if (DIElements.size > 1) {
            processingEnv.logInfo("全局只能存在一个DI注解 @DIMain")
            return false
        }
        val DIPackageElement: PackageElement = processingEnv.elementUtils.getPackageOf(DIElements.last())
        val DIPkName = DIPackageElement.qualifiedName.toString()


        //检查依赖
        val vertxRouterClass = try {
            Class.forName("io.vertx.ext.web.Router")
        } catch (e: ClassNotFoundException) {
            processingEnv.logError("io.vertx:vertx-web 依赖不存在")
            return false
        }


        // 开始构建
        val vertxClass = ClassName("io.vertx.core", "Vertx")
        val diClass = MemberName(DIPkName, "di")
        val instanceClass = MemberName("org.kodein.di", "instance")

        val registerFun = FunSpec.builder("registerVertxRouter")
            .addParameter("router", vertxRouterClass)
            .addStatement("val vertx: %T by %M.%M()", vertxClass, diClass, instanceClass)

        // 查找组件
        val annotatedElements = roundEnv.getElementsAnnotatedWith(Component::class.java)
        if (annotatedElements.isEmpty()) {
            return false
        }
        annotatedElements.forEach { it1: Element ->
            // 获取组件的包名以及类名
            val element = it1.toTypeElementOrNull() ?: return@forEach
            val packageElement: PackageElement = processingEnv.elementUtils.getPackageOf(it1)
            val pkName = packageElement.qualifiedName.toString()
            val targetName = element.simpleName.toString()

            // 筛选出当前类中的 Controller注解
            val controllerElements = roundEnv.getElementsAnnotatedWith(Controller::class.java)
            val controllerElementsTmp = controllerElements.filter {
                val ll = it.enclosingElement
                val elementTmp2 = ll?.toTypeElementOrNull() ?: return@filter true
                return@filter "${elementTmp2.qualifiedName}" == "${pkName}.$targetName"
            }

            // 根据Controller注解值生成代码
            if (controllerElementsTmp.isNotEmpty()) {
                val valName = targetName.substring(0, 1).lowercase() + targetName.substring(1) + "Impl";
                val componentClass = ClassName(pkName, targetName)
                registerFun.addStatement("val ${valName} = %T(vertx)", componentClass)

                controllerElementsTmp.forEach Item@{
                    val param: String = it.getAnnotation(Controller::class.java).path
                    val method: Array<HttpMethods> = it.getAnnotation(Controller::class.java).method
                    var methodStr = """"""
                    method.forEach { httpMethodItem ->
                        methodStr =
                            methodStr.plus(""".method(io.vertx.core.http.HttpMethod.valueOf("${httpMethodItem.toString()}"))""")
                    }

                    registerFun.addStatement("""router.route("${param}")${methodStr}.handler(${valName}::${it.simpleName})""")
                    //processingEnv.logInfo("${it.simpleName}")
                }
            }

        }

        // 构建kt文件
        val file = FileSpec.builder(MainPkName, "VertxRouterController")
            .addFunction(
                registerFun.build()
            )
            .build()

        try {
            file.writeTo(processingEnv.filer)

        } catch (e: Exception) {
            processingEnv.logInfo("${e.message}")
        }
        return true
    }

}