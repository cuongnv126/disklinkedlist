package org.cuongnv.disklinkedlist

import java.io.File
import java.io.IOException

/**
 * Created by cuongnv on Jul 27, 2021
 */

fun Node<Data>.toDiskPackage(): DiskPackage {
    return DiskPackage(
        point = this.self,
        isUse = true,
        prevPoint = this.prev?.self ?: -1,
        nextPoint = this.next?.self ?: -1,
        dataLength = this.item!!.value.size,
        dataByteArray = this.item!!.value
    )
}

fun Data.allocateSize(): Int {
    return DiskPackage.HEAD_SIZE + this.value.size
}

fun Node<Data>.nextPoint(): Int {
    return this.self + item!!.allocateSize()
}

/**
 * When we have range of data in file created by [FileAccessor],
 *
 * Because we always add [DiskPackage] data to pointer nearest with begin of file,
 *
 * So we have 2 cases to skip:
 * - We found byte with value is 0x01 (it's mean begin of [DiskPackage])
 * - We try to seek [DiskPackage.MAX_SIZE], if we couldn't found some begin of
 * [DiskPackage], we will assume that is end of file.
 */
fun FileAccessor.skipZero(): Long {
    val length = length()

    var nonZeroPoint = filePointer
    var haveNonZeroPoint = false

    var seekCount = 0

    while (nonZeroPoint < length) {
        val byte = read()
        if (byte == 0x01) {
            haveNonZeroPoint = true
            break
        }

        seekCount++
        if (seekCount > DiskPackage.MAX_SIZE) {
            break
        }

        nonZeroPoint++
    }

    return if (haveNonZeroPoint) nonZeroPoint else -1
}

fun File.deleteIfExists() {
    if (exists() && !delete()) {
        throw IOException()
    }
}

fun File.renameTo(newFile: File, deleteOld: Boolean) {
    if (deleteOld) deleteIfExists()
    if (!renameTo(newFile)) {
        throw IOException()
    }
}

fun File.size(): Long {
    return if (exists()) length() else 0
}