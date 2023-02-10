/* Communication module for Android terminals.  -*- c-file-style: "GNU" -*-

Copyright (C) 2023 Free Software Foundation, Inc.

This file is part of GNU Emacs.

GNU Emacs is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

GNU Emacs is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Emacs.  If not, see <https://www.gnu.org/licenses/>.  */

package org.gnu.emacs;

import android.view.View;

import android.os.Build;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;

/* This originally extended SurfaceView.  However, doing so proved to
   be too slow, and Android's surface view keeps up to three of its
   own back buffers, which use too much memory (up to 96 MB for a
   single frame.) */

public class EmacsSurfaceView extends View
{
  private static final String TAG = "EmacsSurfaceView";
  private EmacsView view;
  private Bitmap frontBuffer;
  private Canvas bitmapCanvas;
  private Bitmap bitmap;
  private Paint bitmapPaint;

  public
  EmacsSurfaceView (final EmacsView view)
  {
    super (view.getContext ());

    this.view = view;
    this.bitmapPaint = new Paint ();
  }

  private void
  copyToFrontBuffer (Rect damageRect)
  {
    if (damageRect != null)
      bitmapCanvas.drawBitmap (bitmap, damageRect, damageRect,
			       bitmapPaint);
    else
      bitmapCanvas.drawBitmap (bitmap, 0f, 0f, bitmapPaint);
  }

  private void
  reconfigureFrontBuffer (Bitmap bitmap)
  {
    /* First, remove the old front buffer.  */

    if (frontBuffer != null)
      {
	frontBuffer.recycle ();
	frontBuffer = null;
	bitmapCanvas = null;
      }

    this.bitmap = bitmap;

    /* Next, create the new front buffer if necessary.  */

    if (bitmap != null && frontBuffer == null)
      {
	frontBuffer = Bitmap.createBitmap (bitmap.getWidth (),
					   bitmap.getHeight (),
					   Bitmap.Config.ARGB_8888,
					   false);
	bitmapCanvas = new Canvas (frontBuffer);

	/* And copy over the bitmap contents.  */
	copyToFrontBuffer (null);
      }
    else if (bitmap != null)
      /* Just copy over the bitmap contents.  */
      copyToFrontBuffer (null);
  }

  public synchronized void
  setBitmap (Bitmap bitmap, Rect damageRect)
  {
    if (bitmap != this.bitmap)
      reconfigureFrontBuffer (bitmap);
    else if (bitmap != null)
      copyToFrontBuffer (damageRect);

    if (bitmap != null)
      {
	/* In newer versions of Android, the invalid rectangle is
	   supposedly internally calculated by the system.  How that
	   is done is unknown, but calling `invalidateRect' is now
	   deprecated.

	   Fortunately, nobody has deprecated the version of
	   `postInvalidate' that accepts a dirty rectangle.  */

	if (damageRect != null)
	  postInvalidate (damageRect.left, damageRect.top,
			  damageRect.right, damageRect.bottom);
	else
	  postInvalidate ();
      }
  }

  @Override
  public synchronized void
  onDraw (Canvas canvas)
  {
    /* Paint the view's bitmap; the bitmap might be recycled right
       now.  */

    if (frontBuffer != null)
      canvas.drawBitmap (frontBuffer, 0f, 0f, bitmapPaint);
  }
};
