package org.cuongnv.disklinkedlist

/**
 * Created by cuongnv on Jul 27, 2021
 */

class DiskPackage internal constructor(
    val point: Int,
    val isUse: Boolean = false,     // 1 byte
    val prevPoint: Int = -1,        // 4 bytes
    val nextPoint: Int = -1,        // 4 bytes
    val dataLength: Int = 0,        // 4 bytes
    val dataByteArray: ByteArray    // dataLength bytes
) {
    companion object {
        const val HEAD_SIZE = 1 + 4 + 4 + 4
        const val MAX_SIZE = HEAD_SIZE + 1 * 1024

        fun read(accessor: FileAccessor): DiskPackage {
            val point = accessor.filePointer.toInt()
            val isUse = accessor.read() != 0
            val prevPoint = accessor.readInt()
            val nextPoint = accessor.readInt()
            val dataLength = accessor.readInt()

            val byteArray = ByteArray(dataLength)
            accessor.read(byteArray)

            return DiskPackage(
                point = point,
                isUse = isUse,
                prevPoint = prevPoint,
                nextPoint = nextPoint,
                dataLength = dataLength,
                dataByteArray = byteArray
            )
        }
    }

    val isLast get() = prevPoint >= 0 && nextPoint < 0

    fun write(accessor: FileAccessor) {
        accessor.seek(point.toLong())

        accessor.prepareSize(HEAD_SIZE + dataLength)

        accessor.writeByte(if (isUse) 0x01 else 0x00)
        accessor.writeInt(prevPoint)
        accessor.writeInt(nextPoint)
        accessor.writeInt(dataLength)
        accessor.write(dataByteArray)
    }

    fun erase(accessor: FileAccessor) {
        accessor.seek(point.toLong())
        for (i in 0 until HEAD_SIZE + dataLength) {
            accessor.write(0x00)
        }
    }
}
