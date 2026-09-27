package dev.eftrainer.ui.section

enum class TableForm { GRID, CARDS }

const val MIN_COLUMN_DP = 120f
const val COLUMN_GAP_DP = 14f

fun tableForm(
    columns: Int,
    availableDp: Float,
    fontScale: Float = 1f,
): TableForm {
    val minColumn = MIN_COLUMN_DP * fontScale.coerceAtLeast(1f)
    val needed = columns * minColumn + (columns - 1) * COLUMN_GAP_DP
    return if (columns == 2 && needed <= availableDp) TableForm.GRID else TableForm.CARDS
}

fun firstColumnWidth(
    intrinsic: Int,
    otherIntrinsic: Int,
    space: Int,
    minColumn: Int,
): Int {
    if (intrinsic + otherIntrinsic <= space) return intrinsic
    val share = (space.toLong() * intrinsic / (intrinsic + otherIntrinsic).coerceAtLeast(1)).toInt()
    val upper = (space - minColumn).coerceAtLeast(0)
    return share.coerceIn(minColumn.coerceAtMost(upper), upper).coerceAtMost(intrinsic)
}
