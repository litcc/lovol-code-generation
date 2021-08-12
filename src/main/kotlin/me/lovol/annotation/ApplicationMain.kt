package me.lovol.annotation


/**
 * 用来 定位 程序入口的注解
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ApplicationMain
