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

package za.co.absa.enceladus.utils.validation.field

import java.sql.Timestamp
import java.util.TimeZone

import org.apache.spark.sql.{Column, Dataset, Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField}
import za.co.absa.enceladus.utils.validation.{ValidationError, ValidationIssue}
import za.co.absa.enceladus.utils.implicits.StructFieldImplicits.StructFieldImprovements
import za.co.absa.enceladus.utils.time.{DateTimePattern, EnceladusDateTimeParser, TimeZoneNormalizer}
import org.apache.spark.sql.functions._
import za.co.absa.enceladus.utils.error.ErrorMessage

import scala.util.control.NonFatal

trait FieldValidatorDateTime extends FieldValidator {
  override def validateStructField(field: StructField): Seq[ValidationIssue] = {
    val pattern = DateTimePattern.fromStructField(field)
    val defaultValue = field.defaultValueAsString
    val defaultTimeZone = field.getMetadata("timezone")
    patternConversionIssues(pattern, defaultValue) ++
      defaultTimeZoneIssues(defaultTimeZone) ++
      patternAnalysisIssues(pattern, defaultValue, defaultTimeZone)
  }

  private def generateRow(rowType: String, timestamp: Timestamp)
                         (implicit parser: EnceladusDateTimeParser): (String, String) = {
    val q = "\""
    val timestampAsString = parser.format(timestamp)
    (s"Failed to convert $rowType ($q$timestampAsString$q) using ${parser.pattern.toString}", timestampAsString)
  }

  private def patternConversionIssues(pattern: DateTimePattern, defaultValue: Option[String]): Seq[ValidationIssue] = {

    import FieldValidatorDateTime._
    import spark.implicits._

    val descriptionColName = "description"
    val sourceColName = "source"
    val convertedColName = "converted"
    val q = "\""

    try {
      implicit val parser: EnceladusDateTimeParser = EnceladusDateTimeParser(pattern)
      // Checking pattern's ability to be used in formatting date/time values

      val data = Seq(
        generateRow("current time", exampleDate),
        generateRow("epoch", epochStart),
        generateRow("epoch day end", epochStartDayEnd)
      ) ++ defaultValue.map(
        s =>(s"Default value $q$s$q does not adhere to ${pattern.toString}", s)
      ).toSeq
      val df =  data.toDF(descriptionColName, sourceColName)
      val converted = conversion(col(sourceColName), pattern)

      val dfWithConversion = df.select(
        col(descriptionColName),
        col(sourceColName),
        converted.as(convertedColName),
        converted.isNull.as("failed")
      )

      val issues: Seq[ValidationIssue] = dfWithConversion
        .collect()
        .map(x => (x.get(0), x.get(3)))
        .toList
        .collect {
          case (description, true) => ValidationError(s"$description")
      }
      issues
    }
    catch {
      case NonFatal(e) => Seq(ValidationError(e.getMessage))
    }
  }

  private def defaultTimeZoneIssues(defaultTimeZone: Option[String]): Seq[ValidationIssue] = {
    defaultTimeZone.filterNot(TimeZone.getAvailableIDs().contains(_)).map(tz =>
      ValidationError(""""%s" is not a valid time zone designation""".format(tz))
    ).toSeq
  }

  protected def patternAnalysisIssues(pattern: DateTimePattern,
                                      defaultValue: Option[String],
                                      defaultTimeZone: Option[String]): Seq[ValidationIssue]

  protected def conversion(column: Column, pattern: DateTimePattern): Column
}

object FieldValidatorDateTime {

  private val dayMilliSeconds = 24 * 60 * 60 * 1000
  private val exampleDate = new Timestamp(System.currentTimeMillis)
  private val epochStart = new Timestamp(0)
  private val epochStartDayEnd = new Timestamp(dayMilliSeconds - 1)

  private val spark: SparkSession = SparkSession.builder()
    .master("local[*]")
    .appName("FieldValidatorDateTime")
    .config("spark.sql.codegen.wholeStage", value = false)
    .getOrCreate()
  TimeZoneNormalizer.normalizeSessionTimeZone(spark)

}

