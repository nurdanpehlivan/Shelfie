package com.shelfie.app.domain.repository

import com.shelfie.app.domain.model.ProductInfo

interface ProductLookupRepository {

    /**
     * Looks up a packaged product by its barcode. Success with `null` means the product is not
     * in the database; a failure carries a [com.shelfie.app.domain.model.ProductLookupException].
     */
    suspend fun lookup(barcode: String): Result<ProductInfo?>
}
