package com.kristofszilagyi.controllers
import com.kristofszilagyi.shared._
import com.netaporter.uri.Uri
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import JobConfig._
import com.kristofszilagyi.utils.YamlUtils.wrappedYamlString
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.decoding.NoopDecoder
import com.netaporter.uri.encoding.NoopEncoder

object JobConfig {

  implicit val nameFormat: YamlFormat[JobDisplayName] = wrappedYamlString(JobDisplayName.apply)(_.s)
  implicit private val parseConfig: UriConfig = UriConfig(NoopEncoder, NoopDecoder)

  implicit val urlUserFormat: YamlFormat[UserRoot] = wrappedYamlString(s => UserRoot(RawUrl(Uri.parse(s))))(_.u.rawString)
  implicit val urlRestFormat: YamlFormat[RestRoot] = wrappedYamlString(s => RestRoot(RawUrl(Uri.parse(s))))(_.u.rawString)
  implicit val groupNameFormat: YamlFormat[GroupName] = wrappedYamlString(GroupName.apply)(_.s)
}

object JenkinsAccessToken {
  implicit val format: YamlFormat[JenkinsAccessToken] = wrappedYamlString(JenkinsAccessToken.apply)(_.s)
}
final case class JenkinsAccessToken(s: String)

object JenkinsUser {
  implicit val format: YamlFormat[JenkinsUser] = wrappedYamlString(JenkinsUser.apply)(_.s)
}
final case class JenkinsUser(s: String)

object JenkinsJobConfig {
  implicit val format: YamlFormat[JenkinsJobConfig] = yamlFormat4(JenkinsJobConfig.apply)
}
final case class JenkinsJobConfig(name: JobDisplayName, url: UserRoot, user: Option[JenkinsUser], accessToken: Option[JenkinsAccessToken])

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

object GitLabCiJobConfig {
  implicit val format: YamlFormat[GitLabCiJobConfig] = yamlFormat4(GitLabCiJobConfig.apply)
}
final case class GitLabCiJobConfig(name: JobDisplayName, url: UserRoot,
                                   accessToken: Option[GitLabCiAccessToken], jobNameOnGitLab: JobNameOnGitLab)

object GitLabCiConfig {
  implicit val format: YamlFormat[GitLabCiConfig] = yamlFormat1(GitLabCiConfig.apply)
}
final case class GitLabCiConfig(jobs: Seq[GitLabCiJobConfig])


object ConfigGroup {
  implicit val format: YamlFormat[ConfigGroup] = yamlFormat3(ConfigGroup.apply)
}
final case class ConfigGroup(groupName: GroupName, jenkins: JenkinsConfig, gitLabCi: GitLabCiConfig)

object Config {
  implicit val format: YamlFormat[Config] = yamlFormat1(Config.apply)
}
final case class Config(groups: Seq[ConfigGroup])
