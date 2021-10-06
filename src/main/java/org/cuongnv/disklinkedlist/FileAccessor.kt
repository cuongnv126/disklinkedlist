package org.cuongnv.disklinkedlist

import java.io.Closeable
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.io.FileDescriptor
import java.io.RandomAccessFile

/**
 * Created by cuongnv on Jul 27, 2021
 */

class FileAccessor(
    val file: File,
    private val initialByteSize: Long,
    private val scaleFactor: Float,
) : DataOutput, DataInput, Closeable {

    companion object {
        fun makeTemp(original: FileAccessor): FileAccessor {
            val tempFile = File("${original.file.absolutePath}.tmp")
            tempFile.deleteIfExists()
            return FileAccessor(
                file = tempFile,
                initialByteSize = original.initialByteSize,
                scaleFactor = original.scaleFactor
            )
        }
    }

    @Volatile
    private var _mmap: RandomAccessFile? = null
    private val mmap: RandomAccessFile
        get() {
            if (_mmap == null) {
                synchronized(this) {
                    if (_mmap == null) {
                        resetInternal()
                    }
                }
            }
            return _mmap!!
        }

    val filePointer: Long get() = mmap.filePointer
    val fd: FileDescriptor get() = mmap.fd

    init {
        resetInternal()
    }

    fun reset() {
        _mmap = null
    }

    private fun resetInternal() {
        _mmap = RandomAccessFile(file, "rw")
        if (!file.exists()) {
            file.createNewFile()
        }

        // Resize base file to fit for modifies
        val fileLength = length()
        val newAllocate = initialByteSize - fileLength
        if (newAllocate > 0) {
            setLength(initialByteSize)

            // Reset byte data
            seek(fileLength)
            for (i in 0 until newAllocate) {
                writeByte(0xFF)
            }
        }
    }

    @Synchronized
    fun setLength(newLength: Long) {
        mmap.setLength(newLength)
    }

    @Synchronized
    fun prepareSize(len: Int) {
        val point = filePointer
        val targetSize = point + len
        val currentSize = length()

        if (currentSize < targetSize) {
            setLength((targetSize + scaleFactor * initialByteSize).toLong())
        }
    }

    fun length() = mmap.length()
    fun seek(pos: Long) = mmap.seek(pos)

    override fun write(b: Int) = mmap.write(b)
    override fun write(b: ByteArray) = mmap.write(b)
    override fun write(b: ByteArray, off: Int, len: Int) = mmap.write(b, off, len)

    override fun writeBoolean(v: Boolean) = mmap.writeBoolean(v)
    override fun writeByte(v: Int) = mmap.writeByte(v)
    override fun writeShort(v: Int) = mmap.writeShort(v)
    override fun writeChar(v: Int) = mmap.writeChar(v)
    override fun writeInt(v: Int) = mmap.writeInt(v)
    override fun writeLong(v: Long) = mmap.writeLong(v)
    override fun writeFloat(v: Float) = mmap.writeFloat(v)
    override fun writeDouble(v: Double) = mmap.writeDouble(v)
    override fun writeBytes(s: String) = mmap.writeBytes(s)
    override fun writeChars(s: String) = mmap.writeChars(s)
    override fun writeUTF(s: String) = mmap.writeUTF(s)

    override fun readFully(b: ByteArray) = mmap.readFully(b)
    override fun readFully(b: ByteArray, off: Int, len: Int) = mmap.readFully(b, off, len)

    override fun skipBytes(n: Int) = mmap.skipBytes(n)

    fun read() = mmap.read()
    fun read(b: ByteArray) = mmap.read(b)
    override fun readBoolean() = mmap.readBoolean()
    override fun readByte() = mmap.readByte()
    override fun readUnsignedByte() = mmap.readUnsignedByte()
    override fun readShort() = mmap.readShort()
    override fun readUnsignedShort() = mmap.readUnsignedShort()
    override fun readChar() = mmap.readChar()
    override fun readInt() = mmap.readInt()
    override fun readLong() = mmap.readLong()
    override fun readFloat() = mmap.readFloat()
    override fun readDouble() = mmap.readDouble()
    override fun readLine() = mmap.readLine()
    override fun readUTF() = mmap.readUTF()
    override fun close() = mmap.close()
}