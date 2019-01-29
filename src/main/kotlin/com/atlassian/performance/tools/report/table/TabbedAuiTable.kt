package com.atlassian.performance.tools.report.table

internal class AuiTabbedTableFactory(
    private val namedTables: List<NamedAuiTable>
) {
    fun create() = TabbedAuiTable(
        tabs = namedTables
            .mapIndexed { i, (name, table) -> AuiTableTab(name, i.toString(), table) }
    )
}

internal data class NamedAuiTable(
    val name: String,
    internal val table: AuiTable
)

internal class AuiTableFactory(
    headers: List<String>,
    private val rows: List<Map<String, Any>>
) {
    private val headers by lazy { headers.mapIndexed { i, h -> AuiTableHeader(i.toString(), h) } }

    fun create(): AuiTable = AuiTable(
        headers = headers,
        rows = rows.map { createRow(it) }
    )

    private fun createRow(
        row: Map<String, Any>
    ): AuiTableRow = AuiTableRow(
        cells = headers.map { AuiTableCell(it.id, row[it.name]?.toString() ?: "") }
    )
}

internal class TabbedAuiTable(
    private val tabs: List<AuiTableTab>
) {
    fun toHtml(): String = """
        <div class="aui-tabs horizontal-tabs">
            <ul class="tabs-menu">
                ${tabs.joinToString("\n") { it.toMenuItem() }}
            </ul>
            ${tabs.joinToString("\n") { it.toHtml() }}
        </div>
    """.trimIndent()
}

internal data class AuiTableTab(
    val name: String,
    val id: String,
    private val table: AuiTable
) {
    fun toHtml(): String = """
        <div class="tabs-pane" id="tabs-pane-$id">
            ${table.toHtml()}
        </div>
    """.trimIndent()

    fun toMenuItem(): String = """
        <li class="menu-item">
            <a href="#tabs-pane-$id">$name</a>
        </li>
    """.trimIndent()
}

internal data class AuiTable(
    private val headers: List<AuiTableHeader>,
    private val rows: List<AuiTableRow>
) {
    fun toHtml(): String = """
        <table class="aui">
            <thead>
                <tr>
                    ${headers.joinToString("\n") { it.toHtml() }}
                </tr>
            </thead>
            <tbody>
                ${rows.joinToString("\n") { it.toHtml() }}
            </tbody>
        </table>

    """.trimIndent()
}

internal data class AuiTableRow(
    private val cells: List<AuiTableCell>
) {
    fun toHtml(): String = """
        <tr>
        ${cells.joinToString("\n") { it.toHtml() }}
        </tr>
    """.trimIndent()
}

internal data class AuiTableHeader(
    val id: String,
    val name: String
) {
    fun toHtml(): String = """<th id="$id">$name</th>"""
}

internal data class AuiTableCell(
    private val header: String,
    private val content: String
) {
    fun toHtml(): String = """<td headers="$header">$content</td>"""
}