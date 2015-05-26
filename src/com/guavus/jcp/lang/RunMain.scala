package com.guavus.jcp.lang

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random


/**
 * Created by kanwaljit.singh on 01/05/15.
 * Guavus (c) 2015
 */
object RunMain {

  val system = ActorSystem("HellFrozen666")
  var table: ActorRef = _

  val req_map = new mutable.LinkedHashMap[Int, Int]()

  def main(args: Array[String]): Unit = {
    table = system.actorOf(Props(classOf[Table]))
    val diners = new ArrayBuffer[ActorRef]
    for (i <- 0 to 9)
      diners append system.actorOf(Props(classOf[Diner], i))

    val random = new Random()
    for (i <- 1 to 30) {
      var j = random.nextInt() % 10
      if (j < 0)
        j = -j
      if (req_map.contains(j)) {
        req_map(j) = req_map(j) + 1
      }
      else req_map(j) = 1
      diners(j) ! EatCommand
    }

    Thread.sleep(10000)
    diners.foreach(_ ! Finish)
    println(s"Expected: ${req_map}")
  }
}

class Table extends Actor {
  val total_forks = 3
  var current_forks = 3
  var i = 0

  def isForkFree = current_forks > 0

  override def receive = {
    case FreeFork(id) =>
      println(s"Got fork back from $id")
      require(current_forks != total_forks)
      current_forks += 1
    case RequestFork(id) =>
      if (isForkFree) {
        synchronized {
          if (isForkFree) {
            println(s"Giving fork to diner $id")
            sender ! GotFork()
            require(current_forks != 0)
            current_forks -= 1
          }
          else sender ! NoFork
        }
      }
      else sender ! NoFork
  }
}


class Diner(val id: Int) extends Actor {

  var ate = 0

  override def receive: Receive = {

    case GotFork() =>
      println(s"Recieved fork for diner $id, EATING..")
      ate += 1
      sender ! FreeFork(id)
    case EatCommand =>
      println(s"$id requesting for fork")
      RunMain.table ! RequestFork(id)
    case NoFork =>
      RunMain.system.scheduler.scheduleOnce(FiniteDuration(100, "ms"), sender, RequestFork(id))
    case Finish =>
      println(s"I, diner $id, ate $ate times")
  }
}

case class FreeFork(id: Int)

case class GotFork()

case class RequestFork(id: Int)

case object EatCommand

case object NoFork

case object Finish