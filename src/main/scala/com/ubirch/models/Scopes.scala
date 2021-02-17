package com.ubirch.models

import java.net.URL

sealed trait Resource
object Resource {
  final val list: List[Resource] = List(UPP, Thing)
  def fromString(value: String): Option[Resource] = {
    list.find(_.toString.toLowerCase == value)
  }
}
case object UPP extends Resource
case object Thing extends Resource

sealed trait Action
object Action {
  final val list: List[Action] = List(Anchor, Verify, Create, GetInfo)
  def fromString(value: String): Option[Action] = {
    list.find(_.toString.toLowerCase == value)
  }
}
case object Anchor extends Action
case object Verify extends Action
case object Create extends Action
case object GetInfo extends Action

object Scopes {

  type Scope = (Resource, Action)
  final val UPP_Anchor: Scope = (UPP, Anchor)
  final val UPP_Verify: Scope = (UPP, Verify)
  final val Thing_Create: Scope = (Thing, Create)
  final val Thing_GetInfo: Scope = (Thing, GetInfo)
  final val list = List(UPP_Anchor, UPP_Verify, Thing_Create, Thing_GetInfo)

  def asString(scope: Scope): String = {
    val (resource, action) = scope
    s"${resource.toString.toLowerCase}:${action.toString.toLowerCase}"
  }

  def fromString(value: String): List[Scope] = {
    value
      .split(",")
      .toList
      .map(_.split(":").toList)
      .flatMap {
        case List(r, a) =>
          for {
            resource <- Resource.fromString(r)
            action <- Action.fromString(a)
            scope <- list.find(x => x == (resource, action))
          } yield {
            scope
          }
        case _ => Nil
      }
  }

  def audience(scope: Scopes.Scope, ENV: String): Option[URL] = {
    scope match {
      case Scopes.UPP_Anchor => Option(new URL(s"https://niomon.$ENV.ubirch.com"))
      case Scopes.UPP_Verify => Option(new URL(s"https://verify.$ENV.ubirch.com"))
      case Scopes.Thing_Create => Option(new URL(s"https://api.console.$ENV.ubirch.com"))
      case Scopes.Thing_GetInfo => Option(new URL(s"https://api.console.$ENV.ubirch.com"))
      case _ => None
    }
  }

}
