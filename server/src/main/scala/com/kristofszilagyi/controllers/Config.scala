package com.kristofszilagyi.controllers
import com.kristofszilagyi.shared.{JobName, RestRoot, UserRoot}
import com.netaporter.uri.Uri
import net.jcazevedo.moultingyaml._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol._
import JobConfig._

object JobConfig {
  def wrappedYamlString[T](fromString: String => T)(convertToString: T => String): YamlFormat[T] = new YamlFormat[T] {
    def read(yaml: YamlValue): T = yaml match {
      case YamlString(s) => fromString(s)
      case other => deserializationError(s"Should be a string got: $other")
    }

    def write(t: T): YamlValue = YamlString(convertToString(t))
  }

  implicit val nameFormat: YamlFormat[JobName] = wrappedYamlString(JobName.apply)(_.s)
  implicit val urlUserFormat: YamlFormat[UserRoot] = wrappedYamlString(s => UserRoot(Uri.parse(s)))(_.u.toString)
  implicit val urlRestFormat: YamlFormat[RestRoot] = wrappedYamlString(s => RestRoot(Uri.parse(s)))(_.u.toString)
}

object JenkinsJobConfig {
  implicit val format: YamlFormat[JenkinsJobConfig] = yamlFormat2(JenkinsJobConfig.apply)
}
final case class JenkinsJobConfig(name: JobName, url: UserRoot)

object JenkinsConfig {
  implicit val format: YamlFormat[JenkinsConfig] = yamlFormat1(JenkinsConfig.apply)
}
final case class JenkinsConfig(jobs: Seq[JenkinsJobConfig])

object GitJobConfig {
  implicit val format: YamlFormat[GitJobConfig] = yamlFormat3(GitJobConfig.apply)
}
final case class GitJobConfig(name: JobName, uiUrl: UserRoot, restRoot: RestRoot)


object GitConfig {
  implicit val format: YamlFormat[GitConfig] = yamlFormat1(GitConfig.apply)
}
final case class GitConfig(jobs: Seq[GitJobConfig])

object Config {
  implicit val format: YamlFormat[Config] = yamlFormat2(Config.apply)
}
final case class Config(jenkins: JenkinsConfig, git: GitConfig)