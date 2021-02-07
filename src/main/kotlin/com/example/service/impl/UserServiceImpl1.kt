package com.example.service.impl

import com.example.ThymeleafUser
import com.example.service.IUserService

class UserServiceImpl1 :IUserService {
    override fun getName(age: Int):String {
        return "Lei Jun, age is ${age}"
    }

    override fun getUsers(): List<ThymeleafUser> = listOf<ThymeleafUser>(
        ThymeleafUser(1, "雷军"),
        ThymeleafUser(2, "李彦宏"),
        ThymeleafUser(3, "余大嘴"),
        ThymeleafUser(4, "微信"),
        ThymeleafUser(5, "都有"),
        ThymeleafUser(6, "天天动听"),
        ThymeleafUser(7, "千千静听"),
        ThymeleafUser(8, "酷我应用"),
        ThymeleafUser(9, "JetBrains"),
        ThymeleafUser(10, "V2rany"),
    )
}