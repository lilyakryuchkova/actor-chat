package ru.lilya.chat.server

import akka.actor.{Actor, ActorLogging, Props}
import ru.lilya.chat.{ChatLog, ChatMessage, GetInitialLog}

/**
  * Abstraction of chat storage holding the chat log.
  */
trait ChatStorage extends Actor


/**
  * Memory-backed chat storage implementation.
  */
class MemoryChatStorage extends ChatStorage with ActorLogging {
  //self.lifeCycle = Permanent

  private var chatLog = Vector[Array[Byte]]()

  override def preStart(): Unit = log.info("Memory-based chat storage is starting up...")

  def receive = {
    case msg @ ChatMessage(from, message) =>
      log.debug("New chat message {}", message)
      chatLog = chatLog :+ (message getBytes("UTF-8"))

    case GetInitialLog =>
      val messageList = (chatLog take 5).map (bytes => new String(bytes, "UTF-8")).toList
      sender() ! ChatLog(messageList)
  }

  override def postRestart(reason: Throwable) = chatLog = Vector()
}


/**
  * Creates and links a MemoryChatStorage.
  */
trait MemoryChatStorageFactory { this: Actor =>
  val storage = context.actorOf(Props(new MemoryChatStorage()), "in-memory-storage")
    //this.self.spawnLink[MemoryChatStorage] // starts and links ChatStorage
}