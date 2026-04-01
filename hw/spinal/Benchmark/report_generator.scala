package Benchmark

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import scala.jdk.CollectionConverters._

object ReportGenerator {
  val ReportRoot: String = "hw/report"

  case class ReportArtifacts(
      csvPath: String,
      jsonPath: String,
      markdownPath: String,
      summaryMarkdownPath: String
  )

  def currentTimestampTag(): String =
    LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)

  def matrixTag(m: Int, n: Int, k: Int): String =
    if (m == n && n == k) s"${m}x${n}" else s"${m}x${n}x${k}"

  def buildPrefix(design: String, inputMode: String, matrix: String, timestamp: String): String =
    s"$ReportRoot/${sanitize(design)}_${sanitize(inputMode)}_${sanitize(matrix)}_${sanitize(timestamp)}"

  def writeReport(
      prefix: String,
      csvHeaders: Seq[String],
      csvValues: Seq[String],
      jsonBody: String,
      markdownBody: String,
      summaryInputMode: String,
      summaryMatrix: String,
      summaryTimestamp: String
  ): ReportArtifacts = {
    val csvPath = s"$prefix.csv"
    val jsonPath = s"$prefix.json"
    val markdownPath = s"$prefix.md"
    ensureParent(csvPath)
    ensureParent(jsonPath)
    ensureParent(markdownPath)
    val csvContent = csvHeaders.map(csvEscape).mkString(",") + "\n" + csvValues.map(csvEscape).mkString(",") + "\n"
    Files.write(Paths.get(csvPath), csvContent.getBytes(StandardCharsets.UTF_8))
    Files.write(Paths.get(jsonPath), jsonBody.getBytes(StandardCharsets.UTF_8))
    Files.write(Paths.get(markdownPath), markdownBody.getBytes(StandardCharsets.UTF_8))
    val summaryMarkdownPath = updateComparisonSummary(summaryInputMode, summaryMatrix, summaryTimestamp)
    ReportArtifacts(csvPath = csvPath, jsonPath = jsonPath, markdownPath = markdownPath, summaryMarkdownPath = summaryMarkdownPath)
  }

  def jsonObject(fields: Seq[(String, String)]): String =
    fields.map { case (key, value) => s"${jsonString(key)}:$value" }.mkString("{", ",", "}")

  def jsonArray(values: Seq[String]): String =
    values.mkString("[", ",", "]")

  def jsonString(value: String): String =
    "\"" + escapeJson(value) + "\""

  def jsonNumber(value: Long): String = value.toString

  def jsonNumber(value: Int): String = value.toString

  def jsonNumber(value: BigInt): String = value.toString

  def jsonNumber(value: Double): String = {
    if (value.isNaN || value.isInfinity) {
      "null"
    } else {
      value.toString
    }
  }

  def jsonBool(value: Boolean): String =
    if (value) "true" else "false"

  def jsonOptional(value: Option[Long]): String =
    value.map(_.toString).getOrElse("null")

  def jsonMatrixInt(matrix: Array[Array[Int]]): String =
    jsonArray(matrix.map(row => jsonArray(row.map(jsonNumber).toSeq)).toSeq)

  def jsonMatrixBigInt(matrix: Array[Array[BigInt]]): String =
    jsonArray(matrix.map(row => jsonArray(row.map(jsonNumber).toSeq)).toSeq)

  def jsonMatrixDouble(matrix: Array[Array[Double]]): String =
    jsonArray(matrix.map(row => jsonArray(row.map(jsonNumber).toSeq)).toSeq)

  private def ensureParent(path: String): Unit = {
    val parent = new File(path).getParentFile
    if (parent != null) {
      parent.mkdirs()
    }
  }

  private def sanitize(value: String): String =
    value.replaceAll("[^A-Za-z0-9_\\-x]", "_")

  private def csvEscape(value: String): String = {
    val escaped = value.replace("\"", "\"\"")
    s"\"$escaped\""
  }

  private def escapeJson(value: String): String = {
    value.flatMap {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c if c < ' ' => f"\\u${c.toInt}%04x"
      case c => c.toString
    }
  }

  private def updateComparisonSummary(inputMode: String, matrix: String, timestamp: String): String = {
    val summaryPath = s"$ReportRoot/summary_${sanitize(inputMode)}_${sanitize(matrix)}_${sanitize(timestamp)}.md"
    ensureParent(summaryPath)

    val designs = Seq("fixed", "float16", "float8")
    val rows = designs.map { design =>
      findLatestCsv(design, inputMode, matrix).map(csvPath => design -> parseCsvRow(csvPath)).getOrElse(design -> Map.empty[String, String])
    }
    val tableRows = rows
      .map { case (design, data) =>
        val label = design
        if (data.isEmpty) {
          s"| $label | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a | n/a |"
        } else {
          s"| $label | ${field(data, "total_external_bytes", "n/a")} | ${field(data, "total_onchip_bytes", "n/a")} | ${field(data, "mae", "n/a")} | ${field(data, "mse", "n/a")} | ${field(data, "max_abs_error", "n/a")} | ${field(data, "relative_error", "n/a")} | ${field(data, "cosine_similarity", "n/a")} | ${field(data, "shift_count", "n/a")} | ${field(data, "truncate_count", "n/a")} | ${field(data, "saturation_count", "n/a")} | ${field(data, "overflow_count", "n/a")} | ${field(data, "rounding_count", "n/a")} |"
        }
      }
      .mkString("\n")

    val markdown = Seq(
      "# Experiment Summary",
      "",
      s"- Input mode: `$inputMode`",
      s"- Matrix: `$matrix`",
      s"- Timestamp: `$timestamp`",
      "",
      "| Design | External Bytes | On-chip Bytes | MAE | MSE | Max Abs Error | Relative Error | Cosine Similarity | Shift | Truncate | Saturation | Overflow | Rounding |",
      "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
      tableRows
    ).mkString("\n") + "\n"

    Files.write(Paths.get(summaryPath), markdown.getBytes(StandardCharsets.UTF_8))
    summaryPath
  }

  private def findLatestCsv(design: String, inputMode: String, matrix: String): Option[String] = {
    val root = Paths.get(ReportRoot)
    if (!Files.exists(root)) {
      None
    } else {
      Files
        .list(root)
        .iterator()
        .asScala
        .filter(path => Files.isRegularFile(path))
        .map(_.toFile.getName)
        .filter(name =>
          name.startsWith(s"${sanitize(design)}_${sanitize(inputMode)}_${sanitize(matrix)}_") && name.endsWith(".csv")
        )
        .toSeq
        .sorted
        .lastOption
        .map(name => s"$ReportRoot/$name")
    }
  }

  private def parseCsvRow(path: String): Map[String, String] = {
    val lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8).asScala.toSeq
    if (lines.length < 2) {
      Map.empty
    } else {
      val headers = parseCsvLine(lines.head)
      val values = parseCsvLine(lines(1))
      headers.zip(values).toMap
    }
  }

  private def parseCsvLine(line: String): Seq[String] = {
    val result = collection.mutable.ArrayBuffer.empty[String]
    val current = new StringBuilder
    var inQuotes = false
    var idx = 0
    while (idx < line.length) {
      val ch = line.charAt(idx)
      if (ch == '"') {
        if (inQuotes && idx + 1 < line.length && line.charAt(idx + 1) == '"') {
          current.append('"')
          idx += 1
        } else {
          inQuotes = !inQuotes
        }
      } else if (ch == ',' && !inQuotes) {
        result += current.toString()
        current.clear()
      } else {
        current.append(ch)
      }
      idx += 1
    }
    result += current.toString()
    result.toSeq
  }

  private def field(data: Map[String, String], name: String, default: String): String =
    data.getOrElse(name, default)
}
