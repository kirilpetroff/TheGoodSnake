package com.example.sammysnake

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/** FrameLayout, което винаги измерва себе си като квадрат (по-малката от ширина/височина). */
class SquareFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = minOf(width, height)
        val squareSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        super.onMeasure(squareSpec, squareSpec)
    }
}
