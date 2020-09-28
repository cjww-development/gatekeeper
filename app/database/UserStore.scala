package database

import com.cjwwdev.mongo.DatabaseRepository
import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoFailedCreate, MongoFailedUpdate, MongoSuccessCreate, MongoSuccessUpdate, MongoUpdatedResponse}
import models.User
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.mongodb.scala.model.Projections.{excludeId, fields, include}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.reflect.ClassTag

trait UserStore extends DatabaseRepository with CodecReg {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def userBasedCollection(implicit ct: ClassTag[User], codec: CodecRegistry) = collection[User](ct, codec)
  private def documentBasedCollection(implicit ct: ClassTag[Document], codec: CodecRegistry) = collection[Document](ct, codec)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("id"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("userName"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("email"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("salt"), IndexOptions().background(false).unique(true)),
  )

  def createUser(user: User)(implicit ec: ExC): Future[MongoCreateResponse] = {
    userBasedCollection
      .insertOne(user)
      .toFuture()
      .map { _ =>
        logger.info(s"[createNewUser] - Created new user under userId ${user.id}")
        MongoSuccessCreate
      }.recover {
        case e =>
          logger.error(s"[createNewUser] - There was a problem creating a new user under userId ${user.id}", e)
          MongoFailedCreate
      }
  }

  def findUser(query: Bson): Future[Option[User]] = {
    userBasedCollection
      .find(query)
      .first()
      .toFutureOption()
  }

  def projectValue(key: String, value: String, projections: String*)(implicit ec: ExC): Future[Map[String, BsonValue]] = {
    def buildMap(doc: Option[Document]): Map[String, BsonValue] = {
      doc.fold[Map[String, BsonValue]](Map())(_.toMap)
    }

    val inclusions = fields((projections
      .map(str => include(str)) ++ Seq(include("id"), excludeId())):_*)

    documentBasedCollection
      .find(equal(key, value))
      .projection(inclusions)
      .first()
      .toFutureOption()
      .map(buildMap)
      .recover(_ => Map())
  }

  def updateUser(query: Bson, update: Bson)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    userBasedCollection
      .updateOne(query, update)
      .toFuture()
      .map { _ =>
        logger.info(s"[updateUser] - Updated user information")
        MongoSuccessUpdate
      }.recover {
        case e =>
          logger.warn(s"[updateUser] - There was a problem updating the user", e)
          MongoFailedUpdate
      }
  }
}
