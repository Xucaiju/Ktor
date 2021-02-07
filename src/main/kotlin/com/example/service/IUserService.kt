package com.example.service

import com.example.ThymeleafUser

interface IUserService {
    fun getName(age:Int):String
    fun getUsers():List<ThymeleafUser>
}