package com.anb2rw.keyboardt9

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet




class T9KeyboardView(context: Context?, attrs: AttributeSet?) : KeyboardView(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {

        canvas.drawColor(resources.getColor(R.color.background));

        val npd = context.resources.getDrawable(R.drawable.key_selector, null) as StateListDrawable

        val keys = keyboard.keys
        for (key in keys) {
            npd.setBounds(key.x, key.y, key.x + key.width, key.y + key.height)
            val drawableState = key.currentDrawableState
            npd.setState(drawableState)
            npd.draw(canvas)

            paint.textSize = 32f

            var x = (key.x + key.width / 2).toFloat()

            if(key.edgeFlags and Keyboard.EDGE_LEFT == Keyboard.EDGE_LEFT) {
                x += 20
            }
            if(key.edgeFlags and Keyboard.EDGE_RIGHT == Keyboard.EDGE_RIGHT) {
                x -= 20
            }

            if (key.label != null) {
                canvas.drawText(key.label.toString(),
                    x,
                    (key.y + key.height / 2 - 5).toFloat(), paint
                )
                var s : String;
                var sb = StringBuilder()
                for (code in key.codes) {
                    if(code > 0 && (code < 48 || code > 57) && code != 32) {
                        sb.append(code.toChar())
                    }
                }
                s = sb.toString()

                if(s.length > 0) {
                    if(isShifted) {
                        s = s.toUpperCase();
                    }
                    paint.textSize = 20f
                    canvas.drawText(s,
                        x,
                        (key.y + key.height - 10).toFloat(), paint
                    )
                }
            } else {
                if(key.icon is BitmapDrawable) {
                    val bitmap : Bitmap;
                    if(key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                        if(isShifted) {
                            bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shift_filled);
                        } else {
                            bitmap = (key.icon as BitmapDrawable).getBitmap();
                        }
                    } else {
                        bitmap = (key.icon as BitmapDrawable).getBitmap();
                    }
                    var x = (key.x + key.width / 2 - bitmap.width / 2).toFloat()
                    var y = (key.y + key.height / 2 - bitmap.height / 2).toFloat()

                    if(key.edgeFlags and Keyboard.EDGE_LEFT == Keyboard.EDGE_LEFT) {
                        x += 25
                    }
                    if(key.edgeFlags and Keyboard.EDGE_RIGHT == Keyboard.EDGE_RIGHT) {
                        x -= 35
                        y -= 10
                    }

                    canvas.drawBitmap(bitmap, x, y, null)
                    //key.icon.setBounds(key.x, key.y, key.x + key.width, key.y + key.height)
                    //key.icon.draw(canvas)

                    if(key.codes[0] == Keyboard.KEYCODE_SHIFT) {
                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_language);
                        val x = (key.x + key.width / 2 - bitmap.width / 2 - 15).toFloat()
                        val y = (key.y + key.height / 2 - bitmap.height / 2).toFloat()
                        canvas.drawBitmap(bitmap, x, y, null)
                    }
                }
            }
        }
    }

    override fun onLongPress(popupKey: Keyboard.Key?): Boolean {
        if(popupKey?.codes?.get(0) ?: -1 == Keyboard.KEYCODE_SHIFT) {
            onKeyboardActionListener.onKey(Keyboard.KEYCODE_MODE_CHANGE, null)
            return true
        }
        if(popupKey?.codes?.get(0) ?: -1 < 0) {
            return super.onLongPress(popupKey)
        }
        if(popupKey != null) {
            onKeyboardActionListener.onText(popupKey.label)
        }
        return true
    }
}