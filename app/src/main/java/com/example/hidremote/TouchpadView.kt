package com.example.hidremote

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class TouchpadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface MouseActionListener {
        fun onMove(dx: Int, dy: Int)
        fun onLeftClick()
        fun onRightClick()
    }

    var listener: MouseActionListener? = null

    private val paint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            listener?.onLeftClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
             listener?.onRightClick()
        }
        
        // Two finger tap not directly supported by SimpleOnGestureListener, doing manual or using double tap
        override fun onDoubleTap(e: MotionEvent): Boolean {
            return true
        }
    })

    private var lastX = 0f
    private var lastY = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        // Could draw a grid or something cool
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                
                if (dx != 0 || dy != 0) {
                    listener?.onMove(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
        }
        return true
    }
}
