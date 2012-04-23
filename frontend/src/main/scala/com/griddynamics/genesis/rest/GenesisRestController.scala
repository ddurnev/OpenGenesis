/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.rest

import org.springframework.stereotype.Controller
import com.griddynamics.genesis.rest.GenesisRestController._
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation._
import net.liftweb.json.JsonParser
import java.io.InputStreamReader
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.api.{EnvironmentDetails, GenesisService}
import org.springframework.http.HttpStatus
import java.security.Principal
import org.springframework.beans.factory.annotation.Value
import com.griddynamics.genesis.http.TunnelFilter

@Controller
@RequestMapping(Array("/rest"))
class GenesisRestController(genesisService: GenesisService) extends RestApiExceptionsHandler {


    @Value("${genesis.system.server.mode:frontend}")
    var mode = ""


    @RequestMapping(value = Array("create"), method = Array(RequestMethod.POST))
    @ResponseBody
    def createEnv(request: HttpServletRequest, response : HttpServletResponse) = {
        val paramsMap = extractParamsMap(request)
        val envName = extractValue("envName", paramsMap)
        val templateName = extractValue("templateName", paramsMap)
        val templateVersion = extractValue("templateVersion", paramsMap)
        val variables = extractVariables(paramsMap)
        val projectId = extractValue("projectId", paramsMap)

        val user = mode match {
          case "backend" => request.getHeader(TunnelFilter.SEC_HEADER_NAME)
          case _ => getCurrentUser
        }
        genesisService.createEnv(projectId.toInt, envName, user, templateName, templateVersion, variables)
    }

    @RequestMapping(value=Array("envs/{envName}/logs/{stepId}"))
    def stepLogs(@PathVariable("envName") envName: String, @PathVariable stepId: Int, response: HttpServletResponse, request: HttpServletRequest) {
      val logs: Seq[String] = genesisService.getLogs(envName, stepId)
      val text = if (logs.isEmpty)
        "No logs yet"
      else
        logs.reduceLeft(_ + "\n" + _)
      response.setContentType("text/plain")
      response.getWriter.write(text)
      response.getWriter.flush()
    }

    @RequestMapping(value = Array("envs/{envName}"), method = Array(RequestMethod.DELETE))
    @ResponseBody
    def deleteEnv(@PathVariable("envName") envName: String,
                  request: HttpServletRequest) =
        genesisService.destroyEnv(envName, Map[String, String]())

    @RequestMapping(value = Array("templates"), method = Array(RequestMethod.GET))
    @ResponseBody
    def listTemplates(@RequestParam(required = false) project: String, @RequestParam(required = false) tag: String) =
      paramToOption(project) match {
        case _ => genesisService.listTemplates
    }

    //TODO: divide method
    @RequestMapping(value = Array("envs/{envName}"), method = Array(RequestMethod.GET))
    @ResponseBody
    def describeEnv(@PathVariable("envName") envName: String, response : HttpServletResponse) : EnvironmentDetails =
        genesisService.describeEnv(envName).getOrElse(throw new ResourceNotFoundException)

    @RequestMapping(value = Array("envs"), method = Array(RequestMethod.GET))
    @ResponseBody
    def listEnvs(@RequestParam("projectId") projectId: Int, request: HttpServletRequest) = genesisService.listEnvs(projectId)

    @RequestMapping(value = Array("cancel/{envName}"), method = Array(RequestMethod.POST))
    @ResponseBody @ResponseStatus(HttpStatus.NO_CONTENT)
    def cancelWorkflow(@PathVariable("envName") envName: String) { genesisService.cancelWorkflow(envName) }

    @RequestMapping(value = Array("exec/{envName}/{workflowName}"), method = Array(RequestMethod.POST))
    @ResponseBody
    def requestWorkflow(@PathVariable("envName") env: String,
                        @PathVariable("workflowName") workflow: String,
                        request: HttpServletRequest) =
        genesisService.requestWorkflow(env, workflow, extractVariables(extractParamsMap(request)))

}

object GenesisRestController {

    def extractParamsMap(request: HttpServletRequest): Map[String, Any] = {
        try {
            JsonParser.parse(new InputStreamReader(request.getInputStream), false).values.asInstanceOf[Map[String, Any]]
        } catch {
            case _ => throw new InvalidInputException
        }
    }

    def extractParamsMapList(request: HttpServletRequest): List[Map[String, Any]] = {
        try {
            JsonParser.parse(new InputStreamReader(request.getInputStream), false).values.asInstanceOf[List[Map[String, Any]]]
        } catch {
            case _ => throw new InvalidInputException
        }
    }

    def extractVariables(paramsMap: Map[String, Any]): Map[String, String] = {
        if (!paramsMap.contains("variables")) return Map[String, String]()
        paramsMap.apply("variables").asInstanceOf[Map[String, String]]
    }

    def getCurrentUser =  {
      SecurityContextHolder.getContext.getAuthentication.asInstanceOf[Principal].getName
    }

    def extractValue(valueName : String, values : Map[String, Any]) : String = {
        values.get(valueName) match {
            case Some(s) => String.valueOf(s)
            case None => throw new MissingParameterException(valueName)
        }
    }
  
    def extractNotEmptyValue(valueName: String, values: Map[String,  Any]): String = {
        val value = extractValue(valueName, values)
        if (value.isEmpty) {
          throw new MissingParameterException(valueName)
        }
        value
    }

    def extractOption(valueName: String,  values: Map[String, Any]) : Option[String] = {
       values.get(valueName) match {
         case Some(s) => Some(String.valueOf(s))
         case None => None
       }
    }
  
    private def paramToOption(param: String) = {
      if (param == null || param.isEmpty)
        None
      else
        Option(param)
    }

}

