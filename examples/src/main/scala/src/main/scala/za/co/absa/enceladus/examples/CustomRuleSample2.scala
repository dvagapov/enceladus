/*
 * Copyright 2018 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package src.main.scala.za.co.absa.enceladus.examples

import org.apache.spark.sql.{DataFrame, SparkSession}
import src.main.scala.za.co.absa.enceladus.examples.interpreter.rules.custom.LPadCustomConformanceRule
import za.co.absa.enceladus.conformance.CmdConfig
import za.co.absa.enceladus.conformance.interpreter.DynamicInterpreter
import za.co.absa.enceladus.dao.{EnceladusDAO, EnceladusRestDAO}
import za.co.absa.enceladus.model.Dataset

object CustomRuleSample2 {

  case class ExampleRow(id: Int, makeUpper: String, leave: String)
  case class OutputRow(id: Int, makeUpper: String, leave: String, doneUpper: String)

  implicit val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("CustomRuleSample2")
    .config("spark.sql.codegen.wholeStage", value = false)
    .getOrCreate()

  def main(args: Array[String]) {
    import spark.implicits._

    implicit val progArgs: CmdConfig = CmdConfig() // here we may need to specify some parameters (for certain rules)
    implicit val dao: EnceladusDAO = EnceladusRestDAO // you may have to hard-code your own implementation here (if not working with menas)
    implicit val enableCF: Boolean = false

    val inputData: DataFrame = spark.createDataFrame(
      Seq(
        ExampleRow(1, "Hello world", "What a beautiful place"),
        ExampleRow(4, "One Ring to rule them all", "One Ring to find them"),
        ExampleRow(9, "ALREADY CAPS", "and this is lower-case")
      ))

    val conformanceDef: Dataset =  Dataset(
      name = "Custom rule sample 2",
      version = 0,
      hdfsPath = "/a/b/c",
      hdfsPublishPath = "/publish/a/b/c",

      schemaName = "Not really used here",
      schemaVersion = 9999,

      conformance = List(
        LPadCustomConformanceRule(order = 0, outputColumn = "doneUpper", controlCheckpoint = false, inputColumn = "makeUpper", len = 25, pad = "~")
      )
    )

    val outputData: DataFrame = DynamicInterpreter.interpret(conformanceDef, inputData)
    val output: Seq[OutputRow] = outputData.as[OutputRow].collect().toSeq
    print(output)
  }
}
