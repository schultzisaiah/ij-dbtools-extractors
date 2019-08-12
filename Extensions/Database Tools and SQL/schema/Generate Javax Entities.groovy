import com.intellij.database.model.DasColumn
import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.app.persistence.entity;"
typeMapping = [
        (~/(?i)int/)                      : "Long",
        (~/(?i)float|double|decimal|real/): "Double",
//  (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
//  (~/(?i)date/)                     : "java.sql.Date",
//  (~/(?i)time/)                     : "java.sql.Time",
        (~/(?i)datetime|timestamp/)       : "java.time.LocalDateTime",
        (~/(?i)date/)                     : "java.time.LocalDateTime",
        (~/(?i)time/)                     : "java.time.LocalDateTime",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, table, className, fields) }
}

def generate(out, table, className, fields) {
  out.println "package $packageName"
  out.println ""
  out.println "import java.io.Serializable;"
  out.println "import javax.persistence.Column;"
  out.println "import javax.persistence.Entity;"
  out.println "import javax.persistence.Id;"
  out.println "import javax.persistence.Table;"
  out.println "import lombok.Data;"
  out.println ""
  out.println "@Data"
  out.println "@Entity"
  out.println "@Table(schema = \"${table.getDasParent().getName()}\", name = \"${table.getName()}\")"
  out.println "public class $className implements Serializable {"
  out.println ""
  boolean firstComplete = false
  fields.each() {
    if (firstComplete) {
      out.println ""
    } else {
      firstComplete = true
    }
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  private ${it.type} ${it.name};"
  }
  out.println "}"
  out.println ""
  out.println "/* TODO: This assumes that, if you need it, you already have this converter implemented: "
  out.println "package $packageName\n" +
          "\n" +
          "import java.time.LocalDateTime;\n" +
          "import java.time.ZoneId;\n" +
          "import java.util.Date;\n" +
          "import javax.persistence.AttributeConverter;\n" +
          "import javax.persistence.Converter;\n" +
          "\n" +
          "@Converter(autoApply = true)\n" +
          "public class DateToLocalDateTimeConverter implements AttributeConverter<LocalDateTime, Date> {\n" +
          "\n" +
          "  @Override\n" +
          "  public Date convertToDatabaseColumn(LocalDateTime attribute) {\n" +
          "    return attribute == null ? null\n" +
          "        : Date.from(attribute.atZone(ZoneId.systemDefault()).toInstant());\n" +
          "  }\n" +
          "\n" +
          "  @Override\n" +
          "  public LocalDateTime convertToEntityAttribute(Date dbData) {\n" +
          "    return dbData == null ? null\n" +
          "        : LocalDateTime.ofInstant(dbData.toInstant(), ZoneId.systemDefault());\n" +
          "  }\n" +
          "}\n" +
          "*/"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    String annos = getAnnotationString(table, col, spec)
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                       name : javaName(col.getName(), false),
                       type : typeStr,
                       annos: annos]]
  }
}

def getAnnotationString(table, col, spec) {
  String annotationString = ""
  def attrs = table.getColumnAttrs(col)
  if (attrs != null && attrs.contains(DasColumn.Attribute.PRIMARY_KEY)) {
    annotationString += "@Id\n  "
  }
  annotationString += "@Column(name = ${col.getName()}"
  Integer size = -1
  String exception = ""
  if (spec != null && spec.contains("varchar2") && spec.contains("char")) {
    try {
      size = spec.replace("varchar2(", "").replace(" char)", "").toInteger()
      if (size > 0) {
        annotationString += ", length = ${size}"
      }
    } catch (Exception e) {
      exception = " //${e.getMessage()}"
    }
  }
  return annotationString += ")${exception}"
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
          .collect { Case.LOWER.apply(it).capitalize() }
          .join("")
          .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
