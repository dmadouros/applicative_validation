package com.example.demo

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import java.time.LocalDate
import javax.validation.Validator
import javax.validation.constraints.Email
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern

// Inspired by https://fsharpforfunandprofit.com/posts/elevated-world-3/#validation

// data class CustomerId(val value: Int) {
//    companion object {
//        fun create(id: Int): Result<CustomerId, List<String>> {
//            return if (id > 0) Ok(CustomerId(id)) else Err(listOf("CustomerId must be positive"))
//        }
//    }
// }
//
// data class EmailAddress(val value: String) {
//    companion object {
//        fun create(str: String): Result<EmailAddress, List<String>> {
//            return if (str.isNullOrBlank()) {
//                Err(listOf("Email must not be empty"))
//            } else if (str.contains('@')) {
//                Ok(EmailAddress(str))
//            } else {
//                Err(listOf("Email must contain @-sign"))
//            }
//        }
//    }
// }
//
// data class CustomerInfo(val id: CustomerId, val email: EmailAddress) {
//    companion object {
//        // We don't get currying, so we have to use closures :(
//        // This is gonna suck once we get to more than 2 args. Can we get currying please?
//        fun create() = { customerId: CustomerId ->
//            { emailAddress: EmailAddress ->
//                CustomerInfo(customerId, emailAddress)
//            }
//        }
//    }
// }
//
// // Look ma! I can add an apply as an extension function on [Result]
// // Note: Kotlin already has an `apply` method. This might be confusing.
// private infix fun <V, U, E> Result<(U) -> V, List<E>>.apply(
//    result: Result<U, List<E>>
// ): Result<V, List<E>> {
//    return when {
//        (this is Ok && result is Ok) -> Ok(this.value(result.value))
//        (this is Ok && result is Err) -> result
//        (this is Err && result is Ok) -> this
//        (this is Err && result is Err) -> Err(this.error + result.error)
//        else -> throw RuntimeException()
//    }
// }
//
// fun createCustomerResultA(id: Int, email: String): Result<CustomerInfo, List<String>> {
//    val idResult = CustomerId.create(id)
//    val emailResult = EmailAddress.create(email)
//
//    // This works too (out of the box; w/o extension function on a function)
//    return idResult
//        .map(CustomerInfo.create())
//        .apply(emailResult)
// }

// Dto
data class CustomerDto(
    @field:Min(1)
    val id: Int,
    @field:Email
    val email: String,
    @field:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    val establishedDate: String = "2021-11-23"
)

// Domain
data class CustomerInfo(
    @Min(1)
    val id: Int,
    @Email
    val email: String,
    val establishedDate: LocalDate
)

@Service
class CustomerService(
    val validator: Validator
) {
    fun createCustomerResultA(customerDto: CustomerDto): Result<CustomerInfo, List<String>> {
        return Ok(CustomerInfo(customerDto.id, customerDto.email, establishedDate = LocalDate.parse(customerDto.establishedDate)))

//        val violations = validator.validate(customerDto)
//        return if (violations.isNotEmpty()) {
//            Err(violations.map { "${it.propertyPath}: ${it.message}" })
//        } else {
//            Ok(CustomerInfo(customerDto.id, customerDto.email, establishedDate = LocalDate.parse(customerDto.establishedDate)))
//        }
    }
}

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    val context: ConfigurableApplicationContext = runApplication<DemoApplication>(*args)
    val customerService = context.getBean<CustomerService>()

    println("************************************************************************************************************************************")
    println("* CHECK THIS OUT")
    println("************************************************************************************************************************************")
    println("* Bad (id not positive; email blank) ${customerService.createCustomerResultA(CustomerDto(0, "    "))}")
    println("* Bad (email is blank).............. ${customerService.createCustomerResultA(CustomerDto(1, "    "))}")
    println("* Bad (email missing '@')........... ${customerService.createCustomerResultA(CustomerDto(1, "bob"))}")
    println("* Good.............................. ${customerService.createCustomerResultA(CustomerDto(1, "bob@example.com", establishedDate = "0000-02-30"))}")
    println("* Bad (id not positive)............. ${customerService.createCustomerResultA(CustomerDto(0, "bob@example.com"))}")
    println("************************************************************************************************************************************")
}
