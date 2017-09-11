package ru.lilya.chat

sealed trait Event

case class Login(user: String) extends Event
case object Logged extends Event
case class Logged(user: String) extends Event

case class Logout(user: String) extends Event
case class LoggedOut(user: String) extends Event

case object GetInitialLog extends Event
case class ChatLog(log: List[String]) extends Event

case class ChatMessage(from: String, message: String) extends Event
case class NewMessage(from: String, message: String) extends Event

/*
* Inner message for server
* */
case object Down extends Event
case object UserList extends Event
