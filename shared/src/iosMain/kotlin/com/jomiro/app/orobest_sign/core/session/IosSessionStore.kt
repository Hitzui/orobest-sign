@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jomiro.app.orobest_sign.core.session

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal class IosSessionStore : SessionStore {
    private fun <T> query(block: (CFMutableDictionaryRef) -> T): T {
        val dictionary = checkNotNull(CFDictionaryCreateMutable(null, 0, null, null))
        val service = CFBridgingRetain("com.jomiro.orobest-sign.session")
        val account = CFBridgingRetain("access-token")
        try {
            CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(dictionary, kSecAttrService, service)
            CFDictionaryAddValue(dictionary, kSecAttrAccount, account)
            return block(dictionary)
        } finally {
            CFRelease(dictionary)
            CFRelease(service)
            CFRelease(account)
        }
    }

    override suspend fun readToken(): String? = query { dictionary ->
        CFDictionaryAddValue(dictionary, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(dictionary, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(dictionary, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            check(status == errSecSuccess) { "No se pudo leer la sesión del Keychain ($status)" }
            val data = CFBridgingRelease(result.value) as NSData
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        }
    }

    override suspend fun writeToken(token: String): Unit = query { dictionary ->
        val data = checkNotNull((token as NSString).dataUsingEncoding(NSUTF8StringEncoding))
        val value = CFBridgingRetain(data)
        val attributes = checkNotNull(CFDictionaryCreateMutable(null, 0, null, null))
        try {
            CFDictionaryAddValue(attributes, kSecValueData, value)
            CFDictionaryAddValue(attributes, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
            var status = SecItemUpdate(dictionary, attributes)
            if (status == errSecItemNotFound) {
                CFDictionaryAddValue(dictionary, kSecValueData, value)
                CFDictionaryAddValue(dictionary, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                status = SecItemAdd(dictionary, null)
                // Una segunda instancia pudo crear el registro entre ambas llamadas.
                if (status == errSecDuplicateItem) {
                    query { cleanQuery -> status = SecItemUpdate(cleanQuery, attributes) }
                }
            }
            check(status == errSecSuccess) { "No se pudo guardar la sesión en Keychain ($status)" }
        } finally {
            CFRelease(attributes)
            CFRelease(value)
        }
    }

    override suspend fun clear(): Unit = query { dictionary ->
        val status = SecItemDelete(dictionary)
        check(status == errSecSuccess || status == errSecItemNotFound) { "No se pudo eliminar la sesión ($status)" }
    }
}
