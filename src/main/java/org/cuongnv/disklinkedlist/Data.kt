package org.cuongnv.disklinkedlist

/**
 * Created by cuongnv on Jul 27, 2021
 */

data class Data(
    val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

fun String.toData(): Data {
    return Data(toByteArray())
}

fun Data.toKey(): String {
    return String(value)
}