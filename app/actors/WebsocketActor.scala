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

case class SendTransactions(txs: List[Transaction], msgType: String = "txlog")
case class Transaction(sender: String, receiver: String, amount: String)
case class PlayLottery(amount: Int, accName: String, msgType: String = "playLottery")

class WebsocketActor(out: ActorRef) extends Actor {
  import context.dispatcher
  import WebsocketActor._

  var transactions: List[Transaction] = List()
  var lastPlayLotteryReceived = getTime - 10
  val playLotteryRate = 5 //seconds indicating how quickly someone can send request to play the lottery

  val tick = context.system.scheduler.schedule(2 seconds, 5 seconds, self, "tick")

  def receive = {
    case "tick" => 
      //transactions = Transaction("sender", "receiver", "1337") :: transactions
       def updateTransactions() = {
          val txs = Main.getTransactionList()
          val (outTxs, inTxs) = Main.splitOutIn(txs)
          //inTxs are received payments
          val inWithNameFiltered = inTxs.txs.filter { tx =>

            println(s"trying to get accName for: ${tx.account}: ${StellerDummy.accNameForId(tx.account)}")
            //(tx.rawJson \ "tx" \ "Destination").asOpt[String].isDefined &&
            tx.isPayment && 
            StellerDummy.accNameForId(tx.account).isDefined
          }
          println(s"inWithNameFiltered: $inWithNameFiltered")

          val inWithName = inWithNameFiltered.toList.map { tx =>
            val jsonAmount = tx.rawJson \ "tx" \ "Amount"
            val (amount, currency) = jsonAmount match {
              case JsString(value) => (value, "STR")
              case _ => ( (jsonAmount \ "value").as[String], (jsonAmount \ "currency"))
            }            
            val accName = StellerDummy.accNameForId(tx.account).getOrElse("Unknown account")
            //val destName = StellerDummy.accNameForSecret((tx.rawJson \ "tx" \ "Destination").as[String]).getOrElse("Unknown Account")
            Transaction(accName, "Stellar Lottery", amount + s"($currency)")
          }
          val outWithName = outTxs.txs.filter { tx =>
            //(tx.rawJson \ "tx" \ "Destination").asOpt[String].isDefined
            tx.isPayment && 
            StellerDummy.accNameForId((tx.rawJson \ "tx" \ "Destination").as[String]).isDefined
          }.toList.map { tx =>
            val jsonAmount = tx.rawJson \ "tx" \ "Amount"
            val (amount, currency) = jsonAmount match {
              case JsString(value) => (value, "STR")
              case _ => ( (jsonAmount \ "value").as[String], (jsonAmount \ "currency"))
            }
            val destName = StellerDummy.accNameForId((tx.rawJson \ "tx" \ "Destination").as[String]).getOrElse("Unknown Account")
            Transaction("Stellar Lottery", destName, amount + s"($currency)")
          }
          println("inWithName: " + inWithName)
          println("outWithName: " + outWithName)

      }
      updateTransactions()

      out ! (Json.toJson(SendTransactions(transactions)).toString)
    case msg: String =>
      val playLotteryOpt = Json.fromJson[PlayLottery](Json.parse(msg)).asOpt
      val now = getTime
      for {
        playLottery <- playLotteryOpt
        if (playLottery.amount > 0 && playLottery.amount <= 3)
        if (now - lastPlayLotteryReceived >= playLotteryRate)
      } yield {
        println("we have received a valid playLottery msg: " + playLottery)
        lastPlayLotteryReceived = getTime

        //val (accId, secret) = ???
        stellar.API.makePayment(secret = "sf89NDAo9NiJ3YXDUW57oVe2fhJPbcnWNbiPYQYzCT4kdPmaaf3", receiver = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr", sender = "gaMTKErDVNx5ZHQnHAZpCDtw2LfzDbYzcq", amount = "2340000")
      }
  }

  def getTime = System.currentTimeMillis / 1000
}