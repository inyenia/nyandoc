package com.todesking.dox

sealed abstract class Markup
object Markup {
  case class Text(content:String) extends Markup
  case class Paragraph(children:Seq[Markup]) extends Markup
  case class Dl(items:Seq[DlItem]) extends Markup
  case class DlItem(dt:Seq[Markup], dd:Seq[Markup])
  case class UnorderedList(items:Seq[ListItem]) extends Markup
  case class ListItem(contents:Seq[Markup])
  case class LinkInternal(title:String, id:Id) extends Markup
  case class LinkExternal(title:String, url:String) extends Markup
  case class Code(content:String) extends Markup
  case class CodeInline(content:String) extends Markup
  case class Bold(contents:Seq[Markup]) extends Markup
  case class Italic(contents:Seq[Markup]) extends Markup

  def normalize(markups:Seq[Markup]):Seq[Markup] =
    Normalize(markups)

  object Normalize {
    def apply(markups:Seq[Markup]):Seq[Markup] =
      doRecursive(dropEmpty _ andThen removeEmptyLink andThen removeInternalLink andThen textFusion)(markups)

    def doRecursive(f:Seq[Markup]=>Seq[Markup])(markups:Seq[Markup]):Seq[Markup] = {
      def doR(ms:Seq[Markup]) = doRecursive(f)(ms)
      f(
        markups map {
          case Paragraph(cs) => Paragraph(doRecursive(f)(cs))
          case Dl(items) => Dl(items map {item => DlItem(doR(item.dt), doR(item.dd))})
          case UnorderedList(items) => UnorderedList(items map {item => ListItem(doR(item.contents))})
          case Bold(contents) => Bold(doR(contents))
          case Italic(contents) => Italic(doR(contents))
          case other => other
        }
      )
    }

    def dropEmpty(markups:Seq[Markup]) = {
      val empty = Seq()
      markups.flatMap {
        case
          Text("")
          | Paragraph(Seq())
          | Dl(Seq())
          | UnorderedList(Seq())
          | Code("")
          | CodeInline("")
          | Bold(Seq())
          | Italic(Seq())
          =>
            Seq()
        case other => Seq(other)
      }
    }

    def removeEmptyLink(markups:Seq[Markup]) = markups.map {
      case LinkExternal(title, "") =>
        Text(title)
      case x =>
        x
    }

    def removeInternalLink(markups:Seq[Markup]) = markups.map {
      case LinkExternal(title, url) if(url.startsWith(".")) =>
        Text(title)
      case x =>
        x
    }

    def textFusion(markups:Seq[Markup]) =
      if(markups.size < 2) markups
      else {
        val tail = normalize(markups.tail)
        (markups.head, tail.head) match {
          case (Markup.Text(c1), Markup.Text(c2)) =>
            Markup.Text(c1 + " " + c2) +: tail.tail
          case _ => markups.head +: tail
        }
      }
  }
}
