package controllers

import actors.WebsocketActor
import play.api.mvc._
import play.api.mvc._
import play.api.Play.current
//import stellar._
import play.api.libs.json.{JsArray, JsValue, Json}

object Application extends Controller {

  def index = Action {
    /*
    val txLog = StellerDummy.getTxLog()
    println(txLog(0).rawJson)
    */

    Ok(views.html.index("Your new application is ready."))
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    WebsocketActor.props(out)
  }

}

object StellerDummy {
/*
  private val txnlog: String = io.Source.fromInputStream(getClass.getResourceAsStream("/dummyTransactions.json")).getLines().mkString("")

  private val jsv: JsValue = Json.parse(txnlog) \ "result"
  private val txns: List[Transaction] = Transaction.parseList((jsv \ "transactions").as[JsArray])

	def getTxLog(): List[Transaction] = {
    txns
	}
	*/
}