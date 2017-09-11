package ru.lilya.chat

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import ru.lilya.chat.client.UserSession

import scala.io.StdIn

object ChatClient extends App {

  override def main (args: Array[String]): Unit = {

    val system = ActorSystem("Sys", ConfigFactory.load("client"))
    try {
      println("login:")
      val name = StdIn readLine
      val client = system actorOf(Props(new UserSession(name)), "client-" + name)

      def work(line: String): Unit = line match {
        case "logout" =>
          client ! Logout(name)
        case _ =>
          client ! ChatMessage(name, name + ": " + line)
          work(StdIn readLine)
      }

      work (StdIn readLine)

    } finally {
      system terminate
    }
  }
}