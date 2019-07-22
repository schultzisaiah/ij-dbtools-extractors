SEP = ", "
QUOTE     = "\'"
NEWLINE   = System.getProperty("line.separator")

KEYWORDS_LOWERCASE = com.intellij.database.util.DbSqlUtil.areKeywordsLowerCase(PROJECT)
KW_MERGE_INTO = KEYWORDS_LOWERCASE ? "merge into " : "MERGE INTO "
KW_USING = KEYWORDS_LOWERCASE ? " dest using (select " : " DEST USING ( SELECT "
KW_DUAL = KEYWORDS_LOWERCASE ? " from dual) src" : " FROM DUAL) SRC"
KW_ON = KEYWORDS_LOWERCASE ? " on (" : " ON ("
KW_WHEN_MATCHED = KEYWORDS_LOWERCASE ? ") when matched then update set " : ") WHEN MATCHED THEN UPDATE SET "
KW_WHEN_NOT_MATCHED = KEYWORDS_LOWERCASE ? " when not matched then insert (" : " WHEN NOT MATCHED THEN INSERT "
KW_VALUES = KEYWORDS_LOWERCASE ? ") values (" : ") VALUES ("
KW_NULL = KEYWORDS_LOWERCASE ? "null" : "NULL"
KW_DEST = KEYWORDS_LOWERCASE ? "dest" : "DEST"
KW_SRC = KEYWORDS_LOWERCASE ? "src" : "SRC"
KW_TO_DATE_START = KEYWORDS_LOWERCASE ? "to_date('" : "TO_DATE('"
KW_TO_DATE_END = "', 'YYYY-MM-DD HH24:MI:SS')"
DATE_REGEX = "[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9]"

def record(columns, dataRow) {
    OUT.append(KW_MERGE_INTO)
    if (TABLE == null) OUT.append("MY_TABLE")
    else OUT.append(TABLE.getParent().getName()).append(".").append(TABLE.getName())
    OUT.append(KW_USING)

    String keyName = ""

    columns.eachWithIndex { column, idx ->
        if (idx == 0) keyName = column.name()
        def value = dataRow.value(column)
        def skipQuote = value.toString().isNumber() || value == null
        def stringValue = value != null ? FORMATTER.format(dataRow, column) : KW_NULL
        def isDate = stringValue.matches(DATE_REGEX)
        if (isDate) {
            skipQuote = true
            stringValue = KW_TO_DATE_START + stringValue + KW_TO_DATE_END
        }
        if (DIALECT.getDbms().isMysql()) stringValue = stringValue.replace("\\", "\\\\")
        OUT.append(skipQuote ? "": QUOTE).append(isDate ? stringValue : stringValue.replace(QUOTE, QUOTE + QUOTE))
                .append(skipQuote ? "": QUOTE).append(" as ").append(column.name())
                .append(idx != columns.size() - 1 ? SEP : "")
    }

    OUT.append(KW_DUAL).append(KW_ON).append(KW_DEST).append(".").append(keyName).append(" = ")
            .append(KW_SRC).append(".").append(keyName).append(KW_WHEN_MATCHED)

    columns.eachWithIndex { column, idx ->
        if (idx > 0) {
            OUT.append(column.name()).append(" = ").append(KW_SRC).append(".").append(column.name())
                    .append(idx != columns.size() - 1 ? SEP : "")
        }
    }

    OUT.append(KW_WHEN_NOT_MATCHED)

    columns.eachWithIndex { column, idx ->
        if (idx > 0) {
            OUT.append(column.name()).append(idx != columns.size() - 1 ? SEP : "")
        }
    }

    OUT.append(KW_VALUES)

    columns.eachWithIndex { column, idx ->
        if (idx > 0) {
            OUT.append(KW_SRC).append(".").append(column.name())
                    .append(idx != columns.size() - 1 ? SEP : "")
        }
    }
    OUT.append(");").append(NEWLINE)
}

ROWS.each { row -> record(COLUMNS, row) }

