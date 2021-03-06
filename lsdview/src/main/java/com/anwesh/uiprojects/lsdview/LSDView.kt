package com.anwesh.uiprojects.lsdview

/**
 * Created by anweshmishra on 18/07/18.
 */

import android.app.Activity
import android.graphics.Paint
import android.graphics.Canvas
import android.view.View
import android.view.MotionEvent
import android.content.Context
import android.graphics.Color

val nodes : Int = 5

fun Canvas.drawAtMide(cb : () -> Unit) {
    save()
    translate(width.toFloat()/2, height.toFloat()/2)
    cb()
    restore()
}

fun Canvas.drawStepNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = Color.parseColor("#16a085")
    val gap : Float = (Math.min(w, h) * 0.9f) / nodes
    val sc1 : Float = Math.min(0.5f, scale) * 2
    val sc2 : Float = Math.min(0.5f, Math.max(0f, scale - 0.5f)) * 2
    save()
    translate(0.05f * w + i * gap, 0.95f * h - i * gap)
    val x : Float = gap * sc2
    val y : Float = -gap * sc1
    drawLine(x, y, x, -gap, paint)
    drawLine(x, -gap, gap, -gap, paint)
    restore()
}

class LSDView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var prevScale : Float = 0f, var dir : Float = 0f) {

        fun update(stopcb : (Float) -> Unit) {
            scale += 0.1f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                stopcb(prevScale)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            if (dir == 0f) {
                dir = 1 - 2 * prevScale
                startcb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class LSDNode(var i : Int, val state : State = State()) {

        private var next : LSDNode? = null

        private var prev : LSDNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = LSDNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawAtMide {
                canvas.save()
                canvas.scale(-1f, -1f)
                canvas.save()
                canvas.translate(-canvas.width.toFloat()/2, -canvas.height.toFloat()/2)
                canvas.drawStepNode(i, state.scale, paint)
                canvas.restore()
                canvas.restore()
            }
            next?.draw(canvas, paint)
        }

        fun update(stopcb : (Int, Float) -> Unit) {
            state.update {
                stopcb(i, it)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            state.startUpdating(startcb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : LSDNode {
            var curr : LSDNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class LinkedDecreasingStep(var i : Int) {

        private var curr : LSDNode = LSDNode(0)

        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(stopcb : (Int, Float) -> Unit) {
            curr.update{j, scale ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                stopcb(j, scale)
            }
        }

        fun startUpdating(startcb : () -> Unit) {
            curr.startUpdating(startcb)
        }
    }

    data class Renderer(var view : LSDView) {

        val lsd : LinkedDecreasingStep = LinkedDecreasingStep(0)

        val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            lsd.draw(canvas, paint)
            animator.animate {
                lsd.update {j, scale ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            lsd.startUpdating {
                animator.start()
            }
        }
    }

    companion object {
        fun create(activity : Activity)  : LSDView {
            val view : LSDView = LSDView(activity)
            activity.setContentView(view)
            return view
        }
    }
}