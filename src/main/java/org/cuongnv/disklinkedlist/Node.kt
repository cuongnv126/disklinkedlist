package org.cuongnv.disklinkedlist

/**
 * Created by cuongnv on Jul 27, 2021
 */

data class Node<T> constructor(
    // Pointer in disk
    val self: Int,

    // Reflection of prev and next in disk
    private var _prev: Node<T>? = null,
    private var _next: Node<T>? = null,

    // Data
    var item: T? = null,
) {
    var modify: Int = 0

    var prev: Node<T>?
        get() = _prev
        set(value) {
            _prev = value
            modify++
        }

    var next: Node<T>?
        get() = _next
        set(value) {
            _next = value
            modify++
        }

    override fun toString(): String {
        return "self = $self, next = ${next?.self}, prev = ${prev?.self}"
    }
}