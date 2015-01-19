/*
 * Copyright 2014 Treode, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.treode.twitter.server.handler

import java.nio.file.Path

import com.treode.disk.DriveAttachment
import com.treode.store.{Cohort, Store}, Store.Controller
import com.treode.twitter.finagle.http.{RichRequest, mapper}
import com.treode.twitter.util._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.util.{Await,Future}
import org.apache.commons.io.IOUtils
import org.jboss.netty.buffer.ChannelBuffers
//import org.jboss.netty.handler.codec.http._



class AtlasHandler (controller: Controller) extends Service [Request, Response] {

  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Get =>
        Future.value (respond.json (req, controller.cohorts))

      case Method.Put =>
        controller.cohorts = req.readJson [Array [Cohort]]
        Future.value (respond (req, Status.Ok))

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}

class DrivesHandler (controller: Controller) extends Service [Request, Response] {

  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Get =>
        controller.drives
          .map (drives => respond.json (req, drives))
          .toTwitterFuture

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}

class DrivesAttachHandler (controller: Controller) extends Service [Request, Response] {

  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Post =>
        val drives = req.readJson [Seq [DriveAttachment]]
        controller.attach (drives: _*)
          .map (_ => respond (req, Status.Ok))
          .toTwitterFuture

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}

class DrivesDrainHandler (controller: Controller) extends Service [Request, Response] {

  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Post =>
        val paths = req.readJson [Seq [Path]]
        controller.drain (paths: _*)
          .map (_ => respond (req, Status.Ok))
          .toTwitterFuture

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}

class TablesHandler (controller: Controller) extends Service [Request, Response] {

  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Get =>
        controller.tables
          .map (tables => respond.json (req, tables))
          .toTwitterFuture

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}

class AdminUIHandler extends Service [Request, Response] {
  
  def apply (req: Request): Future [Response] = {
    req.method match {

      case Method.Get => {
        println (req)
        val res = respond (req, Status.Ok)
        val resourceURL = getClass.getResource("/META-INF/resources/ui/index.html")
        if ( resourceURL != null ) {
          val conn = resourceURL.openConnection
          val stream = conn.getInputStream
          val bytes = () => { IOUtils.toByteArray(stream) }
          if (stream != null) {
            res.status = Status.Ok
            res.contentType = "text/html"
            res.setContent(ChannelBuffers.copiedBuffer(bytes()))
            Future.value (res)
          } else {
            println ("stream null")
          }
        } else {
          println ("getResource null")
        }
        Future.value (respond (req, Status.Ok))
      }

      case _ =>
        Future.value (respond (req, Status.MethodNotAllowed))
    }}}



  /*
  val proxyOrigin: Service [ HttpRequest, HttpResponse ] =
      Http.newService("localhost:7070")
    val request = new DefaultHttpRequest ( HttpVersion.HTTP_1_1, HttpMethod.GET, "/table/0x100")
    val response: Future[HttpResponse] = proxyOrigin(request)
    response onSuccess { resp: HttpResponse =>
        println ("GET success: " + resp)
    }
    Await.ready(response)
    * /
    */
//} 