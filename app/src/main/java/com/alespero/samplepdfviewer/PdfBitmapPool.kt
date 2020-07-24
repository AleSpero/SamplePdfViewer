package com.alespero.samplepdfviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.util.Log
import android.util.SparseArray
import androidx.core.util.getOrElse

class PdfBitmapPool(val pdfRenderer: PdfRenderer, val config: Bitmap.Config, val densityDpi : Int) {

    val bitmaps: SparseArray<Bitmap> = SparseArray()

    init {
        val initialLoadCount =
            if (pdfRenderer.pageCount < POOL_SIZE) pdfRenderer.pageCount else POOL_SIZE

        for (i in 0 until initialLoadCount) {
            bitmaps.append(i, loadPage(i))
        }
    }

    var currentIndex = 0

    companion object {
        const val POOL_SIZE = 5
        val PDF_RESOLUTION_DPI = 72
    }

    fun getPage(index: Int): Bitmap {
        return bitmaps.getOrElse(index) {
            loadPage(index)
        }
    }

    fun loadMore(newLimitIndex: Int) {

        val newRange = getCurrentRange(newLimitIndex)
        removeOutOfRangeElements(newRange)

        for (i in newRange) {
            if (i != newLimitIndex && i in 0 until bitmaps.size() && bitmaps.indexOfKey(i) < 0)
                bitmaps.append(i, loadPage(i))
        }

        currentIndex = newLimitIndex
    }

    fun getCurrentRange(currentIndex: Int, isMovingBackwards: Boolean = true): IntProgression {
        //if the user is moving next it is more likely that he will go next again, so we render the pages
        //based on this param in order to have the fastest result possible
        val sectionSize = (POOL_SIZE - 1) / 2
        return if (isMovingBackwards) (currentIndex - sectionSize)..(currentIndex + sectionSize)
        else (currentIndex + sectionSize) until currentIndex
    }

    fun removeOutOfRangeElements(newRange: IntProgression) {
        //Removing elements that are out of range, the bitmap is cleared and pushed back to the unused bitmaps stack
        getCurrentRange(currentIndex).filter { !newRange.contains(it) }.forEach {
            val removingBitmap = bitmaps[it]
            removingBitmap?.let { bitmap ->
                bitmaps.remove(it)
            }
        }
    }

    fun loadPage(pageIndex: Int): Bitmap {
        Log.d(PdfBitmapPool::class.java.simpleName, "Loading page at index $pageIndex")
        val page = pdfRenderer.openPage(pageIndex)
        val bitmap = newWhiteBitmap(page.width.toPixelDimension(), page.height.toPixelDimension())
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    fun newWhiteBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        return bitmap
    }


    fun Int.toPixelDimension(scaleFactor: Float = 0.4f): Int {
        return ((densityDpi * this / PDF_RESOLUTION_DPI) * scaleFactor).toInt()
    }
}