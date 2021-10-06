package org.cuongnv.disklinkedlist

import java.io.File
import org.cuongnv.consoleformatter.ConsoleColors
import org.cuongnv.consoleformatter.ConsoleFormat.color

/**
 * Created by cuongnv on Jul 27, 2021
 *
 * Simple implement support allocate and free `zone` and make it's as a LinkedList.
 * Something called `zone` is a continuous byte array in a one file.
 *
 * For example:
 * We will save [Data] to disk as a byte array like this:
 *
 * `1 byte (B) to make use or not | 4B prev | 4B next | 4B length | extra byte array`
 *
 * To make a `zone` to free, we set all byte to `0` (`zero`).
 */

class DiskLinkedList private constructor(
    private val accessor: FileAccessor
) : QuickLinkedList<Data>() {
    companion object {
        fun open(
            file: File,
            initialByteSize: Long,
            scaleFactor: Float
        ): DiskLinkedList {
            val allocator = FileAccessor(file, initialByteSize, scaleFactor)
            return DiskLinkedList(allocator)
        }
    }

    init {
        pull()
    }

    /**
     * Pull from disk.
     * No effect to disk.
     */
    private fun pull() {
        var diskPackages: HashMap<Int, DiskPackage>? = null
        var dpLast: DiskPackage? = null

        // Move pointer to begin of file.
        accessor.seek(0)
        val length = accessor.length()

        while (true) {
            val nonZeroPoint = accessor.skipZero()
            if (nonZeroPoint < 0 || nonZeroPoint >= length) break

            if (diskPackages == null) diskPackages = HashMap()

            accessor.seek(nonZeroPoint)
            val currentDiskPackage = DiskPackage.read(accessor)
            diskPackages[currentDiskPackage.point] = currentDiskPackage
            if (currentDiskPackage.isLast) dpLast = currentDiskPackage
        }

        clear()

        if (diskPackages.isNullOrEmpty()) return

        if (dpLast == null) dpLast = diskPackages.values.first()
        linkFirst(dpLast.point, Data(dpLast.dataByteArray))

        var cursor = dpLast
        var hasError = false
        val rawSize = diskPackages.size

        val previousSet = HashSet<Int>()
        previousSet.add(cursor.point)
        while (true) {
            cursor = diskPackages[cursor!!.prevPoint] ?: break

            if (previousSet.contains(cursor.point)) {
                hasError = true
                break
            }
            previousSet.add(cursor.point)

            linkFirst(cursor.point, Data(cursor.dataByteArray))
        }

        println("buildSize= $size, rawSize= $rawSize, hasError= $hasError")
    }

    private fun addFirstInternal(point: Int, item: Data) {
        linkFirst(point, item).also { it.toDiskPackage().write(accessor) }
    }

    /**
     * Add node before first node.
     */
    fun addFirst(item: Data) {
        val allocateSize = item.allocateSize()

        if (allocateSize > DiskPackage.MAX_SIZE) {
            throw IllegalStateException("Cannot allocate with size= $allocateSize, maxSize= ${DiskPackage.MAX_SIZE}")
        }

        if (first == null) {
            addFirstInternal(0, item)
        } else {
            var beginAvailablePoint = 0
            var foundEmptyZone = false

            // Find current free zone in linked list.
            // If found -> create new disk pointer
            forEachOrdered { currentNode ->
                if (currentNode.self - beginAvailablePoint >= allocateSize) {
                    addFirstInternal(beginAvailablePoint, item)

                    foundEmptyZone = true
                    return@forEachOrdered true
                } else {
                    beginAvailablePoint = currentNode.self + currentNode.item!!.allocateSize()
                    return@forEachOrdered false
                }

            }

            // Not found empty zone
            if (!foundEmptyZone) {
                addFirstInternal(map.values.last().nextPoint(), item)
            }
        }
    }

    /**
     * Remove last inserted in disk.
     */
    fun removeLast(): Data {
        val l = last ?: throw NoSuchElementException("Not found last")
        val oldData = l.item!!
        unlinkLast(l).also { it.toDiskPackage().erase(accessor) }
        return oldData
    }

    fun remove(l: Node<Data>) {
        unlinkLast(l).also { it.toDiskPackage().erase(accessor) }
    }

    /**
     * Move node to first in disk (use in case of get existed node)
     * Re-use current [DiskPackage] in disk.
     */
    fun moveToFirst(node: Node<Data>) {
        val unlinkedNode = unlink(node)
        linkFirst(unlinkedNode.self, unlinkedNode.item!!)
    }

    fun nodeOf(data: Data): Node<Data>? {
        return quickGet(data)
    }

    /**
     * When allocate / free executed, data array in file will fragment base on size of new item inserted.
     * It will make file on disk larger than reality.
     *
     * So to avoid that, we re-arrange data in file.
     */
    fun defragment() {
        synchronize()

        val tempAccessor = FileAccessor.makeTemp(accessor)

        // Re-arrange data based on current LinkedList.
        // Write to temp file.
        var point = 0
        var prev: Node<Data>? = null
        forEach {
            val node = Node(
                self = point,
                _prev = prev,
                _next = null, // update later
                item = it.item
            )

            if (prev != null) {
                prev!!.next = node
                prev!!.toDiskPackage().write(tempAccessor)
            }

            prev = node
            point += node.item!!.allocateSize()

            return@forEach false
        }

        prev?.toDiskPackage()?.write(tempAccessor)

        // Rename file
        tempAccessor.file.renameTo(accessor.file, false)

        // Reset and pull
        accessor.reset()
        pull()
    }

    @Synchronized
    fun synchronize() {
        map.values.forEach {
            if (it.modify > 0) {
                it.toDiskPackage().write(accessor)
                it.modify = 0
            }
        }
        accessor.fd.sync()
    }

    fun log() {
        println(buildString {
            super.forEach {
                if (isNotEmpty()) append(" > ")

                append("${it.self}=${String(it.item!!.value)}".color(ConsoleColors.GREEN_BACKGROUND_BRIGHT))
                return@forEach false
            }
        })
    }
}