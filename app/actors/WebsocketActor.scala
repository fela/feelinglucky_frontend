package actors

import akka.actor._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.duration._
import org.lucky7.feelinglucky._
import controllers.StellerDummy

object WebsocketActor {
  def props(out: ActorRef) = Props(new WebsocketActor(out))

  //val transactionActor = Akka.system.actorOf(Props[TransactionActor], name = "transactionActor")

  implicit val txFormat = Json.format[Transaction]
  implicit val txLogFormat = Json.format[SendTransactions]
  implicit val plFormat = Json.format[PlayLottery]
}

class TransactionActor extends Actor {

  def receive = {
    case _ =>
  }

  override def preStart() = {
  }

 
}

case class SendTransactions(incoming: List[Transaction], outgoing: List[Transaction], msgType: String = "txlog")
case class Transaction(sender: String, receiver: String, amount: String)
case class PlayLottery(amount: Int, accName: String, msgType: String = "playLottery") {
  def getAmount = amount * 1000000
}

class WebsocketActor(out: ActorRef) extends Actor {
  import context.dispatcher
  import WebsocketActor._

  var transactions: List[Transaction] = List()
  var lastPlayLotteryReceived = getTime - 10
  val playLotteryRate = 5 //seconds indicating how quickly someone can send request to play the lottery

  val tick = context.system.scheduler.schedule(500 millis, 50 seconds, self, "tick")

  def receive = {
    case "tick" => 
      //transactions = Transaction("sender", "receiver", "1337") :: transactions
       def updateTransactions(): (List[Transaction], List[Transaction]) = {
          val txs = Main.getTransactionList()
          val (outTxs, inTxs) = Main.splitOutIn(txs)
          //inTxs are received payments
          val inWithNameFiltered = inTxs.txs.filter { tx =>
            tx.isPayment && 
            StellerDummy.accNameForId(tx.account).isDefined
          }
          val inWithName = inWithNameFiltered.toList.sortWith { case (a, b) =>
            val aTime = (a.rawJson \ "tx" \ "date").asOpt[Int]
            val bTime = (b.rawJson \ "tx" \ "date").asOpt[Int]

            (aTime, bTime) match {
              case (Some(at), Some(bt)) => at > bt
              case _ => false
            }
          }.map { tx =>
            val jsonAmount = tx.rawJson \ "tx" \ "Amount"
            val (amount, currency) = jsonAmount match {
              case JsString(value) => (value, "STR")
              case _ => ( (jsonAmount \ "value").as[String], (jsonAmount \ "currency"))
            }            
            val accName = StellerDummy.accNameForId(tx.account).getOrElse("Unknown account")
            Transaction(accName, "Stellar Lottery", amount + s"($currency)")
          }
          val outWithName = outTxs.txs.filter { tx =>
            tx.isPayment && 
            StellerDummy.accNameForId((tx.rawJson \ "tx" \ "Destination").as[String]).isDefined
          }.toList.sortWith { case (a, b) =>
            val aTime = (a.rawJson \ "tx" \ "date").asOpt[Int]
            val bTime = (b.rawJson \ "tx" \ "date").asOpt[Int]

            (aTime, bTime) match {
              case (Some(at), Some(bt)) => at > bt
              case _ => false
            }
          }.map { tx =>
            val jsonAmount = tx.rawJson \ "tx" \ "Amount"
            val (amount, currency) = jsonAmount match {
              case JsString(value) => (value, "STR")
              case _ => ( (jsonAmount \ "value").as[String], (jsonAmount \ "currency"))
            }
            val destName = StellerDummy.accNameForId((tx.rawJson \ "tx" \ "Destination").as[String]).getOrElse("Unknown Account")
            Transaction("Stellar Lottery", destName, amount + s"($currency)")
          }
          (inWithName, outWithName)

      }
      val (inTxs, outTxs) = updateTransactions()

      out ! (Json.toJson(SendTransactions(inTxs, outTxs)).toString)
    case msg: String =>
      val playLotteryOpt = Json.fromJson[PlayLottery](Json.parse(msg)).asOpt
      val now = getTime
      for {
        playLottery <- playLotteryOpt
        if (playLottery.amount > 0 && playLottery.amount <= 3)
        if (now - lastPlayLotteryReceived >= playLotteryRate)
      } yield {
        lastPlayLotteryReceived = getTime

        StellerDummy.accForName(playLottery.accName).foreach { acc =>
          stellar.API.makePayment(
            secret = acc.secretKey, 
            receiver = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr", 
            sender = acc.accId,
            amount = playLottery.getAmount.toString)
        }
      }
  }

  def getTime = System.currentTimeMillis / 1000
}