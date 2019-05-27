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

package za.co.absa.enceladus.migrations.framework.migration

import org.apache.log4j.{LogManager, Logger}
import za.co.absa.enceladus.migrations.framework.MigrationUtils
import za.co.absa.enceladus.migrations.framework.dao.DocumentDb

import scala.collection.mutable

/**
  * A JsonMigration represents an entity that provides transformations for each document of every collection in a model
  * when switching from one version of the model to another.
  *
  * All transformations are JSON string in, JSON string out.
  *
  * Only one transformation is possible per collection.
  *
  * In order to create a JSON migration you need to extend from this trait and provide all the requited transformations:
  *
  * {{{
  *   class MigrationTo1 extends MigrationBase with JsonMigration {
  *
  *     migrate("collection1_name") (jsonIn => {
  *       val jsonOut = collection1Transformations(jsonIn)
  *       jsonOut
  *     })
  *
  *     migrate("collection2_name") (jsonIn => {
  *       val jsonOut = collection2Transformations(jsonIn)
  *       jsonOut
  *     })
  *   }
  * }}}
  *
  */
trait JsonMigration extends Migration {
  type DocumentTransformer = String => String

  private val log: Logger = LogManager.getLogger("JsonMigration")

  /**
    * This function is used by derived classes to add transformations for affected collections.
    * This is used for complex migrations that requite complex model version maps.
    *
    * @param collectionName A collection name to be migrated
    * @param f              A transformation to applied to each document of the collection
    *
    */
  def transformJSON(collectionName: String)(f: String => String): Unit = {
    if (transformers.contains(collectionName)) {
      throw new IllegalArgumentException(s"A transformer for '$collectionName' has already been added.")
    }
    transformers.put(collectionName, f)
  }

  /**
    * Gets a JSON transformer for the specified collection if applicable
    *
    * @param collectionName A collection name to be migrated
    * @return A function that takes a JSON string and returns a transformed JSON string
    *
    */
  def getTransformer(collectionName: String): Option[DocumentTransformer] = transformers.get(collectionName)

  /**
    * Executes a migration on a given database and a list of collection names.
    */
  abstract override def execute(db: DocumentDb, collectionNames: Seq[String]): Unit = {
    super.execute(db, collectionNames)
    collectionNames.foreach(collection =>
      if (transformers.contains(collection)) {
        applyTransformers(db, collection)
      }
    )
  }

  /**
    * Validate the possibility of running a migration given a list of collection names.
    */
  abstract override def validate(collectionNames: Seq[String]): Unit = {
    super.validate(collectionNames)
    transformers.foreach {
      case (collectionToMigrate, _) => if (!collectionNames.contains(collectionToMigrate)) {
        throw new IllegalStateException(
          s"Attempt to apply a transform to a collection that does not exist: $collectionToMigrate.")
      }
    }
  }

  override protected def validateMigration(): Unit = {
    if (targetVersion <= 0) {
      throw new IllegalStateException("The target version of a JsonMigration should be greater than 0.")
    }
  }

  /**
    * Applies a transformer for each document of the collection to produce a migrated collection.
    */
  private def applyTransformers(db: DocumentDb, collectionName: String): Unit = {
    val sourceCollection = MigrationUtils.getVersionedCollectionName(collectionName, targetVersion - 1)
    val targetCollection = MigrationUtils.getVersionedCollectionName(collectionName, targetVersion)
    val documents = db.getDocuments(sourceCollection)
    val transformer = transformers(collectionName)
    log.info(s"Applying a per-document transformation $sourceCollection -> $targetCollection")
    ensureCollectionEmpty(db, targetCollection)
    documents.foreach(doc =>
      db.insertDocument(targetCollection, transformer(doc))
    )
  }

  private def ensureCollectionEmpty(db: DocumentDb, collectionName: String): Unit = {
    if (db.isCollectionExists(collectionName)) {
      db.emptyCollection(collectionName)
    }
  }

  private val transformers = new mutable.HashMap[String, DocumentTransformer]()
}