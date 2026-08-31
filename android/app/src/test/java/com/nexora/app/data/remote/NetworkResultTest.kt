package com.nexora.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `success result holds data and maps correctly`() {
        val result: NetworkResult<Int> = NetworkResult.Success(10)

        assertTrue(result is NetworkResult.Success)
        assertEquals(10, result.getOrNull())
        assertEquals(10, result.getOrElse(0))

        val mapped = result.map { it * 2 }
        assertEquals(20, (mapped as NetworkResult.Success).data)
    }

    @Test
    fun `error result holds error and handles fallbacks`() {
        val errorModel = NetworkError.HttpError(404, "Not Found")
        val result: NetworkResult<String> = NetworkResult.Error(errorModel)

        assertTrue(result is NetworkResult.Error)
        assertNull(result.getOrNull())
        assertEquals("default", result.getOrElse("default"))

        val mapped = result.map { "transformed" }
        assertTrue(mapped is NetworkResult.Error)
        assertEquals(404, ((mapped as NetworkResult.Error).error as NetworkError.HttpError).statusCode)
    }

    @Test
    fun `onSuccess and onError callbacks execute appropriately`() {
        var successCalled = false
        var errorCalled = false

        val successResult: NetworkResult<String> = NetworkResult.Success("OK")
        successResult.onSuccess { successCalled = true }
            .onError { errorCalled = true }

        assertTrue(successCalled)
        assertTrue(!errorCalled)

        successCalled = false
        val errorResult: NetworkResult<String> = NetworkResult.Error(NetworkError.UnknownError())
        errorResult.onSuccess { successCalled = true }
            .onError { errorCalled = true }

        assertTrue(!successCalled)
        assertTrue(errorCalled)
    }
}
