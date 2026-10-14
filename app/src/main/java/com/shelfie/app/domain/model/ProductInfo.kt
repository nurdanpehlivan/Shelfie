package com.shelfie.app.domain.model

/** What a barcode lookup knows about a packaged product. */
data class ProductInfo(
    val barcode: String,
    val name: String,
    val category: Category,
)

/** Failures of a barcode lookup; "not found" is a successful `null`, not an error. */
sealed class ProductLookupException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    class Network(cause: Throwable) : ProductLookupException("Network error", cause)

    class Service(cause: Throwable) : ProductLookupException("Product database error", cause)
}
