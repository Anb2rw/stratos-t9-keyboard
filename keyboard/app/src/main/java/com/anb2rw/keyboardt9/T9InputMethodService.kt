package com.anb2rw.keyboardt9

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.TextView




class T9InputMethodService : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    private lateinit var customInputMethodView: ViewGroup

    private lateinit var keyboard : T9Keyboard
    private lateinit var keyboardEng : T9Keyboard
    private var keyboardView : T9KeyboardView? = null
    private val mComposing = StringBuilder()

    private var textPreview : TextView? = null
    private var textPreviewEnd : TextView? = null
    private var mCapsLock: Boolean = true

    override fun onCreate() {
        super.onCreate()
        keyboard = T9Keyboard(this, R.xml.number_pad);
        keyboardEng = T9Keyboard(this, R.xml.number_pad_en);
    }

    override fun onCreateInputView(): View? {
        customInputMethodView = layoutInflater.inflate(R.layout.keyboard_view, null) as ViewGroup
        textPreview = customInputMethodView.findViewById(R.id.text)
        textPreviewEnd = customInputMethodView.findViewById(R.id.textEnd)
        keyboardView = customInputMethodView.findViewById(R.id.keyboard_view);
        keyboardView?.let {
            it.setKeyboard(keyboard);
            it.setPreviewEnabled(false)
            it.setOnKeyboardActionListener(this);
        }

        updateShiftKeyState(currentInputEditorInfo)

        var previewLayout : View = customInputMethodView.findViewById(R.id.textPreviewLayout)
        Log.d("LOL", "previewLayout: " + previewLayout);
        previewLayout?.setOnTouchListener { v: View, m: MotionEvent ->
            handleClose()
            true
        }

        return customInputMethodView
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 50))
        } else {
            vibrator.vibrate(20)
        }
    }

    override fun onPress(p0: Int) {
    }

    override fun onRelease(p0: Int) {
    }

    override fun swipeLeft() {
        currentInputConnection?.let {
            val et = it.getExtractedText(ExtractedTextRequest(), 0)
            val selectionStart = et.selectionStart
            it.setSelection(selectionStart - 1, selectionStart - 1);

            updateComposing()
        }
    }

    override fun swipeRight() {
        currentInputConnection?.let {

            if(it.getTextAfterCursor(1000, 0).length <= 0) {
                handleCharacter(32);//Space
            } else {
                //+1 selection
                val et = it.getExtractedText(ExtractedTextRequest(), 0)
                val selectionStart = et.selectionStart
                it.setSelection(selectionStart + 1, selectionStart + 1);
            }

            updateComposing()
        }
    }

    override fun swipeUp() {
        toggleCapsLock()
    }

    fun toggleCapsLock() {
        mCapsLock = !mCapsLock;
        updateShiftKeyState(currentInputEditorInfo)
    }

    override fun swipeDown() {
        handleClose()
    }

    private fun handleClose() {
//        commitTyped(currentInputConnection)
        requestHideSelf(0)
        keyboardView!!.closing()
    }

    fun updateComposing() {
        mComposing.setLength(0)
        var end : CharSequence = "";
        currentInputConnection?.let {
            mComposing.append(it.getTextBeforeCursor(1000, 0))
            end = it.getTextAfterCursor(1000, 0);
        }

        textPreview?.setText(mComposing)

        textPreviewEnd?.let {
            it.setText(end);
            if(end.length > 0) {
                it.visibility = View.VISIBLE;
            } else {
                it.visibility = View.GONE;
            }
        }

        previousLength = mComposing.length;
    }

    override fun onStartInputView(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInputView(attribute, restarting)

        updateComposing()
    }

    override fun onFinishInput() {
        super.onFinishInput()

        // Clear current composing text and candidates.
        mComposing.setLength(0)
        previousLength = 0

        keyboardView?.let {
            it.closing()
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd,
            candidatesStart, candidatesEnd
        )

        // If the current selection in the text view changes, we should
        // clear whatever textPreview text we have.
        if (mComposing.length > 0 && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            updateComposing()
//            updateCandidates()
            val ic = currentInputConnection
            ic?.finishComposingText()
        }
    }

    var previousLength = 0;

    private fun handleCharacter(primaryCode: Int) {
        if(mComposing.length != previousLength) {
            mCapsLock = false;
            updateShiftKeyState(currentInputEditorInfo)
        }

        var primaryCode = primaryCode
        if (isInputViewShown) {
            if (keyboardView?.isShifted() == true) {
                primaryCode = Character.toUpperCase(primaryCode)
            }
        }

        val code = primaryCode.toChar()
            mComposing.append(code)
            currentInputConnection.commitText(code.toString(), 1)

        previousLength = mComposing.length;
    }

    private fun handleBackspace() {
        val length = mComposing.length
        if (length > 0) {
            mComposing.delete(length - 1, length)
            textPreview?.setText(mComposing)
            previousLength = mComposing.length;

            val selectedText: CharSequence? = currentInputConnection.getSelectedText(0)
            if (selectedText == null) {
                currentInputConnection.deleteSurroundingText(1, 0)
            } else {
                if (selectedText.isEmpty()) {
                    currentInputConnection.deleteSurroundingText(1, 0)
                } else {
                    currentInputConnection.commitText("", 1)
                }
            }

        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL)
        }
        updateShiftKeyState(currentInputEditorInfo)
    }

    private fun keyDownUp(keyEventCode: Int) {
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode)
        )
        currentInputConnection.sendKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, keyEventCode)
        )
    }

    private fun updateShiftKeyState(attr: EditorInfo?) {
        if (attr != null && keyboardView != null) {
            var caps = 0
            val ei = currentInputEditorInfo
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = currentInputConnection.getCursorCapsMode(attr.inputType)
            }
            keyboardView?.setShifted(mCapsLock || caps != 0)
        }
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        //vibrate()
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                handleBackspace()
            }
            Keyboard.KEYCODE_SHIFT -> {
                toggleCapsLock()
                //handleClose()
            }
            Keyboard.KEYCODE_MODE_CHANGE -> {
                toggleLanguage()
            }
            else -> {
                handleCharacter(primaryCode);
            }
        }
    }

    override fun onText(text: CharSequence) {
        var s = text.toString()

        if (isInputViewShown) {
            if (keyboardView?.isShifted() == true) {
                s = s.toUpperCase()
            }
        }

        mComposing.append(s)
        currentInputConnection.commitText(s, 1)

        previousLength = mComposing.length;
    }

    fun toggleLanguage() {
        keyboardView?.let {
            if(it.keyboard == keyboard) {
                it.setKeyboard(keyboardEng)
            } else {
                it.setKeyboard(keyboard)
            }
            updateShiftKeyState(currentInputEditorInfo)
        }
    }
}