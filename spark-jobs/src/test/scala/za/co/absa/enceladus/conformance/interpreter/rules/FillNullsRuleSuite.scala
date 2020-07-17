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

package za.co.absa.enceladus.conformance.interpreter.rules

import org.apache.spark.sql.DataFrame
import org.scalatest.FunSuite
import za.co.absa.enceladus.conformance.samples.DeepArraySamples
import za.co.absa.enceladus.model.Dataset
import za.co.absa.enceladus.model.conformanceRule.FillNullsConformanceRule
import za.co.absa.enceladus.utils.testUtils.SparkTestBase

class FillNullsRuleSuite extends FunSuite with SparkTestBase with TestRuleBehaviors {
  // scalastyle:off line.size.limit

  private val literalRule = FillNullsConformanceRule(
    order = 1,
    outputColumn = "nameNoNull",
    controlCheckpoint = false,
    inputColumn = "name",
    value = "NoNullValue"
  )
  private val literalArrayRule = FillNullsConformanceRule(
    order = 2,
    outputColumn = "items.itemid2",
    controlCheckpoint = false,
    inputColumn = "items.itemid",
    value = "Gshj1"
  )

  private val literalRulesList1 = List(literalRule)
  private val literalRulesList2 = List(literalRule, literalArrayRule)

  private val literalOrdersDS1 = Dataset(
    name = "Orders Conformance",
    hdfsPath = "src/test/testData/orders",
    hdfsPublishPath = "testData/conformedOrders",
    schemaName = "Orders",
    schemaVersion = 1,
    conformance = literalRulesList1
  )

  private val literalOrdersDS2 = literalOrdersDS1.copy(conformance = literalRulesList2)

  private val conformedLiteralOrdersJSON1: String =
    """{"id":1,"name":"First Order","items":[{"itemid":"ar229","qty":10,"price":5.1,"payments":[{"payid":"pid10","amount":51.0}]},{"itemid":"2891k","qty":100,"price":1.1,"payments":[{"payid":"zk20","amount":100.0}]},{"itemid":"31239","qty":2,"price":55.2,"payments":[]}],"errCol":[],"nameNoNull":"First Order"}
      |{"id":2,"items":[{"itemid":"AkuYdg","qty":100,"price":10.0,"payments":[{"payid":"d101","amount":10.0},{"payid":"d102","amount":20.0}]},{"itemid":"jUa1k0","qty":2,"price":55.2,"payments":[]}],"errCol":[],"nameNoNull":"NoNullValue"}
      |{"id":3,"name":"Third Order","items":[{"qty":10,"price":10000.0,"payments":[{"payid":"pid10","amount":2000.0},{"payid":"pid10","amount":5000.0}]},{"itemid":"Jdha2","qty":100,"price":45.0,"payments":[{"payid":"zk20","amount":150.0},{"payid":"pid10","amount":2000.0}]}],"errCol":[],"nameNoNull":"Third Order"}
      |{"id":4,"name":"Fourth Order","items":[{"itemid":"dLda1","qty":10,"price":5.1,"payments":[{"payid":"pid10","amount":10.0}]},{"itemid":"d2dhJ","qty":100,"price":1.1,"payments":[{"payid":"zk20","amount":15.0}]},{"itemid":"Mska0","qty":2,"price":55.2,"payments":[]},{"itemid":"Gdal1","qty":20,"price":5.2,"payments":[]},{"itemid":"dakl1","qty":99,"price":1.2,"payments":[]}],"errCol":[],"nameNoNull":"Fourth Order"}
      |{"id":5,"name":"Fifths order","items":[{"itemid":"hdUs1J","qty":50,"price":0.2,"payments":[{"payid":"pid10","amount":10.0},{"payid":"pid10","amount":11.0},{"payid":"pid10","amount":12.0}]}],"errCol":[],"nameNoNull":"Fifths order"}"""
      .stripMargin.replace("\r\n", "\n")

  private val conformedLiteralOrdersJSON2: String =
    """{"id":1,"name":"First Order","items":[{"itemid":"ar229","qty":10,"price":5.1,"payments":[{"payid":"pid10","amount":51.0}],"itemid2":"ar229"},{"itemid":"2891k","qty":100,"price":1.1,"payments":[{"payid":"zk20","amount":100.0}],"itemid2":"2891k"},{"itemid":"31239","qty":2,"price":55.2,"payments":[],"itemid2":"31239"}],"errCol":[],"nameNoNull":"First Order"}
      |{"id":2,"items":[{"itemid":"AkuYdg","qty":100,"price":10.0,"payments":[{"payid":"d101","amount":10.0},{"payid":"d102","amount":20.0}],"itemid2":"AkuYdg"},{"itemid":"jUa1k0","qty":2,"price":55.2,"payments":[],"itemid2":"jUa1k0"}],"errCol":[],"nameNoNull":"NoNullValue"}
      |{"id":3,"name":"Third Order","items":[{"qty":10,"price":10000.0,"payments":[{"payid":"pid10","amount":2000.0},{"payid":"pid10","amount":5000.0}],"itemid2":"Gshj1"},{"itemid":"Jdha2","qty":100,"price":45.0,"payments":[{"payid":"zk20","amount":150.0},{"payid":"pid10","amount":2000.0}],"itemid2":"Jdha2"}],"errCol":[],"nameNoNull":"Third Order"}
      |{"id":4,"name":"Fourth Order","items":[{"itemid":"dLda1","qty":10,"price":5.1,"payments":[{"payid":"pid10","amount":10.0}],"itemid2":"dLda1"},{"itemid":"d2dhJ","qty":100,"price":1.1,"payments":[{"payid":"zk20","amount":15.0}],"itemid2":"d2dhJ"},{"itemid":"Mska0","qty":2,"price":55.2,"payments":[],"itemid2":"Mska0"},{"itemid":"Gdal1","qty":20,"price":5.2,"payments":[],"itemid2":"Gdal1"},{"itemid":"dakl1","qty":99,"price":1.2,"payments":[],"itemid2":"dakl1"}],"errCol":[],"nameNoNull":"Fourth Order"}
      |{"id":5,"name":"Fifths order","items":[{"itemid":"hdUs1J","qty":50,"price":0.2,"payments":[{"payid":"pid10","amount":10.0},{"payid":"pid10","amount":11.0},{"payid":"pid10","amount":12.0}],"itemid2":"hdUs1J"}],"errCol":[],"nameNoNull":"Fifths order"}"""
      .stripMargin.replace("\r\n", "\n")

  private val inputDf: DataFrame = spark.createDataFrame(DeepArraySamples.ordersDataWithNulls)

  test("Literal conformance rule test 1") {
    conformanceRuleShouldMatchExpected(inputDf, literalOrdersDS1, conformedLiteralOrdersJSON1)
  }

  test("Literal conformance rule test 2") {
    conformanceRuleShouldMatchExpected(inputDf, literalOrdersDS2, conformedLiteralOrdersJSON2)
  }

  // scalastyle:on line.size.limit
}
