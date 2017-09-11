package ru.lilya.chat.server

import akka.actor
import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Terminated}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import ru.lilya.chat._

import scala.concurrent.duration._


/**
  * Chat server. Manages sessions and redirects all other messages to the Session for the client.
  */
trait ChatServer extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
       case _: Exception => Escalate
  }

  val storage: ActorRef
  var sessions = Map.empty[String, ActorRef]

  override def preStart(): Unit = log.info("Chat server is starting up...")

  // actor message handler
  def receive: Receive = sessionManagement orElse chatManagement orElse innerManagement orElse {
    case "ping" =>
      log.info("ping message has gotten")
      sender() ! ("pong", sender())
  }

  override def postStop() = {
    log.info("Chat server is shutting down...")
    context stop storage
  }

  protected def innerManagement: Receive = {
    case UserList =>
      println("Logged users:")
      (sessions keySet) foreach println
    case Down =>
      context stop self
  }

  protected def sessionManagement: Receive = {
    case Login(username) =>
      log.info("User {} has logged in", username)
      val session = sender()
      context watch session
      sender() ! Logged
      bulk(Logged(username))
      sessions += (username -> session)

    case Logout(username) =>
      log.info("User {} has logged out", username)
      sessions -= username
      bulk(LoggedOut(username))

    case Terminated(actor) =>
      sessions filter { case (_, a) => a == actor } foreach { case (id, _) =>
        log.info("User {} session has terminated", id)
        sessions -= id
        bulk(LoggedOut(id))
      }
  }

  protected def chatManagement: Receive = {
    case msg @ ChatMessage(from, text) =>
      storage forward msg
      bulk(NewMessage(from, text), { case (id, _) =>
        id != from
      })

    case msg @ GetInitialLog =>
      import context.dispatcher
      implicit val timeout = Timeout(5 seconds) // needed for `?` below

      pipe(storage ? msg) to sender()
  }

  private def bulk (msg: Any,
                    condition: ((String, ActorRef)) => Boolean
                    = { case (_,_) => true}) =
    (sessions filter condition) foreach { case (_, actor) => actor ! msg }

}

/**
  * Class encapsulating the full Chat Service.
  * Start service by invoking:
  */
class ChatService extends
  ChatServer with
  MemoryChatStorageFactory {

  override def preStart() = log.info("Chat service has started")

}