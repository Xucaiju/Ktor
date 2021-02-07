# ktor-sample

ktor-sample is your new project powered by [Ktor](http://ktor.io) framework.

<img src="https://repository-images.githubusercontent.com/40136600/f3f5fd00-c59e-11e9-8284-cb297d193133" alt="Ktor" width="100" style="max-width:20%;">

Company website: example.com Ktor Version: 1.5.1 Kotlin Version: 1.4.10
BuildSystem: [Gradle with Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

# Ktor Documentation

Ktor is a framework for quickly creating web applications in Kotlin with minimal effort.

* Ktor project's [Github](https://github.com/ktorio/ktor/blob/master/README.md)
* Getting started with [Gradle](http://ktor.io/quickstart/gradle.html)
* Getting started with [Maven](http://ktor.io/quickstart/maven.html)
* Getting started with [IDEA](http://ktor.io/quickstart/intellij-idea.html)

Selected Features:

* [Routing](#routing-documentation-jetbrainshttpswwwjetbrainscom)
* [Status Pages](#status-pages-documentation-jetbrainshttpswwwjetbrainscom)
* [Thymeleaf](#thymeleaf-documentation-jetbrainshttpswwwjetbrainscom)

## Routing Documentation ([JetBrains](https://www.jetbrains.com))

Allows to define structured routes and associated handlers.

### Description

Routing is a feature that is installed into an Application to simplify and structure page request handling. This page
explains the routing feature. Extracting information about a request, and generating valid responses inside a route, is
described on the requests and responses pages.

```application.install(Routing) {
    get("/") {
        call.respondText("Hello, World!")
    }
    get("/bye") {
        call.respondText("Good bye, World!")
    }

```

`get`, `post`, `put`, `delete`, `head` and `options` functions are convenience shortcuts to a flexible and powerful
routing system. In particular, get is an alias to `route(HttpMethod.Get, path) { handle(body) }`, where body is a lambda
passed to the get function.

### Usage

## Routing Tree

Routing is organized in a tree with a recursive matching system that is capable of handling quite complex rules for
request processing. The Tree is built with nodes and selectors. The Node contains handlers and interceptors, and the
selector is attached to an arc which connects another node. If selector matches current routing evaluation context, the
algorithm goes down to the node associated with that selector.

Routing is built using a DSL in a nested manner:

```
route("a") { // matches first segment with the value "a"
  route("b") { // matches second segment with the value "b"
     get {…} // matches GET verb, and installs a handler
     post {…} // matches POST verb, and installs a handler
  }
}
```

```
method(HttpMethod.Get) { // matches GET verb
   route("a") { // matches first segment with the value "a"
      route("b") { // matches second segment with the value "b"
         handle { … } // installs handler
      }
   }
}
```

Route resolution algorithms go through nodes recursively discarding subtrees where selector didn't match.

Builder functions:

* `route(path)` – adds path segments matcher(s), see below about paths
* `method(verb)` – adds HTTP method matcher.
* `param(name, value)` – adds matcher for a specific value of the query parameter
* `param(name)` – adds matcher that checks for the existence of a query parameter and captures its value
* `optionalParam(name)` – adds matcher that captures the value of a query parameter if it exists
* `header(name, value)` – adds matcher that for a specific value of HTTP header, see below about quality

## Path

Building routing tree by hand would be very inconvenient. Thus there is `route` function that covers most of the use
cases in a simple way, using path.

`route` function (and respective HTTP verb aliases) receives a `path` as a parameter which is processed to build routing
tree. First, it is split into path segments by the `/` delimiter. Each segment generates a nested routing node.

These two variants are equivalent:

```
route("/foo/bar") { … } // (1)

route("/foo") {
   route("bar") { … } // (2)
}
```

### Parameters

Path can also contain parameters that match specific path segment and capture its value into `parameters` properties of
an application call:

```
get("/user/{login}") {
   val login = call.parameters["login"]
}
```

When user agent requests `/user/john` using `GET` method, this route is matched and `parameters` property will
have `"login"` key with value `"john"`.

### Optional, Wildcard, Tailcard

Parameters and path segments can be optional or capture entire remainder of URI.

* `{param?}` –- optional path segment, if it exists it's captured in the parameter
* `*` –- wildcard, any segment will match, but shouldn't be missing
* `{...}` –- tailcard, matches all the rest of the URI, should be last. Can be empty.
* `{param...}` –- captured tailcard, matches all the rest of the URI and puts multiple values for each path segment
  into `parameters` using `param` as key. Use `call.parameters.getAll("param")` to get all values.

Examples:

```
get("/user/{login}/{fullname?}") { … }
get("/resources/{path...}") { … }
```

## Quality

It is not unlikely that several routes can match to the same HTTP request.

One example is matching on the `Accept` HTTP header which can have multiple values with specified priority (quality).

```
accept(ContentType.Text.Plain) { … }
accept(ContentType.Text.Html) { … }
```

The routing matching algorithm not only checks if a particular HTTP request matches a specific path in a routing tree,
but it also calculates the quality of the match and selects the routing node with the best quality. Given the routes
above, which match on the Accept header, and given the request header `Accept: text/plain; q=0.5, text/html` will
match `text/html` because the quality factor in the HTTP header indicates a lower quality fortext/plain (default is 1.0)
.

The Header `Accept: text/plain, text/*` will match `text/plain`. Wildcard matches are considered less specific than
direct matches. Therefore the routing matching algorithm will consider them to have a lower quality.

Another example is making short URLs to named entities, e.g. users, and still being able to prefer specific pages
like `"settings"`. An example would be

* `https://twitter.com/kotlin` -– displays user `"kotlin"`
* `https://twitter.com/settings` -- displays settings page

This can be implemented like this:

```
get("/{user}") { … }
get("/settings") { … }
```

The parameter is considered to have a lower quality than a constant string, so that even if `/settings` matches both,
the second route will be selected.

### Options

No options()

## Status Pages Documentation ([JetBrains](https://www.jetbrains.com))

Allow to respond to thrown exceptions.

### Description

The `StatusPages` feature allows Ktor applications to respond appropriately to any failure state.

### Usage

## Installation

This feature is installed using the standard application configuration:

```
fun Application.main() {
    install(StatusPages)
}
```

## Exceptions

The exception configuration can provide simple interception patterns for calls that result in a thrown exception. In the
most basic case, a `500` HTTP status code can be configured for any exceptions.

```
install(StatusPages) {
    exception<Throwable> { cause ->
        call.respond(HttpStatusCode.InternalServerError)
    }
}
```

More specific responses can allow for more complex user interactions.

```
install(StatusPages) {
    exception<AuthenticationException> { cause ->
        call.respond(HttpStatusCode.Unauthorized)
    }
    exception<AuthorizationException> { cause ->
        call.respond(HttpStatusCode.Forbidden)
    }
}
```

These customizations can work well when paired with custom status code responses, e.g. providing a login page when a
user has not authenticated.

Each call is only caught by a single exception handler, the closest exception on the object graph from the thrown
exception. When multiple exceptions within the same object hierarchy are handled, only a single one will be executed.

```
install(StatusPages) {
    exception<IllegalStateException> { cause ->
        fail("will not reach here")
    }
    exception<ClosedFileSystemException> {
        throw IllegalStateException()
    }
}
intercept(ApplicationCallPipeline.Fallback) {
    throw ClosedFileSystemException()
}
```

Single handling also implies that recursive call stacks are avoided. For example, this configuration would result in the
created IllegalStateException propagating to the client.

```
install(StatusPages) {
    exception<IllegalStateException> { cause ->
        throw IllegalStateException("")
    }
}
```

## Logging Exceptions

It is important to note that adding the handlers above will "swallow" the exceptions generated by your routes. In order
to log the actual errors generated, you can either log the `cause` manually, or simply re-throw it as shown below:

```
install(StatusPages) {
    exception<Throwable> { cause ->
        call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
        throw cause
    }
}
```

## Status

The `status` configuration provides a custom actions for status responses from within the application. Below is a basic
configuration that provides information about the HTTP status code within the response text.

```
install(StatusPages) {
    status(HttpStatusCode.NotFound) {
        call.respond(TextContent("${it.value} ${it.description}", ContentType.Text.Plain.withCharset(Charsets.UTF_8), it))
    }
}
```

## StatusFile

While the `status` configuration provides customizable actions on the response object, the more common solution is to
provide an error HTML page that visitors will see on an error or authorization failure. The `statusFile` configuration
provides that type of functionality.

```
install(StatusPages) {
    statusFile(HttpStatusCode.NotFound, HttpStatusCode.Unauthorized, filePattern = "error#.html")
}
```

This will resolve two resources from the classpath.

* On a `404`, it will return `error404.html`.
* On a `401`, it will return `error401.html`.

The `statusFile` configuration replaces any `#` character with the value of the status code within the list of
configured statuses.

## Redirections using StatusPages

When doing redirections by executing `call.respondRedirect("/moved/here", permanent = true)`, the rest of the callee
function is executed. So when doing redirections inside guard clauses, you have to return the function.

```
routing {
    get("/") {
        if (condition) {
            return@get call.respondRedirect("/invalid", permanent = false)
        }
        call.respondText("Normal response")
    }
}
```

Other frameworks, use exceptions on redirect, so the normal flow is broken and you can execute redirections in guard
clauses or subfunctions without having to worry about returning in all the subfunction chain. You can use the
StatusPages feature to simulate this:

```
fun Application.module() {
    install(StatusPages) {
        exception<HttpRedirectException> { e ->
            call.respondRedirect(e.location, permanent = e.permanent)
        }
    }
    routing {
        get("/") {
            if (condition) {
                redirect("/invalid", permanent = false)
            }
            call.respondText("Normal response")
        }
    }
}

class HttpRedirectException(val location: String, val permanent: Boolean = false) : RuntimeException()
fun redirect(location: String, permanent: Boolean = false): Nothing = throw HttpRedirectException(location, permanent)
```

### Options

* `exceptions` - Configures response based on mapped exception classes
* `status` - Configures response to status code value
* `statusFile` - Configures standard file response from classpath()

## Thymeleaf Documentation ([JetBrains](https://www.jetbrains.com))

Serve HTML content using Thymeleaf template engine

### Description

Ktor includes support for `Thymeleaf` templates through the Thymeleaf feature.

### Usage

Initialize the `Thymeleaf` feature with a `ClassLoaderTemplateResolver`:

```
install(Thymeleaf) {
    setTemplateResolver(ClassLoaderTemplateResolver().apply {
        prefix = "templates/"
        suffix = ".html"
        characterEncoding = "utf-8"
    })
}
```

This TemplateResolver sets up Thymeleaf to look for the template files on the classpath in the "templates" package,
relative to the current class path. A basic template looks like this:

```
<!DOCTYPE html >
<html xmlns:th="http://www.thymeleaf.org">
<body>
<h2 th:text="'Hello ' + ${user.name} + '!'"></h2>
<p>Your email address is <span th:text="${user.email}"></span></p>
</body>
</html>
```

With that template in `resources/templates` it is accessible elsewhere in the the application using the `call.respond()`
method:

```
data class User(val name: String, val email: String)

get("/") {
    val user = User("user name", "user@example.com")
    call.respond(ThymeleafContent("hello", mapOf("user" to user)))
    }
```

### Options

* `setTemplateResolver`()

# Reporting Issues / Support

Please use [our issue tracker](https://youtrack.jetbrains.com/issues/KTOR) for filing feature requests and bugs. If
you'd like to ask a question, we recommmend [StackOverflow](https://stackoverflow.com/questions/tagged/ktor) where
members of the team monitor frequently.

There is also community support on the [Kotlin Slack Ktor channel](https://app.slack.com/client/T09229ZC6/C0A974TJ9)

# Reporting Security Vulnerabilities

If you find a security vulnerability in Ktor, we kindly request that you reach out to the JetBrains security team via
our [responsible disclosure process](https://www.jetbrains.com/legal/terms/responsible-disclosure.html).

# Contributing

Please see [the contribution guide](CONTRIBUTING.md) and the [Code of conduct](CODE_OF_CONDUCT.md) before contributing.

TODO: contribution of features guide (link)