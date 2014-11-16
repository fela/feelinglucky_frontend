package controllers

import models._
import models.Lottery._
import play.api.mvc._
import play.api.mvc._
import play.api.Play.current
import stellar._
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.Cookie
import play.api.mvc.DiscardingCookie
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {

  def index = Action { implicit request =>
    val cookie = request.cookies.get("accName")
    cookie match {
      case Some(accName) => Ok(views.html.index("Your new application is ready."))
      case None => 
        StellerDummy.getRandomAccount match {
          case Some(acc) => Ok(views.html.index("Your new application is ready.")).withCookies(Cookie("accName", acc.accName, httpOnly = false))
          case None => Ok("unfortunately there are no more test accounts left")
        }
    }
  }
/*
  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    WebsocketActor.props(out)
  }
*/

  //PlayLottery(amount: Int, accName: String, msgType: String = "playLottery")
  val playLotteryForm = Form(
    mapping(
      "amount" -> number,
      "accName" -> nonEmptyText
    )(PlayLottery.apply)(PlayLottery.unapply)
  )
  def playLottery = Action { implicit request =>
    playLotteryForm.bindFromRequest.fold(
      formWithErrors => {
        println(s"playLotteryForm hasErrors: $playLotteryForm")
      },
      playLottery => {
        Lottery.playLottery(playLottery)
      }
    )
    Ok("")
  }

  def getTransactions = Action { implicit request =>
    val (inTxs, outTxs) = Lottery.getTransactions
    Ok(Json.toJson(SendTransactions(inTxs, outTxs)))
  }

}

object StellerDummy {

  private val accountNames: List[String] = io.Source.fromInputStream(getClass.getResourceAsStream("/names.txt")).getLines().toList

  case class StellarAccount(accName: String, accId: String, secretKey: String)
  val accounts = List(
    StellarAccount(getAccName, "gaMTKErDVNx5ZHQnHAZpCDtw2LfzDbYzcq", "sf89NDAo9NiJ3YXDUW57oVe2fhJPbcnWNbiPYQYzCT4kdPmaaf3"),
    StellarAccount(getAccName, "gMxUtnoVcknHQKvUrr9ctL4Vs8PJEU9fRG", "s3mUQCq85YTdYyT6trtA81q95aeRwixbyGC3cXan3VNdNHwzhqT"),
    StellarAccount(getAccName, "g45uiJenewBTAfSbHsuRQiVZfFQHaqVbLo", "sfmrmX1U1XZiGswndhn1Hjup2RcHJQUe7dTBSAVXSktdzeab8ow")
  )

  def accNameForId(id: String): Option[String] = accounts.find(_.accId == id).map(_.accName)
  def accForName(name: String): Option[StellarAccount] = accounts.find(_.accName == name)

	private var usedAccounts: Set[String] = Set()

  def getRandomAccount: Option[StellarAccount] = {
    val unusedAcc = accounts.find(a => !usedAccounts.contains(a.accId))
    unusedAcc foreach { acc =>
      usedAccounts = usedAccounts + acc.accId
    }
    unusedAcc
  }

  private var accNameIdx = -1
  private def getAccName: String = {
    accNameIdx += 1
    accountNames(accNameIdx)
  }
}