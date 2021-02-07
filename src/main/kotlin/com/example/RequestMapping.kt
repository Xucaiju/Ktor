package com.example

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

fun String.mappingPath(application: Application, vararg mappings:HttpMethod.()->Unit){
    application.routing{
        mappings.toList().forEachIndexed { index, function ->
            route(this@mappingPath){}
        }
    }
}