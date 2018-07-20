package dao

import javax.inject.Inject

import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class adminDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def selectByName(account:String,password:String) : Future[Option[UserRow]] = {
    db.run(User.filter(_.account === account).filter(_.password === password).result.headOption)
  }

  def addAccount(account: Seq[(String,String)]) : Future[Unit] =
    db.run(User.map(x=>(x.account,x.password)) ++= account).map(_ => ())


  def selectName(account:String) : Future[Seq[UserRow]] = {
    db.run(User.filter(_.account === account).result)
  }

  def updatePassword(id:Int,password:String) : Future[Unit] = {
      db.run(User.filter(_.id === id).map(_.password).update(password).map(_ => ()))
  }

  def getIdByAccount(account:String) : Future[Int] ={
    db.run(User.filter(_.account === account).map(_.id).result.head)
  }

  def getAllUser : Future[Seq[UserRow]] = {
    db.run(User.result)
  }

  def deleteById(id:Int) : Future[Unit] = {
    db.run(User.filter(_.id === id).delete).map(_=>())
  }



}
