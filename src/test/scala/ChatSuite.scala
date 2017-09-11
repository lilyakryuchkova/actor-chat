import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.lilya.chat._
import ru.lilya.chat.server.ChatService

@RunWith(classOf[JUnitRunner])
class ChatSuite extends TestKit(ActorSystem("chat-test", ConfigFactory.load("application")))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val chatActor = system.actorOf(Props(new ChatService), "chatService")

  val here = system.actorSelection("akka.tcp://chat-test@127.0.0.1:2552/user/chatService")

  val users = Vector("lilya", "tom", "martin")
  val messages = Vector("Hi all!", "hello", "how are you", "i'm fine, thank you")

  "A chat actor" must {

    "support remote look-ups" in {
      here ! "ping"
      expectMsg(("pong", testActor))
    }

    "login of first user" in {
      here ! Login(users(0))
      expectMsg(Logged)
    }

    "two users are login" in {
      val session1 = TestProbe()
      here tell (Login(users(0)), session1 ref)
      session1 expectMsg Logged

      val session2 = TestProbe()
      here tell (Login(users(1)), session2 ref)
      session2.expectMsg(Logged)
      session1.expectMsg(Logged(users(1)))
    }

    "two users are login and one is logout" in {
      val session1 = TestProbe()
      here tell (Login(users(0)), session1 ref)
      session1 expectMsg Logged

      val session2 = TestProbe()
      here tell (Login(users(1)), session2 ref)
      session2 expectMsg Logged
      session1 expectMsg Logged(users(1))

      here tell (Logout(users(1)), session2 ref)
      session1 expectMsg(LoggedOut(users(1)))
    }

    "two users are login and one is terminated" in {
      val session1 = TestProbe()
      here tell (Login(users(0)), session1 ref)
      session1 expectMsg Logged

      val session2 = TestProbe()
      here tell (Login(users(1)), session2 ref)
      session2 expectMsg Logged
      session1 expectMsg Logged(users(1))

      system stop (session2 ref)
      session1 expectMsg(LoggedOut(users(1)))
    }

    "first has sent message before second will login" in {
      val session1 = TestProbe()
      here tell (Login(users(0)), session1 ref)
      session1 expectMsg Logged

      here tell (ChatMessage(users(0), messages(0)), session1 ref)
      session1 expectNoMsg

      val session2 = TestProbe()
      here tell (Login(users(1)), session2 ref)
      session2 expectMsg Logged
      session1 expectMsg Logged(users(1))
      here tell (GetInitialLog, session2 ref)
      session2 expectMsg ChatLog(List(messages(0)))
    }

    "first user send message to second user" in {
      val session1 = TestProbe()
      here tell(Login(users(0)), session1 ref)
      session1 expectMsg Logged

      val session2 = TestProbe()
      here tell(Login(users(1)), session2 ref)
      session2 expectMsg Logged
      session1 expectMsg Logged(users(1))

      here tell(ChatMessage(users(0), messages(0)), session1 ref)
      session1 expectNoMsg()
      session2 expectMsg NewMessage(users(0), messages(0))
    }

    "chat with two users has terminated" in {
      val session1 = TestProbe()
      here tell(Login(users(0)), session1 ref)
      session1 expectMsg Logged
      session1 watch (chatActor)

      val session2 = TestProbe()
      here tell(Login(users(1)), session2 ref)
      session2 expectMsg Logged
      session1 expectMsg Logged(users(1))
      session2 watch (chatActor)

      chatActor ! PoisonPill

      session1 expectTerminated(chatActor)
      session2 expectTerminated(chatActor)
    }
  }

}
