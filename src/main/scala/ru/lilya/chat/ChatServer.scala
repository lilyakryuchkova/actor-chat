package ru.lilya.chat

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import ru.lilya.chat.server.ChatService

import scala.io.StdIn

object ChatServer extends App {

  override def main (args: Array[String]): Unit = {

      val system = ActorSystem("Sys", ConfigFactory.load("application"))
      try {
        val chatService = system.actorOf(Props(new ChatService), "chatService")

        def work(line: String): Unit = line match {
          case "down" =>
            chatService ! Down
          case "lst" =>
            chatService ! UserList
            work(StdIn readLine)
          case _ =>
            work(StdIn readLine)
        }

        work (StdIn readLine)

      } finally {
        system terminate
      }
  }
}