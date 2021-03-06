/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.ml.samples.criteo

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.types.StructType


class CriteoAnalyzer(val inputPath: String,
                     val schema: StructType,
                     val features: CriteoFeatures,
                     val numPartitions: Integer,
                     val indexer: TrainingIndexer,
                     val importer: CriteoImporter,
                     val artifactExporter: ArtifactExporter)
                    (implicit val spark: SparkSession) {

  def analyze() {
    val missingReplacer = new CriteoMissingReplacer()

    val cleanedDf = importer.criteoImport
    val noNonNullDf = cleanedDf.na.fill("null")
    val filledDf = noNonNullDf.na.replace(noNonNullDf.columns, Map("" -> "null"))

    val averages = missingReplacer.getAverageIntegerFeatures(
      filledDf, features.integerFeatureLabels)
    averages.foreach {
      case (col: String, df: DataFrame) =>
        artifactExporter.export(col, df)
    }

    val valueCounts = indexer.getCategoricalFeatureValueCounts(filledDf)
    val vocabularies = indexer.getCategoricalColumnVocabularies(valueCounts)

    vocabularies.foreach {
      case (col: String, df: DataFrame) =>
        artifactExporter.export(features.categoricalLabelMap(col), df)
    }

  }

  def apply(): Unit = analyze
}
