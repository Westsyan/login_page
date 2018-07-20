package controllers

import java.io.File
import javax.inject.Inject

import dao.adminDao
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import utils.{ExecCommand, Utils}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AdminController @Inject()(admindao: adminDao) extends Controller {

  def adminHome = Action { implicit request =>
    Ok(views.html.admin.account(request.domain))
  }


  def getAllInfo = Action.async { implicit request =>
    admindao.getAllUser.map { x =>
      val json = x.map { y =>
        val user = y.account
        Json.obj("user" -> user)
      }
      Ok(Json.toJson(json))
    }
  }

  def deleteUser(id: Int) = Action.async { implicit request =>
    admindao.deleteById(id).map { x =>
      val run = Future {
        FileUtils.deleteDirectory(new File(Utils.path + "/otu_platform/data/" + id))
        FileUtils.deleteDirectory(new File(Utils.path + "/parametron_platform/data/" + id))
        FileUtils.deleteDirectory(new File(Utils.path + "/transcriptome_platform/data/" + id))
        FileUtils.deleteDirectory(new File(Utils.path + "/resequencing_platform/data/" + id))
      }
      Ok(Json.toJson("success"))
    }
  }

  def platformHome = Action { implicit request =>
    Ok(views.html.index.home(request.domain))
  }

  def getAllUser = Action.async { implicit request =>
    admindao.getAllUser.map { x =>
      val json = x.map { y =>
        val (name, power) = if (y.id == 1) {
          (y.account, "管理员")
        } else {
          (y.account, "普通用户")
        }

        val operate = if (y.id == 1) {
          s"""<button class="update" onclick="updatePassword(this)" value="${y.account}" id="${y.id}" title="修改密码"><i class="fa fa-pencil"></i></button>
             |<button class="update" onclick="openDeleteTmp()" value="${y.account}" id="${y.id}" title="清理系统缓存"><i class="fa fa-times"></i></button>
           """.stripMargin
        } else {
          s"""
             |<button class="update" onclick="updatePassword(this)" value="${y.account}" id="${y.id}" title="修改密码"><i class="fa fa-pencil"></i></button>
             |<button class="update" onclick="openDelete(this)" value="${y.account}" id="${y.id}" title="删除用户"><i class="fa fa-trash"></i></button>
         """.
            stripMargin
        }
        Json.obj("user" -> name, "power" -> power, "operation" -> operate)
      }
      Ok(Json.toJson(json))
    }

  }

  case class passwordData(uid: Int, password: String)

  val passwordForm = Form(
    mapping(
      "uid" -> number,
      "password" -> text
    )(passwordData.apply)(passwordData.unapply)
  )

  def updatePassword = Action.async { implicit request =>
    val data = passwordForm.bindFromRequest.get
    admindao.updatePassword(data.uid, data.password).map { x =>
      Ok(Json.toJson("success"))
    }
  }

  def deleteTmp = Action{implicit request=>

    val run = Future {
      this.synchronized{
        FileUtils.deleteDirectory(new File(Utils.path + "/otu_platform/tmp"))
        FileUtils.deleteDirectory(new File(Utils.path + "/parametron_platform/tmp"))
        FileUtils.deleteDirectory(new File(Utils.path + "/transcriptome_platform/tmp"))
        FileUtils.deleteDirectory(new File(Utils.path + "/resequencing_platform/tmp"))

        new File(Utils.path + "/otu_platform/tmp").mkdir()
        new File(Utils.path + "/parametron_platform/tmp").mkdir()
        new File(Utils.path + "/transcriptome_platform/tmp").mkdir()
        new File(Utils.path + "/resequencing_platform/tmp").mkdir()
      }
    }
    Ok(Json.toJson("success"))
  }

  def getDisk = Action { implicit request =>
    val sh = "sh /mnt/sdb/platform/getDisk.sh"
    val command = new ExecCommand
    command.exec(sh)
    val buffer = FileUtils.readLines(new File(Utils.path, "disk.txt")).asScala
    val head = buffer.head.split(" ")
    val all = head.head
    val alls = all.split("T").head.toDouble
    val use = head.last
    val uses = use.split("T").head.split("G").head.toDouble
    val per = if (uses > 20) {
      uses / alls / 1024 * 100
    } else {
      uses / alls * 100
    }
    val json = Json.obj("all" -> all, "use" -> use, "per" -> per.formatted("%.2f"))
    Ok(Json.toJson(json))
  }

}
