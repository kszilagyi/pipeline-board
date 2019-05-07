package com.kristofszilagyi.pipelineboard.controllers

import utest._
import utest.framework.{Test, Tree}

object ConfigTest extends TestSuite {
  def tests: Tree[Test] = this {
    // As well as testing what it does on the surface, I also write this trivial test to check that the custom toString doesn't
    // throw. Sadly that is not a given since class.getSimpleName certainly does sometimes.
    'noSecretsInToString {
      assert(!(JenkinsAccessToken("jenkinssecret").toString contains "secret"))
      assert(!(GitLabCiAccessToken("gitlabsecret").toString contains "secret"))
    }
  }
}
