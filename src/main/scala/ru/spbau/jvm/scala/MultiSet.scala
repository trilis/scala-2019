package ru.spbau.jvm.scala

import scala.util.Random

class MultiSet[T](elements: T*)(implicit ordering: Ordering[T]) {

  private var root: OptionalNode = Nil

  private var size = 0

  elements.foreach(add(_))

  def contains(k: T): Boolean = find(root, k) != Nil

  def count(k: T): Int = find(root, k).count()

  def apply(k: T): Int = count(k)

  def add(k: T, count: Int = 1): Unit = {
    size += 1
    val found = find(root, k)
    found match {
      case Nil => root = insert(root, Node(x = k, count = count))
      case Node(_, _, x, _, currentCount) =>
        root = insert(delete(root, x), Node(x = k, count = currentCount + count))
    }
  }

  def remove(k: T): Unit = {
    val found = find(root, k)
    found match {
      case Node(_, _, x, _, count) if count > 1 =>
        size -= 1
        root = insert(delete(root, x), Node(x = k, count = count - 1))
      case Node(_, _, x, _, _) =>
        size -= 1
        root = delete(root, x)
    }
  }

  def clear(): Unit = {
    root = Nil
    size = 0
  }

  def foreach(f: T => Unit): Unit = {
    root.foreach(f)
  }

  def map[S](f: T => S)(implicit ordering: Ordering[S]): MultiSet[S] = {
    val newMultiSet = new MultiSet[S]()
    for (x <- this) {
      newMultiSet.add(f(x))
    }
    newMultiSet
  }

  def withFilter(f: T => Boolean): MultiSet[T] = filter(f)

  def filter(f: T => Boolean): MultiSet[T] = {
    val newMultiSet = new MultiSet[T]()
    for (x <- this) {
      if (f(x)) {
        newMultiSet.add(x)
      }
    }
    newMultiSet
  }

  def |(other: MultiSet[T]): MultiSet[T] = {
    val newMultiSet = new MultiSet[T]()
    for (x <- this) {
      newMultiSet.add(x)
    }
    for (x <- other) {
      newMultiSet.add(x)
    }
    newMultiSet
  }

  def &(other: MultiSet[T]): MultiSet[T] = {
    val newMultiSet = new MultiSet[T]()
    for (x <- this) {
      if (other.contains(x)) {
        newMultiSet.add(x)
      }
    }
    for (x <- other) {
      if (contains(x)) {
        newMultiSet.add(x)
      }
    }
    newMultiSet
  }

  override def toString: String = s"[${root.childrenRepresentation()}]"

  override def equals(obj: Any): Boolean = {
    obj match {
      case other: MultiSet[T] =>
        if (size != other.size) {
          return false
        }
        for (x <- this) {
          if (other(x) != apply(x)) {
            return false
          }
        }
        true
      case _ => false
    }
  }

  def getSize: Int = size

  private def split(node: OptionalNode, k: T): (OptionalNode, OptionalNode) = node match {
    case Nil => (Nil, Nil)
    case Node(left, right, x, y, count) if ordering.lt(x, k) =>
      val (leftHalf, rightHalf) = split(right, k)
      (Node(left, leftHalf, x, y, count), rightHalf)
    case Node(left, right, x, y, count) =>
      val (leftHalf, rightHalf) = split(left, k)
      (leftHalf, Node(rightHalf, right, x, y, count))
  }

  private def merge(node1: OptionalNode, node2: OptionalNode): OptionalNode = node1 match {
    case Nil => node2
    case Node(left1, right1, x1, y1, count1) => node2 match {
      case Nil => node1
      case Node(_, _, _, y2, _) if y1 > y2 => Node(left1, merge(right1, node2), x1, y1, count1)
      case Node(left2, right2, x2, y2, count2) => Node(merge(node1, left2), right2, x2, y2, count2)
    }
  }

  @scala.annotation.tailrec
  private def find(node: OptionalNode, k: T): OptionalNode = node match {
    case Nil => Nil
    case Node(left, _, x, _, _) if ordering.lt(k, x) => find(left, k)
    case Node(_, right, x, _, _) if ordering.gt(k, x) => find(right, k)
    case Node(_, _, _, _, _) => node
  }

  private def delete(node: OptionalNode, k: T): OptionalNode = node match {
    case Nil => Nil
    case Node(left, right, x, _, _) if ordering.equiv(k, x) => merge(left, right)
    case Node(left, right, x, y, count) if ordering.lt(k, x) =>
      val newLeft = delete(left, k)
      Node(newLeft, right, x, y, count)
    case Node(left, right, x, y, count) if ordering.gt(k, x) =>
      val newRight = delete(right, k)
      Node(left, newRight, x, y, count)
  }

  private def insert(currentNode: OptionalNode, newNode: Node): OptionalNode = {
    currentNode match {
      case Nil => newNode
      case Node(_, _, _, y, _) if y < newNode.y =>
        val (leftHalf, rightHalf) = split(currentNode, newNode.x)
        Node(leftHalf, rightHalf, newNode.x, newNode.y, newNode.count)
      case Node(left, right, x, y, count) if ordering.lt(newNode.x, x) =>
        Node(insert(left, newNode), right, x, y, count)
      case Node(left, right, x, y, count) =>
        Node(left, insert(right, newNode), x, y, count)
    }
  }

  private sealed abstract class OptionalNode {
    def foreach(f: T => Unit): Unit

    def childrenRepresentation(): String

    def count(): Int
  }

  private case object Nil extends OptionalNode {
    override def foreach(f: T => Unit): Unit = {}

    override def childrenRepresentation(): String = ""

    override def count(): Int = 0
  }

  private final case class Node(left: OptionalNode = Nil,
                                right: OptionalNode = Nil,
                                x: T,
                                y: Int = Random.nextInt(),
                                count: Int
                               ) extends OptionalNode {

    override def foreach(f: T => Unit): Unit = {
      left.foreach(f)
      for (_ <- 1 to count) {
        f(x)
      }
      right.foreach(f)
    }

    override def childrenRepresentation(): String = {
      var leftSide = left.childrenRepresentation()
      if (leftSide.length > 1) {
        leftSide += ", "
      }
      var rightSide = right.childrenRepresentation()
      if (rightSide.length > 1) {
        rightSide = s", $rightSide"
      }
      leftSide + s"$x -> $count" + rightSide
    }

  }

}
