package hung.deptrai.audiorecorder.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveFormView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var paint = Paint()
    private var amps = ArrayList<Float>()
    private var spike = ArrayList<RectF>()
    private var radius = 6f
    private var w = 9f
    private var d = 6f

    private var sw = 0f
    private var sh = 400f
    private var maxspike = 0
    init{
        paint.color = Color.rgb(244,81,30)
        sw = resources.displayMetrics.widthPixels.toFloat()
        maxspike = (sw/(w+d)).toInt()
    }
    fun addAmp(amp: Float){
        var norm = Math.min(amp.toInt()/7,400).toFloat()
        amps.add(norm)


        spike.clear()
        var ampss = amps.takeLast(maxspike)
        for (i in ampss.indices){
            var left = sw - i*(w+d)
            var top = sh/2 - ampss[i]/2
            var right = left + w
            var bottom = top + ampss[i]
            spike.add(RectF(left,top,right,bottom))
        }
        invalidate()
    }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        spike.forEach {
            canvas?.drawRoundRect(it,radius,radius,paint)
        }
    }
}