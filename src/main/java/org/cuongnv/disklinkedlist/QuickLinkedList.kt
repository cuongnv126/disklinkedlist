package org.cuongnv.disklinkedlist

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap

/**
 * Created by cuongnv on Jul 27, 2021
 *
 * Using map indexing to quick link or access [Node].
 * A base of version modify linked list.
 */

abstract class QuickLinkedList<T> {
    protected val map: ConcurrentSkipListMap<Int, Node<T>> = ConcurrentSkipListMap()

    /**
     * Require T must be a data class or implement hashCode and equals function.
     */
    private val quickIndex: ConcurrentHashMap<T, Node<T>> = ConcurrentHashMap()

    var first: Node<T>? = null
        protected set

    var last: Node<T>? = null
        protected set

    @Transient
    var size: Int = 0
        private set

    val isEmpty get() = size == 0

    // For access from inline function
    val pMap: Map<Int, Node<T>> get() = map

    protected fun linkFirst(point: Int, item: T): Node<T> {
        val f = first
        val newNode = Node(
            self = point,
            _prev = null,
            _next = f,
            item = item
        )

        first = newNode
        if (f == null) last = newNode else f.prev = newNode

        size++
        map[newNode.self] = newNode
        quickIndex[item] = newNode

        return newNode
    }

    protected fun unlinkLast(l: Node<T>): Node<T> {
        val original = l.copy()

        val prev = l.prev
        l.item = null
        l.prev = null
        last = prev
        if (prev == null) first = null else prev.next = null

        map.remove(l.self)
        quickIndex.remove(original.item)

        size--

        return original
    }

    protected fun unlink(x: Node<T>): Node<T> {
        val original = x.copy()

        val next = x.next
        val prev = x.prev

        if (prev == null) {
            first = next
        } else {
            prev.next = next
            x.prev = null
        }

        if (next == null) {
            last = prev
        } else {
            next.prev = prev
            x.next = null
        }

        x.item = null

        map.remove(x.self)
        quickIndex.remove(original.item)

        size--

        return original
    }

    fun clear() {
        size = 0
        first = null
        last = null
        map.clear()
        quickIndex.clear()
    }

    fun quickGet(item: T): Node<T>? {
        if (item == null) throw IllegalArgumentException("Item must not be null")
        return quickIndex[item]
    }

    /**
     * Loop on [map] with ordered pointer in disk.
     */
    inline fun forEachOrdered(block: (node: Node<T>) -> Boolean) {
        pMap.onEach { if (block(it.value)) return }
    }

    /**
     * Loop base on linked list, first for recent added.
     */
    inline fun forEach(block: (node: Node<T>) -> Boolean) {
        if (first == null) return

        var cursor = first!!
        while (true) {
            val willBreak = block(cursor)
            if (willBreak) break
            val next = cursor.next
            if (next == null) break
            else cursor = next
        }
    }
}

