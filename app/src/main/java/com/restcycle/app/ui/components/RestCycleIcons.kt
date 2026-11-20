package com.restcycle.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object RestCycleIcons {
    val Alarm: ImageVector by lazy {
        ImageVector.Builder(
            name = "RestCycleAlarm",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Clock circle
                moveTo(12f, 5f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 5f)
                close()
                // Hands
                moveTo(12f, 8.5f)
                lineTo(12f, 13f)
                lineTo(15f, 14.5f)
                // Top bells
                moveTo(4.5f, 4.5f)
                lineTo(7f, 2.5f)
                moveTo(19.5f, 4.5f)
                lineTo(17f, 2.5f)
            }
        }.build()
    }

    val Timer: ImageVector by lazy {
        ImageVector.Builder(
            name = "RestCycleTimer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Top stem
                moveTo(9.5f, 3f)
                lineTo(14.5f, 3f)
                moveTo(12f, 3f)
                lineTo(12f, 5.5f)
                // Stopwatch circle
                moveTo(12f, 5.5f)
                arcTo(7.5f, 7.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 5.5f)
                close()
                // Sweep hand
                moveTo(12f, 13f)
                lineTo(15.5f, 9.5f)
            }
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "RestCycleInfo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 3f)
                close()
                moveTo(12f, 11f)
                lineTo(12f, 16.5f)
                moveTo(12f, 8f)
                lineTo(12f, 8.2f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "RestCycleSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Horizontal slider lines with circular knobs (clean settings sliders icon)
                moveTo(4f, 7f)
                lineTo(8f, 7f)
                moveTo(13f, 7f)
                lineTo(20f, 7f)
                moveTo(10.5f, 4.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10.49f, 4.5f)
                close()

                moveTo(4f, 17f)
                lineTo(11f, 17f)
                moveTo(16f, 17f)
                lineTo(20f, 17f)
                moveTo(13.5f, 14.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13.49f, 14.5f)
                close()
            }
        }.build()
    }
}
