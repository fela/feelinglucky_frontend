package controllers

import actors.WebsocketActor
import play.api.mvc._
import play.api.mvc._
import play.api.Play.current
import stellar._
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.Cookie
import play.api.mvc.DiscardingCookie

object Application extends Controller {

  def index = Action { implicit request =>

    val cookie = request.cookies.get("accName")
    println("the cookie is " + cookie)
    cookie match {
      case Some(accName) => Ok(views.html.index("Your new application is ready."))
      case None => 
        StellerDummy.getRandomAccount match {
          case Some(acc) => Ok(views.html.index("Your new application is ready.")).withCookies(Cookie("accName", acc.accName, httpOnly = false))
          case None => Ok("unfortunately there are no more test accounts left")
        }
    }

    
  }

  def socket = WebSocket.acceptWithActor[String, String] { request => out =>
    WebsocketActor.props(out)
  }

}

object StellerDummy {

  private val accountNames: List[String] = io.Source.fromInputStream(getClass.getResourceAsStream("/names.txt")).getLines().toList
  println("accnames: " + accountNames)

  case class StellarAccount(accName: String, accId: String, secretKey: String)
  val accounts = List(StellarAccount(getAccName, "gaMTKErDVNx5ZHQnHAZpCDtw2LfzDbYzcq", "sf89NDAo9NiJ3YXDUW57oVe2fhJPbcnWNbiPYQYzCT4kdPmaaf3"))
	private var usedAccounts: Set[String] = Set()

  def getRandomAccount: Option[StellarAccount] = {
    val unusedAcc = accounts.find(a => !usedAccounts.contains(a.accId))
    unusedAcc foreach { acc =>
      usedAccounts = usedAccounts + acc.accId
    }
    unusedAcc
  }

  private def getAccName = {
    accountNames(scala.util.Random.nextInt(accountNames.length))
  }
}