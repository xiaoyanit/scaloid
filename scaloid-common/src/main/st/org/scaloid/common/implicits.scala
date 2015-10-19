$license()$

package org.scaloid.common

import _root_.android.graphics.PorterDuff.Mode
import android.content._
import android.database.Cursor
import android.graphics.Movie
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view._
import org.scaloid.common.{ViewOnClickListener, RichIntent}
import language.implicitConversions

private[scaloid] class UnitConversion(val ext: Double)(implicit context: Context) {
  @inline private def m = context.getResources.getDisplayMetrics
  @inline def dip   : Int = (ext * m.density).toInt
  @inline def sp    : Int = (ext * m.scaledDensity).toInt
  @inline def px2dip: Double = ext / m.density
  @inline def px2sp : Double = ext / m.scaledDensity
}

private[scaloid] class ResourceConversion(val id: Int)(implicit context: Context) {
  @inline def r2Text         : CharSequence        = context.getText(id)
  @inline def r2TextArray    : Array[CharSequence] = context.getResources.getTextArray(id)
  @inline def r2String       : String              = context.getResources.getString(id)
  @inline def r2StringArray  : Array[String]       = context.getResources.getStringArray(id)
  @inline def r2Drawable     : Drawable            = context.getResources.getDrawable(id)
  @inline def r2Movie        : Movie               = context.getResources.getMovie(id)
  @inline def r2Boolean      : Boolean             = context.getResources.getBoolean(id)
  @inline def r2Color        : Int                 = context.getResources.getColor(id)
  @inline def r2Dimension    : Float               = context.getResources.getDimension(id)
  @inline def r2Integer      : Int                 = context.getResources.getInteger(id)
  @inline def r2IntArray     : Array[Int]          = context.getResources.getIntArray(id)
  @inline def r2RawResource  : java.io.InputStream = context.getResources.openRawResource(id)

  @inline def r2Drawable(color: Int, mode: Mode = Mode.MULTIPLY): Drawable = {
    val drawable = context.getResources.getDrawable(id).mutate
    drawable.setColorFilter(color, mode)
    drawable
  }
}

private[scaloid] class StringConversion(val str: String)(implicit context: Context) {
  @inline def toUri          : Uri          = Uri.parse(str)
  @inline def toIntentFilter : IntentFilter = new IntentFilter(str)
}

trait ConversionImplicits {
  @inline implicit def Double2unitConversion(ext: Double)(implicit context: Context): UnitConversion = new UnitConversion(ext)(context)
  @inline implicit def Long2unitConversion  (ext: Long)  (implicit context: Context): UnitConversion = new UnitConversion(ext)(context)
  @inline implicit def Int2unitConversion   (ext: Int)   (implicit context: Context): UnitConversion = new UnitConversion(ext)(context)

  @inline implicit def Int2resource(ext: Int)(implicit context: Context): ResourceConversion = new ResourceConversion(ext)(context)

  // r2String is not provided because it is ambiguous with r2Text
  @inline implicit def r2Text       (id: Int)(implicit context: Context): CharSequence        = context.getText(id)
  @inline implicit def r2TextArray  (id: Int)(implicit context: Context): Array[CharSequence] = context.getResources.getTextArray(id)
  @inline implicit def r2StringArray(id: Int)(implicit context: Context): Array[String]       = context.getResources.getStringArray(id)
  @inline implicit def r2Drawable   (id: Int)(implicit context: Context): Drawable            = context.getResources.getDrawable(id)
  @inline implicit def r2Movie      (id: Int)(implicit context: Context): Movie               = context.getResources.getMovie(id)

  @inline implicit def string2Uri             (str: String): Uri              = Uri.parse(str)
  @inline implicit def string2IntentFilter    (str: String): IntentFilter     = new IntentFilter(str)
  @inline implicit def string2StringConversion(str: String)(implicit context: Context): StringConversion = new StringConversion(str)
}
object ConversionImplicits extends ConversionImplicits

trait InterfaceImplicits {
  implicit def func2ScaloidViewOnClickListener[F](f: (View) => F): ViewOnClickListener =
    new ViewOnClickListener() {
      def func = { v => f(v): Unit }
      def onClickListener = func2ViewOnClickListener(f)
    }

  implicit def lazy2ScaloidViewOnClickListener[F](f: => F): ViewOnClickListener =
    new ViewOnClickListener() {
      def func = { v => f: Unit }
      def onClickListener = lazy2ViewOnClickListener(f)
    }

  implicit def func2ViewOnClickListener[F](f: (View) => F): View.OnClickListener =
    new View.OnClickListener() {
      def onClick(view: View) {
        f(view)
      }
    }

  @deprecated("Can cause confusion when this conversion is used for block of code", "3.6")
  implicit def lazy2ViewOnClickListener[F](f: => F): View.OnClickListener =
    new View.OnClickListener() {
      def onClick(view: View) {
        f
      }
    }

  implicit def func2DialogOnClickListener[F](f: (DialogInterface, Int) => F): DialogInterface.OnClickListener =
    new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        f(dialog, which)
      }
    }

  implicit def lazy2DialogOnClickListener[F](f: => F): DialogInterface.OnClickListener =
    new DialogInterface.OnClickListener {
      def onClick(dialog: DialogInterface, which: Int) {
        f
      }
    }

  implicit def func2runnable[F](f: () => F): Runnable =
    new Runnable() {
      def run() {
        f()
      }
    }

  @deprecated("Can cause confusion when this conversion is used for block of code", "3.6")
  implicit def lazy2runnable[F](f: => F): Runnable =
    new Runnable() {
      def run() {
        f
      }
    }

  implicit def intent2RichIntent(i: Intent) = new RichIntent(i)
}
object InterfaceImplicits extends InterfaceImplicits

class RichCursor(c: Cursor) extends Iterable[Cursor] {
  def iterator = new CursorIterator

  class CursorIterator extends Iterator[Cursor] {
    def hasNext = c.getPosition < c.getCount - 1

    def next() = {
      c.moveToNext()
      c
    }
  }

  def closeAfter[T](body: RichCursor => T) = try body(this) finally c.close()

  def orm[T](body: Cursor => T) = closeAfter(_.map(body).toList)

  def toLong(default: Long): Long = closeAfter(csr => if (c.moveToFirst()) c.getLong(0) else default)

  def toString(default: String): String = closeAfter(csr => if (c.moveToFirst()) c.getString(0) else default)

  def toShort(default: Short): Short = closeAfter(csr => if (c.moveToFirst()) c.getShort(0) else default)

  def toInt(default: Int): Int = closeAfter(csr => if (c.moveToFirst()) c.getInt(0) else default)

  def toDouble(default: Double): Double = closeAfter(csr => if (c.moveToFirst()) c.getDouble(0) else default)

  def toFloat(default: Float): Float = closeAfter(csr => if (c.moveToFirst()) c.getFloat(0) else default)
}

trait DatabaseImplicits {
  implicit def cursor2RichCursor(c: Cursor) = new RichCursor(c)
}

trait Implicits extends ConversionImplicits with InterfaceImplicits with ViewImplicits with WidgetImplicits with DatabaseImplicits
object Implicits extends Implicits
