package com.kristofszilagyi.controllers
import com.kristofszilagyi.shared.{JobDisplayName, RestRoot, UserRoot}
import com.netaporter.uri.Uri
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import JobConfig._
import com.kristofszilagyi.utils.YamlUtils.wrappedYamlString

object JobConfig {

  implicit val nameFormat: YamlFormat[JobDisplayName] = wrappedYamlString(JobDisplayName.apply)(_.s)
  implicit val urlUserFormat: YamlFormat[UserRoot] = wrappedYamlString(s => UserRoot(Uri.parse(s)))(_.u.toString)
  implicit val urlRestFormat: YamlFormat[RestRoot] = wrappedYamlString(s => RestRoot(Uri.parse(s)))(_.u.toString)
}

object JenkinsJobConfig {
  implicit val format: YamlFormat[JenkinsJobConfig] = yamlFormat2(JenkinsJobConfig.apply)
}
final case class JenkinsJobConfig(name: JobDisplayName, url: UserRoot)

object JenkinsConfig {
  implicit val format: YamlFormat[JenkinsConfig] = yamlFormat1(JenkinsConfig.apply)
}
final case class JenkinsConfig(jobs: Seq[JenkinsJobConfig])


object JobNameOnGitLab {
  implicit val format: YamlFormat[JobNameOnGitLab] = wrappedYamlString(JobNameOnGitLab.apply)(_.s)
}
/**
  * This is the name of the job in the pipeline
  */
final case class JobNameOnGitLab(s: String)

object GitLabCiAccessToken {
  implicit val format: YamlFormat[GitLabCiAccessToken] = wrappedYamlString(GitLabCiAccessToken.apply)(_.s)
}
final case class GitLabCiAccessToken(s: String)

object GitJobConfig {
  implicit val format: YamlFormat[GitJobConfig] = yamlFormat5(GitJobConfig.apply)
}
final case class GitJobConfig(name: JobDisplayName, userUrl: UserRoot, restRoot: RestRoot,
                              accessToken: Option[GitLabCiAccessToken], jobNameOnGitLab: JobNameOnGitLab)

object GitConfig {
  implicit val format: YamlFormat[GitConfig] = yamlFormat1(GitConfig.apply)
}
final case class GitConfig(jobs: Seq[GitJobConfig])

object Config {
  implicit val format: YamlFormat[Config] = yamlFormat2(Config.apply)
}
final case class Config(jenkins: JenkinsConfig, gitLab: GitConfig)