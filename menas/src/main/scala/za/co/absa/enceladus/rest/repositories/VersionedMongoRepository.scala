/*
 * Copyright 2018-2019 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.enceladus.rest.repositories

import java.time.ZonedDateTime

import org.mongodb.scala._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import za.co.absa.enceladus.model.menas._
import za.co.absa.enceladus.model.versionedModel.{VersionedModel, VersionedSummary}

import scala.concurrent.Future
import scala.reflect.ClassTag
import za.co.absa.enceladus.rest.exceptions.EntityAlreadyExistsException
import za.co.absa.enceladus.rest.exceptions.NotFoundException

abstract class VersionedMongoRepository[C <: VersionedModel](mongoDb: MongoDatabase)(implicit ct: ClassTag[C])
  extends MongoRepository[C](mongoDb) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private def getParent(oldEntity: C): MenasReference = {
    MenasReference(collection = Some(collectionName), name = oldEntity.name, version = oldEntity.version) 
  }
  
  def getLatestVersions(): Future[Seq[VersionedSummary]] = {
    val pipeline = Seq(
      filter(getNotDisabledFilter),
      Aggregates.group("$name", Accumulators.max("latestVersion", "$version"))
    )
    collection.aggregate[VersionedSummary](pipeline).toFuture()
  }

  def getVersion(name: String, version: Int): Future[Option[C]] = {
    collection.find(getNameVersionFilterEnabled(name, Some(version))).headOption()
  }

  def getLatestVersionValue(name: String): Future[Option[Int]] = {
    val pipeline = Seq(
      filter(getNameFilter(name)),
      filter(getNotDisabledFilter),
      Aggregates.group("$name", Accumulators.max("latestVersion", "$version"))
    )
    collection.aggregate[VersionedSummary](pipeline).headOption().map(_.map(_.latestVersion))
  }

  def getAllVersions(name: String, inclDisabled: Boolean = false): Future[Seq[C]] = {
    val filter = if(inclDisabled) getNameFilter(name) else getNameFilterEnabled(name)
    collection.find(filter).toFuture()
  }

  def create(item: C, username: String): Future[Completed] = {
    super.create(item
      .setCreatedInfo(username)
      .setUpdatedInfo(username)
      .asInstanceOf[C]
    )
  }

  def update(username: String, updated: C): Future[C] = {
    for {
      latestVersion <- getLatestVersionValue(updated.name)
      newVersion <- if(latestVersion.isEmpty) throw new NotFoundException()
           else if(latestVersion != updated.version) throw new EntityAlreadyExistsException(s"Entity ${updated.name} (version. ${updated.version}) already exists.") 
           else Future.successful(latestVersion.get + 1)
      newInfo <- Future.successful(updated.setUpdatedInfo(username).setVersion(newVersion).setParent(Some(getParent(updated))).asInstanceOf[C])
      res <- collection.insertOne(newInfo).head()
    } yield newInfo
    
  }

  def disableVersion(name: String, version: Option[Int], username: String): Future[UpdateResult] = {
    collection.updateMany(getNameVersionFilter(name, version), combine(
      set("disabled", true),
      set("dateDisabled", ZonedDateTime.now()),
      set("userDisabled", username))).toFuture()
  }

  def findRefEqual(refNameCol: String, refVersionCol: String, name: String, version: Option[Int]): Future[Seq[MenasReference]] = {
    val filter = version match {
      case Some(ver) => Filters.and(getNotDisabledFilter, equal(refNameCol, name), equal(refVersionCol, ver))
      case None      => Filters.and(getNotDisabledFilter, equal(refNameCol, name))
    }
    collection
      .find[MenasReference](filter)
      .projection(fields(include("name", "version"), computed("collection", collectionName)))
      .toFuture()
  }

  private[repositories] def getNotDisabledFilter: Bson = {
    notEqual("disabled", true)
  }

  private[repositories] def getNameVersionFilter(name: String, version: Option[Int]): Bson = {
    version match {
      case Some(ver) => Filters.and(getNameFilter(name), equal("version", ver))
      case None      => getNameFilter(name)
    }
  }

  private[repositories] def getNameVersionFilterEnabled(name: String, version: Option[Int]): Bson = {
    Filters.and(getNameVersionFilter(name, version), getNotDisabledFilter)
  }

  private[repositories] def getNameFilterEnabled(name: String): Bson = {
    Filters.and(getNameFilter(name), getNotDisabledFilter)
  }

}
