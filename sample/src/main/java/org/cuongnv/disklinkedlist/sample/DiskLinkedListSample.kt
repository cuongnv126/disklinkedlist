package org.cuongnv.disklinkedlist.sample

import java.io.File
import org.cuongnv.disklinkedlist.DiskLinkedList
import org.cuongnv.disklinkedlist.toData

fun main() {
    println()
    println()
    println()

    val diskLinkedList = DiskLinkedList.open(
        file = File("data.dm"),
        initialByteSize = 20 * 1024L,
        scaleFactor = 0.75f
    )

    diskLinkedList.addFirst("Hello, World!".toData())
    diskLinkedList.log()

    // Add "Second Node" to first.
    diskLinkedList.addFirst("Second Node".toData())
    diskLinkedList.log()

    val second = diskLinkedList.first!!

    // Add 3 node to first
    repeat(3) { index ->
        diskLinkedList.addFirst("Hello $index".toData())
    }
    diskLinkedList.log()

    // Move "Second Node" to first.
    diskLinkedList.moveToFirst(second)
    diskLinkedList.log()

    // Remove last.
    diskLinkedList.removeLast()
    diskLinkedList.log()
    // Output: 26=Second Node > 90=Hello 2 > 70=Hello 1 > 50=Hello 0

    // Then continue to add 2 nodes at first.
    repeat(2) { index ->
        diskLinkedList.addFirst("Bye $index".toData())
    }
    diskLinkedList.log()
    // Output: 110=Bye 1 > 0=Bye 0 > 26=Second Node > 90=Hello 2 > 70=Hello 1 > 50=Hello 0
    // New node "Bye 1" will add at pointer 110 instead of 0 -> fragmented

    diskLinkedList.defragment()
    diskLinkedList.log()
    // Output: 0=Bye 1 > 18=Bye 0 > 36=Second Node > 60=Hello 2 > 80=Hello 1 > 100=Hello 0

    // Sync every change to disk.
    diskLinkedList.synchronize()

    println()
    println()
    println()
}