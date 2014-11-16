package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.duration._
import org.lucky7.feelinglucky._
import controllers.StellerDummy

case class SendTransactions(incoming: List[Transaction], outgoing: List[Transaction], msgType: String = "txlog")
case class Transaction(sender: String, receiver: String, amount: String)
case class PlayLottery(amount: Int, accName: String) {
  def getAmount = amount * 1000000
}

object Lottery {

  implicit val txFormat = Json.format[Transaction]
  implicit val txLogFormat = Json.format[SendTransactions]
  implicit val plFormat = Json.format[PlayLottery]

  def getTransactions: (List[Transaction], List[Transaction]) = {
		def updateTransactions(): (List[Transaction], List[Transaction]) = {
        val txs = Main.getTransactionList()
        val (outTxs, inTxs) = Main.splitOutIn(txs)
        //inTxs are received payments
        val inWithNameFiltered = inTxs.txs.filter { tx =>
          println(s"trying to get accName for: ${tx.account}: ${StellerDummy.accNameForId(tx.account)}")
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
        println("inWithName: " + inWithName)
        println("outWithName: " + outWithName)
        (inWithName, outWithName)

      }
      
      updateTransactions()
  }

  def playLottery(playLottery: PlayLottery) = {
    //val playLotteryOpt = Json.fromJson[PlayLottery](Json.parse(msg)).asOpt
    if (playLottery.amount > 0 && playLottery.amount <= 3) {
      println("we have received a valid playLottery msg: " + playLottery)

      StellerDummy.accForName(playLottery.accName).foreach { acc =>
        println(s"sending from acc: ${acc}")
        stellar.API.makePayment(
          secret = acc.secretKey, 
          receiver = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr", 
          sender = acc.accId,
          amount = playLottery.getAmount.toString)
      }
    }
    /*
    for {
      playLottery <- playLotteryOpt
      if (playLottery.amount > 0 && playLottery.amount <= 3)
      //if (now - lastPlayLotteryReceived >= playLotteryRate) //this has to be removed when we moved from WebSockets to Ajax
    } yield {
      println("we have received a valid playLottery msg: " + playLottery)

      StellerDummy.accForName(playLottery.accName).foreach { acc =>
        println(s"sending from acc: ${acc}")
        stellar.API.makePayment(
          secret = acc.secretKey, 
          receiver = "gsMxVfhj1GmHspP5iARzMxZBZmPya9NALr", 
          sender = acc.accId,
          amount = playLottery.getAmount.toString)
      }
    }  
    */  
  } 

}