/*
 * Copyright 2018 ABSA Group Limited
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

package za.co.absa.enceladus.conformance

import org.apache.commons.configuration2.{BaseConfiguration, Configuration}
import org.apache.spark.sql.types.{IntegerType, StringType}
import org.scalatest.WordSpec
import za.co.absa.enceladus.utils.broadcast.MappingTableFilter

class HyperConformanceSuite extends WordSpec {

  private def getMtConfig(columnName: String, dataType: String, value: String): Configuration = {
    val config = new BaseConfiguration
    config.addProperty("transformer.hyperconformance.mapping.table.filter.col", columnName)
    config.addProperty("transformer.hyperconformance.mapping.table.filter.type", dataType)
    config.addProperty("transformer.hyperconformance.mapping.table.filter.val", value)
    config
  }

  "getMappingTableFilters()" should {
    "skip the definition of filters if no fields are defined" in {
      val conf = new BaseConfiguration

      val filters = HyperConformance.getMappingTableFilters(conf)

      assert(filters.isEmpty)
    }

    "parse string data type properly" in {
      val filters = HyperConformance.getMappingTableFilters(getMtConfig("a", "string", "str"))

      assert(filters.nonEmpty)
      assert(filters.head == MappingTableFilter("a", StringType, "str"))
    }

    "parse int data type properly" in {
      val filters = HyperConformance.getMappingTableFilters(getMtConfig("b", "int", "1"))

      assert(filters.nonEmpty)
      assert(filters.head == MappingTableFilter("b", IntegerType, "1"))
    }

    "throw an exception if filters are partially defined" in {
      val conf = getMtConfig("c", "long", "2")
      conf.clearProperty("transformer.hyperconformance.mapping.table.filter.val")


      intercept[IllegalArgumentException] {
        HyperConformance.getMappingTableFilters(conf)
      }
    }
  }

}
