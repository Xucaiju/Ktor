package com.example.exception

class AuthenticationException(var msg:String) : RuntimeException()
class AuthorizationException : RuntimeException()