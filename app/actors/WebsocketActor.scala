package actors

import akka.actor._
import play.api.libs.json._
import play.api.libs.functional.syntax._  
import scala.concurrent.duration._

object WebsocketActor {
  def props(out: ActorRef) = Props(new WebsocketActor(out))

  implicit val txFormat = Json.format[Transaction]
  implicit val txLogFormat = Json.format[SendTransactions]
  implicit val plFormat = Json.format[PlayLottery]
}

case class SendTransactions(txs: List[Transaction], msgType: String = "txlog")
case class Transaction(sender: String, receiver: String, amount: String)
case class PlayLottery(amount: Int, msgType: String = "playLottery")

class WebsocketActor(out: ActorRef) extends Actor {
  import context.dispatcher
  import WebsocketActor._

  var transactions: List[Transaction] = List()
  var lastPlayLotteryReceived = getTime
  val playLotteryRate = 5 //seconds indicating how quickly someone can send request to play the lottery
  
  val tick = context.system.scheduler.schedule(2 seconds, 5000 millis, self, "tick")

  def receive = {
    case "tick" =>
      transactions = Transaction("sender", "receiver", "1337") :: transactions
      out ! (Json.toJson(SendTransactions(transactions)).toString)
    case msg: String =>
      val playLotteryOpt = Json.fromJson[PlayLottery](Json.parse(msg)).asOpt
      val now = getTime
      for {
        playLottery <- playLotteryOpt
        if (playLottery.amount > 0 && playLottery.amount <= 3)
        if (now - lastPlayLotteryReceived >= playLotteryRate)
      } yield {
        println("we have received a valid playLottery msg")
        lastPlayLotteryReceived = getTime
      }
  }

  def getTime = System.currentTimeMillis / 1000
}