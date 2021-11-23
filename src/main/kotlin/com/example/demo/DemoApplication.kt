package com.example.demo

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

// Inspired by https://fsharpforfunandprofit.com/posts/elevated-world-3/#validation

data class CustomerId(val value: Int) {
    companion object {
        fun create(id: Int): Result<CustomerId, List<String>> {
            return if (id > 0) Ok(CustomerId(id)) else Err(listOf("CustomerId must be positive"))
        }
    }
}

data class EmailAddress(val value: String) {
    companion object {
        fun create(str: String): Result<EmailAddress, List<String>> {
            return if (str.isNullOrBlank()) {
                Err(listOf("Email must not be empty"))
            } else if (str.contains('@')) {
                Ok(EmailAddress(str))
            } else {
                Err(listOf("Email must contain @-sign"))
            }
        }
    }
}

data class CustomerInfo(val id: CustomerId, val email: EmailAddress)

// Look ma! I can add an apply as an extension function on [Result]
private infix fun <V, U, E> Result<(U) -> V, List<E>>.apply(
    result: Result<U, List<E>>
): Result<V, List<E>> {
    return when {
        (this is Ok && result is Ok) -> Ok(this.value(result.value))
        (this is Err && result is Ok) -> this
        (this is Ok && result is Err) -> result
        (this is Err && result is Err) -> Err(this.error + result.error)
        else -> throw RuntimeException()
    }
}

// With this I can inline `map` the way I wish it was
private infix fun <A, B, E> ((A) -> B).map(idResult: Result<A, List<E>>): Result<B, List<E>> {
    return idResult.map { this(it) }
}

fun createCustomerResultA(id: Int, email: String): Result<CustomerInfo, List<String>> {
    val idResult = CustomerId.create(id)
    val emailResult = EmailAddress.create(email)

    // We don't get currying, so we have to use closures :(
    // This is gonna suck once we get to more than 2 args. Can we get currying please?
    val createCustomerInfo = { customerId: CustomerId ->
        { emailAddress: EmailAddress ->
            CustomerInfo(customerId, emailAddress)
        }
    }

    // But this line is pretty slick!
    return createCustomerInfo map idResult apply emailResult

    // This works too (out of the box; w/o extension function on a function)
    // return idResult map createCustomerInfo apply emailResult
}

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    println("************************************************************************************************************************************")
    println("* CHECK THIS OUT")
    println("************************************************************************************************************************************")
    println("* Bad (id not positive; email blank) ${createCustomerResultA(0, "    ")}")
    println("* Bad (email is blank).............. ${createCustomerResultA(1, "    ")}")
    println("* Bad (email missing '@')........... ${createCustomerResultA(1, "bob")}")
    println("* Good.............................. ${createCustomerResultA(1, "bob@example.com")}")
    println("* Bad (id not positive)............. ${createCustomerResultA(0, "bob@example.com")}")
    println("************************************************************************************************************************************")
    runApplication<DemoApplication>(*args)
}
