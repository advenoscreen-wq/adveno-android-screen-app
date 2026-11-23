package com.adveno.tv.core.network

import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Centralized classifier for network transport and connectivity exceptions.
 * Identifies whether an error is caused by device or transport-level network loss,
 * preventing technical DNS messages, socket traces, or hostnames from leaking to the UI.
 */
object NetworkExceptionClassifier {

    const val USER_FRIENDLY_NETWORK_ERROR = "Unable to connect to Adveno server. Please check your internet connection."

    /**
     * Inspects a [Throwable] hierarchy to determine if the root cause is a network transport failure.
     */
    fun isNetworkException(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 10) {
            when (current) {
                is UnknownHostException,
                is ConnectException,
                is NoRouteToHostException,
                is PortUnreachableException,
                is SocketTimeoutException,
                is SocketException,
                is SSLException -> return true
                is IOException -> {
                    val msg = current.message?.lowercase() ?: ""
                    if (msg.contains("unable to resolve host") ||
                        msg.contains("no address associated with hostname") ||
                        msg.contains("failed to connect") ||
                        msg.contains("connection refused") ||
                        msg.contains("connection reset") ||
                        msg.contains("broken pipe") ||
                        msg.contains("network is unreachable") ||
                        msg.contains("network unreachable") ||
                        msg.contains("software caused connection abort") ||
                        msg.contains("timeout") ||
                        msg.contains("timed out")
                    ) {
                        return true
                    }
                }
            }
            current = current.cause
            depth++
        }
        return false
    }
}
