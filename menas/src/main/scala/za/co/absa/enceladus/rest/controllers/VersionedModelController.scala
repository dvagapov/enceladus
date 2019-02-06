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

package za.co.absa.enceladus.rest.controllers

import java.util.Optional
import java.util.concurrent.CompletableFuture

import com.mongodb.client.result.UpdateResult
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation._
import za.co.absa.enceladus.model.UsedIn
import za.co.absa.enceladus.model.versionedModel._
import za.co.absa.enceladus.rest.exceptions.NotFoundException
import za.co.absa.enceladus.rest.services.VersionedModelService


abstract class VersionedModelController[C <: VersionedModel](versionedModelService: VersionedModelService[C])
  extends BaseController {

  import za.co.absa.enceladus.rest.utils.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  @GetMapping(Array("/list"))
  @ResponseStatus(HttpStatus.OK)
  def getList(): CompletableFuture[Seq[VersionedSummary]] = {
    versionedModelService.getLatestVersions()
  }

  @GetMapping(Array("/detail/{name}/{version}"))
  @ResponseStatus(HttpStatus.OK)
  def getVersionDetail(@PathVariable name: String,
                       @PathVariable version: Int): CompletableFuture[C] = {
    versionedModelService.getVersion(name, version).map {
      case Some(entity) => entity
      case None         => throw notFound()
    }
  }

  @GetMapping(Array("/detail/{name}/latest"))
  @ResponseStatus(HttpStatus.OK)
  def getLatestDetail(@PathVariable name: String): CompletableFuture[C] = {
    versionedModelService.getLatestVersion(name).map {
      case Some(entity) => entity
      case None         => throw NotFoundException()
    }
  }

  @GetMapping(Array("/isUniqueName/{name}"))
  @ResponseStatus(HttpStatus.OK)
  def isUniqueName(@PathVariable name: String): CompletableFuture[Boolean] = {
    versionedModelService.isUniqueName(name)
  }

  @GetMapping(Array("/usedIn/{name}/{version}"))
  @ResponseStatus(HttpStatus.OK)
  def usedIn(@PathVariable name: String,
             @PathVariable version: Int): CompletableFuture[UsedIn] = {
    versionedModelService.getUsedIn(name, Some(version))
  }

  @GetMapping(Array("/allVersions/{name}"))
  @ResponseStatus(HttpStatus.OK)
  def getAllVersions(@PathVariable name: String): CompletableFuture[Seq[C]] = {
    versionedModelService.getAllVersions(name)
  }

  @PostMapping(Array("/create"))
  @ResponseStatus(HttpStatus.CREATED)
  def create(@AuthenticationPrincipal principal: UserDetails,
             @RequestBody item: C): CompletableFuture[C] = {
    versionedModelService.create(item, principal.getUsername).map {
      case Some(entity) => entity
      case None         => throw notFound()
    }
  }

  @PostMapping(Array("/edit"))
  @ResponseStatus(HttpStatus.CREATED)
  def edit(@AuthenticationPrincipal user: UserDetails,
           @RequestBody item: C): CompletableFuture[C] = {
    versionedModelService.update(user.getUsername, item).map {
      case Some(entity) => entity
      case None         => throw notFound()
    }
  }

  @GetMapping(Array("/disable/{name}", "/disable/{name}/{version}"))
  @ResponseStatus(HttpStatus.OK)
  def disable(@PathVariable name: String,
              @PathVariable version: Optional[Int]): CompletableFuture[UpdateResult] = {
    val v = if (version.isPresent) Some(version.get) else None
    versionedModelService.disableVersion(name, v)
  }

}
