package ru.lilya.chat.client

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, Props, ReceiveTimeout, Terminated}
import akka.pattern.ask
import akka.util.Timeout
import ru.lilya.chat._

import scala.concurrent.duration._

object UserSession {
  def props(name: String): Props = Props(new UserSession(name))
}

class UserSession (val name: String) extends Actor with ActorLogging{

  private val loginTime = System.currentTimeMillis

  override def preStart(): Unit =
    log.info("121New session for user {} has been created at {}", name, loginTime)

  val path = "akka.tcp://Sys@127.0.0.1:2552/user/chatService"
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    log.info("Trying to connect to chat...")
    context.actorSelection(path) ! Identify(path)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(3 seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(chat)) =>
      context.watch(chat)
      context.become(active(chat))
      chat ! Login(name)

    case ActorIdentity(`path`, None) =>
      log.info(s"Remote actor not available: $path")
    case ReceiveTimeout =>
      sendIdentifyRequest()
    case _ =>
      log.info("Not ready yet")
  }

  def active(chat: ActorRef): Actor.Receive =
    sessionManagement(chat) orElse chatLifecycle(chat) //orElse userInteraction

  private def chatLifecycle(chat: ActorRef): Receive = {
    case Terminated(`chat`) =>
      log.info("Chat terminated")
      context unwatch chat
      context unbecome()
      sendIdentifyRequest()

    case ReceiveTimeout =>
      log.info("Chat is not available: receive timeout")
  }

  private def sessionManagement(chat: ActorRef): Receive = {
    case msg @ Login(_)  =>
      chat ! msg
    case msg @ Logout(_)  =>
      chat ! msg

    case Logged =>
      println("You are logged!")
      implicit val timeout = Timeout(5 seconds) // needed for `?` below
      import context.dispatcher

      val f = (chat ? GetInitialLog).mapTo[ChatLog]
      f foreach(_.log foreach println)

    case Logged(username) =>
      println(s"User $username has logged in")

    case LoggedOut(username) =>
      println(s"User $username has logged out")

    case NewMessage(_, message) =>
      println(s"$message")

    case msg @ ChatMessage(_, _)  =>
      chat ! msg
  }

  //protected def userInteraction: Receive
}