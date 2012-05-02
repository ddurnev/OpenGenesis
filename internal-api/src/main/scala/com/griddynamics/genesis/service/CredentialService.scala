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
package com.griddynamics.genesis.service

import com.griddynamics.genesis.model.{VirtualMachine, Environment}
import com.griddynamics.genesis.repository.CredentialsRepository


class Credentials(val identity : String, val credential : String)

trait CredentialService {
    def getCredentialsForEnvironment(env : Environment) : Option[Credentials]
}

trait VmCredentialService {
  def getCredentialsForVm(env: Environment, vm: VirtualMachine): Option[Credentials]

//  def updateVmCredentials(vm: VirtualMachine, credentials: Credentials)
}

class DefaultCredentialsService(credentialsStore: CredentialsRepository) extends VmCredentialService {

  def getCredentialsForVm(env: Environment, vm: VirtualMachine): Option[Credentials] = {
     for {
      provider <- vm.cloudProvider
      keypair <- vm.keyPair
      creds <- credentialsStore.find(env.projectId, provider, keypair)
      credential <- creds.credential
    } yield new Credentials(creds.identity, credential)
  }
}