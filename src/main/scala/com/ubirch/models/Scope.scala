package com.ubirch.models

import java.net.URL

sealed trait Resource
object Resource {
  final val list: List[Resource] = List(UPP, Thing, User)
  def fromString(value: String): Option[Resource] = {
    list.find(_.toString.toLowerCase == value)
  }
}
case object UPP extends Resource
case object Thing extends Resource
case object User extends Resource

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
case object Bootstrap extends Action

case class Scope(resource: Resource, action: Action)

object Scope {

  final val UPP_Anchor: Scope = Scope(UPP, Anchor)
  final val UPP_Verify: Scope = Scope(UPP, Verify)
  final val Thing_Create: Scope = Scope(Thing, Create)
  final val Thing_GetInfo: Scope = Scope(Thing, GetInfo)
  final val Thing_Bootstrap: Scope = Scope(Thing, Bootstrap)
  final val User_GetInfo: Scope = Scope(User, GetInfo)
  final val list = List(UPP_Anchor, UPP_Verify, Thing_Create, Thing_GetInfo, Thing_Bootstrap, User_GetInfo)

  def asString(scope: Scope): String = {
    s"${scope.resource.toString.toLowerCase}:${scope.action.toString.toLowerCase}"
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
            maybeScope = Scope(resource, action)
            scope <- list.find(_ == maybeScope)
          } yield {
            scope
          }
        case _ => Nil
      }
  }

  def audience(scope: Scope, ENV: String): Option[URL] = {
    scope match {
      case UPP_Anchor => Option(new URL(s"https://niomon.$ENV.ubirch.com"))
      case UPP_Verify => Option(new URL(s"https://verify.$ENV.ubirch.com"))
      case Thing_Create => Option(new URL(s"https://api.console.$ENV.ubirch.com"))
      case Thing_GetInfo => Option(new URL(s"https://api.console.$ENV.ubirch.com"))
      case Thing_Bootstrap => Option(new URL(s"https://token.$ENV.ubirch.com"))
      case User_GetInfo => Option(new URL(s"https://api.console.$ENV.ubirch.com"))
      case _ => None
    }
  }

}
